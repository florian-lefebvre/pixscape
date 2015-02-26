/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author gvuidel
 */
public abstract class MultiViewResult extends AbstractViewResult {

    private final TreeMap<Double, Raster> views;
    private final TreeMap<Double, GridEnvelope2D> zones;
    private final MultiComputeViewJava compute;
    
    private final TreeMap<Double, Point2D> coords;
    
    protected WritableRaster view, land;

    public MultiViewResult(GridCoordinates2D cg, TreeMap<Double, Raster> views, TreeMap<Double, GridEnvelope2D> zones, MultiComputeViewJava compute) {
        super(cg);
        this.compute = compute;
        this.views = views;
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

    public final TreeMap<Double, Raster> getViews() {
        return views;
    }

    public final TreeMap<Double, GridEnvelope2D> getZones() {
        return zones;
    }

    public final TreeMap<Double, ScaleData> getDatas() {
        return compute.getDatas();
    }
    
    @Override
    public final synchronized Raster getView() {
        if(view == null) {
            calcViewLand();
        }
        return view;
    }

    @Override
    public final synchronized Raster getLanduse() {
        if(land == null) {
            calcViewLand();
        }
        return land;
    }

    @Override
    public final GridGeometry2D getGrid() {
        return compute.getDatas().firstEntry().getValue().getGridGeometry();
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
    
    protected abstract void calcViewLand();

    protected final boolean isInside(double res, int x, int y, double dmin, double dmax) {
        final Point2D p = coords.get(res);
        double d2 = res*res * (Math.pow(x-p.getX(), 2) + Math.pow(y-p.getY(), 2));
        return d2 >= dmin*dmin && d2 < dmax*dmax;
    }
}
