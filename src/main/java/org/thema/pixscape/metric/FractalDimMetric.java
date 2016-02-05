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
import org.thema.common.RasterImage;
import org.thema.common.swing.TaskMonitor;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.process.BoxCounting;

/**
 * Fractal dimension of the sight in planimetric view.
 * Calculates the fractal dimension with box counting method.
 * 
 * Does not support codes nor distance ranges.
 * @author Gilles Vuidel
 */
public class FractalDimMetric extends AbstractMetric implements ViewShedMetric {

    /**
     * Creates a new FractalDimMetric
     */
    public FractalDimMetric() {
        super(false);
    }
    
    @Override
    public Double[] calcMetric(ViewShedResult result) {
        Raster view = result.getView();
        BoxCounting boxCounting = new BoxCounting(new RasterImage(view));
        boxCounting.execute(new TaskMonitor.EmptyMonitor());
        return new Double[] {boxCounting.getDimension()};
    }
    
    
    @Override
    public String getShortName() {
        return "FD";
    }
}
