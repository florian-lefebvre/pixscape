/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.SortedSet;
import org.thema.pixscape.ComputeView.ViewResult;
import org.thema.pixscape.ComputeView.ViewShedResult;
import org.thema.pixscape.ComputeView.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class ShannonMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    @Override
    public double calcMetric(ViewShedResult result) {
        return calcShannon(result);
    }
    
    @Override
    public double calcMetric(ViewTanResult result) {
        return calcShannon(result);
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
