/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class DistMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    public DistMetric() {
        super(false);
    }

    @Override
    public String getShortName() {
        return "DIST";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        double n = 0, sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        Raster view = result.getView();
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                if(view.getSample(x, y, 0) != 1) {
                    continue;
                }
                final double d = result.getCoord().distance(x, y) * result.getRes2D();
                n++;
                sum += d;
                if(d < min) {
                    min = d;
                }
                if(d > max) {
                    max = d;
                }
            }
        }
        
        return new Double[] {n, sum, sum/n, min, max};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        int[] view = ((DataBufferInt)result.getView().getDataBuffer()).getData();
        final int w = result.getGrid().getGridRange2D().width;
        double n = 0, sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < view.length; i++) {
            final int ind = view[i];
            if(ind == -1) {
                continue;
            }
            final int x = ind % w;
            final int y = ind / w;
            final double d = result.getCoord().distance(x, y) * result.getRes2D();
            n++;
            sum += d;
            if(d < min) {
                min = d;
            }
            if(d > max) {
                max = d;
            }
        }
        
        return new Double[] {n, sum, sum/n, min, max};
    }

    @Override
    public String[] getResultNames() {
        return new String [] {"DISTn", "DISTsum", "DISTavg", "DISTmin", "DISTmax"};
    }
    
    
}
