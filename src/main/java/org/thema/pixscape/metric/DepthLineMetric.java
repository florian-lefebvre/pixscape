/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.Raster;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class DepthLineMetric extends AbstractMetric implements ViewTanMetric {

    public DepthLineMetric() {
        super(false);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult tanResult) {
        Raster view = tanResult.getView();
        final int w = view.getWidth();
        double len = 0;
        final double firstDist = tanResult.getMaxDistance(0);
        double precDist = firstDist;
        double area = 0;
        final double cosa = Math.cos(tanResult.getAres());
        final double sina = Math.sin(tanResult.getAres());
        for(int x = 1; x < w; x++) {
            double dist = tanResult.getMaxDistance(x);
            // alkachi theorem : a^2 = b^2*c^2 - 2*b*c*cos(alpha)
            len += Math.sqrt(Math.pow(dist, 2) + Math.pow(precDist, 2) - 2*dist*precDist*cosa);
            // sinus formula : area = b*c*sin(alpha)/2
            area += dist*precDist*sina / 2;
            precDist = dist;
        }
        
        // alkachi theorem : a^2 = b^2*c^2 - 2*b*c*cos(alpha)
        len += Math.sqrt(Math.pow(firstDist, 2) + Math.pow(precDist, 2) - 2*firstDist*precDist*cosa);
        // sinus formula : area = b*c*sin(alpha)/2
        area += firstDist*precDist*sina / 2;
        
        // in case the view is bounded in horizontal angle (< 360°)
        double coef = 2*Math.PI / (w*tanResult.getAres());
        
        return new Double[]{(len*coef) / (2 * Math.sqrt(Math.PI * area * coef))};
    }
    
    @Override
    public String getShortName() {
        return "DL";
    }
    
}
