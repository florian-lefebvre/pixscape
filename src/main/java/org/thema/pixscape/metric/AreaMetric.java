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
public class AreaMetric extends AbstractDistMetric implements ViewShedMetric, ViewTanMetric {

    public AreaMetric() {
        super(true);
    }
    
    public AreaMetric(int code) {
        this();
        setCodes(new TreeSet<>(Arrays.asList(code)));
    }
    
    @Override
    protected double calcMetric(ViewShedResult result, double dmin, double dmax) {
        return calcArea(result, dmin, dmax);
    }

    @Override
    protected double calcMetric(ViewTanResult result, double dmin, double dmax) {
        return calcArea(result, dmin, dmax);
    }
    
    public double calcArea(ViewResult result, double dmin, double dmax) {
        if(getCodes().isEmpty()) {
            return result.getArea(dmin, dmax);
        } else {
            double[] count = result.getAreaLand(dmin, dmax);
            double sum = 0;
            for(int code : getCodes()) {
                sum += count[code];
            }
            return sum;
        }
    }
    
    @Override
    public String getShortName() {
        return "A";
    }
}
