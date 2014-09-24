/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.thema.pixscape.ComputeView;

/**
 *
 * @author gvuidel
 */
public class IJIMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    @Override
    public String getShortName() {
        return "IJI";
    }

    @Override
    public double calcMetric(ComputeView.ViewShedResult result) {
        int[][] border = new int[256][256];
        Raster view = result.getView();
        Raster land = result.getLanduse();
        int tot = 0;
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                if(view.getSample(x, y, 0) != 1)
                    continue;
                final int l = land.getSample(x, y, 0);
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) == 1) {
                    final int l1 = land.getSample(x+1, y, 0);
                    if(l != l1) {
                        if(l < l1)
                            border[l][l1]++;
                        else 
                            border[l1][l]++;
                        tot++;
                    }
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) == 1) {
                    final int l1 = land.getSample(x, y+1, 0);
                    if(l != l1) {
                        if(l < l1)
                            border[l][l1]++;
                        else 
                            border[l1][l]++;
                        tot++;
                    }
                }
            }
        }
        
        return calcIJI(result, border, tot);
    }

    @Override
    public double calcMetric(ComputeView.ViewTanResult result) {
        int[][] border = new int[256][256];
        Raster view = result.getView();
        byte[] land = ((DataBufferByte)result.getLanduse().getDataBuffer()).getData();
        int tot = 0;
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                final int ind = view.getSample(x, y, 0);
                if(ind == -1)
                    continue;
                final int l = land[ind];
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) != -1) {
                    final int l1 = land[view.getSample(x+1, y, 0)];
                    if(l != l1) {
                        if(l < l1)
                            border[l][l1]++;
                        else 
                            border[l1][l]++;
                        tot++;
                    }
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) != -1) {
                    final int l1 = land[view.getSample(x, y+1, 0)];
                    if(l != l1) {
                        if(l < l1)
                            border[l][l1]++;
                        else 
                            border[l1][l]++;
                        tot++;
                    }
                }
            }
        }
        
        return calcIJI(result, border, tot);
    }
    
    private double calcIJI(ComputeView.ViewResult result, int[][] border, int tot) {
        int m = getCodes(result).size();
        if(tot == 0 || m < 3)
            return Double.NaN;
        
        double sum = 0;
        for(int c1 : getCodes(result)) {
            for(int c2 : getCodes(result).tailSet(c1)) {
                double val = border[c1][c2];
                if(val > 0)
                    sum += -(val / tot) * Math.log(val / tot);
            }
        }
        
        return sum / Math.log(0.5*m*(m-1)) * 100;
    }
}
