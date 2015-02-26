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
public class PerimeterMetric extends AbstractMetric implements ViewShedMetric {

    public PerimeterMetric() {
        super(false);
    }
    
    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {result.getPerimeter()};
    }
    
    
    @Override
    public String getShortName() {
        return "P";
    }
}
