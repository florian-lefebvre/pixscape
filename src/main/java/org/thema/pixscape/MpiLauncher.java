
package org.thema.pixscape;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.SchemaException;
import org.thema.parallel.mpi.MainMPI;
import org.thema.parallel.mpi.OpenMPIInterface;

/**
 * Start point for MPI execution.
 * 
 * @author Gilles Vuidel
 */
public class MpiLauncher extends MainMPI {

    /**
     * Creates a new MpiLauncher passing command line argument
     * @param args command line argument from public static void main method
     */
    public MpiLauncher(String[] args) {
        super(new OpenMPIInterface(), args);
    }
    
    @Override
    public void master() {
        try {
            new CLITools().execute(args);
        } catch (IOException | SchemaException ex) {
            Logger.getLogger(MpiLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initWorker(String [] args) throws IOException {
        // does nothing
        // project must be loaded from the task.init method if needed
    }
    
}
