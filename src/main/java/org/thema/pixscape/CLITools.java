/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.parallel.ParallelFExecutor;
import org.thema.common.swing.TaskMonitor;
import org.thema.parallel.ExecutorService;
import org.thema.parallel.ParallelExecutor;
import org.thema.parallel.ParallelTask;
import org.thema.pixscape.Project.Aggregate;

/**
 *
 * @author gvuidel
 */
public class CLITools {
    private Project project;
    private File resDir;
    private double zEye = 1.8;
    private double zDest = -1;
    private Bounds bounds = new Bounds();
    
    public void execute(String [] arg) throws Throwable {
        if(arg[0].equals("--help")) {
            System.out.println("Usage :\njava -jar pimage.jar [-mpi | -proc n | -cuda n]\n"  +
                    //"dtm=raster_file [resz=val] [dsm=raster_file] [land=raster_file]\n" +
                    "--project project_file.xml\n" +
                    "[-zeye val] [-zdest val] [-resdir path]\n" +
                    "[-bounds [dmin=val] [dmax=val] [orien=val] [amp=val] [zmin=val] [zmax=val]] command\n" +
                    "Commands list :\n" +
                    "--viewshed [indirect] x y\n" +
                    "--viewtan [prec=deg] x y\n" +
                    "--global [sample=val] [indirect] [from=code1,..,coden] [to=code1,..,coden] [aggr=sum|shannon] [dist=d1,...,dn]\n" +
                    "--globaltan [sample=val] [prec=deg] [from=code1,..,coden] [to=code1,..,coden] [aggr=sum|shannon]\n");
            return;
        }
        
        TaskMonitor.setHeadlessStream(new PrintStream(File.createTempFile("java", "monitor")));

        List<String> args = new ArrayList<>(Arrays.asList(arg));
        int useCUDA = 0;
        
        // parallel options
        while(!args.isEmpty() && !args.get(0).startsWith("--project")) {
            String p = args.remove(0);
            switch (p) {
                case "-proc":
                    int n = Integer.parseInt(args.remove(0));
                    ParallelFExecutor.setNbProc(n);
                    ParallelExecutor.setNbProc(n);
                    break;
                case "-cuda":
                    useCUDA = Integer.parseInt(args.remove(0));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option " + p);
            }
        }
        if(args.isEmpty())
            throw new IllegalArgumentException("Need --project");
        
        //project = createProject(args);
        args.remove(0);
        project = Project.loadProject(new File(args.remove(0)));
        project.setUseCUDA(useCUDA);
        resDir = new File(".");
        
        // global options
        if(!args.isEmpty() && !args.get(0).startsWith("--")) {
            String p = args.remove(0);
            switch (p) {
                case "-bounds":
                    while(!args.isEmpty() && !args.get(0).startsWith("-")) {
                        p = args.remove(0);
                        if(p.startsWith("dmin=")) {
                            bounds.setDmin(Double.parseDouble(p.split("=")[1]));
                        } else if(p.startsWith("dmax=")) {
                            bounds.setDmax(Double.parseDouble(p.split("=")[1]));
                        } else if(p.startsWith("orien=")) {
                            bounds = bounds.createBounds(Double.parseDouble(p.split("=")[1]));
                        } else if(p.startsWith("amp=")) {
                            bounds = bounds.createBounds(bounds.getOrientation(), Double.parseDouble(p.split("=")[1]));
                        } else if(p.startsWith("zmin=")) {
                            bounds.setZMin(Double.parseDouble(p.split("=")[1]));
                        } else if(p.startsWith("zmax=")) {
                            bounds.setZMax(Double.parseDouble(p.split("=")[1]));
                        } else
                            throw new IllegalArgumentException("Unknown option " + p);
                    }
                    break;
                case "-zeye":
                    p = args.remove(0);
                    zEye = Double.parseDouble(p);
                    break;
                case "-zdest":
                    p = args.remove(0);
                    zDest = Double.parseDouble(p);
                    break;
                case "-resdir":
                    p = args.remove(0);
                    resDir = new File(p);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option " + p);
            }
        }
        
        if(args.isEmpty())
            throw new IllegalArgumentException("No command to execute");
        
        try {
            String p = args.remove(0);
            // treat each command
            while(p != null) {
                switch (p) {
                    case "--viewshed":
                        viewShed(args);
                        break;
                    case "--viewtan":
                        viewTan(args);
                        break;
                    case "--global":
                        globalVisibility(args);
                        break;
                    case "--globaltan":
                        globalTan(args);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command " + p);
                }

                p = !args.isEmpty() ? args.remove(0) : null;
            }
        } finally {
            project.close();
        }
    }

