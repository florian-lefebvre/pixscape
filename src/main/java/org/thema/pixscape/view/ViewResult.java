
package org.thema.pixscape.view;

import java.awt.image.Raster;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;

/**
 * Stores the result of a view calculation : viewshed or tangential view.
 * 
 * @author Gilles Vuidel
 */
public interface ViewResult {

    /**
     * @return the landuse codes associated with the datascales
     */
    SortedSet<Integer> getCodes();

    /**
     * @return the grid coordinate of the starting point (point of view or observed point)
     */
    GridCoordinates2D getCoord();

    /**
     * Returns the view area.
     * @return the view area in squared data unit for viewshed and squared degree for tangential view
     */
    double getArea();

    /**
     * Returns the area for each landuse code.
     * @return the view area in squared data unit for viewshed and squared degree for tangential view
     */
    double[] getAreaLand();
    
    /**
     * Returns the view area between distance dmin and dmax
     * @param dmin minimal distance (include)
     * @param dmax maximal distance (exclude)
     * @return the view area in squared data unit for viewshed and squared degree for tangential view
     */
    double getArea(double dmin, double dmax);

    /**
     * Returns the view area between distance dmin and dmax for each landuse code.
     * @param dmin minimal distance (include)
     * @param dmax maximal distance (exclude)
     * @return the view area in squared data unit for viewshed and squared degree for tangential view
     */
    double[] getAreaLand(double dmin, double dmax);

    /**
     * @return the grid geometry of the datascale
     */
    GridGeometry2D getGrid();

    /**
     * @return the resolution of the datascale
     */
    double getRes2D();

    /**
     * Returns the resulting view.
     * For viewshed, it is a Byte Raster with the same size than the scaledata containing one when the pixel is seen or sees, zero otherwise.
     * In some cases pixel value may be -1 for non tested pixels.
     * For tangential view, it is an Integer Raster containing the index of the pixel seen or -1 if no pixel is seen.
     * @return the view
     */
    Raster getView();
    
}
