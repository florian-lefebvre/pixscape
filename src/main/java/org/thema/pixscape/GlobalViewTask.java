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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
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
import org.geotools.coverage.grid.GridCoverageFactory;
import org.thema.common.ProgressBar;
import org.thema.data.IOImage;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.Project.Aggregate;

/**
 *
 * @author gvuidel
 */
public class GlobalViewTask extends AbstractParallelTask<WritableRaster, SerializableState> implements Serializable {
    
    private final double startZ;
    
    // option for 
    private double destZ;
    private boolean direct;
    
    // option for tangential view
    private final double anglePrec;
    
    private final Bounds bounds;
    private final SortedSet<Integer> from;
    private final SortedSet<Integer> to;
    private final Aggregate aggr;
    private final int sample;
    private final File resDir;
    private boolean byLanduse;
    
    private transient ComputeView compute;
    private transient Raster dtm, land;
    
    private transient WritableRaster vis;
    private transient ImageWriter writer;

    public GlobalViewTask(double startZ, double destZ, boolean direct, Bounds bounds, int sample, File resDir, ProgressBar monitor) {
        this(startZ, destZ, direct, bounds, null, null, Aggregate.SUM, sample, resDir, monitor);
    }
    
    public GlobalViewTask(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode,  Set<Integer> toCode, Aggregate aggr, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        if(fromCode != null) {
            this.from = new TreeSet<>(direct ? fromCode : toCode);
            this.to = new TreeSet<>(direct ? toCode : fromCode);
        } else {
            from = to = null;
        }
        this.aggr = aggr;
        this.sample = sample;
        this.resDir = resDir;
        this.anglePrec = Double.NaN;
        initParams();
    }
    
    public GlobalViewTask(double startZ, double anglePrec, Bounds bounds, int sample, File resDir, ProgressBar monitor) {
        this(startZ, anglePrec, bounds, null, null, Aggregate.SUM, sample, resDir, monitor);
    }
    
    public GlobalViewTask(double startZ, double anglePrec, Bounds bounds, Set<Integer> fromCode,  Set<Integer> toCode, Aggregate aggr, int sample, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.anglePrec = anglePrec;
        this.bounds = bounds;
        if(fromCode != null) {
            this.from = new TreeSet<>(fromCode);
            this.to = new TreeSet<>(toCode);
        } else {
            from = to = null;
        }
        this.aggr = aggr;
        this.sample = sample;
        this.resDir = resDir;
        
        initParams();
    }

    private void initParams() {
        // needed for getSplitRange
        dtm = Project.getProject().getDtm();
        if(aggr == Aggregate.NONE)
            throw new IllegalArgumentException();
        if(aggr == Aggregate.SHANNON && (from == null || !Project.getProject().hasLandUse()))
            throw new IllegalArgumentException("Shannon needs land use");
        byLanduse = aggr == Aggregate.SHANNON || (from != null);
    }
    
    private boolean isTanView() {
        return !Double.isNaN(anglePrec);
    }
    
    public boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public void init() {
        super.init(); 
        compute = Project.getProject().getComputeView();
        dtm = Project.getProject().getDtm();
        land = Project.getProject().getLandUse();
    }
    
    @Override
    public SerializableState execute(int y0, int y1) {
        WritableRaster r = Raster.createWritableRaster(new BandedSampleModel(getResultDatatype(), dtm.getWidth()/sample, y1-y0, 1), new Point(0, y0));

        for(int y = y0; y < y1; y++) {
            if(isCanceled())
                break;
            for(int x = 0; x < dtm.getWidth()/sample; x++) {   
                GridCoordinates2D c = new GridCoordinates2D(x*sample+sample/2, y*sample+sample/2);
                if(byLanduse && !from.contains(land.getSample(c.x, c.y, 0)))
                    continue;
                
                if(byLanduse) {
                    double [] landuse = isTanView() ? compute.aggrViewTanLand(c, startZ, anglePrec, bounds, to)
                            : compute.aggrViewShedLand(c, startZ, destZ, direct, bounds, to);
                    double sum = 0;
                    for(int code : to) {
                        sum += landuse[code];
                    }
                    if(aggr == Aggregate.SUM) {
                        r.setSample(x, y, 0, sum);  
                    } else {
                        double shannon = 0;
                        for(int code : to) {
                            final double val = landuse[code];
                            if(val > 0) {
                                shannon += - val/sum * Math.log(val/sum);
                            }
                        }
                        r.setSample(x, y, 0, shannon/Math.log(to.size()));
                    }
                } else {
                    double sum = isTanView() ? compute.aggrViewTan(c, startZ, anglePrec, bounds)
                            : compute.aggrViewShed(c, startZ, destZ, direct, bounds);
                    r.setSample(x, y, 0, sum);  
                }
            }
            incProgress(1);
        }
        return SerializerFactory.getState(r);
    }

    @Override
    public int getSplitRange() {
        return dtm.getHeight()/sample;
    }
    @Override
    public WritableRaster getResult() {
        return vis;
    }
    
    public File getResultFile() {
        if(byLanduse)
            return new File(resDir, "global-" + aggr.toString().toLowerCase() + "-" + (direct ? "direct" : "indirect") + "-from" + from + "-to" + to + "_" + bounds + ".tif");
        else
            return new File(resDir, "global-" + (direct ? "direct" : "indirect") + "_" + bounds + ".tif");
    }
    
    public int getResultDatatype() {
        if(aggr == Aggregate.SHANNON || isTanView())
            return DataBuffer.TYPE_FLOAT;
        else
            return DataBuffer.TYPE_INT;
    }

    @Override
    public void gather(SerializableState s) {
        Raster r = (Raster) s.getObject();
        if(isSaved()) {
            try {
                if(writer == null) {
                    writer = new TIFFImageWriterSpi().createWriterInstance();
                    writer.setOutput(new FileImageOutputStream(getResultFile()));
                    writer.prepareWriteEmpty(null, ImageTypeSpecifier.createBanded(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                            new int[]{0}, new int[]{0}, getResultDatatype(), false, false), dtm.getWidth()/sample, dtm.getHeight()/sample, null, null, null);
                }
                writer.prepareReplacePixels(0, new Rectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()));
                TIFFImageWriteParam param = new TIFFImageWriteParam(Locale.FRENCH);
                param.setDestinationOffset(r.getBounds().getLocation());
                writer.replacePixels(r, param);
                writer.endReplacePixels();
            } catch (IOException ex) {
                Logger.getLogger(GlobalViewTask.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else {
            if(vis == null) {
                vis = Raster.createWritableRaster(new BandedSampleModel(getResultDatatype(), dtm.getWidth()/sample, dtm.getHeight()/sample, 1), null);
            }
            vis.setRect(r);
        }
    }

    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            try {
                writer.endWriteEmpty();
                writer.dispose();
                // TODO tfw is false when sample > 1
                IOImage.createTIFFWorldFile(Project.getProject().getDtmCov(), getResultFile().getAbsolutePath());
            } catch (IOException ex) {
                Logger.getLogger(GlobalViewTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
