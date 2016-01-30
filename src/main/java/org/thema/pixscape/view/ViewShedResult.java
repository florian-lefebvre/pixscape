
package org.thema.pixscape.view;

import com.vividsolutions.jts.geom.Geometry;
import java.awt.image.Raster;

/**
 * Results from viewshed computation.
 * 
 * @author Gilles Vuidel
 */
public interface ViewShedResult extends ViewResult {
 
    /**
     * @return the landuse included in the datascale
     */
    Raster getLanduse();
    
    /**
     * @return the full perimeter (including holes) of the viewshed in data unit
     */
    double getPerimeter();
    
    /**
     * 
     * @return the viewshed in vector geometry
     */
    Geometry getPolygon();
}
