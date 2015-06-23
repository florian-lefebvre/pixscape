
package org.thema.pixscape.view;

import java.util.HashMap;
import java.util.Map;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 * Base class for ViewResult.
 * 
 * @author Gilles Vuidel
 */
public abstract class AbstractViewResult implements ViewResult {
    
    private static final Pair UNBOUND = new Pair(0, Double.POSITIVE_INFINITY);
    
    private final GridCoordinates2D coord;
    private double area = -1;
    private Map<Pair, double []> areaLand = null;

    /**
     * Creates a new Viewresult
     * @param coord the coordinate of the point of view or observed point in grid coordinate
     */
    public AbstractViewResult(GridCoordinates2D coord) {
        this.coord = coord;
        areaLand = new HashMap<>();
    }
    
    @Override
    public final GridCoordinates2D getCoord() {
        return coord;
    }
    
    @Override
    public synchronized double getArea() {
        if(area == -1) {
            area = getArea(UNBOUND.min, UNBOUND.max);
        }
        return area;
    }

    @Override
    public double[] getAreaLand() {
        return getAreaLand(UNBOUND.min, UNBOUND.max);
    }

    @Override
    public double[] getAreaLand(double dmin, double dmax) {
        final Pair pair = new Pair(dmin, dmax);
        synchronized(this) {
            if(!areaLand.containsKey(pair)) {
                areaLand.put(pair, calcAreaLand(dmin, dmax));
            }
        }
        return areaLand.get(pair);
    }
    
    protected abstract double[] calcAreaLand(double dmin, double dmax);
    

    private static class Pair {
        private double min, max;

        private Pair(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.min) ^ (Double.doubleToLongBits(this.min) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.max) ^ (Double.doubleToLongBits(this.max) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Pair other = (Pair) obj;
            if (Double.doubleToLongBits(this.min) != Double.doubleToLongBits(other.min)) {
                return false;
            }
            if (Double.doubleToLongBits(this.max) != Double.doubleToLongBits(other.max)) {
                return false;
            }
            return true;
        }
        
    }
}
