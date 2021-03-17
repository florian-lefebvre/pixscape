/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.thema.pixscape;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import org.geotools.feature.SchemaException;
import org.thema.common.collection.HashMapList;
import org.thema.data.IOImage;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.IOFeature;
import org.thema.parallel.AbstractParallelTask;

/**
 * Parallel task for creating multiple projects from land modifications and executing CLI commands for each.
 * This task works in theaded and MPI mode.
 * This task does not return result. The result is stored directly.
 * 
 * @author Gilles Vuidel
 */
public class LandModTask extends AbstractParallelTask<Void, Void> implements Serializable {

    /** project file for loading project for MPI mode */
    private File prjFile;
    private File fileZone;
    private String idField;
    private String codeField, heightField;
    private List<String> zoneIds;
    private List<String> args;
    private File fileDsm;
    
    private transient Project project;
    private transient HashMapList<String, DefaultFeature> zones;
    private transient Raster finalDsm;

    /**
     * Creates a new LandmodTask
     * @param project the initial project (must be saved for MPI mode)
     * @param fileZone the shapefile containing polygons of land modifications
     * @param idField the shapefile field containing identifier
     * @param codeField the shapefile field containing the new land code
     * @param fileDsm the raster file containing the new DSM
     * @param selIds a list of zone ids or null for calculating for all zones
     * @param args the CLI commands to execute after creating each project
     */
    public LandModTask(Project project, File fileZone, String idField, String codeField, File fileDsm, List<String> selIds, List<String> args) {
        this.project = project;
        this.prjFile = project.getProjectFile();
        this.fileZone = fileZone;
        this.idField = idField;
        this.codeField = codeField;
        this.heightField = null;
        this.zoneIds = selIds;
        this.fileDsm = fileDsm;
        this.args = args;
    }
    /**
     * Creates a new LandmodTask
     * @param project the initial project (must be saved for MPI mode)
     * @param fileZone the shapefile containing polygons of land modifications
     * @param idField the shapefile field containing identifier
     * @param codeField the shapefile field containing the new land code
     * @param heightField the shapefile field containing the new height for DSM
     * @param selIds a list of zone ids or null for calculating for all zones
     * @param args the CLI commands to execute after creating each project
     */
    public LandModTask(Project project, File fileZone, String idField, String codeField, String heightField, List<String> selIds, List<String> args) {
        this.project = project;
        this.prjFile = project.getProjectFile();
        this.fileZone = fileZone;
        this.idField = idField;
        this.codeField = codeField;
        this.heightField = heightField;
        this.zoneIds = selIds;
        this.fileDsm = null;
        this.args = args;
    }

    @Override
    public void init() {
        try {
            // useful for MPI only, because project is not serializable
            if(project == null) {
                project = Project.load(prjFile);
            }
        
            List<DefaultFeature> features = IOFeature.loadFeatures(fileZone);
            if(!features.get(0).getAttributeNames().contains(idField)) {
                throw new IllegalArgumentException("Unknow field : " + idField);
            }
            if(!features.get(0).getAttributeNames().contains(codeField)) {
                throw new IllegalArgumentException("Unknow field : " + codeField);
            }
            zones = new HashMapList<>();
            for(DefaultFeature f : features) {
                zones.putValue(f.getAttribute(idField).toString(), f);
            }
            
            if(zoneIds == null) {
                zoneIds = new ArrayList(zones.keySet());
                // sort to ensure the same order for several JVM in MPI mode
                Collections.sort(zoneIds);
            } else {
                // check if selected ids exists
                if(!zones.keySet().containsAll(zoneIds)) {
                    zoneIds.removeAll(zones.keySet());
                    throw new IllegalArgumentException("Unknown ids : " + Arrays.deepToString(zoneIds.toArray()));
                }
            }
            if(fileDsm != null) {
                finalDsm = IOImage.loadCoverage(fileDsm).getRenderedImage().getData();
            }
            
            super.init();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public Void execute(int start, int end) {
        for(String id : zoneIds.subList(start, end)) {
            try {
                File newPrjFile = createProject(id, zones.get(id)).getProjectFile();
                
                // execute next commands
                ArrayList<String> newArgs = new ArrayList<>(args);
                newArgs.add(0, "--project");
                newArgs.add(1, newPrjFile.getAbsolutePath());
                new CLITools().execute(newArgs.toArray(new String[0]));
            } catch (IOException | SchemaException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    @Override
    public int getSplitRange() {
        return zoneIds.size();
    }

    @Override
    public void gather(Void results) {
    }

    /**
     * @return nothing
     * @throws UnsupportedOperationException
     */
    @Override
    public Void getResult() {
        throw new UnsupportedOperationException(); 
    }

    /**
     * Creates a new Project based on the initial project while changing the landmap on areas covering the features zones
     * @param id the identifier for the new project
     * @param zones the zones to change in the land map and dsm map
     * @return the new created project
     * @throws IOException
     * @throws SchemaException 
     */
    private Project createProject(String id, List<DefaultFeature> zones) throws IOException, SchemaException {
        Raster src = project.getDefaultScaleData().getLand();
        WritableRaster land = src.createCompatibleWritableRaster();
        land.setRect(src);
        src = project.getDefaultScaleData().getDsm();
        WritableRaster dsm = src.createCompatibleWritableRaster();
        dsm.setRect(src);
        
        TreeSet<Integer> codes = new TreeSet<>(project.getCodes());
        AffineTransformation trans = project.getDefaultScaleData().getWorld2Grid();
        // update land map
        for(DefaultFeature zone : zones) {
            int code = ((Number)zone.getAttribute(codeField)).intValue();
            int height = heightField != null ? ((Number)zone.getAttribute(heightField)).intValue() : 0;
            Geometry trGeom = trans.transform(zone.getGeometry());
            for(int i = 0; i < trGeom.getNumGeometries(); i++) {
                Geometry transGeom = trGeom.getGeometryN(i);
                Envelope env = transGeom.getEnvelopeInternal();
                int miny = Math.max((int)env.getMinY(), land.getMinY());
                int minx = Math.max((int)env.getMinX(), land.getMinX());
                int maxy = Math.min((int)Math.ceil(env.getMaxY()), land.getMinY() + land.getHeight());
                int maxx = Math.min((int)Math.ceil(env.getMaxX()), land.getMinX() + land.getWidth());
                Coordinate c = new Coordinate();
                GeometryFactory geomFact = new GeometryFactory();
                for(c.y = miny+0.5; c.y < maxy; c.y++) {
                    for(c.x = minx+0.5; c.x < maxx; c.x++) {
                        if(transGeom.intersects(geomFact.createPoint(c))) {
                            final int x = (int)c.x;
                            final int y = (int)c.y;
                            land.setSample(x, y, 0, code);
                            dsm.setSample(x, y, 0, heightField != null ? height : finalDsm.getSampleDouble(x, y, 0));
                        }
                    }
                }
            }
            codes.add(code);
        }
        
        // create project
        File dir = new File(project.getDirectory(), id);   
        return project.dupProject(project.getName() + "-" + id, dir, new ScaleData(project.getDtmCov(), land, dsm));
    }
}
