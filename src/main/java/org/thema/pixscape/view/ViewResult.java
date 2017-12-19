/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.thema.pixscape.view;

import java.awt.image.Raster;
import java.util.Set;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.thema.pixscape.ScaleData;

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
     * Returns the area for all landuse code, if no codes are set, returns the total area
     * @return the view area in squared data unit for viewshed and squared degree for tangential view
     */
    double getAreaLandCodes(Set<Integer> codes);
    
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
     * @return the datascale used
     */
    ScaleData getData();
    
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
    
    /**
     * Returns the land use category of the pixel seen at this position
     * @param x the abscisse of the view
     * @param y the ordinate of the view
     * @return the and use category of the pixel seen at this position or -1 if nothing is visible
     */
    int getLand(int x, int y);
    
    /**
     * @return the landuse view of the seen pixels
     */    
    Raster getLanduseView();
}
