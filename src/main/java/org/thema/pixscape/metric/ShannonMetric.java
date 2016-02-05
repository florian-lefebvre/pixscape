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

import java.util.List;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Shannon entropy of landuse classes for planimetric and tangential view.
 * 
 * Calculates the Shannon entropy on the landuse seen.
 * Support landuse codes, landuse code groups and distance ranges.
 * @author Gilles Vuidel
 */
public class ShannonMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new ShannonMetric
     */
    public ShannonMetric() {
        super(true);
    }

    @Override
    public final Double[] calcMetric(ViewShedResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    protected double calcMetric(ViewResult result, double dmin, double dmax) {
        double[] count = result.getAreaLand(dmin, dmax);
        double shannon = 0;
        double sum = 0;
        for(int code : getCodes(result)) {
            sum += count[code];
        }
        int n = 0;
        if(!hasCodeGroup()) {
            for(int code : getCodes(result)) {
                final double nb = count[code];
                if(nb > 0) {
                    shannon += - nb/sum * Math.log(nb/sum);
                }
                n++;
            }
        } else {
            for(List<Integer> codes : getCodeGroups().values()) {
                double nb = 0;
                for(int code : codes) {
                    nb += count[code];
                }
                if(nb > 0) {
                    shannon += - nb/sum * Math.log(nb/sum);
                }
                n++;
            }
        }
        return shannon / Math.log(n);
    }

    @Override
    public String getShortName() {
        return "S";
    }
}
