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

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.remote.SerializableState;
import javax.media.jai.remote.SerializerFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.ProgressBar;
import org.thema.data.IOImage;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.ComputeView;

/**
 * Parallel task for calculating metrics on a grid sampling.
 * The task works in threaded and MPI mode.
 * 
 * @author Gilles Vuidel
 */
public class GridMetricTask extends AbstractParallelTask<Map<String, WritableRaster>, Map<String, SerializableState>> implements Serializable {
    
    /** project file for loading project for MPI mode */
    private File prjFile;
    
    private final boolean isTan;
    
    private final double startZ;
    
    // options for viewshed
    private double destZ;
    private boolean inverse;
    
    private final Bounds bounds;
    
    private final List<? extends Metric> metrics;
    
    // sampling
    private final int sample;
    private final SortedSet<Integer> from;
    
    private final File resDir;
    
    private transient Project project;
    private transient GridGeometry2D grid;
    private transient ComputeView compute;
    private transient RenderedImage dtm, land;
    private transient Map<String, WritableRaster> result;
    private transient Map<String, ImageWriter> writers;

    /**
     * Creates a new GridMetricTask for viewshed metric.
     * If resDir == null, the results are kept in memory and can be retrieved by {@link #getResult()}, 
     * else the results are not kept in memory and directly saved in tiff files progressively.
     * @param project the project (must be saved for MPI mode)
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param inverse if false the starting point is the observer, else the starting point is the observed point
     * @param bounds the 3D limits of the sight 
     * @param fromCode calculates only from this land use codes, may be null for calculating from all landuse codes
     * @param metrics the metrics to calculate
     * @param sample if equals to 1 then calculates the sight from all pixels, if equals 2, calculates for one pixel on 2 in x and y direction, etc...
     * @param resDir the directory for storing result files, may be null for not saving the results
     * @param monitor the progress monitor, may be null
     */
    public GridMetricTask(Project project, double startZ, double destZ, boolean inverse, Bounds bounds, Set<Integer> fromCode, List<ViewShedMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.project = project;
        this.prjFile = project.getProjectFile();
        this.startZ = startZ;
        this.destZ = destZ;
        this.inverse = inverse;
        this.bounds = bounds;
        this.from = fromCode != null && !fromCode.isEmpty() ? new TreeSet<>(fromCode) : null;
        this.metrics = metrics;
        this.sample = sample;
        this.resDir = resDir;
        this.isTan = false;
    }
    
    /**
     * Creates a new GridMetricTask for tangential metric.
     * If resDir == null, the results are kept in memory and can be retrieved by {@link #getResult()}, 
     * else the results are not kept in memory and directly saved in tiff files progressively.
     * @param project the project (must be saved for MPI mode)
     * @param startZ the height of the eye of the observer
     * @param bounds the 3D limits of the sight 
     * @param fromCode calculates only from this land use codes, may be null for calculating from all landuse codes
     * @param metrics the metrics to calculate
     * @param sample if equals to 1 then calculates the sight from all pixels, if equals 2, calculates for one pixel on 2 in x and y direction, etc...
     * @param resDir the directory for storing result files, may be null for not saving the results
     * @param monitor the progress monitor, may be null
     */
    public GridMetricTask(Project project, double startZ, Bounds bounds, Set<Integer> fromCode, List<ViewTanMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.project = project;
        this.prjFile = project.getProjectFile();
        this.startZ = startZ;
        this.bounds = bounds;
        this.from = fromCode != null && !fromCode.isEmpty() ? new TreeSet<>(fromCode) : null;
        this.metrics = metrics;
        this.sample = sample;
        this.resDir = resDir;
        this.isTan = true;
    }

