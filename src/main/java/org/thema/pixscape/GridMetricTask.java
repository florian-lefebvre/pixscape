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
import org.thema.common.ProgressBar;
import org.thema.data.IOImage;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.ComputeView.ViewShedResult;
import org.thema.pixscape.ComputeView.ViewTanResult;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 *
 * @author gvuidel
 */
public class GridMetricTask extends AbstractParallelTask<Map<Metric, WritableRaster>, Map<Metric, SerializableState>> implements Serializable {
    
    private final double startZ;
    
    // options for viewshed
    private double destZ;
    private boolean direct;
    
    // option for tangential view
    private final double anglePrec;
    
    private final Bounds bounds;
    
    private final List<? extends Metric> metrics;
    
    // sampling
    private final int sample;
    private final SortedSet<Integer> from;
    
    private final File resDir;
    
    private transient ComputeView compute;
    private transient Raster dtm, land;
    private transient Map<Metric, WritableRaster> result;
    private transient Map<Metric, ImageWriter> writers;

    public GridMetricTask(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode, List<ViewShedMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        this.from = fromCode != null ? new TreeSet<>(fromCode) : null;
        this.metrics = metrics;
        this.sample = sample;
        this.resDir = resDir;
        this.anglePrec = Double.NaN;
    }
    
    public GridMetricTask(double startZ, double anglePrec, Bounds bounds, Set<Integer> fromCode, List<ViewTanMetric> metrics, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.anglePrec = anglePrec;
        this.bounds = bounds;
        this.from = fromCode != null ? new TreeSet<>(fromCode) : null;
        this.metrics = metrics;
        this.sample = sample;
        this.resDir = resDir;
    }

    @Override
    public void init() {
        // needed for getSplitRange
        dtm = Project.getProject().getDtm();
        super.init(); 
        compute = Project.getProject().getComputeView();
        land = Project.getProject().getLandUse();
    }
    
    private boolean isTanView() {
        return !Double.isNaN(anglePrec);
    }
    
    public boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<Metric, SerializableState> execute(int y0, int y1) {
        Map<Metric, WritableRaster> map = new HashMap<>();
        for(Metric metric : metrics) {
            WritableRaster r = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, dtm.getWidth()/sample, y1-y0, 1), new Point(0, y0));
            Arrays.fill(((DataBufferFloat)r.getDataBuffer()).getData(), Float.NaN);
            map.put(metric, r);
        }
        final int w = dtm.getWidth()/sample;
        for(int y = y0; y < y1; y++) {
            if(isCanceled())
                break;
            for(int x = 0; x < w; x++) {
                GridCoordinates2D c = new GridCoordinates2D(x*sample+sample/2, y*sample+sample/2);
                if(from != null && !from.contains(land.getSample(c.x, c.y, 0)))
                    continue;
                List<Double> values;
                if(isTanView()) {
                    values = compute.aggrViewTan(c, startZ, anglePrec, bounds, (List) metrics);
                } else {
                    values = compute.aggrViewShed(c, startZ, destZ, direct, bounds, (List) metrics);
                }
                for(int i = 0; i < metrics.size(); i++)
                    map.get(metrics.get(i)).setSample(x, y, 0, values.get(i));
            }
            incProgress(1);
        }
        Map<Metric, SerializableState> serialMap = new HashMap<>();
        for(Metric m : map.keySet())
            serialMap.put(m, SerializerFactory.getState(map.get(m)));
        return serialMap;
    }

    @Override
    public int getSplitRange() {
        return dtm.getHeight()/sample;
    }
    
    @Override
    public Map<Metric, WritableRaster> getResult() {
        return result;
    }

    @Override
    public void gather(Map<Metric, SerializableState> map) {
        if(isSaved()) {
            if(writers == null)
                writers = new HashMap<>();
            for(Metric metric : map.keySet()) {
                try {
                    if(!writers.containsKey(metric)) {
                        ImageWriter writer = new TIFFImageWriterSpi().createWriterInstance();
                        writer.setOutput(new FileImageOutputStream(getResultFile(metric)));
                        writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                new int[]{0}, new int[]{0}, DataBuffer.TYPE_FLOAT, false, false), dtm.getWidth()/sample, dtm.getHeight()/sample, null, null, null);
                        writers.put(metric, writer);
                    }
                    ImageWriter writer = writers.get(metric);
                    Raster r = (Raster) map.get(metric).getObject();
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
            if(result == null)
                result = new HashMap<>();
            for(Metric metric : map.keySet()) {
                if(!result.containsKey(metric))
                    result.put(metric, Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, dtm.getWidth()/sample, dtm.getHeight()/sample, 1), null));
                result.get(metric).setRect((Raster) map.get(metric).getObject());
            }
        }
    }
    
    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            try {
                for(Metric metric : writers.keySet()) {
                    writers.get(metric).endWriteEmpty();
                    writers.get(metric).dispose();
                    // TODO tfw is false when sample > 1
                    IOImage.createTIFFWorldFile(Project.getProject().getDtmCov(), getResultFile(metric).getAbsolutePath());
                }
            } catch (IOException ex) {
                Logger.getLogger(GlobalViewTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public File getResultFile(Metric metric) {
        if(isTanView())
            return new File(resDir, metric + "-aprec" + anglePrec + "-" + bounds + ".tif");
        else
            return new File(resDir, metric + "-" + (direct ? "direct" : "indirect") + "-" + bounds + ".tif");
    }
}
