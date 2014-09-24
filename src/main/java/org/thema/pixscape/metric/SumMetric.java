/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.TreeSet;
import org.thema.pixscape.ComputeView.ViewResult;
import org.thema.pixscape.ComputeView.ViewShedResult;
import org.thema.pixscape.ComputeView.ViewTanResult;

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
    public double calcMetric(ViewShedResult result) {
        return calcSum(result) * result.getRes2D()*result.getRes2D();
    }
    
    @Override
    public double calcMetric(ViewTanResult result) {
        return calcSum(result) * Math.pow(result.getAres()*180/Math.PI, 2);
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
