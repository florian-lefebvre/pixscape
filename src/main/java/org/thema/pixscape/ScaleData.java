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

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import org.locationtech.jts.geom.util.AffineTransformation;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.operation.TransformException;
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
    private transient RenderedImage dtm, land, dsm;
    private transient Raster dtmRaster, landRaster, dsmRaster;
    private transient Double maxZ;
    
    private transient GridGeometry2D gridGeom;
    
    private SortedSet<Integer> codes;
    

    /**
     * Creates a new ScaleData.
     * The unit of the coordinate system in dtmCov must be metric
     * The coordinate system for the land and dsm is supposed to be the same
     * The elevation resolution of the DSM must be metric.
     * @param dtmCov the DTM coverage
     * @param land the land use raster, may be null
     * @param dsm the DSM raster or null
     * @param resZ the elevation resolution in meter of the DTM
     * @throws IllegalArgumentException if land or dsm have not the same size than the dtmCov
     * @throws IllegalArgumentException if land has float number type or if values are outside of [0-255]
     */
    public ScaleData(GridCoverage2D dtmCov, RenderedImage land, RenderedImage dsm, double resZ) {
        double noData = Double.NaN;
        NoDataContainer noDataProp = CoverageUtilities.getNoDataProperty(dtmCov);
        if(noDataProp != null) {
            noData = noDataProp.getAsSingleValue();
        }
        
        RenderedImage img = dtmCov.getRenderedImage();
        if(img.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT || resZ != 1 || !Double.isNaN(noData)) {
            RandomIter r = RandomIterFactory.create(img, null);
            TiledImage dtmFloat = new TiledImage(0, 0, img.getWidth(), img.getHeight(), 0, 0, 
                        new BandedSampleModel(DataBuffer.TYPE_FLOAT, 1000, 1000, 1), 
                        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, ColorModel.OPAQUE, DataBuffer.TYPE_FLOAT));
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
        } else {
            init(dtmCov, land, dsm);
        }
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
    ScaleData(GridCoverage2D dtmCov, RenderedImage land, RenderedImage dsm) {
        this(dtmCov, land, dsm, 1);
    }
    
    private ScaleData(ScaleData data, Rectangle rect) {
        resolution = data.getResolution();
        dtmCov = data.getDtmCov();
        dtm = data.getDtm();
        dsm = data.getDsm();
        land = data.getLand();
        dtmRaster = data.getDtmRaster(rect);
        dsmRaster = data.getDsmRaster(rect);
        landRaster = data.getLandRaster(rect);
        codes = data.getCodes();
    }
    
    private void init(GridCoverage2D dtmCov, RenderedImage land, RenderedImage dsm) {
        this.dtmCov = dtmCov;
        
        GridEnvelope2D r = dtmCov.getGridGeometry().getGridRange2D();
        resolution = dtmCov.getEnvelope2D().getWidth() / r.getWidth();

        if(land != null) {
            if(land.getWidth() != r.getWidth() || land.getHeight() != r.getHeight()) {
                throw new IllegalArgumentException("Land use raster size does not correspond to DTM raster size");
            }
            boolean recode = land.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE;
            if(recode) {
                this.land = new TiledImage(0, 0, land.getWidth(), land.getHeight(), 0, 0, 
                        new BandedSampleModel(DataBuffer.TYPE_BYTE, 1000, 1000, 1),  
                        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE));
            } else {
                this.land = land;
            }
            
            boolean [] boolCodes = new boolean[256];
            for(int yt = 0; yt < land.getNumYTiles(); yt++) {
                for(int xt = 0; xt < land.getNumXTiles(); xt++) {
                    Raster tile = land.getTile(xt+land.getMinTileX(), yt+land.getMinTileY());
                    for(int yi = 0; yi < tile.getHeight(); yi++) {
                        for(int xi = 0; xi < tile.getWidth(); xi++) {
                            int val = tile.getSample(xi+tile.getMinX(), yi+tile.getMinY(), 0);
                            if(val < 0 || val > 255) {
                                throw new IllegalArgumentException("Land codes must be integer between 0 and 255");
                            }
                            boolCodes[val] = true;
                            if(recode) {
                                ((TiledImage)this.land).setSample(xi+tile.getMinX(), yi+tile.getMinY(), 0, val);
                            }
                        }
                    }
                }
            }
            codes = new TreeSet<>();
            for(int i = 0; i < boolCodes.length; i++) {
                if(boolCodes[i]) {
                    codes.add(i);
                }
            }
            
        }
        
        if(dsm != null) {
            if(dsm.getWidth() != r.getWidth() || dsm.getHeight() != r.getHeight()) {
                throw new IllegalArgumentException("DSM raster size does not correspond to DTM raster size");
            }
            if(dsm.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT) {
                RandomIter rDsm = RandomIterFactory.create(dsm, null);
                this.dsm = new TiledImage(0, 0, dsm.getWidth(), dsm.getHeight(), 0, 0, 
                        new BandedSampleModel(DataBuffer.TYPE_FLOAT, 1000, 1000, 1), 
                        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, ColorModel.OPAQUE, DataBuffer.TYPE_FLOAT));
                for(int y = 0; y < dsm.getHeight(); y++) {
                    for(int x = 0; x < dsm.getWidth(); x++) {
                        final double val = rDsm.getSampleDouble(x, y, 0);
                        ((TiledImage)this.dsm).setSample(x, y, 0, val);
                    }
                }
            } else {
                this.dsm = dsm;
            }
        }
        
        this.dtm = dtmCov.getRenderedImage();
        if(isLoadable()) {
            dtmRaster = this.dtm.getData();
            if(this.dsm != null) {
                dsmRaster = this.dsm.getData();
            }
            if(this.land != null) {
                landRaster = this.land.getData();
            }
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
     * @return the DTM RenderedImage
     */
    public RenderedImage getDtm() {
        return dtm;
    }
    
    /**
     * Returns a RenderedImage of type float and meter unit
     * @return the land use RenderedImage or null if none
     */
    public RenderedImage getLand() {
        return land;
    }

    /**
     * Returns a RenderedImage of type float and meter unit
     * @return the DSM RenderedImage or null if none
     */
    public RenderedImage getDsm() {
        return dsm;
    }

    public Raster getDtmRaster() {
        if(dtmRaster == null) {
            throw new OutOfMemoryError("Not enough memory, try to increase the memory allocated to Pixscape");
        }
        return dtmRaster;
    }
    
    public Raster getDtmRaster(Rectangle env) {
        if(dtmRaster != null) {
            return dtmRaster;
        } else {
            return dtm.getData(env);
        }
    }

    public Raster getLandRaster() {
        if(land != null && landRaster == null) {
            throw new OutOfMemoryError("Not enough memory, try to increase the memory allocated to Pixscape");
        }
        return landRaster;
    }
    
    public Raster getLandRaster(Rectangle env) {
        if(land != null) {
            if(landRaster != null) {
                return landRaster;
            } else {
                return land.getData(env);
            }
        } else {
            return null;
        }
    }

    public Raster getDsmRaster() {
        if(dsm != null && dsmRaster == null) {
            throw new OutOfMemoryError("Not enough memory, try to increase the memory allocated to Pixscape");
        }
        return dsmRaster;
    }
    
    public Raster getDsmRaster(Rectangle env) {
        if(dsm != null) {
            if(dsmRaster != null) {
                return dsmRaster;
            } else {
                return dsm.getData(env);
            }
        } else {
            return null;
        }
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
    public final GridGeometry2D getGridGeometry() {
        if(gridGeom == null) {
            gridGeom = dtmCov.getGridGeometry();
        }
        return gridGeom;
    }
    
    /**
     * Transform a point from world coordinate to grid coordinate
     * @param p the point in world coordinate
     * @return the point in grid coordinate
     */
    public final GridCoordinates2D getWorld2Grid(DirectPosition2D p) {
        try {
            return getGridGeometry().worldToGrid(p);
        } catch (TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
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
        if(dtmRaster != null) {
            double z = dtmRaster.getSampleDouble(x, y, 0);
            if(dsm != null) {
                z += dsmRaster.getSampleDouble(x, y, 0);
            }
            return z;
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Calculates, if not already done, and returns the maximum full elevation.
     * The full elevation is the dtm elevation plus the dsm elevation if present
     * @return the maximum full elevation of the map
     */
    public synchronized double getMaxZ() {
        if(maxZ == null) {
            RandomIter rDtm = RandomIterFactory.create(dtm, null);
            RandomIter rDsm = dsm != null ? RandomIterFactory.create(dsm, null) : null;
            double max = Double.NEGATIVE_INFINITY;
            final int w = getDtm().getWidth();
            final int h = getDtm().getHeight();
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    final double z = rDtm.getSampleDouble(x, y, 0) + (rDsm != null ? rDsm.getSampleDouble(x, y, 0) : 0);
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
     * Check if DTM DSM and landuse can be loaded completely in memory
     * @return 
     */
    public boolean isLoadable() {
        double coef = dsm == null ? 5 : 10;
        return Runtime.getRuntime().maxMemory() > coef*dtm.getWidth()*dtm.getHeight() && dtm.getWidth()*(long)dtm.getHeight() < Integer.MAX_VALUE;
    }
    
    /**
     * Loads the 1, 2 or 3 rasters from a directory.
     * @param dir the directory containing the rasters of this scale data
     * @throws IOException 
     */
    void load(File dir) throws IOException {
        dtmCov = IOImage.loadTiffWithoutCRS(new File(dir, "dtm-" + resolution + ".tif"));
        
        this.dtm = dtmCov.getRenderedImage();

        File dsmFile = new File(dir, "dsm-" + resolution + ".tif");
        if(dsmFile.exists()) {
            GridCoverage2D dsmCov = IOImage.loadTiffWithoutCRS(dsmFile);
            dsm = dsmCov.getRenderedImage();
        }

        File luFile = new File(dir, "land-" + resolution + ".tif");
        if(luFile.exists()) {
            GridCoverage2D landCov = IOImage.loadTiffWithoutCRS(luFile);
            land = landCov.getRenderedImage();
        }
        
        if(isLoadable()) {
            dtmRaster = dtm.getData();
            if(this.dsm != null) {
                dsmRaster = dsm.getData();
            }
            if(this.land != null) {
                landRaster = land.getData();
            }
        }
        
    }
    
    /**
     * Saves the 1, 2 or 3 rasters to a directory.
     * @param dir the directory for storing the rasters of this scale data
     * @throws IOException 
     */
    void save(File dir) throws IOException {
        IOImage.saveTiffCoverage(new File(dir, "dtm-" + getResolution() + ".tif"), dtmCov);
        if(dsm != null) {
            GridCoverage2D dsmCov = new GridCoverageFactory().create("", dsm, dtmCov.getEnvelope2D());
            IOImage.saveTiffCoverage(new File(dir, "dsm-" + getResolution() + ".tif"), dsmCov);
        }
        if(hasLandUse()) {
            GridCoverage2D landCov = new GridCoverageFactory().create("", land, dtmCov.getEnvelope2D());
            IOImage.saveTiffCoverage(new File(dir, "land-" + getResolution() + ".tif"), landCov);
        }
    }
    
    public ScaleData getSubData(Rectangle env) {
        return new ScaleData(this, env);
    }
}
