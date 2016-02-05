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
