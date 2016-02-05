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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.RasterStyle;
import org.thema.pixscape.Project;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Special metric for saving the complete view in a raster file for planimetric and tangential view.
 * Use only for debugging purpose.
 * 
 * @author Gilles Vuidel
 */
public class RasterMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    private Project project;
    
    /**
     * Creates a new RasterMetric for the given project
     * @param project the current project
     */
    public RasterMetric(Project project) {
        super(false);
    }

    @Override
    public String getShortName() {
        return "Rast";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        try {
            Raster view = result.getView();
            RasterLayer l = new RasterLayer("rast", view, project.getDtmCov().getEnvelope2D());
            l.setCRS(project.getCRS());
            l.saveRaster(new File(project.getDirectory(), "viewshed-" + result.getCoord() + ".tif"));
            return new Double[]{1.0};
        } catch (IOException ex) {
            Logger.getLogger(RasterMetric.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new Double[]{0.0};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        try {
            Raster view = result.getView();
            RasterLayer l = new RasterLayer("rast", new RasterShape(view, view.getBounds(), new RasterStyle(), true));
            l.saveRaster(new File(project.getDirectory(), "viewtan-" + result.getCoord() + ".tif"));
            return new Double[]{1.0};
        } catch (IOException ex) {
            Logger.getLogger(RasterMetric.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new Double[]{0.0};
    }
    
}