    @Override
    public void init() {
        // useful for MPI only, because project is not serializable
        if(project == null) {
            try {
                project = Project.load(prjFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        // needed for getSplitRange
        dtm = project.getDtm();
        super.init(); 
        grid = project.getDtmCov().getGridGeometry();
        compute = project.getDefaultComputeView();
        land = project.getLandUse();
    }
    
    private boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<String, SerializableState> execute(int y0, int y1) {
        Map<String, WritableRaster> map = new HashMap<>();
        for(Metric metric : metrics) {
            for(String resName : metric.getResultNames()) {
                WritableRaster r = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, dtm.getWidth()/sample, y1-y0, 1), new Point(0, y0));
                Arrays.fill(((DataBufferFloat)r.getDataBuffer()).getData(), Float.NaN);
                map.put(resName, r);
            }
        }
        final int w = dtm.getWidth()/sample;
        RandomIter rDtm = RandomIterFactory.create(dtm, null);
        RandomIter rLand = from != null ? RandomIterFactory.create(land, null) : null;
        for(int y = y0; y < y1; y++) {
            if(isCanceled()) {
                break;
            }
            for(int x = 0; x < w; x++) {
                GridCoordinates2D c = new GridCoordinates2D(x*sample+sample/2, y*sample+sample/2);
                
                if(from != null && !from.contains(rLand.getSample(c.x, c.y, 0)) || Float.isNaN(rDtm.getSampleFloat(c.x, c.y, 0))) {
                    continue;
                }
                long time = System.currentTimeMillis();
                DirectPosition2D p = null;
                try {
                    p = (DirectPosition2D) grid.gridToWorld(c);
                } catch (TransformException ex) {
                    Logger.getLogger(GridMetricTask.class.getName()).log(Level.SEVERE, null, ex);
                }
                List<Double[]> values;
                if(isTan) {
                    values = compute.aggrViewTan(p, startZ, bounds, (List) metrics);
                } else {
                    values = compute.aggrViewShed(p, startZ, destZ, inverse, bounds, (List) metrics);
                }
                for(int i = 0; i < metrics.size(); i++) {
                    int j = 0;
                    for(String resName : metrics.get(i).getResultNames()) {
                        map.get(resName).setSample(x, y, 0, values.get(i)[j++]);
                    }
                }
                long dt = System.currentTimeMillis()-time;
                Logger.getLogger(GridMetricTask.class.getName()).fine(dt + " ms");
            }                   
            incProgress(1);
        }
        Map<String, SerializableState> serialMap = new HashMap<>();
        for(String s : map.keySet()) {
            serialMap.put(s, SerializerFactory.getState(map.get(s)));
        }
        return serialMap;
    }

    @Override
    public int getSplitRange() {
        return dtm.getHeight()/sample;
    }
    
    @Override
    public Map<String, WritableRaster> getResult() {
        return result;
    }

    @Override
    public void gather(Map<String, SerializableState> map) {
        if(isSaved()) {
            if(writers == null) {
                writers = new HashMap<>();
            }
            for(String resName : map.keySet()) {
                try {
                    if(!writers.containsKey(resName)) {
                        ImageWriter writer = new TIFFImageWriterSpi().createWriterInstance();                       
                        writer.setOutput(new FileImageOutputStream(getResultFile(resName)));
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                new int[]{0}, new int[]{0}, DataBuffer.TYPE_FLOAT, false, false), dtm.getWidth()/sample, dtm.getHeight()/sample, 
                                null, null, param);
                        writers.put(resName, writer);
                    }
                    ImageWriter writer = writers.get(resName);
                    Raster r = (Raster) map.get(resName).getObject();
                    writer.prepareReplacePixels(0, new Rectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()));
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setDestinationOffset(r.getBounds().getLocation());
                    writer.replacePixels(r, param);
                    writer.endReplacePixels();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            if(result == null) {
                result = new HashMap<>();
            }
            for(String resName : map.keySet()) {
                if(!result.containsKey(resName)) {
                    result.put(resName, Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, dtm.getWidth()/sample, dtm.getHeight()/sample, 1), null));
                }
                result.get(resName).setRect((Raster) map.get(resName).getObject());
            }
        }
    }
    
    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            GridGeometry2D savedGrid = grid;
            if(sample > 1) {
                double r = project.getDefaultScaleData().getResolution();
                Envelope2D env = grid.getEnvelope2D();
                int w = dtm.getWidth()/sample;
                int h = dtm.getHeight()/sample;
                env = new Envelope2D(env.getCoordinateReferenceSystem(), 
                        env.x, env.y-(h*sample*r-env.getHeight()), w * sample*r, h * sample*r);
                savedGrid = new GridGeometry2D(new Rectangle(w, h), env);
            }
            try {
                for(String resName : writers.keySet()) {
                    writers.get(resName).endWriteEmpty();
                    writers.get(resName).dispose();
                    IOImage.createTIFFWorldFile(savedGrid, getResultFile(resName).getAbsolutePath());
                }
            } catch (IOException ex) {
                Logger.getLogger(GridMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private File getResultFile(String resName) {
        if(isTan) {
            return new File(resDir, resName + "-" + bounds + ".tif");
        } else {
            return new File(resDir, resName + (inverse ? "-inverse" : "") + "-" + bounds + ".tif");
        }
    }
}
