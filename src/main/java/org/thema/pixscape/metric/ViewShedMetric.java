
package org.thema.pixscape.metric;

import org.thema.pixscape.view.ViewShedResult;

/**
 * Interface for planimetric metric.
 * 
 * @author Gilles Vuidel
 */
public interface ViewShedMetric extends Metric {
    
    /**
     * Calculates the metric based on planimetric result.
     * The size of the array equals to {@link #getResultNames() } size.
     * @param result the planimetric result
     * @return the metric results 
     */
    public Double [] calcMetric(ViewShedResult result);
}
