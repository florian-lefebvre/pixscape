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

import org.thema.pixscape.view.ViewShedResult;

/**
 * Compacity index for planimetric view.
 * Calculates the compacity index of the sight.
 * 
 * Does not support codes nor distance ranges.
 * 
 * @author Gilles Vuidel
 */
public class CompactMetric extends AbstractMetric implements ViewShedMetric {

    /**
     * Creates a new CompactMetric
     */
    public CompactMetric() {
        super(false);
    }
    
    @Override
    public Double[] calcMetric(ViewShedResult result) {
        double a = result.getArea();
        double p = result.getPerimeter();
        
        return new Double[] {p / (2 * Math.sqrt(Math.PI * a))};
    }
    
    @Override
    public String getShortName() {
        return "C";
    }
}
