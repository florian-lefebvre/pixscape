
package org.thema.pixscape.metric;

import org.thema.pixscape.view.ViewShedResult;

/**
 * Compacity index for planimetric view.
 * Calculates the compacity index of the sight.
 * 
 * Does not support codes nor distance ranges.
 * 
 * @author Gilles Vuidel
 */
public class CompactMetric extends AbstractMetric implements ViewShedMetric {

    /**
     * Creates a new CompactMetric
     */
    public CompactMetric() {
        super(false);
    }
    
    @Override
    public Double[] calcMetric(ViewShedResult result) {
        double a = result.getArea();
        double p = result.getPerimeter();
        
        return new Double[] {p / (2 * Math.sqrt(Math.PI * a))};
    }
    
    @Override
    public String getShortName() {
        return "C";
    }
}
