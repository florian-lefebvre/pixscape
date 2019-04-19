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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * ComputeView for only one scale (resolution)
 * 
 * @author Gilles Vuidel
 */
public abstract class SimpleComputeView extends ComputeView {
    
    private final ScaleData data;

    /**
     * Creates a new SimpleComputeView.
     * @param data the data for this resolution
     * @param aPrec the precision in degree for tangential view
     * @param earthCurv take into account earth curvature ?
     * @param coefRefraction refraction correction, 0 for no correction
     */
    public SimpleComputeView(ScaleData data, double aPrec, boolean earthCurv, double coefRefraction) {
        super(aPrec, earthCurv, coefRefraction);
        this.data = data;
    }
    
    /**
     * 
     * @return the data for this scale
     */
    public final ScaleData getData() {
        return data;
    }
    
    /**
     * Transform a point from world coordinate to grid coordinate
     * @param p the point in world coordinate
     * @return the point in grid coordinate
     */
    protected final GridCoordinates2D getWorld2Grid(DirectPosition2D p) {
        return data.getWorld2Grid(p);
    }
}
