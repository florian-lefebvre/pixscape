
package org.thema.pixscape;

import com.vividsolutions.jts.geom.util.AffineTransformation;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.thema.common.RasterImage;
import org.thema.data.IOImage;

/**
 *
 * @author Gilles Vuidel
 */
public class ScaleData {
    private double resolution;
    private transient GridCoverage2D dtmCov;
    private transient Raster dtm;
    private transient Raster land, dsm;
    
    private SortedSet<Integer> codes;
    

    public ScaleData(GridCoverage2D cov, Raster land, Raster dsm, double resZ) {
        double noData = Double.NaN;
        if(cov.getProperty("GC_NODATA") != null && (cov.getProperty("GC_NODATA") instanceof Number)) {
            noData = ((Number)cov.getProperty("GC_NODATA")).doubleValue();
        }
        final RenderedImage img = cov.getRenderedImage();
        final RandomIter r = RandomIterFactory.create(img, null);
        final WritableRaster dtmFloat = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, img.getWidth(), img.getHeight(), 1), null);
        for(int y = 0; y < img.getHeight(); y++) {
            for(int x = 0; x < img.getWidth(); x++) {
                final double val = r.getSampleDouble(x, y, 0);
                if(val == noData) {
                    dtmFloat.setSample(x, y, 0, Float.NaN);
                } else {
                    dtmFloat.setSample(x, y, 0, val * resZ);
                }
            }
        }
  
        init(new GridCoverageFactory().create("", dtmFloat, cov.getEnvelope2D()), land, dsm);
    }
    
    ScaleData(GridCoverage2D dtmCov, Raster land, Raster dsm) {
        this(dtmCov, land, dsm, 1);
    }
    
    private void init(GridCoverage2D dtmCov, Raster land, Raster dsm) {
        this.dtmCov = dtmCov;
        this.land = land;
        this.dsm = dsm;
        
        GridEnvelope2D r = dtmCov.getGridGeometry().getGridRange2D();
        resolution = dtmCov.getEnvelope2D().getWidth() / r.getWidth();

        if(land != null) {
            if(land.getWidth() != r.getWidth() || land.getHeight() != r.getHeight()) {
                throw new IllegalArgumentException("Land use raster size does not correspond to DTM raster size");
            }
            int type = land.getSampleModel().getDataType();
            if(type == DataBuffer.TYPE_FLOAT || type == DataBuffer.TYPE_DOUBLE) {
                throw new IllegalArgumentException("Float types are not supported for land use");
            }
            codes = new TreeSet<>();
            for(int yi = 0; yi < land.getHeight(); yi++) {
                for(int xi = 0; xi < land.getWidth(); xi++) {
                    codes.add(land.getSample(xi, yi, 0));
                }
            }
        }
        
        if(dsm != null) {
            if(dsm.getWidth() != r.getWidth() || dsm.getHeight() != r.getHeight()) {
                throw new IllegalArgumentException("DSM raster size does not correspond to DTM raster size");
            }
        }
    }
    
    public double getResolution() {
        return resolution;
    }

    public GridCoverage2D getDtmCov() {
        return dtmCov;
    }

    public synchronized Raster getDtm() {
        if(dtm == null) {
            if(dtmCov.getRenderedImage() instanceof BufferedImage) {
                this.dtm = ((BufferedImage)dtmCov.getRenderedImage()).getRaster();
            } else {
                this.dtm = dtmCov.getRenderedImage().getData();
            }
        }
        return dtm;
    }

    public Raster getLand() {
        return land;
    }

    public Raster getDsm() {
        return dsm;
    }

    public SortedSet<Integer> getCodes() {
        return codes;
    }

    public GridGeometry2D getGridGeometry() {
        return dtmCov.getGridGeometry();
    }
    
    public AffineTransformation getGrid2Space() {
        Envelope2D zone = dtmCov.getEnvelope2D();
        GridEnvelope2D range = getGridGeometry().getGridRange2D();
        return new AffineTransformation(
                zone.getWidth() / range.getWidth(), 0, zone.getMinX(),
                0, -zone.getHeight() / range.getHeight(), zone.getMaxY());
    }
    
    public boolean hasLandUse() {
        return land != null;
    }
    
    public final double getZ(int x, int y) {
        double z = dtm.getSample(x, y, 0);
        if(dsm != null) {
            z += dsm.getSample(x, y, 0);
        }
        return z;
    }
    
    
    void load(File dir) throws IOException {
        dtmCov = IOImage.loadTiffWithoutCRS(new File(dir, "dtm-" + resolution + ".tif"));
        
        File dsmFile = new File(dir, "dsm-" + resolution + ".tif");
        if(dsmFile.exists()) {
            GridCoverage2D cov = IOImage.loadTiffWithoutCRS(dsmFile);
            if(cov.getRenderedImage() instanceof BufferedImage) {
                dsm = ((BufferedImage)cov.getRenderedImage()).getRaster();
            } else {
                dsm = cov.getRenderedImage().getData();
            }
        }
        
        File luFile = new File(dir, "land-" + resolution + ".tif");
        if(luFile.exists()) {
            GridCoverage2D cov = IOImage.loadTiffWithoutCRS(luFile);
            if(cov.getRenderedImage() instanceof BufferedImage) {
                land = ((BufferedImage)cov.getRenderedImage()).getRaster();
            } else {
                land = cov.getRenderedImage().getData();
            }
        }
    }
    
    void save(File dir) throws IOException {
        new GeoTiffWriter(new File(dir, "dtm-" + getResolution() + ".tif")).write(dtmCov, null);
        if(dsm != null) {
            GridCoverage2D dsmCov = new GridCoverageFactory().create("", new RasterImage(dsm), dtmCov.getEnvelope2D());
            new GeoTiffWriter(new File(dir, "dsm-" + getResolution() + ".tif")).write(dsmCov, null);
        }
        if(hasLandUse()) {
            GridCoverage2D landCov = new GridCoverageFactory().create("", new RasterImage(land), dtmCov.getEnvelope2D());
            new GeoTiffWriter(new File(dir, "land-" + getResolution() + ".tif")).write(landCov, null);
        }
    }
}
