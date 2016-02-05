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

/**
 * Results from tangential view computation.
 * 
 * @author Gilles Vuidel
 */
public interface ViewTanResult extends ViewResult {
    
    /**
     * @return the angular resolution in radian
     */
    double getAres();

    /**
     * @return max value for theta1 (orientation) in pixel
     */
    int getThetaWidth();
    
    /**
     * @return max value for theta2 (z angle) in pixel
     */
    int getThetaHeight();
    
    /**
     * Returns the distance of the farthest seen pixel for the orientation theta1
     * @param theta1 the orientation in pixel [0 getThetaWidth()[
     * @return the maximal distance for the orientation theta1
     */
    double getMaxDistance(int theta1);
    
    /**
     * Returns the distance to the pixel seen at this position
     * @param theta1 the orientation in pixel [0 getThetaWidth()[
     * @param theta2 the z angle in pixel [0 getThetaHeight()[
     * @return the distance or NaN if nothing is visible at this position
     */
    double getDistance(int theta1, int theta2);
    
    
    /**
     * Returns the elevation of the pixel seen at this position
     * @param theta1 the orientation in pixel [0 getThetaWidth()[
     * @param theta2 the z angle in pixel [0 getThetaHeight()[
     * @return the elevation of the pixel seen at this position or NaN if nothing is visible
     */
    double getElevation(int theta1, int theta2);
    
    /**
     * @return the tangential view of the distance to the seen pixels
     */
    Raster getDistanceView();
    /**
     * @return the tangential view of the elevation of the seen pixels
     */    
    Raster getElevationView();
    /**
     * @return the tangential view of the landuse of the seen pixels
     */    
    Raster getLanduseView();
}
