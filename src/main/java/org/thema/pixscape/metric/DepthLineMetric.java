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


package org.thema.pixscape.metric;

import java.awt.image.Raster;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Compacity index of the shape formed by the sky line transposed on the "ground", for tangential view only.
 * Does not support codes nor distance ranges.
 * 
 * @author Gilles Vuidel
 */
public class DepthLineMetric extends AbstractMetric implements ViewTanMetric {

    /**
     * Creates a new DepthLineMetric
     */
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
