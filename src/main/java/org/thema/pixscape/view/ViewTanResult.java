/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.WritableRaster;

/**
 *
 * @author gvuidel
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
     * @return max value for theta2(z angle) in pixel
     */
    int getThetaHeight();
    
    double getMaxDistance(int theta1);
    
    double getDistance(int theta1, int theta2);
    
    int getLandUse(int theta1, int theta2);
    
    double getElevation(int theta1, int theta2);
    
    WritableRaster getDistanceView();
    
    WritableRaster getElevationView();
    
    WritableRaster getLanduseView();
}
