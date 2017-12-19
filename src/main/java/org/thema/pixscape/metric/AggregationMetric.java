/*
 * Copyright (C) 2017 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class AggregationMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    public AggregationMetric() {
        super(true);
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {calcAgg(result, false)};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        return new Double[] {calcAgg(result, result.isView360())};
    }
    
    @Override
    public String getShortName() {
        return "AG";
    }

    private Double calcAgg(ViewResult result, boolean cylinder) {
        int [] count = new int[256];
        int [] border = new int[256];
        Raster land = result.getLanduseView();

        for(int y = 0; y < land.getHeight(); y++) {
            for(int x = 0; x < land.getWidth(); x++) {
                final int l = land.getSample(x, y, 0);
                if(l == -1) {
                    continue;
                }
                count[l]++;
                if(x < land.getWidth()-1) {
                    final int l1 = land.getSample(x+1, y, 0);
                    if(l == l1) {
                        border[l]++;
                    }
                } else if(cylinder) {
                    final int l1 = land.getSample(0, y, 0);
                    if(l == l1) {
                        border[l]++;
                    }
                }
                if(y < land.getHeight()-1) {
                    final int l1 = land.getSample(x, y+1, 0);
                    if(l == l1) {
                        border[l]++;
                    }
                }
            }
        }
        
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("AG does not support land category groups");
        }
        int a = 0;
        double sum = 0;
        for(int code : getCodes(result)) {
            int ai = count[code];
            if(ai == 0) {
                continue;
            }
            int n = (int) Math.sqrt(ai);
            int m = ai - n*n;
            double max;
            if(m == 0) {
                max = 2*n * (n-1);
            } else if(m <= n) {
                max = 2*n * (n-1) + 2*m-1;
            } else {
                max = 2*n * (n-1) + 2*m-2;
            }
            sum += border[code] / max * ai;
            a += ai;
        }
        
        if(a == 0) {
            return 0.0;
        }
        
        return sum / a * 100;
    }
    
}
