/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import org.thema.pixscape.view.ViewShedResult;

/**
 *
 * @author gvuidel
 */
public class CompactMetric extends AbstractMetric implements ViewShedMetric {

    public CompactMetric() {
        super(false);
    }
    

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        double a = result.getArea();
        double p = result.getPerimeter();
        
        return new Double[] {p / (2 * Math.sqrt(Math.PI * a))};
    }
    
    @Override
    public String getShortName() {
        return "C";
    }
}
