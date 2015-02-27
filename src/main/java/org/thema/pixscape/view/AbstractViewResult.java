
package org.thema.pixscape.view;

import org.geotools.coverage.grid.GridCoordinates2D;

/**
 * Base class for ViewResult.
 * 
 * @author Gilles Vuidel
 */
public abstract class AbstractViewResult implements ViewResult {
    private final GridCoordinates2D coord;
    private double area = -1;
    private double [] areaLand = null;

    /**
     * Creates a new Viewresult
     * @param coord the coordinate of the point of view or observed point in grid coordinate
     */
    public AbstractViewResult(GridCoordinates2D coord) {
        this.coord = coord;
    }
    
    @Override
    public final GridCoordinates2D getCoord() {
        return coord;
    }
    
    @Override
    public synchronized double getArea() {
        if(area == -1) {
            area = getArea(0, Double.POSITIVE_INFINITY);
        }
        return area;
    }

    @Override
    public synchronized double[] getAreaLand() {
        if(areaLand == null) {
            areaLand = getAreaLand(0, Double.POSITIVE_INFINITY);
        }
        return areaLand;
    }
    
    /**
     * Tests if distance interval is infinite.
     * @param dmin
     * @param dmax
     * @return true if dmin == 0 and dmax == +inf
     */
    protected final boolean isUnboundedDistance(double dmin, double dmax) {
        return dmin == 0 && dmax == Double.POSITIVE_INFINITY;
    }

}
