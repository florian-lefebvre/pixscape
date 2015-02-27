package org.thema.pixscape.view.cuda;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.CUresult;
import jcuda.driver.JCudaDriver;
import static jcuda.driver.JCudaDriver.cuCtxCreate;
import static jcuda.driver.JCudaDriver.cuCtxSynchronize;
import static jcuda.driver.JCudaDriver.cuDeviceGet;
import static jcuda.driver.JCudaDriver.cuDeviceGetCount;
import static jcuda.driver.JCudaDriver.cuInit;
import static jcuda.driver.JCudaDriver.cuLaunchKernel;
import static jcuda.driver.JCudaDriver.cuMemAlloc;
import static jcuda.driver.JCudaDriver.cuMemFree;
import static jcuda.driver.JCudaDriver.cuMemcpyDtoH;
import static jcuda.driver.JCudaDriver.cuMemcpyHtoD;
import static jcuda.driver.JCudaDriver.cuModuleGetFunction;
import static jcuda.driver.JCudaDriver.cuModuleLoad;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.SimpleComputeView;
import org.thema.pixscape.view.SimpleViewShedResult;
import org.thema.pixscape.view.SimpleViewTanResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * CUDA implementation of SimpleComputeView.
 * 
 * @author Gilles Vuidel
 */
public class ComputeViewCUDA extends SimpleComputeView {
    
    private final int NCORE = 512;    
    
    private Raster dtm;
    private float[] dtmBuf, dsmBuf;
    private byte[] landBuf;

    private final int nbDev;
    private int ncode;
    private final File ptxFile;
    
    private final BlockingQueue<CUDARunnable> queue = new ArrayBlockingQueue<>(100);

