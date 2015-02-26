/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.SortedSet;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class CONTAGMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    public CONTAGMetric() {
        super(true);
    }

    @Override
    public String getShortName() {
        return "CONTAG";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        int [] count = new int[256];
        int[][] border = new int[256][256];
        Raster view = result.getView();
        Raster land = result.getLanduse();

        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                if(view.getSample(x, y, 0) != 1) {
                    continue;
                }
                final int l = land.getSample(x, y, 0);
                count[l]++;
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) == 1) {
                    final int l1 = land.getSample(x+1, y, 0);
                    border[l][l1]++;
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) == 1) {
                    final int l1 = land.getSample(x, y+1, 0);
                    border[l][l1]++;
                }
                if(x > 0 && view.getSample(x-1, y, 0) == 1) {
                    final int l1 = land.getSample(x-1, y, 0);
                    border[l][l1]++;
                }
                if(y > 0 && view.getSample(x, y-1, 0) == 1) {
                    final int l1 = land.getSample(x, y-1, 0);
                    border[l][l1]++;
                }
            }
        }
        
        return new Double[] {calcCONTAG(result, border, count)};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        int [] count = new int[256];
        int[][] border = new int[256][256];
        Raster view = result.getView();
        byte[] land = ((DataBufferByte)result.getLanduse().getDataBuffer()).getData();

        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                final int ind = view.getSample(x, y, 0);
                if(ind == -1) {
                    continue;
                }
                final int l = land[ind];
                count[l]++;
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) != -1) {
                    final int l1 = land[view.getSample(x+1, y, 0)];
                    border[l][l1]++;
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) != -1) {
                    final int l1 = land[view.getSample(x, y+1, 0)];
                    border[l][l1]++;
                }
                if(x > 0 && view.getSample(x-1, y, 0) != -1) {
                    final int l1 = land[view.getSample(x-1, y, 0)];
                    border[l][l1]++;
                }
                if(y > 0 && view.getSample(x, y-1, 0) != -1) {
                    final int l1 = land[view.getSample(x, y-1, 0)];
                    border[l][l1]++;
                }
            }
        }
        
        return new Double[] {calcCONTAG(result, border, count)};
    }
    
    private double calcCONTAG(ViewResult result, int[][] border, int [] count) {
        final SortedSet<Integer> codes = getCodes(result);
        final int m = codes.size();
        if(m < 2) {
            return Double.NaN;
        }
        
        double tot = 0;
        for(int nb : count) {
            tot += nb;
        }
        
        int[] sumBorder = new int[256];
        for(int c1 : codes) {
            int sum = 0;
            int [] b = border[c1];
            for(int c2 : codes) {
                sum += b[c2];
            }
            sumBorder[c1] = sum;
        }
        
        double sum = 0;
        for(int c1 : codes) {
            for(int c2 : codes) {
                double val = count[c1]/tot * border[c1][c2];
                if(val > 0) {
                    sum += (val / sumBorder[c1]) * Math.log(val / sumBorder[c1]);
                }
            }
        }
        
        return (1 + sum / (2*Math.log(m))) * 100;
    }
}
