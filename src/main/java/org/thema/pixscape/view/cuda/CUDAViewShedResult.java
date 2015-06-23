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
import org.thema.pixscape.view.SimpleViewShedResult;

/**
 *
 * @author gvuidel
 */
class CUDAViewShedResult extends SimpleViewShedResult {
    
    private final ComputeViewCUDA.CUDAContext context;
    
    CUDAViewShedResult(GridCoordinates2D cg, ComputeViewCUDA.CUDAContext context, ComputeViewCUDA compute) {
        super(cg, null, compute);
        this.context = context;
    }

    @Override
    protected double[] calcAreaLandUnbounded() {
        double [] countLand = new double[getCodes().last() + 1];
        for (int code : getCodes()) {
            countLand[code] = context.getSumLandView((byte) code) * getRes2D()*getRes2D();
        }
        
        return countLand;
    }

    @Override
    protected double calcAreaUnbounded() {
        return context.getSumView() * getRes2D()*getRes2D();
    }

    @Override
    public synchronized Raster getView() {
        if (view == null) {
            view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, getGrid().getGridRange2D().width, getGrid().getGridRange2D().height, 1, null);
            byte[] viewBuf = ((DataBufferByte) view.getDataBuffer()).getData();
            context.getView(viewBuf);
        }
        return view;
    }
    
}
