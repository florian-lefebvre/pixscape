
package org.thema.pixscape.view;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.JTS;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;
import org.thema.process.Vectorizer;

/**
 * ViewShedResult implementation for multiscale computation.
 * 
 * @author Gilles Vuidel
 */
public class MultiViewShedResult extends MultiViewResult implements ViewShedResult {

    /**
     * Creates a new MultiViewShedResult.
     * 
     * @param cg the point of view or observed point in grid coordinate
     * @param views the viewshed for each scale
     * @param zones the zone where viewshed has been calculated for each scale
     * @param compute the compute view used
     */
    MultiViewShedResult(GridCoordinates2D cg, TreeMap<Double, Raster> views, TreeMap<Double, GridEnvelope2D> zones, MultiComputeViewJava compute) {
        super(cg, views, zones, compute);
    }

    @Override
    public double getPerimeter() {
        throw new UnsupportedOperationException("Perimeter is not supported with multi resolution data"); 
    }

    @Override
    public double getArea(double dmin, double dmax) {
        final boolean unbounded = Bounds.isUnboundedDistance(dmin, dmax);
        double area = 0;
        for(Double res : getViews().keySet()) {
            final double res2 = res*res;
            final Raster v = getViews().get(res);    
            final Point2D p = getCoords().get(res);
            final GridEnvelope2D zone = getZones().get(res);
            for(int y = zone.y; y < zone.getMaxY(); y++) {
                for(int x = zone.x; x < zone.getMaxX(); x++) {
                    if(v.getSample(x, y, 0) == 1 && (unbounded || isInside(res2, p, x, y, dmin, dmax))) {
                        area += res2;
                    }
                }
            }
        }
        return area;
    }

    @Override
    protected double[] calcAreaLand(double dmin, double dmax) {
        final boolean unbounded = Bounds.isUnboundedDistance(dmin, dmax);
        final double [] counts = new double[256];
        final double sqrt2 = Math.sqrt(2);
        for(Double res : getViews().keySet()) {
            final GridEnvelope2D zone = getZones().get(res);
            int size = Math.max(zone.width, zone.height);
            if(size*res*sqrt2 < dmin) {
                continue;
            }
            final double res2 = res*res;
            final Raster v = getViews().get(res);     
            final Raster land = getDatas().get(res).getLand();
            final Point2D p = getCoords().get(res);
            for(int y = zone.y; y < zone.getMaxY(); y++) {
                for(int x = zone.x; x < zone.getMaxX(); x++) {
                    if(v.getSample(x, y, 0) == 1 && (unbounded || isInside(res2, p, x, y, dmin, dmax))) {
                        counts[land.getSample(x, y, 0)] += res2;
                    }
                }
            }
            //size = Math.min(zone.width, zone.height);
            if(size*res > dmax*2) {
                break;
            }
        }

        return counts;
    }

    @Override
    protected synchronized void calcViewLand() {
        ScaleData first = getDatas().firstEntry().getValue();
        GridGeometry2D firstGrid = first.getGridGeometry();
        view = getViews().firstEntry().getValue().createCompatibleWritableRaster(firstGrid.getGridRange2D());
        if(first.getLand() != null) {
            land = first.getLand().createCompatibleWritableRaster();
        }
        for(Double res : getViews().descendingKeySet()) {
            int[] tab = new int[0];
            GridGeometry2D grid = getDatas().get(res).getGridGeometry();
            Raster v = getViews().get(res);
            Raster l = getDatas().get(res).getLand();
            GridEnvelope2D zone = getZones().get(res);
            for(int y = zone.y; y < zone.getMaxY(); y++) {
                for(int x = zone.x; x < zone.getMaxX(); x++) {
                    try {
                        Rectangle r = firstGrid.worldToGrid(grid.gridToWorld(new GridEnvelope2D(x, y, 1, 1)));
                        if(!r.intersects(view.getBounds())) {
                            continue;
                        }
                        if(!view.getBounds().contains(r)) {
                            r = r.intersection(view.getBounds());
                        }
                        if(tab.length != r.width*r.height) {
                            tab = new int[r.width*r.height];
                        }
                        Arrays.fill(tab, v.getSample(x, y, 0));
                        view.setSamples(r.x, r.y, r.width, r.height, 0, tab);
                        if(land != null) {
                            Arrays.fill(tab, l.getSample(x, y, 0));
                            land.setSamples(r.x, r.y, r.width, r.height, 0, tab);
                        }
                    } catch (TransformException ex) {
                       throw new IllegalArgumentException(ex);
                    }
                }
            }
        }
        
    }
    
    @Override
    public Geometry getPolygon() {
        List<Geometry> geoms = new ArrayList<>();
        for(double res : getViews().keySet()) {
            Geometry poly = Vectorizer.vectorize(getViews().get(res), 1);
            poly.apply(getDatas().get(res).getGrid2World());
            geoms.add(poly);
        }
        return JTS.flattenGeometryCollection(new GeometryFactory().buildGeometry(geoms));
    }
}
