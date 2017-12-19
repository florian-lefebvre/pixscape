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


package org.thema.pixscape.view;

import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.ScaleData;

/**
 * Base class for ViewResult with several scales.
 * 
 * @author Gilles Vuidel
 */
public abstract class MultiViewResult extends AbstractViewResult {

    private final TreeMap<Double, GridEnvelope2D> zones;
    protected final MultiComputeViewJava compute;
    
    private final TreeMap<Double, Point2D> coords;
    
    protected WritableRaster view;

    /**
     * Creates a new MultiViewResult.
     * 
     * @param cg the point of view or observed point in grid coordinate
     * @param zones the zone where viewshed has been calculated for each scale
     * @param compute the compute view used
     */
    public MultiViewResult(GridCoordinates2D cg, TreeMap<Double, GridEnvelope2D> zones, MultiComputeViewJava compute) {
        super(cg);
        this.compute = compute;
        this.zones = zones;
        this.coords = new TreeMap<>();
        try {    
            Point2D p = getDatas().firstEntry().getValue().getGridGeometry().getGridToCRS2D().transform(cg, new Point2D.Double());
            for(ScaleData data : getDatas().values()) {
                coords.put(data.getResolution(), data.getGridGeometry().getCRSToGrid2D().transform(p, new Point2D.Double()));
            }
        } catch (TransformException ex) {
            Logger.getLogger(MultiViewResult.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the zone where viewshed has been calculated for each scale
     */
    public final TreeMap<Double, GridEnvelope2D> getZones() {
        return zones;
    }

    /**
     * @return the data for each scale
     */
    public final TreeMap<Double, ScaleData> getDatas() {
        return compute.getDatas();
    }

    /**
     * Returns the grid coordinates of the point of view for each scale.
     * @return the grid coordinates of the point of view for each scale
     */
    public final TreeMap<Double, Point2D> getCoords() {
        return coords;
    }
    
    /**
     * Returns the view in the first scale grid geometry.
     * {@inheritDoc }
     */
    @Override
    public final synchronized Raster getView() {
        if(view == null) {
            calcViewLand();
        }
        return view;
    }
    
    @Override
    public final synchronized Raster getLanduseView() {
        if(landuse == null) {
            calcViewLand();
        }
        
        return landuse;
    }
    
    @Override
    public final int getLand(int x, int y) {
        return getLanduseView().getSample(x, y, 0);
    }

    @Override
    public final ScaleData getData() {
        return compute.getDatas().firstEntry().getValue();
    }

    @Override
    public final GridGeometry2D getGrid() {
        return getData().getGridGeometry();
    }

    @Override
    public final double getRes2D() {
        return compute.getDatas().firstKey();
    }

    @Override
    public final SortedSet<Integer> getCodes() {
        TreeSet<Integer> codes = new TreeSet<>();
        for(ScaleData data : compute.getDatas().values()) {
            codes.addAll(data.getCodes());
        }
        return codes;
    }
    
    /**
     * Calculates the view and the landuse at the first scale and stores them into view and land.
     */
    protected abstract void calcViewLand();
    
    /**
     * Calculates the distance between the point of view and (x, y), and checks if it is in the interval [dmin dmax[.
     * 
     * @param res2 the squared resolution of the data
     * @param p the point of view in grid coordinate at scale res
     * @param x x grid coordinate at scale res
     * @param y y grid coordinate at scale res
     * @param dmin the minimal distance
     * @param dmax the maximal distance
     * @return true if the distance to the point (x, y) is in the interval [dmin dmax[
     */
    protected final boolean isInside(double res2, Point2D p, int x, int y, double dmin, double dmax) {
        double d2 = res2 * (Math.pow(x-p.getX(), 2) + Math.pow(y-p.getY(), 2));
        return d2 >= dmin*dmin && d2 < dmax*dmax;
    }
}