    private void viewShed(List<String> args) throws IOException, TransformException {
        if(args.size() < 2)
            throw new IllegalArgumentException("viewshed command needs at least 2 parameters");
        boolean direct = true;
        if(args.get(0).equals("indirect")) {
            direct = false;
            args.remove(0);
        }
        DirectPosition2D c = new DirectPosition2D(Double.parseDouble(args.remove(0)), Double.parseDouble(args.remove(0)));
        WritableRaster view = project.calcViewShed(c, zEye, zDest, direct, bounds);
        new GeoTiffWriter(new File(resDir, "viewshed-" + c.x + "," + c.y + "-" + (direct ? "direct" : "indirect") + ".tif")).write(
                new GridCoverageFactory().create("view", view, project.getDtmCov().getEnvelope2D()), null);
    }
    
    private void viewTan(List<String> args) throws IOException, TransformException {
        if(args.size() < 2)
            throw new IllegalArgumentException("viewtan command needs at least 2 parameters");
        double aPrec = 0.1 * Math.PI / 180;
        if(args.get(0).startsWith("prec=")) {
            aPrec = Double.parseDouble(args.remove(0).split("=")[1]);
        }
        DirectPosition2D p = new DirectPosition2D(Double.parseDouble(args.remove(0)), Double.parseDouble(args.remove(0)));
        WritableRaster viewTan = project.calcViewTan(p, zEye, aPrec, bounds);
        // create landuse, z and dist images.
        GridCoordinates2D c = project.getDtmCov().getGridGeometry().worldToGrid(p);
        WritableRaster viewTanZ = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 
                viewTan.getWidth(), viewTan.getHeight(), 1), null);
        WritableRaster viewTanDist = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 
                viewTan.getWidth(), viewTan.getHeight(), 1), null);
        WritableRaster viewTanLand = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, 
                viewTan.getWidth(), viewTan.getHeight(), 1), null);

        project.fillViewTan(c, viewTan, viewTanZ, viewTanDist, viewTanLand);
        Envelope2D env = new Envelope2D(null, -180, -90, 360, 180);
        new GeoTiffWriter(new File(resDir, "viewtan-" + c.x + "," + c.y + "-z.tif")).write(
                new GridCoverageFactory().create("view", viewTanZ, env), null);
        new GeoTiffWriter(new File(resDir, "viewtan-" + c.x + "," + c.y + "-dist.tif")).write(
                new GridCoverageFactory().create("view", viewTanDist, env), null);
        if(project.hasLandUse())
            new GeoTiffWriter(new File(resDir, "viewtan-" + c.x + "," + c.y + "-land.tif")).write(
                new GridCoverageFactory().create("view", viewTanLand, env), null);
    }

    private void globalVisibility(List<String> args) throws IOException {
        int sample = 1;
        if(!args.isEmpty() && args.get(0).startsWith("sampling=")) {
            sample = Integer.parseInt(args.remove(0).split("=")[1]);
        }
        
        boolean direct = true;
        if(!args.isEmpty() && args.get(0).equals("indirect")) {
            direct = false;
            args.remove(0);
        }
        HashSet<Integer> from = new HashSet<>();
        if(!args.isEmpty() && args.get(0).startsWith("from=")) {
            String [] codes = args.remove(0).split("=")[1].split(",");
            for(String code : codes)
                from.add(Integer.parseInt(code));
        }
        HashSet<Integer> to = new HashSet<>();
        if(!args.isEmpty() && args.get(0).startsWith("to=")) {
            String [] codes = args.remove(0).split("=")[1].split(",");
            for(String code : codes)
                to.add(Integer.parseInt(code));
        }
        Aggregate aggr = Aggregate.NONE;
        if(!args.isEmpty() && args.get(0).startsWith("aggr=")) {
            String s = args.remove(0).split("=")[1];
            switch (s) {
                case "sum":
                    aggr = Aggregate.SUM;
                    break;
                case "shannon":
                    aggr = Aggregate.SHANNON;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown aggregation : " + s);
            }
        
        }
        TreeSet<Double> distSet = new TreeSet<>();
        distSet.add(bounds.getDmin());
        if(!args.isEmpty() && args.get(0).startsWith("dist=")) {
            String [] di = args.remove(0).split("=")[1].split(",");
            for(String d : di)
                distSet.add(Double.parseDouble(d));
        }
        List<Double> dists = new ArrayList<>(distSet.subSet(bounds.getDmin(), bounds.getDmax()));
        dists.add(bounds.getDmax());
        
        for(int i = 0; i < dists.size()-1; i++) {
            bounds.setDmin(dists.get(i));
            bounds.setDmax(dists.get(i+1));
            ParallelTask task;
            if(from.isEmpty() && to.isEmpty() && aggr != Aggregate.SHANNON) {
                //project.calcVisibility(zEye, zDest, direct, bounds, true, null);
                task = new GlobalViewTask(zEye, zDest, direct, bounds, sample, resDir, null);
            } else {
                if(!project.hasLandUse()) {
                    throw new IllegalArgumentException("No land use defined");
                }
                if(from.isEmpty())
                    from.addAll(project.getCodes());
                if(to.isEmpty())
                    to.addAll(project.getCodes());
                if(aggr == Aggregate.NONE) {
//                    project.calcVisibility(zEye, zDest, direct, bounds, from, to, true, null);
                    task = new GlobalViewLandUseTask(zEye, zDest, direct, bounds, from, to, sample, resDir, null);   
                } else {
                    task = new GlobalViewTask(zEye, zDest, direct, bounds, from, to, aggr, sample, resDir, null);   
//                    project.calcVisibility(zEye, zDest, direct, bounds, from, to, aggr, true, null);             
                } 
            }
            ExecutorService.execute(task);
        }
    }
    
    private void globalTan(List<String> args) throws IOException {
        int sample = 1;
        if(!args.isEmpty() && args.get(0).startsWith("sampling=")) {
            sample = Integer.parseInt(args.remove(0).split("=")[1]);
        }
        
        double aPrec = 0.1 * Math.PI / 180;
        if(!args.isEmpty() && args.get(0).startsWith("prec=")) {
            aPrec = Double.parseDouble(args.remove(0).split("=")[1]);
        }
        
        HashSet<Integer> from = new HashSet<>();
        if(!args.isEmpty() && args.get(0).startsWith("from=")) {
            String [] codes = args.remove(0).split("=")[1].split(",");
            for(String code : codes)
                from.add(Integer.parseInt(code));
        }
        HashSet<Integer> to = new HashSet<>();
        if(!args.isEmpty() && args.get(0).startsWith("to=")) {
            String [] codes = args.remove(0).split("=")[1].split(",");
            for(String code : codes)
                to.add(Integer.parseInt(code));
        }
        Aggregate aggr = Aggregate.NONE;
        if(!args.isEmpty() && args.get(0).startsWith("aggr=")) {
            String s = args.remove(0).split("=")[1];
            switch (s) {
                case "sum":
                    aggr = Aggregate.SUM;
                    break;
                case "shannon":
                    aggr = Aggregate.SHANNON;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown aggregation : " + s);
            }
        }
        
        ParallelTask task;
        if(from.isEmpty() && to.isEmpty() && aggr != Aggregate.SHANNON) {
            task = new GlobalViewTask(zEye, aPrec, bounds, sample, resDir, null);
        } else {
            if(!project.hasLandUse()) {
                throw new IllegalArgumentException("No land use defined");
            }
            if(from.isEmpty())
                from.addAll(project.getCodes());
            if(to.isEmpty())
                to.addAll(project.getCodes());
            if(aggr == Aggregate.NONE) {
                task = new GlobalViewLandUseTask(zEye, aPrec, bounds, from, to, sample, resDir, null);   
            } else {
                task = new GlobalViewTask(zEye, aPrec, bounds, from, to, aggr, sample, resDir, null);             
            } 
        }
        ExecutorService.execute(task);
        
    }

    
//    public static Project createProject(List<String> args) throws IOException {
//
//        File dtmFile = new File(args.remove(0).split("=")[1]);
//        double resZ = 1;
//        
//        if(!args.isEmpty() && args.get(0).startsWith("resz=")) {
//            resZ = Double.parseDouble(args.remove(0).split("=")[1]);
//        }
//        
//        Project project = new Project(IOImage.loadCoverage(dtmFile), resZ);
//
//        if(!args.isEmpty() && args.get(0).startsWith("dsm=")) {
//            File f = new File(args.remove(0).split("=")[1]);
//            project.setDSM(IOImage.loadCoverage(f));
//        } 
//        if(!args.isEmpty() && args.get(0).startsWith("land=")) {
//            File f = new File(args.remove(0).split("=")[1]);
//            project.setLandUse(IOImage.loadCoverage(f));
//        }
//
//        return project;
//    }
    
}
