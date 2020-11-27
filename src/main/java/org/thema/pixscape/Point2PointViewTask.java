/*
 * Copyright (C) 2016 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import org.locationtech.jts.geom.Point;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.common.ProgressBar;
import org.thema.data.feature.Feature;
import org.thema.data.IOFeature;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.MultiViewshedTask.RasterValue;
import org.thema.pixscape.view.SimpleComputeView;

/**
 *
 * @author gvuidel
 */
public class Point2PointViewTask extends AbstractParallelTask<Map, Map> implements Serializable {
    
    public enum Agreg { EYE, OBJECT }
    
    /** project file for loading project for MPI mode only */
    private File prjFile;

    private File pointEyeFile;
    private String idEyeField;
    
    /** The height of the observer */
    private double zEye;
    
    private File pointObjFile;
    private String idObjField;
    
    /** The height of the observed points, -1 if not used */
    private double zDest;
    
    private RasterValue outValue;
    
    private Agreg agreg; 
    
    private transient Project project;
    private transient List<? extends Feature> eyePoints;
    private transient List<? extends Feature> objPoints;
    
    private Map result;
    
    /** 
     * The default 3D limits of the sight. This limits are overriden by shapefile attributes if present
     */
    private Bounds bounds;

    public Point2PointViewTask(File pointEyeFile, String idEyeField, double zEye, File pointObjFile, String idObjField, double zDest, 
            RasterValue outValue, Agreg agreg, Bounds bounds, Project project, ProgressBar monitor) {
        super(monitor);
        this.project = project;
        this.prjFile = project.getProjectFile();
        this.pointEyeFile = pointEyeFile;
        this.idEyeField = idEyeField;
        this.zEye = zEye;
        this.pointObjFile = pointObjFile;
        this.idObjField = idObjField;
        this.zDest = zDest;
        this.outValue = outValue;
        this.agreg = agreg;
        this.bounds = bounds;
    }
    
    @Override
    public void init() {
        try {
            eyePoints = IOFeature.loadFeatures(pointEyeFile, idEyeField);
            objPoints = IOFeature.loadFeatures(pointObjFile, idObjField);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        super.init();
        // useful for MPI only, because project is not serializable
        if(project == null) {
            try {
                project = Project.load(prjFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            result = new HashMap();
        }
    }

    @Override
    public Map execute(int start, int end) {
        Map map = new HashMap();
        SimpleComputeView compute = project.getSimpleComputeView();
        for(Feature point : eyePoints.subList(start, end)) {
            Point p = (Point) point.getGeometry();
            Bounds b = bounds.updateBounds(point);
            double zOrig = this.zEye;
            if(point.getAttributeNames().contains("height")) {
                zOrig = ((Number)point.getAttribute("height")).doubleValue();
            }
            GridCoordinates2D orig = compute.getData().getWorld2Grid(new DirectPosition2D(p.getX(), p.getY()));
            Map<Object, Double> objs = agreg == null ? new HashMap<>() : map;
            double sum = 0; // for agreg == eye
            for(Feature obj : objPoints) {
                double zDest = this.zDest;
                if(obj.getAttributeNames().contains("height")) {
                    zDest = ((Number)obj.getAttribute("height")).doubleValue();
                }
                p = (Point) obj.getGeometry();
                GridCoordinates2D dest = compute.getData().getWorld2Grid(new DirectPosition2D(p.getX(), p.getY()));
                double val = compute.calcRay(orig, zOrig, dest, zDest, b, outValue == RasterValue.AREA);
                if(outValue == RasterValue.COUNT) {
                    val = val > 0 ? 1.0 : 0.0;
                }
                
                if(agreg == null) {
                    if(val > 0) {
                        objs.put(obj.getId(), val);
                    }
                } else if(agreg == Agreg.EYE) {
                    sum += val;
                } else {
                    Double prec = objs.putIfAbsent(obj.getId(), val);
                    if(prec != null) {
                        objs.put(obj.getId(), prec+val);
                    }
                }
            }
            if(agreg == null) {
                map.put(point.getId(), objs);
            } else if(agreg == Agreg.EYE) {
                map.put(point.getId(), sum);
            } else {
                // does nothing cause map = objs
            }
            
            incProgress(1);
        }
        
        return map;
    }

    @Override
    public void gather(Map m) {
        if(agreg == Agreg.OBJECT) {
            for(Object objId : m.keySet()) {
                Double val = (Double)m.get(objId);
                Double prec = (Double) result.putIfAbsent(objId, val);
                if(prec != null) {
                    result.put(objId, prec+val);
                }
            }
        } else {
            result.putAll(m);
        }
    }
    
    @Override
    public int getSplitRange() {
        return eyePoints.size();
    }

    @Override
    public Map getResult() {
        return result;
    }
}
