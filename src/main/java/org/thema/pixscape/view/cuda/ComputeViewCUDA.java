package org.thema.pixscape.view.cuda;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
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
import org.thema.pixscape.Bounds;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.ComputeView;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class ComputeViewCUDA extends ComputeView {
    
    private final int NCORE = 512;    
    
    private float[] dtmBuf, dsmBuf;
    private byte[] landBuf;

    private final int nbDev;
    private int ncode;
    private final File ptxFile;
    
    private final BlockingQueue<CUDARunnable> queue = new ArrayBlockingQueue<>(100);
    /**
     * 
     * @param dtm
     * @param resZ
     * @param res2D
     * @param land can be null
     * @param dsm  can be null
     */
    public ComputeViewCUDA(Raster dtm, double resZ, double res2D, Raster land, SortedSet<Integer> codes, Raster dsm, int nbGPU) throws IOException {
        super(dtm, resZ, res2D, land, codes, dsm);
        if(dtm.getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT && resZ == 1)
            dtmBuf = ((DataBufferFloat)dtm.getDataBuffer()).getData();
        else {
            DataBuffer buf = dtm.getDataBuffer();
            dtmBuf = new float[buf.getSize()];
            for(int i = 0; i < dtmBuf.length; i++)
                dtmBuf[i] = (float) (buf.getElemDouble(i));
        }
        if(land != null) {
            if(land.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE)
                landBuf = ((DataBufferByte)land.getDataBuffer()).getData();
            else {
                DataBuffer buf = land.getDataBuffer();
                landBuf = new byte[buf.getSize()];
                for(int i = 0; i < landBuf.length; i++) {
                    int v = buf.getElem(i);
                    if(v < 0 || v > 255)
                        throw new RuntimeException("Land use code > 255 is not supported with CUDA");
                    landBuf[i] = (byte) v;
                }
            }
            ncode = -1;
            for(int i = 0; i < landBuf.length; i++){
                int v = landBuf[i];
                if(v > ncode)
                    ncode = v;
            }
            ncode++;
        }
        
        if(dsm != null) {
            if(dsm.getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT)
                dsmBuf = ((DataBufferFloat)dsm.getDataBuffer()).getData();
            else {
                DataBuffer buf = dsm.getDataBuffer();
                dsmBuf = new float[buf.getSize()];
                for(int i = 0; i < dsmBuf.length; i++) {
                    dsmBuf[i] = (float) buf.getElem(i);
                }
            }
        }
        
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);
        // Create the PTX file by calling the NVCC
        //ptxFileName = preparePtxFile("/home/gvuidel/Documents/PImage/pimage/target/classes/view_kernel.cu");
//        preparePtxFile("/home/gvuidel/Documents/PImage/pimage/target/classes/view_kernel.cu");
        ptxFile = File.createTempFile("view_kernel", ".ptx");
        extract("/view_kernel.ptx", ptxFile);
        // Initialize the driver 
        cuInit(0);
        int [] nb = new int[1];
        cuDeviceGetCount(nb);
        nbDev = Math.min(nb[0], nbGPU);
        // launch cuda threads, one for each GPU
        for(int dev = 0; dev < nbDev; dev++)
            new Thread(new CUDAThread(dev), "CUDA-thread-" + dev).start();
        
    }
    
    @Override
    public List<Double[]> aggrViewShed(final GridCoordinates2D cg, final double startZ, final double destZ, final boolean direct, 
            final Bounds bounds, final List<? extends ViewShedMetric> metrics) {
        CUDARunnable<List<Double[]>> r = new CUDARunnable<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {

                cudaContext.clearView();
                cuCtxSynchronize();

                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();

                CUDAViewShedResult view = new CUDAViewShedResult(cg, cudaContext, org.thema.pixscape.view.cuda.ComputeViewCUDA.this);
                List<Double[]> results = new ArrayList<>(metrics.size());
                for(ViewShedMetric m : metrics)
                    results.add(m.calcMetric(view));
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
    public double aggrViewShed(final GridCoordinates2D cg, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds) {
        
        CUDARunnable<Integer> r = new CUDARunnable<Integer>() {
            @Override
            public Integer run(CUDAContext cudaContext) {
//                long t1 = System.currentTimeMillis();
                cudaContext.clearView();
                cuCtxSynchronize();
//                long t2 = System.currentTimeMillis();
                
                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();
//                long t3 = System.currentTimeMillis();
                
                int sum = cudaContext.getSumView();
//                long t4 = System.currentTimeMillis();
//                System.out.println(cudaContext.nDev + " - clr " + (t2-t1) + " - view " + (t3-t2) + " - sum " + (t4-t3) + " ms");
                return sum;
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
    public double[] aggrViewShedLand(final GridCoordinates2D cg, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds, final SortedSet<Integer> codes) {
        CUDARunnable<double[]> r = new CUDARunnable<double[]>() {
            @Override
            public double[] run(CUDAContext cudaContext) {
//                long t1 = System.currentTimeMillis();
                cudaContext.clearView();
                cuCtxSynchronize();
//                long t2 = System.currentTimeMillis();
                
                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();
//                long t3 = System.currentTimeMillis();
                
                double sum[] = new double[codes.last()+1];
                for(Integer code : codes)
                    sum[code] = cudaContext.getSumLandView(code.byteValue());
//                long t4 = System.currentTimeMillis();
//                System.out.println(cudaContext.nDev + " - clr " + (t2-t1) + " - view " + (t3-t2) + " - sumland " + (t4-t3) + " ms");
                return sum;
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
    public ViewShedResult calcViewShed(final GridCoordinates2D cg, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds)  {
            
        CUDARunnable<ViewShedResult> r = new CUDARunnable<ViewShedResult>() {
            @Override
            public ViewShedResult run(CUDAContext cudaContext) {
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
                byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
                //long time = System.currentTimeMillis();

                cudaContext.clearView();
                cuCtxSynchronize();

                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();

                //System.out.println("Nb : " + cudaContext.getSumView());

                 cudaContext.getView(viewBuf);
                

                //System.out.println((System.currentTimeMillis()-time) + " ms");
                return new ViewShedResult(cg, view, ComputeViewCUDA.this);
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
    public List<Double[]> aggrViewTan(final GridCoordinates2D cg, final double startZ, final double ares, 
            final Bounds bounds, final List<? extends ViewTanMetric> metrics) {
        CUDARunnable<List<Double[]>> r = new CUDARunnable<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {
  
                cudaContext.viewTan(cg.x, cg.y, (float) startZ, (float) ares, bounds);

                CUDAViewTanResult view = new CUDAViewTanResult(ares, cg, cudaContext, ComputeViewCUDA.this);
                List<Double[]> results = new ArrayList<>(metrics.size());
                for(ViewTanMetric m : metrics)
                    results.add(m.calcMetric(view));
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
    public double aggrViewTan(final GridCoordinates2D cg, final double startZ, final double ares, final Bounds bounds) {
        CUDARunnable<Double> r = new CUDARunnable<Double>() {
            @Override
            public Double run(CUDAContext cudaContext) {            
                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, bounds);
                int sum = cudaContext.getSumViewTan();
                return sum * Math.pow(ares*180/Math.PI, 2);
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
    public double[] aggrViewTanLand(final GridCoordinates2D cg, final double startZ, final double ares, final Bounds bounds, final SortedSet<Integer> codes) {
        CUDARunnable<double[]> r = new CUDARunnable<double[]>() {
            @Override
            public double[] run(CUDAContext cudaContext) {              
                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, bounds);
                double [] sum = new double[codes.last()+1];
                for(Integer code : codes) {
                    sum[code] = cudaContext.getSumLandViewTan(code.byteValue()) * Math.pow(ares*180/Math.PI, 2);
                }
                return sum;
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
    public ViewTanResult calcViewTan(final GridCoordinates2D cg, final double startZ, final double ares, final Bounds bounds)  {
        CUDARunnable<ViewTanResult> r = new CUDARunnable<ViewTanResult>() {
            @Override
            public ViewTanResult run(CUDAContext cudaContext) {
               
                //long time = System.currentTimeMillis();

                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, bounds);

                //System.out.println("Nb : " + cudaContext.getSumView());
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, cudaContext.getWa(), cudaContext.getHa(), 1, null);
                int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
                cudaContext.getViewTan(viewBuf);

                //System.out.println((System.currentTimeMillis()-time) + " ms");
                return new ViewTanResult(ares, cg, view, ComputeViewCUDA.this);
            }
        };
        
        try {
            queue.put(r);
            return r.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    

    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     *
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    private static String preparePtxFile(String cuFileName) throws IOException
    {
        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1)
        {
            endIndex = cuFileName.length()-1;
        }
        String ptxFileName = cuFileName.substring(0, endIndex+1)+"ptx";
        File ptxFile = new File(ptxFileName);
        if (ptxFile.exists())
        {
            return ptxFileName;
        }

        File cuFile = new File(cuFileName);
        if (!cuFile.exists())
        {
            throw new IOException("Input file not found: "+cuFileName);
        }
        String modelString = "-m"+System.getProperty("sun.arch.data.model");
        String command =
            "nvcc " + modelString + " -ptx "+
            cuFile.getPath()+" -o "+ptxFileName;

        System.out.println("Executing\n"+command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage =
            new String(toByteArray(process.getErrorStream()));
        String outputMessage =
            new String(toByteArray(process.getInputStream()));
        int exitValue = 0;
        try
        {
            exitValue = process.waitFor();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                "Interrupted while waiting for nvcc output", e);
        }

        if (exitValue != 0)
        {
            System.out.println("nvcc process exitValue "+exitValue);
            System.out.println("errorMessage:\n"+errorMessage);
            System.out.println("outputMessage:\n"+outputMessage);
            throw new IOException(
                "Could not create .ptx file: "+errorMessage);
        }

        System.out.println("Finished creating PTX file");
        return ptxFileName;
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

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    @Override
    public void dispose() {
        try {
            // for stoping CUDAThreads
            for(int dev = 0; dev < nbDev; dev++)
                queue.put(CUDARunnable.END);
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
            } else
                cuMemAlloc(dsmDev, 1 * Sizeof.FLOAT);
            
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
        
        void viewShed(int x, int y, float startZ, float destZ, boolean direct, Bounds bounds) {
            Pointer viewShedParam;
            CUfunction fun;
            if(bounds.isUnbounded()) {
                viewShedParam = Pointer.to(
                    Pointer.to(new int[]{x}),
                    Pointer.to(new int[]{y}),
                    Pointer.to(new float[]{startZ}),
                    Pointer.to(new float[]{destZ}),
                    Pointer.to(dtmDev),
                    Pointer.to(new int[]{dtm.getWidth()}),
                    Pointer.to(new int[]{dtm.getHeight()}),
                    Pointer.to(new float[]{(float)resZ}),
                    Pointer.to(new float[]{(float)res2D}),
                    Pointer.to(new int[]{dsmBuf != null ? 1 : 0}),
                    Pointer.to(dsmDev),
                    Pointer.to(viewDev)
                );
                fun = direct ? funViewShed : funViewShedInd;
            } else {
                viewShedParam = Pointer.to(
                    Pointer.to(new int[]{x}),
                    Pointer.to(new int[]{y}),
                    Pointer.to(new float[]{startZ}),
                    Pointer.to(new float[]{destZ}),
                    Pointer.to(dtmDev),
                    Pointer.to(new int[]{dtm.getWidth()}),
                    Pointer.to(new int[]{dtm.getHeight()}),
                    Pointer.to(new float[]{(float)resZ}),
                    Pointer.to(new float[]{(float)res2D}),
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
        
        void viewTan(int x, int y, double startZ, double ares, Bounds bounds) {
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
                Pointer.to(new int[]{x}),
                Pointer.to(new int[]{y}),
                Pointer.to(new double[]{startZ}),
                Pointer.to(dtmDev),
                Pointer.to(new int[]{w}),
                Pointer.to(new int[]{h}),
                Pointer.to(new double[]{(double)resZ}),
                Pointer.to(new double[]{(double)res2D}),
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
            if(viewBuf.length != size)
                throw new IllegalArgumentException("Bad size buffer");
            cuMemcpyDtoH(Pointer.to(viewBuf), viewDev, size * Sizeof.CHAR);
        }
        
        void getViewTan(int[] viewBuf) {
            if(viewBuf.length != wa*ha)
                throw new IllegalArgumentException("Bad size buffer");
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
                    if(list.isEmpty())
                        list.add(queue.take());  
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
