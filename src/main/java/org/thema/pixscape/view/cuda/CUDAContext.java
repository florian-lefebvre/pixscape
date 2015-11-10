
package org.thema.pixscape.view.cuda;

import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;
import static jcuda.driver.JCudaDriver.cuCtxSynchronize;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * Class storing the CUDA context for one GPU device, the function and GPU memory pointers.
 * 
 * @author Gilles Vuidel
 */
class CUDAContext {
    private final int NCORE = 512;
    
    private final ScaleData data;
    private Raster dtm;
    private float[] dtmBuf;
    private float[] dsmBuf;
    private byte[] landBuf;
    private final int nDev;
    private final int w;
    private final int h;
    private final int size;
    private final int sumSize = NCORE;
    private int wa;
    private int ha;
    private CUcontext ctx;
    private CUfunction funViewShed;
    private CUfunction funViewTan;
    private CUfunction funViewShedInd;
    private CUfunction funViewShedBounded;
    private CUfunction funViewShedIndBounded;
    private CUfunction funSumV;
    private CUfunction funSumVT;
    private CUfunction funSumLandV;
    private CUfunction funSumLandVT;
    private CUfunction funClearView;
    private CUfunction funClearViewTan;
    private CUdeviceptr dtmDev;
    private CUdeviceptr landDev;
    private CUdeviceptr dsmDev;
    private CUdeviceptr sumDev;
    private CUdeviceptr viewDev;
    private CUdeviceptr viewTanDev;

