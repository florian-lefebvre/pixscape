
package org.thema.pixscape.view;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.ScaleData;

/**
 * ComputeView for only one scale (resolution)
 * 
 * @author Gilles Vuidel
 */
public abstract class SimpleComputeView extends ComputeView {
    
    private final ScaleData data;

    /**
     * Creates a new SimpleComputeView.
     * @param data the data for this resolution
     * @param aPrec the precision in degree for tangential view
     */
    public SimpleComputeView(ScaleData data, double aPrec) {
        super(aPrec);
        this.data = data;
    }

    /**
     * 
     * @return the data for this scale
     */
    public final ScaleData getData() {
        return data;
    }
    
    /**
     * Transform a point from world coordinate to grid coordinate
     * @param p the point in world coordinate
     * @return the point in grid coordinate
     */
    protected final GridCoordinates2D getWorld2Grid(DirectPosition2D p) {
        try {
            return data.getGridGeometry().worldToGrid(p);
        } catch (TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
