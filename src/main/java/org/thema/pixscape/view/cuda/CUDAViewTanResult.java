/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view.cuda;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
class CUDAViewTanResult extends ViewTanResult {
    ComputeViewCUDA.CUDAContext context;

    CUDAViewTanResult(double ares, GridCoordinates2D cg, ComputeViewCUDA.CUDAContext context, ComputeViewCUDA compute) {
        super(ares, cg, null, compute);
        this.context = context;
    }

    @Override
    public int[] getCountLand() {
        if (countLand == null) {
            countLand = new int[getCodes().last() + 1];
            for (int code : getCodes()) {
                countLand[code] = context.getSumLandViewTan((byte) code);
            }
        }
        return countLand;
    }

    @Override
    public int getCount() {
        if (count == -1) {
            count = context.getSumViewTan();
        }
        return count;
    }

    @Override
    public Raster getView() {
        if (view == null) {
            int wa = (int) Math.ceil(2 * Math.PI / ares);
            int ha = (int) Math.ceil(Math.PI / ares);
            view = Raster.createBandedRaster(DataBuffer.TYPE_INT, wa, ha, 1, null);
            int[] viewBuf = ((DataBufferInt) view.getDataBuffer()).getData();
            context.getViewTan(viewBuf);
        }
        return view;
    }
    
}
