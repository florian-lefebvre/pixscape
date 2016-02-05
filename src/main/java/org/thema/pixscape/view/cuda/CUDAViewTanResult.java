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
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.view.SimpleViewTanResult;

/**
 * SimpleViewTanResult implementation for CUDA GPU execution.
 * 
 * @author Gilles Vuidel
 */
final class CUDAViewTanResult extends SimpleViewTanResult {
    
    private final CUDAContext context;

    /**
     * Creates a new CUDAViewTanResult
     * @param cg the point of view or observed point in grid coordinate
     * @param context the CUDA context for calling CUDA functions
     * @param compute the CUDA compute view used
     */
    CUDAViewTanResult(GridCoordinates2D cg, CUDAContext context, ComputeViewCUDA compute) {
        super(cg, null, compute);
        this.context = context;
    }

    @Override
    protected double[] calcAreaLandUnbounded() {
        double [] countLand = new double[getCodes().last() + 1];
        for (int code : getCodes()) {
            countLand[code] = context.getSumLandViewTan((byte) code) * Math.pow(getAres()*180/Math.PI, 2);
        }

        return countLand;
    }

    @Override
    protected double calcAreaUnbounded() {
        return context.getSumViewTan() * Math.pow(getAres()*180/Math.PI, 2);
    }

    @Override
    public synchronized Raster getView() {
        if (view == null) {
            view = Raster.createBandedRaster(DataBuffer.TYPE_INT, context.getWa(), context.getHa(), 1, null);
            int[] viewBuf = ((DataBufferInt) view.getDataBuffer()).getData();
            context.getViewTan(viewBuf);
        }
        return view;
    }
    
}
