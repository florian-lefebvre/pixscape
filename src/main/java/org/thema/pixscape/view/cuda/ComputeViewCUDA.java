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
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcuda.driver.CUresult;
import jcuda.driver.JCudaDriver;
import static jcuda.driver.JCudaDriver.cuDeviceGetCount;
import static jcuda.driver.JCudaDriver.cuInit;
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
    
    /** the number of GPU devices really used */
    private final int nbDev;
    
    /** The execution blocking queue */
    private final BlockingQueue<CUDAExec> queue = new ArrayBlockingQueue<>(100);

    /**
     * Creates a new ComputeViewCUDA.
     * Creates one thread for each GPU device
     * @param data the data for this resolution
     * @param aPrec the precision in degree for tangential view
     * @param earthCurv take into account earth curvature ?
     * @param coefRefraction refraction correction, 0 for no correction
     * @param nbGPU the number of GPU devices to use, must be > 0
     */
    public ComputeViewCUDA(ScaleData data, double aPrec, boolean earthCurv, double coefRefraction, int nbGPU) {
        super(data, aPrec, earthCurv, coefRefraction);
        
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);
        
        // Initialize the driver 
        cuInit(0);
        int [] nb = new int[1];
        cuDeviceGetCount(nb);
        nbDev = Math.min(nb[0], nbGPU);
        
        // launch cuda threads, one for each GPU device
        for(int dev = 0; dev < nbDev; dev++) {
            new Thread(new CUDAThread(dev), "CUDA-thread-" + dev).start();
        }
        
    }
    
    @Override
    public List<Double[]> aggrViewShed(final DirectPosition2D p, final double startZ, final double destZ, final boolean inverse, 
            final Bounds bounds, final List<? extends ViewShedMetric> metrics) {
        CUDAExec<List<Double[]>> r = new CUDAExec<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewShed(cg, (float) startZ, (float) destZ, inverse, bounds);

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
            return r.getResult();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public ViewShedResult calcViewShed(final DirectPosition2D p, final double startZ, final double destZ, 
            final boolean inverse, final Bounds bounds)  {
            
        CUDAExec<ViewShedResult> r = new CUDAExec<ViewShedResult>() {
            @Override
            public ViewShedResult run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, getData().getDtm().getWidth(), getData().getDtm().getHeight(), 1, null);
                byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewShed(cg, (float) startZ, (float) destZ, inverse, bounds);
                cudaContext.getView(viewBuf);
                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return new SimpleViewShedResult(cg, view, ComputeViewCUDA.this);
            }
        };
        
        try {
            queue.put(r);
            return r.getResult();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    @Override
    public List<Double[]> aggrViewTan(final DirectPosition2D p, final double startZ, 
            final Bounds bounds, final List<? extends ViewTanMetric> metrics) {
        CUDAExec<List<Double[]>> r = new CUDAExec<List<Double[]>>() {
            @Override
            public List<Double[]> run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewTan(cg, startZ, getRadaPrec(), bounds);

                CUDAViewTanResult view = new CUDAViewTanResult(cg, cudaContext, ComputeViewCUDA.this);
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
            return r.getResult();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    @Override
    public ViewTanResult calcViewTan(final DirectPosition2D p, final double startZ, final Bounds bounds)  {
        CUDAExec<ViewTanResult> r = new CUDAExec<ViewTanResult>() {
            @Override
            public ViewTanResult run(CUDAContext cudaContext) {
                long time = System.currentTimeMillis();
                GridCoordinates2D cg = getWorld2Grid(p);
                cudaContext.viewTan(cg, startZ, getRadaPrec(), bounds);

                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, cudaContext.getWa(), cudaContext.getHa(), 1, null);
                int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
                cudaContext.getViewTan(viewBuf);

                Logger.getLogger(ComputeViewCUDA.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
                return new SimpleViewTanResult(cg, view, ComputeViewCUDA.this);
            }
        };
        
        try {
            queue.put(r);
            return r.getResult();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void dispose() {
        try {
            // for stoping CUDAThreads
            for(int dev = 0; dev < nbDev; dev++) {
                queue.put(CUDAExec.END);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @return true if CUDA is available
     */
    public static boolean isCUDAAvailable() {
        try {
            int res = cuInit(0);
            return res == CUresult.CUDA_SUCCESS;
        } catch(LinkageError e) {
            Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.WARNING, null, e);
            return false;
        }
    }

    /**
     * Runnable created for one GPU device for executing CUDAExec from the execution queue of ComputeViewCUDA
     */
    private class CUDAThread implements Runnable {
        private CUDAContext context;
        private int dev;
        
        /**
         * Creates a new runnable CUDAThread for the given device index
         * @param dev the device index
         */
        public CUDAThread(int dev) {
            this.dev = dev;
        }

        @Override
        public void run() {
            try {
                context = new CUDAContext(ComputeViewCUDA.this, dev);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            try {
                boolean end = false;
                List<CUDAExec> list = new ArrayList<>();
                do {
                    list.clear();
                    queue.drainTo(list);
                    if(list.isEmpty()) {
                        list.add(queue.take());
                    }  
                    for(CUDAExec run : list) {
                        if(run == CUDAExec.END) {
                            end = true;
                            break;
                        }

                        run.execute(context);
                    }
                      
                } while(!end);
            } catch (InterruptedException ex) {
                Logger.getLogger(ComputeViewCUDA.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                context.dispose();
            }
        }
        
    }
    
}
