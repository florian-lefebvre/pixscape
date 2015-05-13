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
import org.thema.pixscape.view.SimpleViewTanResult;

/**
 *
 * @author gvuidel
 */
class CUDAViewTanResult extends SimpleViewTanResult {
    
    private final ComputeViewCUDA.CUDAContext context;

    CUDAViewTanResult(double ares, GridCoordinates2D cg, ComputeViewCUDA.CUDAContext context, ComputeViewCUDA compute) {
        super(ares, cg, null, compute);
        this.context = context;
    }

    @Override
    protected double[] getAreaLandUnbounded() {
        double [] countLand = new double[getCodes().last() + 1];
        for (int code : getCodes()) {
            countLand[code] = context.getSumLandViewTan((byte) code) * Math.pow(getAres()*180/Math.PI, 2);
        }

        return countLand;
    }

    @Override
    public double getAreaUnbounded() {
        return context.getSumViewTan() * Math.pow(getAres()*180/Math.PI, 2);
    }

    @Override
    public Raster getView() {
        if (view == null) {
            int wa = (int) Math.ceil(2 * Math.PI / getAres());
            int ha = (int) Math.ceil(Math.PI / getAres());
            view = Raster.createBandedRaster(DataBuffer.TYPE_INT, wa, ha, 1, null);
            int[] viewBuf = ((DataBufferInt) view.getDataBuffer()).getData();
            context.getViewTan(viewBuf);
        }
        return view;
    }
    
}
