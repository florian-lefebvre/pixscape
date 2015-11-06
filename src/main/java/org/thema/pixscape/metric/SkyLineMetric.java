
package org.thema.pixscape.metric;

import java.awt.image.Raster;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Skyline length ratio for tangential view only.
 * Calculates the ratio between the skyline length and a straight line.
 * Support distance ranges for intermediate skylines.
 * 
 * @author Gilles Vuidel
 */
public class SkyLineMetric extends AbstractDistMetric implements ViewTanMetric {

    /** 
     * Creates a new SkyLineMetric
     */
    public SkyLineMetric() {
        super(false);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult result) {
        return calcMetric((ViewResult)result);
    }

    @Override
    protected double calcMetric(ViewResult result, double dmin, double dmax) {
        final ViewTanResult tanResult = (ViewTanResult) result;
        boolean maxUnbounded = Double.isInfinite(dmax);
        Raster view = result.getView();
        final int w = view.getWidth();
        final int h = view.getHeight();
        double len = 0;
        int y = 0;
        while(y < h && view.getSample(0, y, 0) == -1 && (maxUnbounded || tanResult.getDistance(0, y) > dmax)) {
            y++;
        }
        int firstY = y;
        int precY = y;
        for(int x = 1; x < w; x++) {
            y = 0;
            while(y < h && view.getSample(x, y, 0) == -1 && (maxUnbounded || tanResult.getDistance(x, y) > dmax)) {
                y++;
            } 
            len += Math.sqrt(Math.pow(y-precY, 2) + 1);
            precY = y;
        }
        len += Math.sqrt(Math.pow(y-firstY, 2) + 1);
        
        return len / w;
    }
    
    @Override
    public String getShortName() {
        return "SL";
    }
    
}
