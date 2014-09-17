/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thema.pixscape;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thema.parallel.mpi.MainMPI;
import org.thema.parallel.mpi.OpenMPIInterface;

/**
 *
 * @author gvuidel
 */
public class MpiLauncher extends MainMPI {

    public MpiLauncher(String[] args) {
        super(new OpenMPIInterface(), args);
    }
    
    @Override
    public void master() {
        try {
            new CLITools().execute(args);
        } catch (Throwable ex) {
            Logger.getLogger(MpiLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initWorker(String [] args) throws IOException {
        if(args.length < 2 || !args[0].equals("--project"))
            throw new IllegalArgumentException();
        
        Project.loadProject(new File(args[1]));
    }
    
}
