/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.remote.SerializableState;
import javax.media.jai.remote.SerializerFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.ProgressBar;
import org.thema.data.IOImage;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.ComputeView;

/**
 *
 * @author gvuidel
 */
public class GridMetricTask extends AbstractParallelTask<Map<String, WritableRaster>, Map<String, SerializableState>> implements Serializable {
    
    private final boolean isTan;
    
    private final double startZ;
    
    // options for viewshed
    private double destZ;
    private boolean direct;
    
    private final Bounds bounds;
    
    private final List<? extends Metric> metrics;
    
    // sampling
    private final int sample;
    private final SortedSet<Integer> from;
    
    private final File resDir;
    
    private transient GridGeometry2D grid;
    private transient ComputeView compute;
    private transient Raster dtm, land;
    private transient Map<String, WritableRaster> result;
    private transient Map<String, ImageWriter> writers;

    public GridMetricTask(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode, List<ViewShedMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        this.from = fromCode != null && !fromCode.isEmpty() ? new TreeSet<>(fromCode) : null;
        this.metrics = metrics;
        this.sample = sample;
        this.resDir = resDir;
        this.isTan = false;
    }
    
    public GridMetricTask(double startZ, Bounds bounds, Set<Integer> fromCode, List<ViewTanMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
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
        // needed for getSplitRange
        dtm = Project.getProject().getDtm();
        grid = Project.getProject().getDtmCov().getGridGeometry();
        super.init(); 
        compute = Project.getProject().getDefaultComputeView();
        land = Project.getProject().getLandUse();
    }
    
    public boolean isSaved() {
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
        for(int y = y0; y < y1; y++) {
            if(isCanceled()) {
                break;
            }
            for(int x = 0; x < w; x++) {
                GridCoordinates2D c = new GridCoordinates2D(x*sample+sample/2, y*sample+sample/2);
                
                if(from != null && !from.contains(land.getSample(c.x, c.y, 0))) {
                    continue;
                }
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
                    values = compute.aggrViewShed(p, startZ, destZ, direct, bounds, (List) metrics);
                }
                for(int i = 0; i < metrics.size(); i++) {
                    int j = 0;
                    for(String resName : metrics.get(i).getResultNames()) {
                        map.get(resName).setSample(x, y, 0, values.get(i)[j++]);
                    }
                }
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
                        writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                new int[]{0}, new int[]{0}, DataBuffer.TYPE_FLOAT, false, false), dtm.getWidth()/sample, dtm.getHeight()/sample, null, null, null);
                        writers.put(resName, writer);
                    }
                    ImageWriter writer = writers.get(resName);
                    Raster r = (Raster) map.get(resName).getObject();
                    writer.prepareReplacePixels(0, new Rectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()));
                    TIFFImageWriteParam param = new TIFFImageWriteParam(Locale.FRENCH);
                    param.setDestinationOffset(r.getBounds().getLocation());
                    writer.replacePixels(r, param);
                    writer.endReplacePixels();
                } catch (IOException ex) {
                    Logger.getLogger(GridMetricTask.class.getName()).log(Level.SEVERE, null, ex);
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
            try {
                for(String resName : writers.keySet()) {
                    writers.get(resName).endWriteEmpty();
                    writers.get(resName).dispose();
                    // TODO tfw is false when sample > 1
                    IOImage.createTIFFWorldFile(Project.getProject().getDtmCov(), getResultFile(resName).getAbsolutePath());
                }
            } catch (IOException ex) {
                Logger.getLogger(GridMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public File getResultFile(String resName) {
        if(isTan) {
            return new File(resDir, resName + "-" + bounds + ".tif");
        } else {
            return new File(resDir, resName + "-" + (direct ? "direct" : "indirect") + "-" + bounds + ".tif");
        }
    }
}
