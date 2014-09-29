/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view.cuda;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.view.ViewShedResult;

/**
 *
 * @author gvuidel
 */
class CUDAViewShedResult extends ViewShedResult {
    ComputeViewCUDA.CUDAContext context;

    CUDAViewShedResult(GridCoordinates2D cg, ComputeViewCUDA.CUDAContext context, ComputeViewCUDA compute) {
        super(cg, null, compute);
        this.context = context;
    }

    @Override
    public int[] getCountLand() {
        if (countLand == null) {
            countLand = new int[getCodes().last() + 1];
            for (int code : getCodes()) {
                countLand[code] = context.getSumLandView((byte) code);
            }
        }
        return countLand;
    }

    @Override
    public int getCount() {
        if (count == -1) {
            count = context.getSumView();
        }
        return count;
    }

    @Override
    public Raster getView() {
        if (view == null) {
            view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, getDtm().getWidth(), getDtm().getHeight(), 1, null);
            byte[] viewBuf = ((DataBufferByte) view.getDataBuffer()).getData();
            context.getView(viewBuf);
        }
        return view;
    }
    
}
