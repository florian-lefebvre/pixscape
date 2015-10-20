/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.List;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class ShannonMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

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
