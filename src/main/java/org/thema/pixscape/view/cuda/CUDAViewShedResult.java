/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.thema.pixscape.view.cuda;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.view.SimpleViewShedResult;

/**
 * SimpleViewShedResult implementation for CUDA GPU execution.
 * 
 * @author Gilles Vuidel
 */
final class CUDAViewShedResult extends SimpleViewShedResult {
    
    private final CUDAContext context;
    
    /**
     * Creates a new CUDAViewShedResult
     * @param cg the point of view or observed point in grid coordinate
     * @param context the CUDA context for calling CUDA functions
     * @param compute the CUDA compute view used
     */
    CUDAViewShedResult(GridCoordinates2D cg, CUDAContext context, ComputeViewCUDA compute) {
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
