
package org.thema.pixscape.metric;

import org.thema.pixscape.Bounds;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Area metric for planimetric and tangential view.
 * Calculates the sight area in square meter for planimetric and in square degree for tangential.
 * Support landuse codes and distance ranges.
 * For code support, code groups has no effect (ie. A[1,2] = A[1-2])
 * 
 * @author Gilles Vuidel
 */
public class AreaMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new AreaMetric
     */
    public AreaMetric() {
        super(true);
    }
    
    /**
     * Creates a new AreaMetric just for one landuse code
     * @param code the landuse code
     */
    public AreaMetric(int code) {
        this();
        addCode(code);
    }
    
    @Override
    public final Double[] calcMetric(ViewShedResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    protected final double calcMetric(ViewResult result, double dmin, double dmax) {
        if(getCodes().isEmpty()) {
            return Bounds.isUnboundedDistance(dmin, dmax) ? result.getArea() : result.getArea(dmin, dmax);
        } else {
            double[] count = result.getAreaLand(dmin, dmax);
            double sum = 0;
            for(int code : getCodes()) {
                sum += count[code];
            }
            return sum;
        }
    }
    
    @Override
    public String getShortName() {
        return "A";
    }
}
