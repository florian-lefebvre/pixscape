/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view.cuda;

/**
 *
 * @author gvuidel
 */
abstract class CUDARunnable<T> {
    public static final CUDARunnable END = new CUDARunnable() {
        @Override
        public Object run(ComputeViewCUDA.CUDAContext context) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    T result;

    public abstract T run(ComputeViewCUDA.CUDAContext context);

    public synchronized T get() throws InterruptedException {
        while (result == null) {
            wait();
        }
        if (result instanceof Throwable) {
            throw new RuntimeException((Throwable) result);
        } else {
            return result;
        }
    }
    
}
