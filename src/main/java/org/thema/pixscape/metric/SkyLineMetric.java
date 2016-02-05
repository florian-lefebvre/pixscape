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
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Skyline length ratio for tangential view only.
 * Calculates the ratio between the skyline length and a straight line.
 * Support distance ranges for intermediate skylines.
 * 
 * @author Gilles Vuidel
 */
public class SkyLineMetric extends AbstractDistMetric implements ViewTanMetric {

    /** 
     * Creates a new SkyLineMetric
     */
    public SkyLineMetric() {
        super(false);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult result) {
        return calcMetric((ViewResult)result);
    }

    @Override
    protected double calcMetric(ViewResult result, double dmin, double dmax) {
        final ViewTanResult tanResult = (ViewTanResult) result;
        boolean maxUnbounded = Double.isInfinite(dmax);
        Raster view = result.getView();
        final int w = view.getWidth();
        final int h = view.getHeight();
        double len = 0;
        int y = 0;
        while(y < h && view.getSample(0, y, 0) == -1 && (maxUnbounded || tanResult.getDistance(0, y) > dmax)) {
            y++;
        }
        int firstY = y;
        int precY = y;
        for(int x = 1; x < w; x++) {
            y = 0;
            while(y < h && view.getSample(x, y, 0) == -1 && (maxUnbounded || tanResult.getDistance(x, y) > dmax)) {
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
