/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

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
    protected double calcMetric(ViewShedResult result, double dmin, double dmax) {
        return calcShannon(result, dmin, dmax);
    }

    @Override
    protected double calcMetric(ViewTanResult result, double dmin, double dmax) {
        return calcShannon(result, dmin, dmax);
    }
    
    private double calcShannon(ViewResult result, double dmin, double dmax) {
        double[] count = result.getAreaLand(dmin, dmax);
        double shannon = 0;
        double sum = 0;
        for(int code : getCodes(result)) {
            sum += count[code];
        }
        for(int code : getCodes(result)) {
            final double nb = count[code];
            if(nb > 0) {
                shannon += - nb/sum * Math.log(nb/sum);
            }
        }
        return shannon/Math.log(getCodes(result).size());
    }

    @Override
    public String getShortName() {
        return "S";
    }
}