    public ComputeViewCUDA(ScaleData data, double aPrec, int nbGPU) throws IOException {
        super(data, aPrec);
        dtm = data.getDtm();
        if(dtm.getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT) {
            dtmBuf = ((DataBufferFloat)dtm.getDataBuffer()).getData();
        } else {
            throw new IllegalArgumentException("DTM is not float data type");
        }
        if(data.hasLandUse()) {
            if(data.getLand().getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE) {
                landBuf = ((DataBufferByte)data.getLand().getDataBuffer()).getData();
            } else {
                DataBuffer buf = data.getLand().getDataBuffer();
                landBuf = new byte[buf.getSize()];
                for(int i = 0; i < landBuf.length; i++) {
                    int v = buf.getElem(i);
                    if(v < 0 || v > 255) {
                        throw new RuntimeException("Land use code > 255 is not supported with CUDA");
                    }
                    landBuf[i] = (byte) v;
                }
            }
            ncode = -1;
            for(int i = 0; i < landBuf.length; i++){
                int v = landBuf[i];
                if(v > ncode) {
                    ncode = v;
                }
            }
            ncode++;
        }
        
        if(data.getDsm() != null) {
            if(data.getDsm().getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT) {
                dsmBuf = ((DataBufferFloat)data.getDsm().getDataBuffer()).getData();
            } else {
                DataBuffer buf = data.getDsm().getDataBuffer();
                dsmBuf = new float[buf.getSize()];
                for(int i = 0; i < dsmBuf.length; i++) {
                    dsmBuf[i] = (float) buf.getElem(i);
                }
            }
        }
        
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);
        ptxFile = File.createTempFile("view_kernel", ".ptx");
        extract("/view_kernel.ptx", ptxFile);
        // Initialize the driver 
        cuInit(0);
        int [] nb = new int[1];
        cuDeviceGetCount(nb);
        nbDev = Math.min(nb[0], nbGPU);
        // launch cuda threads, one for each GPU
        for(int dev = 0; dev < nbDev; dev++) {
            new Thread(new CUDAThread(dev), "CUDA-thread-" + dev).start();
        }
        
    }
    
    @Override
    public List<Double[]> aggrViewShed(final DirectPosition2D p, final double startZ, final double destZ, final boolean direct, 
            final Bounds bounds, final List<? extends ViewShedMetric> metrics) {
        CUDARunnable<List<Double[]>> r = new CUDARunnable<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.clearView();
                cuCtxSynchronize();

                cudaContext.viewShed(cg, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();

                CUDAViewShedResult view = new CUDAViewShedResult(cg, cudaContext, org.thema.pixscape.view.cuda.ComputeViewCUDA.this);
                List<Double[]> results = new ArrayList<>(metrics.size());
                for(ViewShedMetric m : metrics) {
                    results.add(m.calcMetric(view));
                }
                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return results;
            }
        };
        
        try {
            queue.put(r);
            return r.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public ViewShedResult calcViewShed(final DirectPosition2D p, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds)  {
            
        CUDARunnable<ViewShedResult> r = new CUDARunnable<ViewShedResult>() {
            @Override
            public ViewShedResult run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
                byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.clearView();
                cuCtxSynchronize();
                cudaContext.viewShed(cg, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();
                cudaContext.getView(viewBuf);
                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return new SimpleViewShedResult(cg, view, ComputeViewCUDA.this);
            }
        };
        
        try {
            queue.put(r);
            return r.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    @Override
    public List<Double[]> aggrViewTan(final DirectPosition2D p, final double startZ, 
            final Bounds bounds, final List<? extends ViewTanMetric> metrics) {
        CUDARunnable<List<Double[]>> r = new CUDARunnable<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewTan(cg, (float) startZ, (float) aPrec, bounds);

                CUDAViewTanResult view = new CUDAViewTanResult(aPrec, cg, cudaContext, ComputeViewCUDA.this);
                List<Double[]> results = new ArrayList<>(metrics.size());
                for(ViewTanMetric m : metrics) {
                    results.add(m.calcMetric(view));
                }
                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return results;
            }
        };
        
        try {
            queue.put(r);
            return r.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    @Override
    public ViewTanResult calcViewTan(final DirectPosition2D p, final double startZ, final Bounds bounds)  {
        CUDARunnable<ViewTanResult> r = new CUDARunnable<ViewTanResult>() {
            @Override
            public ViewTanResult run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewTan(cg, (double) startZ, (double) aPrec, bounds);

                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, cudaContext.getWa(), cudaContext.getHa(), 1, null);
                int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
                cudaContext.getViewTan(viewBuf);

                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return new SimpleViewTanResult(aPrec, cg, view, ComputeViewCUDA.this);
            }
        };
        
        try {
            queue.put(r);
            return r.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static void extract(String jarpath, File file) throws IOException {      
        byte [] buf = new byte[8192];
        try (InputStream stream = ComputeViewCUDA.class.getResourceAsStream(jarpath); FileOutputStream fout = new FileOutputStream(file)) {
            int nb;
            while ((nb = stream.read(buf)) != -1) {
                fout.write(buf, 0, nb);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            // for stoping CUDAThreads
            for(int dev = 0; dev < nbDev; dev++) {
                queue.put(CUDARunnable.END);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static boolean isCUDAAvailable() {
        try {
            int res = cuInit(0);
            return res == CUresult.CUDA_SUCCESS;
        } catch(Throwable e) {
            Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.INFO, null, e);
            return false;
        }
    }

    class CUDAContext {
        final int nDev;
        final int w = dtm.getWidth();
        final int h = dtm.getHeight();
        final int size = w*h;
        final int sumSize = NCORE;
        int wa, ha;
        
        private CUcontext ctx;
        private CUfunction funViewShed, funViewTan, funViewShedInd, funViewShedBounded, funViewShedIndBounded, 
                funSumV, funSumVT, funSumLandV, funSumLandVT, funClearView, funClearViewTan, funAddView;
        private CUdeviceptr dtmDev, landDev, dsmDev, sumDev, viewDev, viewTanDev;
        public CUDAContext(int nDev) {
            this.nDev = nDev;
            CUdevice device = new CUdevice();
            cuDeviceGet(device, nDev); 
            ctx = new CUcontext();
            cuCtxCreate(ctx, 0, device);

            // Load the ptx file.
            CUmodule module = new CUmodule();
            cuModuleLoad(module, ptxFile.getAbsolutePath());

            // Obtain a functions pointer
            funViewShed = new CUfunction();
            cuModuleGetFunction(funViewShed, module, "calcRayDirect");
            funViewTan = new CUfunction();
            cuModuleGetFunction(funViewTan, module, "calcRayTan");
            funViewShedInd = new CUfunction();
            cuModuleGetFunction(funViewShedInd, module, "calcRayIndirect");
            funViewShedBounded = new CUfunction();
            cuModuleGetFunction(funViewShedBounded, module, "calcRayDirectBounded");
            funViewShedIndBounded = new CUfunction();
            cuModuleGetFunction(funViewShedIndBounded, module, "calcRayIndirectBounded");
            funSumV = new CUfunction();
            cuModuleGetFunction(funSumV, module, "sumView");
            funSumVT = new CUfunction();
            cuModuleGetFunction(funSumVT, module, "sumViewTan");
            funSumLandV = new CUfunction();
            cuModuleGetFunction(funSumLandV, module, "sumLandView");
            funSumLandVT = new CUfunction();
            cuModuleGetFunction(funSumLandVT, module, "sumLandViewTan");
            funClearView = new CUfunction();
            cuModuleGetFunction(funClearView, module, "clearView");
            funClearViewTan = new CUfunction();
            cuModuleGetFunction(funClearViewTan, module, "clearViewTan");
            funAddView = new CUfunction();
            cuModuleGetFunction(funAddView, module, "addView");

            // Allocate the device input data, and copy the
            // host input data to the device
            dtmDev = new CUdeviceptr();
            cuMemAlloc(dtmDev, size * Sizeof.FLOAT);
            cuMemcpyHtoD(dtmDev, Pointer.to(dtmBuf), size * Sizeof.FLOAT);

            if(landBuf != null) {
                landDev = new CUdeviceptr();
                cuMemAlloc(landDev, size * Sizeof.BYTE);
                cuMemcpyHtoD(landDev, Pointer.to(landBuf), size * Sizeof.BYTE);
//                sumCombDev = new CUdeviceptr();
//                cuMemAlloc(sumCombDev, ncode * Sizeof.INT);
            }
            dsmDev = new CUdeviceptr();
            if(dsmBuf != null) {
                cuMemAlloc(dsmDev, size * Sizeof.FLOAT);
                cuMemcpyHtoD(dsmDev, Pointer.to(dsmBuf), size * Sizeof.FLOAT);
            } else {
                cuMemAlloc(dsmDev, 1 * Sizeof.FLOAT);
            }
            
            sumDev = new CUdeviceptr();
            cuMemAlloc(sumDev, sumSize * Sizeof.INT);
            

            // Allocate device output memory
            viewDev = new CUdeviceptr();
            cuMemAlloc(viewDev, size * Sizeof.CHAR);

        }
        
        void clearView() {
            Pointer clearViewParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(new int[]{size})
            );
            cuLaunchKernel(funClearView,
                w, h/NCORE+1, 1,     // Grid dimension
                NCORE, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                clearViewParam, null // Kernel- and extra parameters
            );
        }
        
        void clearViewTan() {
            Pointer clearViewParam = Pointer.to(
                Pointer.to(viewTanDev),
                Pointer.to(new int[]{wa*ha})
            );
            cuLaunchKernel(funClearViewTan,
                wa, ha/NCORE+1, 1,     // Grid dimension
                NCORE, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                clearViewParam, null // Kernel- and extra parameters
            );
        }
        
        void viewShed(GridCoordinates2D c, float startZ, float destZ, boolean direct, Bounds bounds) {
            Pointer viewShedParam;
            CUfunction fun;
            if(bounds.isUnbounded()) {
                viewShedParam = Pointer.to(
                    Pointer.to(new int[]{c.x}),
                    Pointer.to(new int[]{c.y}),
                    Pointer.to(new float[]{startZ}),
                    Pointer.to(new float[]{destZ}),
                    Pointer.to(dtmDev),
                    Pointer.to(new int[]{dtm.getWidth()}),
                    Pointer.to(new int[]{dtm.getHeight()}),
                    Pointer.to(new float[]{(float)getData().getResolution()}),
                    Pointer.to(new int[]{dsmBuf != null ? 1 : 0}),
                    Pointer.to(dsmDev),
                    Pointer.to(viewDev)
                );
                fun = direct ? funViewShed : funViewShedInd;
            } else {
                viewShedParam = Pointer.to(
                    Pointer.to(new int[]{c.x}),
                    Pointer.to(new int[]{c.y}),
                    Pointer.to(new float[]{startZ}),
                    Pointer.to(new float[]{destZ}),
                    Pointer.to(dtmDev),
                    Pointer.to(new int[]{dtm.getWidth()}),
                    Pointer.to(new int[]{dtm.getHeight()}),
                    Pointer.to(new float[]{(float)getData().getResolution()}),
                    Pointer.to(new int[]{dsmBuf != null ? 1 : 0}),
                    Pointer.to(dsmDev),
                    Pointer.to(viewDev),
                    Pointer.to(new float[]{(float)bounds.getDmin2()}),
                    Pointer.to(new float[]{(float)bounds.getDmax2()}),
                    Pointer.to(new float[]{(float)bounds.getAlphaleft()}),
                    Pointer.to(new float[]{(float)bounds.getAlpharight()}),
                    Pointer.to(new float[]{(float)bounds.getSlopemin2()}),
                    Pointer.to(new float[]{(float)bounds.getSlopemax2()})
                );
                fun = direct ? funViewShedBounded : funViewShedIndBounded;
            }
            cuLaunchKernel(fun,
                    2*(w+h)/NCORE+1,  1, 1,      // Grid dimension
                    NCORE, 1, 1,      // Block dimension
                    0, null,               // Shared memory size and stream
                    viewShedParam, null // Kernel- and extra parameters
                );
        }
        
        void viewTan(GridCoordinates2D c, double startZ, double ares, Bounds bounds) {
            int wa = (int)Math.ceil(bounds.getAmplitudeRad()/ares);
            int ha = (int)Math.ceil(Math.PI/ares);   
            if(this.wa != wa || this.ha != ha) {
                if(viewTanDev != null) {
                    cuMemFree(viewTanDev);
                }
                viewTanDev = null;
            }
            if(viewTanDev == null) {
                viewTanDev = new CUdeviceptr();
                cuMemAlloc(viewTanDev, wa*ha * Sizeof.INT);
                this.wa = wa;
                this.ha = ha;
            }
            
            clearViewTan();
            cuCtxSynchronize();
            
            Pointer viewTanParam = Pointer.to(
                Pointer.to(new int[]{c.x}),
                Pointer.to(new int[]{c.y}),
                Pointer.to(new double[]{startZ}),
                Pointer.to(dtmDev),
                Pointer.to(new int[]{w}),
                Pointer.to(new int[]{h}),
                Pointer.to(new double[]{(double)getData().getResolution()}),
                Pointer.to(new int[]{dsmBuf != null ? 1 : 0}),
                Pointer.to(dsmDev),
                Pointer.to(viewTanDev),
                Pointer.to(new int[]{wa}),
                Pointer.to(new double[]{ares}),
                Pointer.to(new double[]{(double)bounds.getDmin()}),
                Pointer.to(new double[]{(double)bounds.getDmax()}),
                Pointer.to(new double[]{(double)bounds.getAlphaleft()}),
                Pointer.to(new double[]{(double)bounds.getAlpharight()}),
                Pointer.to(new double[]{(double)bounds.getSlopemin()}),
                Pointer.to(new double[]{(double)bounds.getSlopemax()})
            );
            
            cuLaunchKernel(funViewTan,
                    wa/NCORE+1,  1, 1,      // Grid dimension
                    NCORE, 1, 1,      // Block dimension
                    0, null,               // Shared memory size and stream
                    viewTanParam, null // Kernel- and extra parameters
                );
            cuCtxSynchronize();
        }
        
        int getSumView() {
            Pointer sumVParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(new int[]{size}),
                Pointer.to(sumDev)
            );
            cuLaunchKernel(funSumV,
                sumSize,  1, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                NCORE*Sizeof.INT, null,               // Shared memory size and stream
                sumVParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            int sum[] = new int[sumSize];
            cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
            int nb = 0;
            for(int i = 0; i < sumSize; i++) {
                nb += sum[i];
            }
            
            return nb;
        }
        
        int getSumViewTan() {
            Pointer sumVParam = Pointer.to(
                Pointer.to(viewTanDev),
                Pointer.to(new int[]{wa*ha}),
                Pointer.to(sumDev)
            );
            cuLaunchKernel(funSumVT,
                sumSize,  1, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                NCORE*Sizeof.INT, null,               // Shared memory size and stream
                sumVParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            int sum[] = new int[sumSize];
            cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
            int nb = 0;
            for(int i = 0; i < sumSize; i++) {
                nb += sum[i];
            }
            
            return nb;
        }
        
        int getSumLandView(byte code) {
            Pointer sumVParam = Pointer.to(
                Pointer.to(viewDev),
                Pointer.to(new int[]{size}),
                Pointer.to(landDev),
                Pointer.to(new byte[]{code}),
                Pointer.to(sumDev)
            );
            cuLaunchKernel(funSumLandV,
                sumSize, 1, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                NCORE*Sizeof.INT, null,               // Shared memory size and stream
                sumVParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            int sum[] = new int[sumSize];
            cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
            int nb = 0;
            for(int i = 0; i < sumSize; i++) {
                nb += sum[i];
            }
            return nb;
        }
        
        int getSumLandViewTan(byte code) {
            Pointer sumVParam = Pointer.to(
                Pointer.to(viewTanDev),
                Pointer.to(new int[]{wa*ha}),
                Pointer.to(landDev),
                Pointer.to(new byte[]{code}),
                Pointer.to(sumDev)
            );
            cuLaunchKernel(funSumLandVT,
                sumSize, 1, 1,      // Grid dimension
                NCORE, 1, 1,      // Block dimension
                NCORE*Sizeof.INT, null,               // Shared memory size and stream
                sumVParam, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
            
            int sum[] = new int[sumSize];
            cuMemcpyDtoH(Pointer.to(sum), sumDev, sumSize * Sizeof.INT);
            int nb = 0;
            for(int i = 0; i < sumSize; i++) {
                nb += sum[i];
            }
            return nb;
        }
        
        private void dispose() {
            cuMemFree(dtmDev);
            cuMemFree(viewDev);
            cuMemFree(sumDev);
            if(landDev != null) {
                cuMemFree(landDev);
            }
            cuMemFree(dsmDev);
            if(viewTanDev != null) {
                cuMemFree(viewTanDev);
            }
            JCudaDriver.cuCtxDestroy(ctx);
        }

        void getView(byte[] viewBuf) {
            if(viewBuf.length != size) {
                throw new IllegalArgumentException("Bad size buffer");
            }
            cuMemcpyDtoH(Pointer.to(viewBuf), viewDev, size * Sizeof.CHAR);
        }
        
        void getViewTan(int[] viewBuf) {
            if(viewBuf.length != wa*ha) {
                throw new IllegalArgumentException("Bad size buffer");
            }
            cuMemcpyDtoH(Pointer.to(viewBuf), viewTanDev, wa*ha * Sizeof.INT);
        }

        public int getWa() {
            return wa;
        }

        public int getHa() {
            return ha;
        }
    }

    
    private class CUDAThread implements Runnable {
        private CUDAContext context;
        private int dev;
        public CUDAThread(int dev) {
            this.dev = dev;
        }

        @Override
        public void run() {
            context = new CUDAContext(dev);
            try {
                boolean end = false;
                List<CUDARunnable> list = new ArrayList<>();
                do {
                    list.clear();
                    queue.drainTo(list);
                    if(list.isEmpty()) {
                        list.add(queue.take());
                    }  
                    for(CUDARunnable run : list) {
                        if(run == CUDARunnable.END) {
                            end = true;
                            break;
                        }
                        try {
                            run.result = run.run(context);
                        } catch(Throwable e) {
                            run.result = e;
                        }
                        synchronized(run) {
                            run.notify();
                        }
                    }
                      
                } while(!end);
            } catch (InterruptedException ex) {
                Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
            }
            context.dispose();
        }
        
        public CUDAContext getContext() {
            return context;
        }
        
    }
    
}
