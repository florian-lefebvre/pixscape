package org.thema.pixscape.view;

import java.awt.image.WritableRaster;

/**
 * Results from tangential view computation.
 * 
 * @author Gilles
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
     * Returns the landuse code seen at this position
     * @param theta1 the orientation in pixel [0 getThetaWidth()[
     * @param theta2 the z angle in pixel [0 getThetaHeight()[
     * @return the landuse code seen at this position or -1 if nothing is visible
     */
    int getLandUse(int theta1, int theta2);
    
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
    WritableRaster getDistanceView();
    /**
     * @return the tangential view of the elevation of the seen pixels
     */    
    WritableRaster getElevationView();
    /**
     * @return the tangential view of the landuse of the seen pixels
     */    
    WritableRaster getLanduseView();
}
