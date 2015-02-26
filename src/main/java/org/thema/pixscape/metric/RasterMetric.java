/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author gvuidel
 */
public class RasterMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    public RasterMetric() {
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
            RasterLayer l = new RasterLayer("rast", view, Project.getProject().getDtmCov().getEnvelope2D());
            l.setCRS(Project.getProject().getCRS());
            l.saveRaster(new File(Project.getProject().getDirectory(), "viewshed-" + result.getCoord() + ".tif"));
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
            l.saveRaster(new File(Project.getProject().getDirectory(), "viewtan-" + result.getCoord() + ".tif"));
            return new Double[]{1.0};
        } catch (IOException ex) {
            Logger.getLogger(RasterMetric.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new Double[]{0.0};
    }
    
}
