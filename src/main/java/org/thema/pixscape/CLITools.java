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
import java.util.List;
import java.util.SortedSet;
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
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

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
    private int sample = 1;
    private SortedSet<Integer> from = null;
    private File pointFile = null;
    
    public void execute(String [] arg) throws Throwable {
        if(arg[0].equals("--help")) {
            System.out.println("Usage :\njava -jar pixscape.jar --metrics\n" +
                    "java -jar pixscape.jar [-mpi | -proc n | -cuda n]\n"  +
                    "--project project_file.xml\n" +
                    "[-zeye val] [-zdest val] [-resdir path]\n" +
                    "[-bounds [dmin=val] [dmax=val] [orien=val] [amp=val] [zmin=val] [zmax=val]]\n" +
                    "[-sampling n=val | land=code1,..,coden | points=shapefile] command\n" +
                    "Commands list :\n" +
                    "--viewshed [indirect] x y\n" +
                    "--viewtan [prec=deg] x y\n" +
                    "--viewmetric [indirect] metric1[[code1,...,coden]] ... metricn[[code1,...,coden]]\n" +
                    "--tanmetric [prec=deg] metric1[[code1,...,coden]] ... metricn[[code1,...,coden]]\n");
            return;
        }
        
        if(arg[0].equals("--metrics")) {
            showMetrics();
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
        if(args.isEmpty()) {
            throw new IllegalArgumentException("Need --project");
        }
        
        //project = createProject(args);
        args.remove(0);
        project = Project.loadProject(new File(args.remove(0)));
        project.setUseCUDA(useCUDA);
        resDir = new File(".");
        
        // global options
        while(!args.isEmpty() && !args.get(0).startsWith("--")) {
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
                            throw new IllegalArgumentException("Unknown param for bounds option " + p);
                    }
                    break;
                case "-sampling":
                    p = args.remove(0);
                    if(p.startsWith("n=")) {
                        sample = Integer.parseInt(p.split("=")[1]);
                    } else if(p.startsWith("land=")) {
                        from = new TreeSet<>();
                        String [] codes = args.remove(0).split("=")[1].split(",");
                        for(String code : codes)
                            from.add(Integer.parseInt(code));
                    } else if(p.startsWith("points")) {
                        pointFile = new File(p.split("=")[1]);
                    } else
                        throw new IllegalArgumentException("Unknown param for sampling option " + p);
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
                    resDir.mkdirs();
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
                    case "--viewmetric":
                        viewMetric(args);
                        break;
                    case "--tanmetric":
                        tanMetric(args);
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
        Raster view = project.calcViewShed(c, zEye, zDest, direct, bounds);
        new GeoTiffWriter(new File(resDir, "viewshed-" + c.x + "," + c.y + "-" + (direct ? "direct" : "indirect") + ".tif")).write(
                new GridCoverageFactory().create("view", (WritableRaster)view, project.getDtmCov().getEnvelope2D()), null);
    }
    
    private void viewTan(List<String> args) throws IOException, TransformException {
        if(args.size() < 2)
            throw new IllegalArgumentException("viewtan command needs at least 2 parameters");
        double aPrec = 0.1;
        if(args.get(0).startsWith("prec=")) {
            aPrec = Double.parseDouble(args.remove(0).split("=")[1]);
        }
        aPrec = aPrec * Math.PI / 180;
        DirectPosition2D p = new DirectPosition2D(Double.parseDouble(args.remove(0)), Double.parseDouble(args.remove(0)));
        Raster viewTan = project.calcViewTan(p, zEye, aPrec, bounds);
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

    private void viewMetric(List<String> args) throws IOException {
        boolean direct = true;
        if(!args.isEmpty() && args.get(0).equals("indirect")) {
            direct = false;
            args.remove(0);
        }
        List<ViewShedMetric> metrics = new ArrayList<>();
        while(!args.isEmpty()) {
            String s = args.remove(0);
            TreeSet<Integer> codes = null;
            if(s.contains("[")) {
                codes = new TreeSet<>();
                String lst = s.split("\\[")[1].replace("]", "");
                String [] tokens = lst.split(",");
                for(String code : tokens)
                    codes.add(Integer.parseInt(code));
                s = s.split("\\[")[0];
            }
            Metric m = Project.getMetric(s);
            if(!(m instanceof ViewShedMetric))
                throw new IllegalArgumentException("The metric " + m + " is not a viewshed metric");
            m.setCodes(codes);
            metrics.add((ViewShedMetric) m);
        }
        ParallelTask task;
        if(pointFile == null) {
            task = new GridMetricTask(zEye, zDest, direct, bounds, from, metrics, sample, resDir, null);
        } else {
            task = new PointMetricTask(zEye, zDest, direct, bounds, metrics, pointFile, resDir, null);
        }
        
        ExecutorService.execute(task);
    }
    
    private void tanMetric(List<String> args) throws IOException {
        double aPrec = 0.1;
        if(!args.isEmpty() && args.get(0).startsWith("prec=")) {
            aPrec = Double.parseDouble(args.remove(0).split("=")[1]);
        }
        aPrec = aPrec * Math.PI / 180;
        List<ViewTanMetric> metrics = new ArrayList<>();
        while(!args.isEmpty()) {
            String s = args.remove(0);
            TreeSet<Integer> codes = null;
            if(s.contains("[")) {
                codes = new TreeSet<>();
                String lst = s.split("\\[")[1].replace("]", "");
                String [] tokens = lst.split(",");
                for(String code : tokens)
                    codes.add(Integer.parseInt(code));
                s = s.split("\\[")[0];
            }
            Metric m = Project.getMetric(s);
            if(!(m instanceof ViewTanMetric))
                throw new IllegalArgumentException("The metric " + m + " is not a tangential metric");
            m.setCodes(codes);
            metrics.add((ViewTanMetric) m);
        }
        ParallelTask task;
        if(pointFile == null) {
            task = new GridMetricTask(zEye, aPrec, bounds, from, metrics, sample, resDir, null);
        } else {
            task = new PointMetricTask(zEye, aPrec, bounds, metrics, pointFile, resDir, null);
        }
        
        ExecutorService.execute(task);
    }

    private void showMetrics() {
        System.out.println("===== Metrics =====");
        for(Metric indice : Project.METRICS) {
            System.out.println(indice.getShortName());
        }
       
    }
    
    
}
