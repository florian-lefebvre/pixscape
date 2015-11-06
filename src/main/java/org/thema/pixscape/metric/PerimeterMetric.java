
package org.thema.pixscape.metric;

import org.thema.pixscape.view.ViewShedResult;

/**
 * Perimeter of the sight (including holes) for planimetric view only.
 * 
 * Does not support codes nor distance ranges.
 * @author Gilles Vuidel
 */
public class PerimeterMetric extends AbstractMetric implements ViewShedMetric {

    /**
     * Creates a new PerimeterMetric
     */
    public PerimeterMetric() {
        super(false);
    }
    
    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {result.getPerimeter()};
    }
    
    
    @Override
    public String getShortName() {
        return "P";
    }
}
