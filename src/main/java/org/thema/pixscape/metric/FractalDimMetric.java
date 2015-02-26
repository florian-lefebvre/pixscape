/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.Raster;
import org.thema.common.RasterImage;
import org.thema.common.swing.TaskMonitor;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.process.BoxCounting;

/**
 *
 * @author Gilles Vuidel
 */
public class FractalDimMetric extends AbstractMetric implements ViewShedMetric {

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
