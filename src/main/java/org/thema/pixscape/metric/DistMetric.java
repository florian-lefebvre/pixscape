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

import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Distance distribution metric for planimetric and tangential view.
 * Calculates the min,max,sum,average distances for pixels seen.
 * Does not support codes nor distance ranges.
 * @author Gilles Vuidel
 */
public class DistMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new DistMetric
     */
    public DistMetric() {
        super(false);
    }

    @Override
    public String getShortName() {
        return "DIST";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        double n = 0, sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        Raster view = result.getView();
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                if(view.getSample(x, y, 0) != 1) {
                    continue;
                }
                final double d = result.getCoord().distance(x, y) * result.getRes2D();
                n++;
                sum += d;
                if(d < min) {
                    min = d;
                }
                if(d > max) {
                    max = d;
                }
            }
        }
        
        return new Double[] {n, sum, sum/n, min, max};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        int[] view = ((DataBufferInt)result.getView().getDataBuffer()).getData();
        final int w = result.getGrid().getGridRange2D().width;
        double n = 0, sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < view.length; i++) {
            final int ind = view[i];
            if(ind == -1) {
                continue;
            }
            final int x = ind % w;
            final int y = ind / w;
            final double d = result.getCoord().distance(x, y) * result.getRes2D();
            n++;
            sum += d;
            if(d < min) {
                min = d;
            }
            if(d > max) {
                max = d;
            }
        }
        
        return new Double[] {n, sum, sum/n, min, max};
    }

    @Override
    public String[] getResultNames() {
        return new String [] {"DISTn", "DISTsum", "DISTavg", "DISTmin", "DISTmax"};
    }
    
    
}
