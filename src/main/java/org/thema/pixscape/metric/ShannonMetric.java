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
public class ShannonMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {calcShannon(result)};
    }
    
    @Override
    public Double[] calcMetric(ViewTanResult result) {
        return new Double[] {calcShannon(result)};
    }
    
    private double calcShannon(ViewResult result) {
        int[] count = result.getCountLand();
        double shannon = 0;
        double sum = 0;
        for(int code : getCodes(result)) 
            sum += count[code];
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
        return "SHAN";
    }
}
