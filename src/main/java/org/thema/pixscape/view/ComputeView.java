
package org.thema.pixscape.view;

import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 * Base class for all visibility computing.
 * 
 * @author Gilles Vuidel
 */
public abstract class ComputeView {
    
    /**
     * Alpha precision in radian
     */
    protected double aPrec;

    /**
     * Creates a new instance of ComputeView
     * @param aPrec the precision in degree for tangential view
     */
    public ComputeView(double aPrec) {
        setaPrec(aPrec);
    }
    
    /**
     * Calculate the viewshed from cg and calculate all the metrics on this viewshed.
     * 
     * @param cg the point of view if direct=true, the observed point otherwise. cg is in world coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param direct if true, observer is on cg, else observed point is on cg
     * @param bounds the limits of the viewshed
     * @param metrics the metrics to calculate
     * @return the results of the metrics
     */
    public List<Double[]> aggrViewShed(DirectPosition2D cg, double startZ, double destZ, boolean direct, Bounds bounds, List<? extends ViewShedMetric> metrics) {
        
        ViewShedResult view = calcViewShed(cg, startZ, destZ, direct, bounds); 
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewShedMetric m : metrics) {
            results.add(m.calcMetric(view));
        }
        return results;
    }
    
    /**
     * Calculate the tangential view from cg and calculate all the metrics on this view.
     * 
     * @param cg the point of view in world coordinate
     * @param startZ the height of the eye of the observer
     * @param bounds the limits of the viewshed
     * @param metrics the metrics to calculate
     * @return the results of the metrics
     */
    public List<Double[]> aggrViewTan(DirectPosition2D cg, double startZ, Bounds bounds, List<? extends ViewTanMetric> metrics) {
        
        ViewTanResult view = calcViewTan(cg, startZ, bounds);
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewTanMetric m : metrics) {
            results.add(m.calcMetric(view));
        }
        return results;
    }
    
    /**
     * Calculate the tangential view from cg.
     * 
     * @param cg the point of view in world coordinate
     * @param startZ the height of the eye of the observer
     * @param bounds the limits of the viewshed
     * @return the resulting tangential view
     */
    public abstract ViewTanResult calcViewTan(DirectPosition2D cg, double startZ, Bounds bounds) ;
    
    
    /**
     * Calculate the viewshed from cg.
     * 
     * @param cg the point of view if direct=true, the observed point otherwise. cg is in world coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param direct if true, observer is on cg, else observed point is on cg
     * @param bounds the limits of the viewshed
     * @return the resulting viewshed
     */
    public abstract ViewShedResult calcViewShed(DirectPosition2D cg, double startZ, double destZ, boolean direct, Bounds bounds) ;
    
    
    /**
     * Default implementation does nothing
     */
    public void dispose() {
        
    }

    /**
     * Alpha precision in degree for tangential view.
     * 
     * @return the precision in degree for tangential view
     */
    public double getaPrec() {
        return aPrec * 180 / Math.PI;
    }

    /**
     * Set the precision in degree for tangential view.
     * @param aPrec alpha precision in degree
     */
    public final void setaPrec(double aPrec) {
        this.aPrec = aPrec * Math.PI / 180;
    }
    
    
}
