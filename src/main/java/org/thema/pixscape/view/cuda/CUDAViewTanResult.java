
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
