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
 * Interface for planimetric metric.
 * 
 * @author Gilles Vuidel
 */
public interface ViewShedMetric extends Metric {
    
    /**
     * Calculates the metric based on planimetric result.
     * The size of the array equals to {@link #getResultNames() } size.
     * @param result the planimetric result
     * @return the metric results 
     */
    public Double [] calcMetric(ViewShedResult result);
}
