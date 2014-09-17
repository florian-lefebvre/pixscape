package org.thema.pixscape;

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

/**
 *
 * @author gvuidel
 */
public class ComputeViewCUDA implements ComputeView {
    
    private final int NCORE = 512;
    
    private final Raster dtm, land, dsm;
    
    private float[] dtmBuf, dsmBuf;
    private byte[] landBuf;
    
    /** resolution of the grid dtm in meter */
    private final double res2D;
    /** resolution of altitude Z in meter */
    private final double resZ;

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
    public ComputeViewCUDA(Raster dtm, double resZ, double res2D, Raster land, Raster dsm, int nbGPU) throws IOException {
        this.dtm = dtm;
        if(dtm.getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT && resZ == 1)
            dtmBuf = ((DataBufferFloat)dtm.getDataBuffer()).getData();
        else {
            DataBuffer buf = dtm.getDataBuffer();
            dtmBuf = new float[buf.getSize()];
            for(int i = 0; i < dtmBuf.length; i++)
                dtmBuf[i] = (float) (buf.getElemDouble(i));
        }
        this.resZ = resZ;
        this.res2D = res2D;
        this.land = land;
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
        
            
        this.dsm = dsm;
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

//    public Map<Integer, WritableRaster> calcVisibility(final double startZ, final double destZ, final boolean direct, final Bounds bounds, 
//            Set<Integer> fromCode,  Set<Integer> toCode, final ProgressBar progressBar) {
//        final Set<Integer> from = direct ? fromCode : toCode;
//        final Set<Integer> to = direct ? toCode : fromCode;
//        
//        final Map<Integer, WritableRaster> map = new HashMap<>(); 
//        for(int code : to) {
//            WritableRaster r = Raster.createBandedRaster(DataBuffer.TYPE_INT, dtm.getWidth(), dtm.getHeight(), 1, null);
//            Arrays.fill(((DataBufferInt)r.getDataBuffer()).getData(), -1);
//            map.put(code, r);
//        }
//        final int w = dtm.getWidth();
//        final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1024);
//        
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    for(int y = 0; y < dtm.getHeight(); y++) {
//                        if(progressBar.isCanceled())
//                            break;
//                        for(int x = 0; x < w; x++) {
//                            if(from.contains(land.getSample(x, y, 0)))
//                                queue.put(y*w+x);
//                        }
//                        progressBar.incProgress(1);
//                        System.out.println("line " + y + "/" + dtm.getHeight());
//                    }
//                    for(int i = 0; i < nbDev; i++)
//                        queue.put(-1);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }).start();
//        
//        
//        SimpleParallelTask task = new SimpleParallelTask.IterParallelTask(nbDev, null) {
//            @Override
//            protected void executeOne(final Integer dev) {
////                final byte [] viewBuf = new byte[dtm.getWidth()*dtm.getHeight()];
////                final int [] sum = new int[Collections.max(to)+1];
//                CUDAContext cudaContext = new CUDAContext(dev);
//                try {
//                    int ind = queue.take();
//                    while(ind != -1) {   
//                        final long t2 = System.currentTimeMillis();
//                        cudaContext.clearView();
//                        cuCtxSynchronize();
//                        cudaContext.viewShed(ind%w, ind/w, (float) startZ, (float) destZ, direct, bounds);
//                        cuCtxSynchronize();
//                        final long t3 = System.currentTimeMillis();
//                        
////                        int[] sumLandView = cudaContext.getSumLandView();
////                        for(int code : to)
////                            map.get(code).setSample(x, y, 0, sumLandView[code]);
//                        
//                        for(int code : to)
//                            map.get(code).setSample(ind%w, ind/w, 0, cudaContext.getSumLandView((byte) code));
//                        
////                        cudaContext.getView(viewBuf);
//
//                        final long t4 = System.currentTimeMillis();
////                        Arrays.fill(sum, 0);
////                        for(int i = 0; i < viewBuf.length; i++) {
////                            if(viewBuf[i] == 1) {
////                                final int os = land.getSample(i%w, i/w, 0);
////                                if(os < sum.length)
////                                    sum[os]++;  
////                            }
////                        }
////                        for(int code : to)
////                            map.get(code).setSample(ind%w, ind/w, 0, sum[code]);
//                        
//                        final long t5 = System.currentTimeMillis();
////                        System.out.println("View : " + (t3-t2) + " - Transfert : " + (t4-t3) + " - Sum : " + (t5-t4));
//                        ind = queue.take();
//                    }
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
//                } finally {
//                    cudaContext.dispose();
//                }
//            }
//        };
//        
//        long time = System.currentTimeMillis();
//        new ParallelFExecutor(task, nbDev).executeAndWait();
//        System.out.println((System.currentTimeMillis()-time) + " ms");
//
//        return map;
//    }
//    
//    public WritableRaster calcVisibility(final double startZ, final double destZ, final boolean direct, final Bounds bounds, final ProgressBar progressBar) {
//        final WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, dtm.getWidth(), dtm.getHeight(), 1, null);
//        
//        SimpleParallelTask task = new SimpleParallelTask.IterParallelTask(nbDev, null) {
//            @Override
//            protected void executeOne(final Integer dev) {
//                final int h = (int) Math.ceil(dtm.getHeight() / nbDev);
//                final int hStart = h*dev;
//                final int hEnd = Math.min((dev+1)*h, dtm.getHeight());
//                CUDAContext cudaContext = new CUDAContext(dev);
//                for(int y = hStart; y < hEnd; y++) {
//                    if(progressBar.isCanceled()) {
//                        cancelTask();
//                        break;
//                    }
//                    for(int x = 0; x < dtm.getWidth(); x++) {
//                        final long t1 = System.currentTimeMillis();
//                        cudaContext.clearView();
//                        cuCtxSynchronize();
//                        cudaContext.viewShed(x, y, (float) startZ, (float) destZ, direct, bounds);
//                        cuCtxSynchronize();
//                        final long t2 = System.currentTimeMillis();
//                        view.setSample(x, y, 0, cudaContext.getSumView());               
//                        final long t3 = System.currentTimeMillis();
////                        System.out.println("View : " + (t2-t1) + " - Sum : " + (t3-t2));
//                    }
//                    progressBar.incProgress(1);
//                }
//                cudaContext.dispose();
//            }
//        };
//        
//        long time = System.currentTimeMillis();
//        new ParallelFExecutor(task, nbDev).executeAndWait();
//        System.out.println((System.currentTimeMillis()-time) + " ms");
//        return view;
//    }

    @Override
    public double aggrViewShed(final GridCoordinates2D cg, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds) {
        
        CUDARunnable<Integer> r = new CUDARunnable<Integer>() {
            @Override
            public Integer run(CUDAContext cudaContext) {
                cudaContext.clearView();
                cuCtxSynchronize();
                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();
                return cudaContext.getSumView();
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
                cudaContext.clearView();
                cuCtxSynchronize();
                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();
                double sum[] = new double[codes.last()+1];
                for(Integer code : codes)
                    sum[code] = cudaContext.getSumLandView(code.byteValue());
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
    public WritableRaster calcViewShed(final GridCoordinates2D cg, final double startZ, final double destZ, 
            final boolean direct, final Bounds bounds)  {
            
        CUDARunnable<WritableRaster> r = new CUDARunnable<WritableRaster>() {
            @Override
            public WritableRaster run(CUDAContext cudaContext) {
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
                byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
                //long time = System.currentTimeMillis();

                cudaContext.clearView();
                cuCtxSynchronize();

                cudaContext.viewShed(cg.x, cg.y, (float) startZ, (float) destZ, direct, bounds);
                cuCtxSynchronize();

                //System.out.println("Nb : " + cudaContext.getSumView());

                cudaContext.getView(viewBuf);
                // Clean up.
                //        cudaContext.dispose();
                //        contexts.add(cudaContext);
                //System.out.println((System.currentTimeMillis()-time) + " ms");
                return view;
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
                int wa = (int)Math.ceil(2*Math.PI/ares);
                int ha = (int)Math.ceil(Math.PI/ares);                
                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, wa, ha, bounds);
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
                int wa = (int)Math.ceil(2*Math.PI/ares);
                int ha = (int)Math.ceil(Math.PI/ares);                
                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, wa, ha, bounds);
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
    public WritableRaster calcViewTan(final GridCoordinates2D cg, final double startZ, final double ares, final Bounds bounds)  {
        CUDARunnable<WritableRaster> r = new CUDARunnable<WritableRaster>() {
            @Override
            public WritableRaster run(CUDAContext cudaContext) {
                int wa = (int)Math.ceil(2*Math.PI/ares);
                int ha = (int)Math.ceil(Math.PI/ares);                
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, wa, ha, 1, null);
                int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
                //long time = System.currentTimeMillis();

                cudaContext.viewTan(cg.x, cg.y, (double) startZ, (double) ares, wa, ha, bounds);

                //System.out.println("Nb : " + cudaContext.getSumView());

                cudaContext.getViewTan(viewBuf);

                //System.out.println((System.currentTimeMillis()-time) + " ms");
                return view;
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
            // for stoping CUDAThread
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

    private class CUDAContext {
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
        
        private void clearView() {
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
        
        private void clearViewTan() {
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
        
        private void viewShed(int x, int y, float startZ, float destZ, boolean direct, Bounds bounds) {
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
        
        private void viewTan(int x, int y, double startZ, double ares, int wa, int ha, Bounds bounds) {
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
        
        private int getSumView() {
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
        
        private int getSumViewTan() {
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
        
        private int getSumLandView(byte code) {
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
        
        private int getSumLandViewTan(byte code) {
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

        private void getView(byte[] viewBuf) {
            if(viewBuf.length != size)
                throw new IllegalArgumentException("Bad size buffer");
            cuMemcpyDtoH(Pointer.to(viewBuf), viewDev, size * Sizeof.CHAR);
        }
        
        private void getViewTan(int[] viewBuf) {
            if(viewBuf.length != wa*ha)
                throw new IllegalArgumentException("Bad size buffer");
            cuMemcpyDtoH(Pointer.to(viewBuf), viewTanDev, wa*ha * Sizeof.INT);
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
                CUDARunnable run = queue.take();
                while(run != CUDARunnable.END) {
                    try {
                        run.result = run.run(context);
                    } catch(Throwable e) {
                        run.result = e;
                    }
                    synchronized(run) {
                        run.notify();
                    }
                    run = queue.take();    
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
            }
            context.dispose();
        }
        
        public CUDAContext getContext() {
            return context;
        }
        
    }
    
    private static abstract class CUDARunnable<T> {
        public static final CUDARunnable END = new CUDARunnable() {
            @Override
            public Object run(CUDAContext context) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        private T result;
        public abstract T run(CUDAContext context);
        public synchronized T get() throws InterruptedException {
            while(result == null) {
                wait();
            }
            if(result instanceof Throwable)
                throw new RuntimeException((Throwable) result);
            else
                return result;
        }
    }
}
