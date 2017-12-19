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


package org.thema.pixscape.view;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Results from viewshed computation.
 * 
 * @author Gilles Vuidel
 */
public interface ViewShedResult extends ViewResult {
 
    
    /**
     * @return the full perimeter (including holes) of the viewshed in data unit
     */
    double getPerimeter();
    
    /**
     * 
     * @return the viewshed in vector geometry
     */
    Geometry getPolygon();
}
