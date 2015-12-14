
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
 * Contains the date for one scale (resolution) :
 * - the DTM (Digital Terrain Model) : represents the elevation of the ground
 * - the DSM (Digital Surface Model)(optional) : represents the height of the objetcs on the ground
 * - the land use (optional)
 * Loads and saves the rasters from/to the project directory.
 * 
 * @author Gilles Vuidel
 */
public final class ScaleData {
    private double resolution;
    private transient GridCoverage2D dtmCov;
    private transient Raster dtm;
    private transient Raster land, dsm;
    private transient Double maxZ;
    
    private SortedSet<Integer> codes;
    

    /**
     * Creates a new ScaleData.
     * The unit of the coordinate system ind dtmCov must be metric
     * The coordinate system for the land and dsm is supposed to be the same
     * The elevation resolution of the DSM must be metric.
     * @param dtmCov the DTM coverage
     * @param land the land use raster, may be null
     * @param dsm the DSM raster or null
     * @param resZ the elevation resolution in meter of the DTM
     * @throws IllegalArgumentException if land or dsm have not the same size than the dtmCov
     * @throws IllegalArgumentException if land has float number type or if values are outside of [0-255]
     */
    public ScaleData(GridCoverage2D dtmCov, Raster land, Raster dsm, double resZ) {
        double noData = Double.NaN;
        if(dtmCov.getProperty("GC_NODATA") != null && (dtmCov.getProperty("GC_NODATA") instanceof Number)) {
            noData = ((Number)dtmCov.getProperty("GC_NODATA")).doubleValue();
        }
        RenderedImage img = dtmCov.getRenderedImage();
        if(img.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT || resZ != 1 || !Double.isNaN(noData)) {
            RandomIter r = RandomIterFactory.create(img, null);
            WritableRaster dtmFloat = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, img.getWidth(), img.getHeight(), 1), null);
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
            init(new GridCoverageFactory().create("", dtmFloat, dtmCov.getEnvelope2D()), land, dsm);
        }

        init(dtmCov, land, dsm);
    }
    
    /**
     * Creates a new ScaleData.
     * The unit of the coordinate system ind dtmCov must be metric
     * The coordinate system for the land and dsm is supposed to be the same
     * The elevation resolution of the DTM and the DSM must be metric.
     * @param dtmCov the DTM coverage
     * @param land the land use raster, may be null
     * @param dsm the DSM raster or null
     * @param resZ the elevation resolution of the DTM
     * @throws IllegalArgumentException if land or dsm have not the same size than the dtmCov
     * @throws IllegalArgumentException if land has float number type or if values are outside of [0-255]
     */
    ScaleData(GridCoverage2D dtmCov, Raster land, Raster dsm) {
        this(dtmCov, land, dsm, 1);
    }
    
    private void init(GridCoverage2D dtmCov, Raster land, Raster dsm) {
        this.dtmCov = dtmCov;
        
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
            this.land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, land.getWidth(), land.getHeight(), 1), null);
            codes = new TreeSet<>();
            for(int yi = 0; yi < land.getHeight(); yi++) {
                for(int xi = 0; xi < land.getWidth(); xi++) {
                    int val = land.getSample(xi, yi, 0) & 0xFF;
                    if(val < 0 || val > 255) {
                        throw new IllegalArgumentException("Land codes must be in [0-255]");
                    }
                    codes.add(val);
                    ((WritableRaster)this.land).setSample(xi, yi, 0, val);
                }
            }
        }
        
        if(dsm != null) {
            if(dsm.getWidth() != r.getWidth() || dsm.getHeight() != r.getHeight()) {
                throw new IllegalArgumentException("DSM raster size does not correspond to DTM raster size");
            }
            if(dsm.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT) {
                this.dsm = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, dsm.getWidth(), dsm.getHeight(), 1), null);
                for(int y = 0; y < dsm.getHeight(); y++) {
                    for(int x = 0; x < dsm.getWidth(); x++) {
                        final double val = dsm.getSampleDouble(x, y, 0);
                        ((WritableRaster)this.dsm).setSample(x, y, 0, val);
                    }
                }
            } else {
                this.dsm = dsm;
            }
        }
        
        if(dtmCov.getRenderedImage() instanceof BufferedImage) {
            this.dtm = ((BufferedImage)dtmCov.getRenderedImage()).getRaster();
        } else {
            this.dtm = dtmCov.getRenderedImage().getData();
        }
    }
    
    /**
     * @return the 2D resolution of the data in meter ie. pixel size
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * DTM values are always in meter.
     * @return the DTM coverage
     */
    public GridCoverage2D getDtmCov() {
        return dtmCov;
    }

    /**
     * DTM values are always in meter.
     * @return the DTM raster
     */
    public Raster getDtm() {
        return dtm;
    }
    
    /**
     * Returns a raster of type float and meter unit
     * @return the land use raster or null if none
     */
    public Raster getLand() {
        return land;
    }

    /**
     * Returns a raster of type float and meter unit
     * @return the DSM raster or null if none
     */
    public Raster getDsm() {
        return dsm;
    }

    /**
     * @return the landuse codes contained in the landuse raster or null of no landuse raster
     */
    public SortedSet<Integer> getCodes() {
        return codes;
    }

    /**
     * @return grid geometry of the coverage
     */
    public GridGeometry2D getGridGeometry() {
        return dtmCov.getGridGeometry();
    }
    
    /**
     * Creates and return a transformation from grid coordinate to world coordinate
     * @return transformation from grid to world coordinate
     */
    public AffineTransformation getGrid2World() {
        Envelope2D zone = dtmCov.getEnvelope2D();
        GridEnvelope2D range = getGridGeometry().getGridRange2D();
        return new AffineTransformation(
                zone.getWidth() / range.getWidth(), 0, zone.getMinX(),
                0, -zone.getHeight() / range.getHeight(), zone.getMaxY());
    }
    
    /**
     * Creates and return a transformation from world coordinate to grid coordinate
     * @return transformation from wrold to grid coordinate
     */
    public AffineTransformation getWorld2Grid() {
        Envelope2D envelope = dtmCov.getEnvelope2D();
        GridEnvelope2D range = getGridGeometry().getGridRange2D();
        double sx = range.getWidth() / envelope.getWidth();
        double sy = range.getHeight() / envelope.getHeight();
        return new AffineTransformation(
                sx, 0, -envelope.getMinX()*sx, 
                0, -sy, envelope.getMaxY()*sy);
    }
    
    /**
     * @return does the data contain land use raster ?
     */
    public boolean hasLandUse() {
        return land != null;
    }
    
    /**
     * Returns the full elevation at position (x, y).
     * The full elevation is the dtm elevation plus the dsm elevation if present
     * @param x x in grid coordinate
     * @param y y in grid coordinate
     * @return the elevation at (x,y) position
     */
    public double getZ(int x, int y) {
        double z = dtm.getSample(x, y, 0);
        if(dsm != null) {
            z += dsm.getSample(x, y, 0);
        }
        return z;
    }
    
    /**
     * Calculates, if not already done, and returns the maximum full elevation.
     * The full elevation is the dtm elevation plus the dsm elevation if present
     * @return the maximum full elevation of the map
     */
    public synchronized double getMaxZ() {
        if(maxZ == null) {
            double max = Double.NEGATIVE_INFINITY;
            final int w = getDtm().getWidth();
            final int h = getDtm().getHeight();
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    final double z = getZ(x, y);
                    if(z > max) {
                        max = z;
                    }
                }
            }
            maxZ = max;
        }
        return maxZ;
    }
    
    /**
     * Loads the 1, 2 or 3 rasters from a directory.
     * @param dir the directory containing the rasters of this scale data
     * @throws IOException 
     */
    void load(File dir) throws IOException {
        dtmCov = IOImage.loadTiffWithoutCRS(new File(dir, "dtm-" + resolution + ".tif"));
        this.dtm = dtmCov.getRenderedImage().getData();
        
        File dsmFile = new File(dir, "dsm-" + resolution + ".tif");
        if(dsmFile.exists()) {
            GridCoverage2D dsmCov = IOImage.loadTiffWithoutCRS(dsmFile);
            dsm = dsmCov.getRenderedImage().getData();
        }
        
        File luFile = new File(dir, "land-" + resolution + ".tif");
        if(luFile.exists()) {
            GridCoverage2D landCov = IOImage.loadTiffWithoutCRS(luFile);
            land = landCov.getRenderedImage().getData();
        }
    }
    
    /**
     * Saves the 1, 2 or 3 rasters to a directory.
     * @param dir the directory for storing the rasters of this scale data
     * @throws IOException 
     */
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
