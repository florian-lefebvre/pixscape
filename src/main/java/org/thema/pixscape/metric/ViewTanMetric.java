/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import org.thema.pixscape.ComputeView.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public interface ViewTanMetric extends Metric {
    
    public double calcMetric(ViewTanResult result) ;
}
