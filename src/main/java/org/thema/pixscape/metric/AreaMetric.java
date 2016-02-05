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

import org.thema.pixscape.Bounds;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Area metric for planimetric and tangential view.
 * Calculates the sight area in square meter for planimetric and in square degree for tangential.
 * Support landuse codes and distance ranges.
 * For code support, code groups has no effect (ie. A[1,2] = A[1-2])
 * 
 * @author Gilles Vuidel
 */
public class AreaMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new AreaMetric
     */
    public AreaMetric() {
        super(true);
    }
    
    /**
     * Creates a new AreaMetric just for one landuse code
     * @param code the landuse code
     */
    public AreaMetric(int code) {
        this();
        addCode(code);
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
    protected final double calcMetric(ViewResult result, double dmin, double dmax) {
        if(getCodes().isEmpty()) {
            return Bounds.isUnboundedDistance(dmin, dmax) ? result.getArea() : result.getArea(dmin, dmax);
        } else {
            double[] count = result.getAreaLand(dmin, dmax);
            double sum = 0;
            for(int code : getCodes()) {
                sum += count[code];
            }
            return sum;
        }
    }
    
    @Override
    public String getShortName() {
        return "A";
    }
}