    /**
     * Creates a new CUDAContext for the given GPU device index.
     * Loads functions, allocate GPU memory and copy scaledata to GPU memory.
     * @param data the monoscale data
     * @param nDev the GPU device index
     * @throws IOException 
     */
    public CUDAContext(ScaleData data, int nDev) throws IOException {
        this.nDev = nDev;
        this.data = data;
        CUdevice device = new CUdevice();
        JCudaDriver.cuDeviceGet(device, nDev);
        ctx = new CUcontext();
        JCudaDriver.cuCtxCreate(ctx, 0, device);
        // Load the ptx file.
        CUmodule module = new CUmodule();
        File ptxFile = File.createTempFile("view_kernel", ".ptx");
        extract("/view_kernel.ptx", ptxFile);
        JCudaDriver.cuModuleLoad(module, ptxFile.getAbsolutePath());
        // Obtain a functions pointer
        funViewShed = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funViewShed, module, "calcRayDirect");
        funViewTan = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funViewTan, module, "calcRayTan");
        funViewShedInd = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funViewShedInd, module, "calcRayIndirect");
        funViewShedBounded = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funViewShedBounded, module, "calcRayDirectBounded");
        funViewShedIndBounded = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funViewShedIndBounded, module, "calcRayIndirectBounded");
        funSumV = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funSumV, module, "sumView");
        funSumVT = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funSumVT, module, "sumViewTan");
        funSumLandV = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funSumLandV, module, "sumLandView");
        funSumLandVT = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funSumLandVT, module, "sumLandViewTan");
        funClearView = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funClearView, module, "clearView");
        funClearViewTan = new CUfunction();
        JCudaDriver.cuModuleGetFunction(funClearViewTan, module, "clearViewTan");
        dtm = data.getDtm();
        dtmBuf = ((DataBufferFloat) dtm.getDataBuffer()).getData();
        if (data.hasLandUse()) {
            landBuf = ((DataBufferByte) data.getLand().getDataBuffer()).getData();
        }
        if (data.getDsm() != null) {
            dsmBuf = ((DataBufferFloat) data.getDsm().getDataBuffer()).getData();
        }
        w = dtm.getWidth();
        h = dtm.getHeight();
        size = w * h;
        // Allocate the device input data, and copy the
        // host input data to the device
        dtmDev = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(dtmDev, size * Sizeof.FLOAT);
        JCudaDriver.cuMemcpyHtoD(dtmDev, Pointer.to(dtmBuf), size * Sizeof.FLOAT);
        if (landBuf != null) {
            landDev = new CUdeviceptr();
            JCudaDriver.cuMemAlloc(landDev, size * Sizeof.BYTE);
            JCudaDriver.cuMemcpyHtoD(landDev, Pointer.to(landBuf), size * Sizeof.BYTE);
        }
        dsmDev = new CUdeviceptr();
        if (dsmBuf != null) {
            JCudaDriver.cuMemAlloc(dsmDev, size * Sizeof.FLOAT);
            JCudaDriver.cuMemcpyHtoD(dsmDev, Pointer.to(dsmBuf), size * Sizeof.FLOAT);
        } else {
            JCudaDriver.cuMemAlloc(dsmDev, 1 * Sizeof.FLOAT);
        }
        sumDev = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(sumDev, sumSize * Sizeof.INT);
        // Allocate device output memory
        viewDev = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(viewDev, size * Sizeof.CHAR);
    }

    /**
     * Call clearView CUDA function, for clearing the result of viewshed calculation in GPU memory.
     */
    private void clearView() {
        Pointer clearViewParam = Pointer.to(Pointer.to(viewDev), Pointer.to(new int[]{size}));
        JCudaDriver.cuLaunchKernel(funClearView, w, h / NCORE + 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        0, null, // Shared memory size and stream
        clearViewParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
    }

    /**
     * Call clearViewTan CUDA function, for clearing the result of viewtan calculation in GPU memory.
     */
    private void clearViewTan() {
        Pointer clearViewParam = Pointer.to(Pointer.to(viewTanDev), Pointer.to(new int[]{wa * ha}));
        JCudaDriver.cuLaunchKernel(funClearViewTan, wa, ha / NCORE + 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        0, null, // Shared memory size and stream
        clearViewParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
    }

    /**
     * Calculates the viewshed from the point c.
     * Calls the appropriate CUDA function depending on direct and bounds.
     * 
     * @param c the point of view if direct=true, the observed point otherwise. c is in grid coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param direct if true, observer is on c, else observed point is on c
     * @param bounds the limits of the viewshed
     */
    public void viewShed(GridCoordinates2D c, float startZ, float destZ, boolean direct, Bounds bounds) {
        clearView();
        
        Pointer viewShedParam;
        CUfunction fun;
        if (bounds.isUnbounded()) {
            viewShedParam = Pointer.to(Pointer.to(new int[]{c.x}), Pointer.to(new int[]{c.y}), Pointer.to(new float[]{startZ}), Pointer.to(new float[]{destZ}), Pointer.to(dtmDev), Pointer.to(new int[]{dtm.getWidth()}), Pointer.to(new int[]{dtm.getHeight()}), Pointer.to(new float[]{(float) data.getResolution()}), Pointer.to(new int[]{dsmBuf != null ? 1 : 0}), Pointer.to(dsmDev), Pointer.to(viewDev));
            fun = direct ? funViewShed : funViewShedInd;
        } else {
            viewShedParam = Pointer.to(Pointer.to(new int[]{c.x}), Pointer.to(new int[]{c.y}), Pointer.to(new float[]{startZ}), Pointer.to(new float[]{destZ}), Pointer.to(dtmDev), Pointer.to(new int[]{dtm.getWidth()}), Pointer.to(new int[]{dtm.getHeight()}), Pointer.to(new float[]{(float) data.getResolution()}), Pointer.to(new int[]{dsmBuf != null ? 1 : 0}), Pointer.to(dsmDev), Pointer.to(viewDev), Pointer.to(new float[]{(float) bounds.getDmin2()}), Pointer.to(new float[]{(float) bounds.getDmax2()}), Pointer.to(new float[]{(float) bounds.getTheta1Left()}), Pointer.to(new float[]{(float) bounds.getTheta1Right()}), Pointer.to(new float[]{(float) bounds.getSlopemin2()}), Pointer.to(new float[]{(float) bounds.getSlopemax2()}));
            fun = direct ? funViewShedBounded : funViewShedIndBounded;
        }
        JCudaDriver.cuLaunchKernel(fun, 2 * (w + h) / NCORE + 1, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        0, null, // Shared memory size and stream
        viewShedParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
    }

    /**
     * Calculate the tangential view from c.
     * Calls the vienTan CUDA function.
     * 
     * @param c the point of view in grid coordinate
     * @param ares the alpha resolution in radian
     * @param startZ the height of the eye of the observer
     * @param bounds the limits of the viewshed
     */
    public void viewTan(GridCoordinates2D c, double startZ, double ares, Bounds bounds) {
        int wa = (int) Math.ceil(bounds.getAmplitudeRad() / ares);
        int ha = (int) Math.ceil(Math.PI / ares);
        if (this.wa != wa || this.ha != ha) {
            if (viewTanDev != null) {
                JCudaDriver.cuMemFree(viewTanDev);
            }
            viewTanDev = null;
        }
        if (viewTanDev == null) {
            viewTanDev = new CUdeviceptr();
            JCudaDriver.cuMemAlloc(viewTanDev, wa * ha * Sizeof.INT);
            this.wa = wa;
            this.ha = ha;
        }
        clearViewTan();

        Pointer viewTanParam = Pointer.to(Pointer.to(new int[]{c.x}), Pointer.to(new int[]{c.y}), Pointer.to(new double[]{startZ}), Pointer.to(dtmDev), Pointer.to(new int[]{w}), Pointer.to(new int[]{h}), Pointer.to(new double[]{(double) data.getResolution()}), Pointer.to(new int[]{dsmBuf != null ? 1 : 0}), Pointer.to(dsmDev), Pointer.to(viewTanDev), Pointer.to(new int[]{wa}), Pointer.to(new double[]{ares}), Pointer.to(new double[]{(double) bounds.getDmin()}), Pointer.to(new double[]{(double) bounds.getDmax()}), Pointer.to(new double[]{(double) bounds.getTheta1Left()}), Pointer.to(new double[]{(double) bounds.getTheta1Right()}), Pointer.to(new double[]{(double) bounds.getSlopemin()}), Pointer.to(new double[]{(double) bounds.getSlopemax()}));
        JCudaDriver.cuLaunchKernel(funViewTan, wa / NCORE + 1, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        0, null, // Shared memory size and stream
        viewTanParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
    }

    /**
     * Returns the viewshed area.
     * @return the viewshed area in pixel
     */
    public int getSumView() {
        Pointer sumVParam = Pointer.to(Pointer.to(viewDev), Pointer.to(new int[]{size}), Pointer.to(sumDev));
        JCudaDriver.cuLaunchKernel(funSumV, sumSize, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        NCORE * Sizeof.INT, null, // Shared memory size and stream
        sumVParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
        int[] sum = new int[sumSize];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
        int nb = 0;
        for (int i = 0; i < sumSize; i++) {
            nb += sum[i];
        }
        return nb;
    }

    /**
     * Returns the tangential view area.
     * @return the tangential view area in pixel
     */
    public int getSumViewTan() {
        Pointer sumVParam = Pointer.to(Pointer.to(viewTanDev), Pointer.to(new int[]{wa * ha}), Pointer.to(sumDev));
        JCudaDriver.cuLaunchKernel(funSumVT, sumSize, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        NCORE * Sizeof.INT, null, // Shared memory size and stream
        sumVParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
        int[] sum = new int[sumSize];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
        int nb = 0;
        for (int i = 0; i < sumSize; i++) {
            nb += sum[i];
        }
        return nb;
    }

    /**
     * Returns the viewshed area for a given landuse code.
     * @param code the landuse code
     * @return the viewshed area in pixel
     */
    public int getSumLandView(byte code) {
        Pointer sumVParam = Pointer.to(Pointer.to(viewDev), Pointer.to(new int[]{size}), Pointer.to(landDev), Pointer.to(new byte[]{code}), Pointer.to(sumDev));
        JCudaDriver.cuLaunchKernel(funSumLandV, sumSize, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        NCORE * Sizeof.INT, null, // Shared memory size and stream
        sumVParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
        int[] sum = new int[sumSize];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
        int nb = 0;
        for (int i = 0; i < sumSize; i++) {
            nb += sum[i];
        }
        return nb;
    }

    /**
     * Returns the tangential view area for a given landuse code.
     * @param code the landuse code
     * @return the tangential view area in pixel
     */
    public int getSumLandViewTan(byte code) {
        Pointer sumVParam = Pointer.to(Pointer.to(viewTanDev), Pointer.to(new int[]{wa * ha}), Pointer.to(landDev), Pointer.to(new byte[]{code}), Pointer.to(sumDev));
        JCudaDriver.cuLaunchKernel(funSumLandVT, sumSize, 1, 1, // Grid dimension
        NCORE, 1, 1, // Block dimension
        NCORE * Sizeof.INT, null, // Shared memory size and stream
        sumVParam, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();
        int[] sum = new int[sumSize];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
        int nb = 0;
        for (int i = 0; i < sumSize; i++) {
            nb += sum[i];
        }
        return nb;
    }

    /**
     * Free all GPU memory and destroy the CUDA context
     */
    public void dispose() {
        JCudaDriver.cuMemFree(dtmDev);
        JCudaDriver.cuMemFree(viewDev);
        JCudaDriver.cuMemFree(sumDev);
        if (landDev != null) {
            JCudaDriver.cuMemFree(landDev);
        }
        JCudaDriver.cuMemFree(dsmDev);
        if (viewTanDev != null) {
            JCudaDriver.cuMemFree(viewTanDev);
        }
        JCudaDriver.cuCtxDestroy(ctx);
    }

    /**
     * Copy the resulting viewshed from GPU memory to viewBuf.
     * {@link #viewShed} must be called before.
     * @param viewBuf the result buffer
     * @throws IllegalArgumentException if the buffer size does not correspond to GPU buffer : width*height
     */
    public void getView(byte[] viewBuf) {
        if (viewBuf.length != size) {
            throw new IllegalArgumentException("Bad size buffer");
        }
        JCudaDriver.cuMemcpyDtoH(Pointer.to(viewBuf), viewDev, size * Sizeof.CHAR);
    }

    /**
     * Copy the resulting tangential view from GPU memory to viewBuf.
     * {@link #viewTan} must be called before.
     * @param viewBuf the result buffer
     * @throws IllegalArgumentException if the buffer size does not correspond to GPU buffer : wa*ha
     */
    public void getViewTan(int[] viewBuf) {
        if (viewBuf.length != wa * ha) {
            throw new IllegalArgumentException("Bad size buffer");
        }
        JCudaDriver.cuMemcpyDtoH(Pointer.to(viewBuf), viewTanDev, wa * ha * Sizeof.INT);
    }

    /**
     * @return the width of the resulting tangential view
     */
    public int getWa() {
        return wa;
    }

    /**
     * @return the height of the resulting tangential view
     */
    public int getHa() {
        return ha;
    }

    private void extract(String jarpath, File file) throws IOException {
        byte[] buf = new byte[8192];
        try (final InputStream stream = ComputeViewCUDA.class.getResourceAsStream(jarpath);final FileOutputStream fout = new FileOutputStream(file)) {
            int nb;
            while ((nb = stream.read(buf)) != -1) {
                fout.write(buf, 0, nb);
            }
        }
    }
    
}
