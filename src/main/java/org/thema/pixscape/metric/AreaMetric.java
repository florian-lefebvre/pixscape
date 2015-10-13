/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import org.thema.pixscape.Bounds;
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
        addCode(code);
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
    protected final double calcMetric(ViewResult result, double dmin, double dmax) {
        if(getCodes().isEmpty()) {
            return Bounds.isUnboundedDistance(dmin, dmax) ? result.getArea() : result.getArea(dmin, dmax);
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
