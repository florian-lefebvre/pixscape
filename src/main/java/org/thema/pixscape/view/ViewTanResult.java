/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 *
 * @author gvuidel
 */
public class ViewTanResult extends ViewResult {
    protected double ares;
    protected int[] countLand;

    public ViewTanResult(double ares, GridCoordinates2D cg, Raster view, ComputeView compute) {
        super(cg, view, compute);
        this.ares = ares;
    }

    public double getAres() {
        return ares;
    }

    @Override
    public synchronized int getCount() {
        if (count == -1) {
            int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
            int nb = 0;
            for (int ind : buf) {
                if (ind > -1) {
                    nb++;
                }
            }
            count = nb;
        }
        return count;
    }

    @Override
    public synchronized int[] getCountLand() {
        if (countLand == null) {
            final int[] count = new int[256];
            final int w = getDtm().getWidth();
            int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
            for (int i = 0; i < buf.length; i++) {
                if (buf[i] > -1) {
                    final int ind = buf[i];
                    count[getLanduse().getSample(ind % w, ind / w, 0)]++;
                }
            }
            countLand = count;
        }
        return countLand;
    }
    
}
