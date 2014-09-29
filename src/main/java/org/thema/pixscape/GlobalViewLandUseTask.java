/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import org.thema.pixscape.view.ComputeView;
import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
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

/**
 *
 * @author gvuidel
 */
public class GlobalViewLandUseTask extends AbstractParallelTask<Map<Integer, WritableRaster>, Map<Integer, SerializableState>> implements Serializable {
    
    private final double startZ;
    
    private double destZ;
    private boolean direct;
    
    // option for tangential view
    private final double anglePrec;
    
    private final Bounds bounds;
    private final SortedSet<Integer> from;
    private final SortedSet<Integer> to;
    private final int sample;
    private final File resDir;
    
    private transient ComputeView compute;
    private transient Raster dtm, land;
    private transient Map<Integer, WritableRaster> result;
    private transient Map<Integer, ImageWriter> writers;

    public GlobalViewLandUseTask(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode,  Set<Integer> toCode, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        this.from = new TreeSet<>(direct ? fromCode : toCode);
        this.to = new TreeSet<>(direct ? toCode : fromCode);
        this.sample = sample;
        this.resDir = resDir;
        this.anglePrec = Double.NaN;
        // needed for getSplitRange
        dtm = Project.getProject().getDtm();
    }
    
    public GlobalViewLandUseTask(double startZ, double anglePrec, Bounds bounds, Set<Integer> fromCode,  Set<Integer> toCode, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.anglePrec = anglePrec;
        this.bounds = bounds;
        this.from = new TreeSet<>(fromCode);
        this.to = new TreeSet<>(toCode);
        this.sample = sample;
        this.resDir = resDir;
        // needed for getSplitRange
        dtm = Project.getProject().getDtm();
    }

    @Override
    public void init() {
        super.init(); 
        compute = Project.getProject().getComputeView();
        dtm = Project.getProject().getDtm();
        land = Project.getProject().getLandUse();
    }
    
    private boolean isTanView() {
        return !Double.isNaN(anglePrec);
    }
    
    public boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<Integer, SerializableState> execute(int y0, int y1) {
        Map<Integer, WritableRaster> map = new HashMap<>();
        for(int code : to) {
            WritableRaster r = Raster.createBandedRaster(DataBuffer.TYPE_INT, dtm.getWidth()/sample, y1-y0, 1, new Point(0, y0));
            Arrays.fill(((DataBufferInt)r.getDataBuffer()).getData(), -1);
            map.put(code, r);
        }
        final int w = dtm.getWidth()/sample;
        for(int y = y0; y < y1; y++) {
            if(isCanceled())
                break;
            for(int x = 0; x < w; x++) {
                GridCoordinates2D c = new GridCoordinates2D(x*sample+sample/2, y*sample+sample/2);
                if(!from.contains(land.getSample(c.x, c.y, 0)))
                    continue;
                double [] sum = isTanView() ? compute.aggrViewTanLand(c, startZ, anglePrec, bounds, to)
                        : compute.aggrViewShedLand(c, startZ, destZ, direct, bounds, to);
                for(int code : to) {
                    map.get(code).setSample(x, y, 0, sum[code]);
                }
            }
            incProgress(1);
        }
        Map<Integer, SerializableState> serialMap = new HashMap<>();
        for(Integer i : map.keySet())
            serialMap.put(i, SerializerFactory.getState(map.get(i)));
        return serialMap;
    }

    @Override
    public int getSplitRange() {
        return dtm.getHeight()/sample;
    }
    
    @Override
    public Map<Integer, WritableRaster> getResult() {
        return result;
    }

    @Override
    public void gather(Map<Integer, SerializableState> map) {
        if(isSaved()) {
            if(writers == null)
                writers = new HashMap<>();
            for(int code : map.keySet()) {
                try {
                    if(!writers.containsKey(code)) {
                        ImageWriter writer = new TIFFImageWriterSpi().createWriterInstance();
                        writer.setOutput(new FileImageOutputStream(getResultFile(code)));
                        writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                new int[]{0}, new int[]{0}, DataBuffer.TYPE_INT, false, false), dtm.getWidth()/sample, dtm.getHeight()/sample, null, null, null);
                        writers.put(code, writer);
                    }
                    ImageWriter writer = writers.get(code);
                    Raster r = (Raster) map.get(code).getObject();
                    writer.prepareReplacePixels(0, new Rectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()));
                    TIFFImageWriteParam param = new TIFFImageWriteParam(Locale.FRENCH);
                    param.setDestinationOffset(r.getBounds().getLocation());
                    writer.replacePixels(r, param);
                    writer.endReplacePixels();
                } catch (IOException ex) {
                    Logger.getLogger(GlobalViewLandUseTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            if(result == null)
                result = new HashMap<>();
            for(int code : map.keySet()) {
                if(!result.containsKey(code))
                    result.put(code, Raster.createBandedRaster(DataBuffer.TYPE_INT, dtm.getWidth()/sample, dtm.getHeight()/sample, 1, null));
                result.get(code).setRect((Raster) map.get(code).getObject());
            }
        }
    }
    
    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            try {
                for(int code : writers.keySet()) {
                    writers.get(code).endWriteEmpty();
                    writers.get(code).dispose();
                    // TODO tfw is false when sample > 1
                    IOImage.createTIFFWorldFile(Project.getProject().getDtmCov(), getResultFile(code).getAbsolutePath());
                }
            } catch (IOException ex) {
                Logger.getLogger(GlobalViewTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public File getResultFile(int code) {
        return new File(resDir, "global-" + (direct ? "direct" : "indirect") + "-" + code + "_" + bounds + ".tif");
    }
}
