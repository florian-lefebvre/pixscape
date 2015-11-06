
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
