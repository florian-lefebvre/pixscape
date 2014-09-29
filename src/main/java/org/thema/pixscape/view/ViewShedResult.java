/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 *
 * @author gvuidel
 */
public class ViewShedResult extends ViewResult {
    protected int[] countLand;

    public ViewShedResult(GridCoordinates2D cg, Raster view, ComputeView compute) {
        super(cg, view, compute);
    }

    @Override
    public synchronized int getCount() {
        if (count == -1) {
            byte[] buf = ((DataBufferByte) view.getDataBuffer()).getData();
            int nb = 0;
            for (int v : buf) {
                if (v == 1) {
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
            byte[] buf = ((DataBufferByte) view.getDataBuffer()).getData();
            for (int i = 0; i < buf.length; i++) {
                if (buf[i] == 1) {
                    count[getLanduse().getSample(i % w, i / w, 0)]++;
                }
            }
            countLand = count;
        }
        return countLand;
    }
    
}
