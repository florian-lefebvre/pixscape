
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

    private final TreeMap<Double, Raster> views;
    private final TreeMap<Double, GridEnvelope2D> zones;
    private final MultiComputeViewJava compute;
    
    private final TreeMap<Double, Point2D> coords;
    
    protected WritableRaster view, land;

    /**
     * Creates a new MultiViewResult.
     * 
     * @param cg the point of view or observed point in grid coordinate
     * @param views the viewfor each scale
     * @param zones the zone where viewshed has been calculated for each scale
     * @param compute the compute view used
     */
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

    /**
     * @return the viewshed for each scale
     */
    public final TreeMap<Double, Raster> getViews() {
        return views;
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

    /**
     * Returns the landuse in the first scale grid geometry.
     * {@inheritDoc }
     */
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
    
    /**
     * Calculates the viewshed and the landuse at the first scale and stores them into view and land.
     */
    protected abstract void calcViewLand();

    /**
     * Calculates the distance between the point of view and (x, y), and checks if it is in the interval [dmin dmax[.
     * 
     * @param res the resolution of the data
     * @param x x grid coordinate at scale res
     * @param y y grid coordinate at scale res
     * @param dmin the minimal distance
     * @param dmax the maximal distance
     * @return true if the distance to the point (x, y) is in the interval [dmin dmax[
     */
    protected final boolean isInside(double res, int x, int y, double dmin, double dmax) {
        final Point2D p = coords.get(res);
        double d2 = res*res * (Math.pow(x-p.getX(), 2) + Math.pow(y-p.getY(), 2));
        return d2 >= dmin*dmin && d2 < dmax*dmax;
    }
}
