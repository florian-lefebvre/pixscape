/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.TreeSet;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class SumMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    public SumMetric() {
    }
    
    public SumMetric(int code) {
        setCodes(new TreeSet<>(Arrays.asList(code)));
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {calcSum(result) * result.getRes2D()*result.getRes2D()};
    }
    
    @Override
    public Double[] calcMetric(ViewTanResult result) {
        return new Double[] {calcSum(result) * Math.pow(result.getAres()*180/Math.PI, 2)};
    }
    
    public int calcSum(ViewResult result) {
        if(getCodes().isEmpty()) {
            return result.getCount();
        } else {
            int[] count = result.getCountLand();
            int sum = 0;
            for(int code : getCodes()) 
                sum += count[code];
            return sum;
        }
    }
    
    @Override
    public String getShortName() {
        return "SUM";
    }
}
