
package org.thema.pixscape.view;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Results from viewshed computation.
 * 
 * @author Gilles Vuidel
 */
public interface ViewShedResult extends ViewResult {
 
    /**
     * @return the full perimeter (including holes) of the viewshed in data unit
     */
    public double getPerimeter();
    
    /**
     * 
     * @return the viewshed in vector geometry
     */
    public Geometry getPolygon();
}
