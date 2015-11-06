
package org.thema.pixscape.view;

import java.awt.image.Raster;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.thema.pixscape.ScaleData;

/**
 * Base class for ViewResult at only one scale.
 * 
 * @author Gilles Vuidel
 */
public abstract class SimpleViewResult extends AbstractViewResult {
    
    protected final SimpleComputeView compute;
    private final int w;
    
    /**
     * The resulting view
     */
    protected Raster view;
    
    /**
     * Creates a new SimpleViewResult
     * @param cg the point of view or observed point in grid coordinate
     * @param view the resulting view, may be null
     * @param compute the compute view used
     */
    public SimpleViewResult(GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg);
        this.compute = compute;
        this.view = view;
        w = getGrid().getGridRange2D().width;
    }

    @Override
    public Raster getView() {
        return view;
    }

    @Override
    public final Raster getLanduse() {
        return compute.getData().getLand();
    }

    @Override
    public final GridGeometry2D getGrid() {
        return compute.getData().getGridGeometry();
    }

    @Override
    public final double getRes2D() {
        return compute.getData().getResolution();
    }

    @Override
    public final SortedSet<Integer> getCodes() {
        return compute.getData().getCodes();
    }
    
    /**
     * @return the datascale used
     */
    public final ScaleData getData() {
        return compute.getData();
    }
    
    /**
     * Calculates the distance between the point of view and (x, y), and checks if it is in the interval [dmin dmax[.
     * 
     * @param x x in grid coordinate
     * @param y y in grid coordinate
     * @param dmin the minimal distance
     * @param dmax the maximal distance
     * @return true if the distance to the point (x, y) is in the interval [dmin dmax[
     */
    protected final boolean isInside(int x, int y, double dmin, double dmax) {
        double d2 = getRes2D()*getRes2D() * (Math.pow(x-getCoord().x, 2) + Math.pow(y-getCoord().y, 2));
        return d2 >= dmin*dmin && d2 < dmax*dmax;
    }
    
    /**
     * @return the width of the grid geometry of the data
     */
    protected final int getW() {
        return w;
    }

}
