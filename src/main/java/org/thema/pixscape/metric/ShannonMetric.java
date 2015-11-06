
package org.thema.pixscape.metric;

import java.util.List;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Shannon entropy of landuse classes for planimetric and tangential view.
 * 
 * Calculates the Shannon entropy on the landuse seen.
 * Support landuse codes, landuse code groups and distance ranges.
 * @author Gilles Vuidel
 */
public class ShannonMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new ShannonMetric
     */
    public ShannonMetric() {
        super(true);
    }

    @Override
    public final Double[] calcMetric(ViewShedResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    public final Double[] calcMetric(ViewTanResult result) {
        return calcMetric((ViewResult)result);
    }
    
    @Override
    protected double calcMetric(ViewResult result, double dmin, double dmax) {
        double[] count = result.getAreaLand(dmin, dmax);
        double shannon = 0;
        double sum = 0;
        for(int code : getCodes(result)) {
            sum += count[code];
        }
        int n = 0;
        if(!hasCodeGroup()) {
            for(int code : getCodes(result)) {
                final double nb = count[code];
                if(nb > 0) {
                    shannon += - nb/sum * Math.log(nb/sum);
                }
                n++;
            }
        } else {
            for(List<Integer> codes : getCodeGroups().values()) {
                double nb = 0;
                for(int code : codes) {
                    nb += count[code];
                }
                if(nb > 0) {
                    shannon += - nb/sum * Math.log(nb/sum);
                }
                n++;
            }
        }
        return shannon / Math.log(n);
    }

    @Override
    public String getShortName() {
        return "S";
    }
}
