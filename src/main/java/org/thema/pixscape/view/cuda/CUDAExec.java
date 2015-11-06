
package org.thema.pixscape.view.cuda;

/**
 * Abstract class for executing a calculation in a CUDA thread.
 * 
 * @author Gilles Vuidel
 */
abstract class CUDAExec<T> {
    
    /**
     * Special no-op CUDAExec for stoping CUDAThreads
     */
    public static final CUDAExec END = new CUDAExec() {
        @Override
        public Object run(CUDAContext context) {
            throw new UnsupportedOperationException();
        }
    };
    
    private T result;
    private Throwable error;

    /**
     * Launch the calculation.
     * Must be called from a CUDA thread
     * @param context the CUDA context
     */
    public void execute(CUDAContext context) {
        try {
            result = run(context);
        } catch(Exception e) {
            error = e;
        } finally {
            if(result == null && error == null) {
                error = new UnknownError();
            }
            synchronized(this) {
                notify();
            }
        }
    }
    
    /**
     * The calculation executed in CUDAThread
     * @param context the cuda context for execution
     * @return the result of the calculation
     */
    protected abstract T run(CUDAContext context);
    
    /**
     * Returns the result of the calculation launched by {@link #execute}, waiting if needed for the end of the calculation.
     * @return the result of the calculation
     * @throws InterruptedException 
     * @throws RuntimeException if an exception has occured during the calculation in the CUDA thread
     */
    public synchronized T getResult() throws InterruptedException {
        while (result == null && error == null) {
            wait();
        }
        if (error != null) {
            throw new RuntimeException(error);
        } else {
            return result;
        }
    }
    
}
