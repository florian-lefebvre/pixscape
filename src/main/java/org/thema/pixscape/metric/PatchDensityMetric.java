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

import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class PatchDensityMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {
    
    
    public PatchDensityMetric() {
        super(true);
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("PD does not support land category groups");
        }
        return new Double[] {result.getNbPatch(getCodes()) / result.getArea()};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("PD does not support land category groups");
        }
        return new Double[] {result.getNbPatch(getCodes()) / result.getArea()};
    }

    @Override
    public String getShortName() {
        return "PD";
    }
    
}
