/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.Raster;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class SkyLineMetric extends AbstractDistMetric implements ViewTanMetric {

    public SkyLineMetric() {
        super(false);
    }

    @Override
    protected double calcMetric(ViewShedResult result, double dmin, double dmax) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected double calcMetric(ViewTanResult result, double dmin, double dmax) {
        boolean maxUnbounded = Double.isInfinite(dmax);
        Raster view = result.getView();
        final int w = view.getWidth();
        final int h = view.getHeight();
        double len = 0;
        int y = 0;
        while(y < h && view.getSample(0, y, 0) == -1 && (maxUnbounded || result.getDistance(0, y) > dmax)) {
            y++;
        }
        int firstY = y;
        int precY = y;
        for(int x = 1; x < w; x++) {
            y = 0;
            while(y < h && view.getSample(x, y, 0) == -1 && (maxUnbounded || result.getDistance(x, y) > dmax)) {
                y++;
            } 
            len += Math.sqrt(Math.pow(y-precY, 2) + 1);
            precY = y;
        }
        len += Math.sqrt(Math.pow(y-firstY, 2) + 1);
        
        return len / w;
    }
    
    @Override
    public String getShortName() {
        return "SL";
    }
    
}
