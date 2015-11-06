
package org.thema.pixscape.metric;

import org.thema.pixscape.view.ViewTanResult;

/**
 * Interface for tangential metric.
 * 
 * @author Gilles Vuidel
 */
public interface ViewTanMetric extends Metric {
    
    /**
     * Calculates the metric based on tangential result.
     * The size of the array equals to {@link #getResultNames() } size.
     * @param result the tangential result
     * @return the metric results 
     */
    public Double [] calcMetric(ViewTanResult result);
}
