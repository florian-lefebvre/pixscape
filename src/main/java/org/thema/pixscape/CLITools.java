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

import au.com.bytecode.opencsv.CSVWriter;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Coordinate;
import org.thema.common.Config;
import org.thema.common.ConsoleProgress;
import org.thema.data.IOImage;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.data.IOFeature;
import org.thema.drawshape.PanelMap;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.style.table.UniqueColorTable;
import org.thema.parallel.ExecutorService;
import org.thema.parallel.ParallelExecutor;
import org.thema.parallel.ParallelTask;
import org.thema.pixscape.MultiViewshedTask.RasterValue;
import org.thema.pixscape.Point2PointViewTask.Agreg;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.MultiViewTanResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Command Line Interface class.
 * 
 * @author Gilles Vuidel
 */
public class CLITools {
    private Project project;
    private File resDir;
    private double zEye;
    private double zDest = -1;
    private Bounds bounds = new Bounds();
    private int sample = 1;
    private SortedSet<Integer> from = null;
    private File pointFile = null;
    private String idField = null;
    
    /**
     * Executes the commands from the command line
     * @param arg the command line arguments
     * @throws IOException
     * @throws SchemaException
     */
    public void execute(String [] argarr) throws IOException, SchemaException {
        if(argarr[0].equals("--help")) {
            System.out.println("Usage :\njava -jar pixscape.jar --metrics\n" +
                    "java -jar pixscape.jar --create prj_name dtm_raster_file [dsm=raster_file] [landuse=raster_file] [dir=path]\n" +
                    "java -jar pixscape.jar [-mpi | -proc n | -cuda n] --project project_file.xml\n" +
                    "[--landmod zone=filezones.gpkg id=fieldname code=fieldname [height=fieldname | dsm=file.tif] [selid=id1,...,idn]]\n" +
                    "[-zeye val] [-zdest val] [-resdir path]\n" +
                    "[-bounds [dmin=val] [dmax=val] [orien=val] [amp=val] [zmin=val] [zmax=val]]\n" +
                    "[-sampling n=val | land=code1,..,coden | points=pointfile.gpkg id=fieldname]\n" +
                    "[-multi dmin=val | -mono]\n" +
                    "[-earth flat|curved [refrac=val]]\n" +
                    " commands\n\n" +
                    "Commands list :\n" +
                    "--viewshed [inverse] [point=coord_x,coord_y] [resname=name]\n" +
                    "--viewtan [prec=deg] [point=coord_x,coord_y] [resname=name]\n" +
                    "--multiviewshed format=vector|raster [inverse] [degree=height|area] [resname=name]\n" +
                    "--planmetric [inverse] metric1[[code1,...,coden]][_d1,...,dm] ... metricn[[code1,...,coden]][_d1,...,dm]\n" +
                    "--tanmetric [prec=deg] metric1[[code1,...,coden]][_d1,...,dm] ... metricn[[code1,...,coden]][_d1,...,dm]\n" +
                    "--toobject [degree=height|area] [agreg=eye|object] objects=pointfile.gpkg id=fieldname [resname=name]\n" +
                    "--addscale dtm_raster_file [dsm=raster_file] [landuse=raster_file]\n");
            return;
        }
        
        if(argarr[0].equals("--metrics")) {
            showMetrics();
            return;
        }
        
        Config.setProgressBar(new ConsoleProgress());

        List<String> args = new ArrayList<>(Arrays.asList(argarr));
        int useCUDA = 0;
        
        // parallel options
        while(!args.isEmpty() && !args.get(0).startsWith("--")) {
            String p = args.remove(0);
            switch (p) {
                case "-proc":
                    int n = Integer.parseInt(args.remove(0));
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
            throw new IllegalArgumentException("Need --project command");
        }
        String arg = args.remove(0);
        if(arg.equals("--create")) {
            project = createProject(args);
        } else {
            project = Project.load(new File(args.remove(0)));
        }
        project.setUseCUDA(useCUDA);
        resDir = project.getDirectory();
        zEye = project.getStartZ();
        
        // land mod special command
        if(!args.isEmpty() && args.get(0).equals("--landmod")) {
            args.remove(0);
            landmod(args);
            return;
        }
        
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
                        } else {
                            throw new IllegalArgumentException("Unknown param for bounds option " + p);
                        }
                    }
                    break;
                case "-sampling":
                    p = args.remove(0);
                    if(p.startsWith("n=")) {
                        sample = Integer.parseInt(p.split("=")[1]);
                    } else if(p.startsWith("land=")) {
                        from = new TreeSet<>();
                        String [] codes = p.split("=")[1].split(",");
                        for(String code : codes) {
                            from.add(Integer.parseInt(code));
                        }
                    } else if(p.startsWith("points")) {
                        pointFile = new File(p.split("=")[1]);
                        idField = args.remove(0).split("=")[1];
                    } else {
                        throw new IllegalArgumentException("Unknown param for sampling option " + p);
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
                    resDir.mkdirs();
                    break;
                case "-multi":
                    p = args.remove(0);
                    project.setMinDistMS(Double.parseDouble(p.split("=")[1]));
                    break;
                case "-mono":
                    project.setMinDistMS(0);
                    break;
                case "-earth":
                    p = args.remove(0);
                    if(p.equals("curved")) {
                        project.setEarthCurv(true);
                        if(!args.isEmpty() && args.get(0).startsWith("refrac=")) {
                            p = args.remove(0);
                            project.setCoefRefraction(Double.parseDouble(p.split("=")[1]));
                        }
                    } else {
                        project.setEarthCurv(false);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option " + p);
            }
        }
        
        if(args.isEmpty()) {
            Logger.getLogger(CLITools.class.getName()).log(Level.WARNING, "No command to execute");
            return;
        }
        
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
                    case "--multiviewshed":
                        multiViewShed(args);
                        break;
                    case "--planmetric":
                        viewMetric(args);
                        break;
                    case "--tanmetric":
                        tanMetric(args);
                        break;   
                    case "--toobject":
                        toObject(args);
                        break;   
                    case "--addscale":
                        addScale(args);
                        break;   
                    default:
                        throw new IllegalArgumentException("Unknown command " + p);
                }

                p = !args.isEmpty() ? args.remove(0) : null;
            }
        } finally {
            project.dispose();
        }
    }

    private void viewShed(List<String> args) throws IOException {   
        Map<String, String> params = extractAndCheckParams(args, Collections.EMPTY_LIST, Arrays.asList("inverse", "resname", "coord"));
        boolean inverse = params.containsKey("inverse");
        
        String name = null;
        if(params.containsKey("resname")) {
            name = params.get("resname");
        }
        
        if(params.containsKey("coord")) {
            String[] coords = params.get("coord").split(",");
            DirectPosition2D p = new DirectPosition2D(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
            Raster view = project.getDefaultComputeView().calcViewShed(p, zEye, zDest, inverse, bounds).getView();
            IOImage.saveTiffCoverage(new File(resDir, (name == null ? "viewshed-" + p.x + "," + p.y : name) + ".tif"),
                    new GridCoverageFactory().create("view", (WritableRaster)view, project.getDtmCov().getEnvelope2D()));
        } else {
            if(name == null) {
                name = "viewshed"+ (inverse ? "-inverse" : "");
            }
            List<DefaultFeature> points = IOFeature.loadFeatures(pointFile, idField);
            for(Feature p : points) {
                Bounds b = bounds.updateBounds(p);
                double zE = zEye;
                double zD = zDest;
                if(p.getAttributeNames().contains("height")) {
                    if(inverse) {
                        zD = ((Number)p.getAttribute("height")).doubleValue();
                    } else {
                        zE = ((Number)p.getAttribute("height")).doubleValue();
                    }
                }
                Raster view = project.getDefaultComputeView().calcViewShed(
                        new DirectPosition2D(p.getGeometry().getCoordinate().x, p.getGeometry().getCoordinate().y),
                        zE, zD, inverse, b).getView();
                IOImage.saveTiffCoverage(new File(resDir, name + "_" + p.getId() + ".tif"),
                        new GridCoverageFactory().create("view", (WritableRaster)view, project.getDtmCov().getEnvelope2D()));
                
            }
        } 

    }
    
    private void viewTan(List<String> args) throws IOException {
        Map<String, String> params = extractAndCheckParams(args, Collections.EMPTY_LIST, Arrays.asList("prec", "resname", "coord"));

        if(params.containsKey("prec")) {
            double aPrec = Double.parseDouble(params.get("prec"));
            project.setAlphaPrec(aPrec);
        }
        
        String name = null;
        if(params.containsKey("resname")) {
            name = params.get("resname");
        }
        
        if(params.containsKey("coord")) {
            String[] coords = params.get("coord").split(",");
            DirectPosition2D p = new DirectPosition2D(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
            calcViewTan(p, zEye, bounds, name == null ? "viewtan-" + p.x + "," + p.y : name);
        } else {
            if(name == null) {
                name = "viewtan";
            }
            List<DefaultFeature> points = IOFeature.loadFeatures(pointFile, idField);
            for(Feature p : points) {
                Bounds b = bounds.updateBounds(p);
                double height = zEye;
                if(p.getAttributeNames().contains("height")) {
                    height = ((Number)p.getAttribute("height")).doubleValue();
                }
                calcViewTan(new DirectPosition2D(p.getGeometry().getCoordinate().x, p.getGeometry().getCoordinate().y), height, b, name + "_" + p.getId());
            }
        }
    }
        
    private void calcViewTan(DirectPosition2D p, double height, Bounds bounds, String name) throws IOException {
        ViewTanResult result = project.getDefaultComputeView().calcViewTan(p, height, bounds);
        int yMin = (int) ((90-bounds.getZMax()) / project.getAlphaPrec());
        int yMax = (int) ((90-bounds.getZMin()) / project.getAlphaPrec());
        Envelope2D env = new Envelope2D(null, bounds.getOrientation()-bounds.getAmplitude()/2, bounds.getZMin(), bounds.getAmplitude(), bounds.getZMax()-bounds.getZMin());
        if(!(result instanceof MultiViewTanResult)) {
            IOImage.saveTiffCoverage(new File(resDir, name + "-elev.tif"),
                    new GridCoverageFactory().create("view", ((WritableRaster)result.getElevationView())
                            .createWritableChild(0, yMin, result.getThetaWidth(), yMax-yMin, 0, 0, null), env));
            IOImage.saveTiffCoverage(new File(resDir, name + "-dist.tif"),
                    new GridCoverageFactory().create("view", ((WritableRaster)result.getDistanceView())
                            .createWritableChild(0, yMin, result.getThetaWidth(), yMax-yMin, 0, 0, null), env));
        }
        if(project.hasLandUse()) {
            GridCoverage2D cov = new GridCoverageFactory().create("view", ((WritableRaster)result.getLanduseView())
                    .createWritableChild(0, yMin, result.getThetaWidth(), yMax-yMin, 0, 0, null), env);
            IOImage.saveTiffCoverage(new File(resDir, name + "-land.tif"), cov);

            PanelMap map = new PanelMap();
            RasterStyle s = new RasterStyle(new UniqueColorTable((Map)project.getLandColors()), false, false);
            s.setNoDataValue(-1);
            map.addShape(new RasterShape(((WritableRaster)result.getLanduseView())
                    .createWritableChild(0, yMin, result.getThetaWidth(), yMax-yMin, 0, 0, null), env, s, true));
            map.exportPng(new File(resDir, name + "-land.png"), result.getThetaWidth());
           
        }

    }

    private void multiViewShed(List<String> args) throws IOException, SchemaException {   
        Map<String, String> params = extractAndCheckParams(args, Arrays.asList("format"), Arrays.asList("inverse", "degree", "resname"));
        
        if(pointFile == null) {
            throw new IllegalArgumentException("-sampling points option is mandatory for --multiviewshed");
        }
        
        boolean vectorOutput = params.get("format").equals("vector");
        boolean inverse = params.containsKey("inverse");
        RasterValue outValue = RasterValue.COUNT;
        if(params.containsKey("degree")) {
            outValue = params.get("degree").equals("area") ? RasterValue.AREA : RasterValue.HEIGHT;
        }

        String name = "multiviewshed" + (inverse ? "-inverse" : "");
        if(params.containsKey("resname")) {
            name = params.get("resname");
        }
        
        List<Feature> points = (List)IOFeature.loadFeatures(pointFile, idField);
        MultiViewshedTask task = new MultiViewshedTask(points, project, inverse, zDest, bounds, vectorOutput, outValue, Config.getProgressBar("MultiViewshed"));
        
        ExecutorService.execute(task);
        
        task.saveResult(resDir, name);

    }
    
    private void viewMetric(List<String> args) throws IOException {
        boolean inverse = false;
        if(!args.isEmpty() && args.get(0).equals("inverse")) {
            inverse = true;
            args.remove(0);
        }
        List<ViewShedMetric> metrics = new ArrayList<>();
        while(!args.isEmpty()) {
            String s = args.remove(0);
            Metric m = Project.getMetricWithParams(s);            
            if(!(m instanceof ViewShedMetric)) {
                throw new IllegalArgumentException("The metric " + m + " is not a viewshed metric");
            }
            metrics.add((ViewShedMetric) m);
        }
        ParallelTask task;
        if(pointFile == null) {
            task = new GridMetricTask(project, zEye, zDest, inverse, bounds, from, metrics, sample, resDir, Config.getProgressBar("Metric"));
        } else {
            task = new PointMetricTask(project, zEye, zDest, inverse, bounds, metrics, pointFile, idField, resDir, Config.getProgressBar("Metric"));
        }
        
        ExecutorService.execute(task);
    }
    
    private void tanMetric(List<String> args) throws IOException {
        if(!args.isEmpty() && args.get(0).startsWith("prec=")) {
            double aPrec = Double.parseDouble(args.remove(0).split("=")[1]);
            project.setAlphaPrec(aPrec);
        }

        List<ViewTanMetric> metrics = new ArrayList<>();
        while(!args.isEmpty()) {
            String s = args.remove(0);
            Metric m = Project.getMetricWithParams(s);        
            if(!(m instanceof ViewTanMetric)) {
                throw new IllegalArgumentException("The metric " + m + " is not a tangential metric");
            }
            metrics.add((ViewTanMetric) m);
        }
        ParallelTask task;
        if(pointFile == null) {
            task = new GridMetricTask(project, zEye, bounds, from, metrics, sample, resDir, Config.getProgressBar("Metric"));
        } else {
            task = new PointMetricTask(project, zEye, bounds, metrics, pointFile, idField, resDir, Config.getProgressBar("Metric"));
        }
        
        ExecutorService.execute(task);
    }
    
    private void toObject(List<String> args) throws IOException, SchemaException {   
        
        if(pointFile == null) {
            throw new IllegalArgumentException("-sampling points option is mandatory for --toobject command");
        }
        
        Map<String, String> params = extractAndCheckParams(args, Arrays.asList("objects", "id"), Arrays.asList("degree", "agreg", "resname"));
        
        RasterValue outValue = RasterValue.COUNT;
        if(params.containsKey("degree")) {
            outValue = params.get("degree").equals("area") ? RasterValue.AREA : RasterValue.HEIGHT;
        }
        Agreg agreg = null;
        if(params.containsKey("agreg")) {
            if(params.get("agreg").equals("eye")) {
                agreg = Agreg.EYE;
            } else {
                agreg = Agreg.OBJECT;
            }
        }
        String resname = "toobject";
        if(params.containsKey("resname")) {
            resname = params.get("resname");
        }
        File objFile = new File(params.get("objects"));
        String objId = params.get("id");

        Point2PointViewTask task = new Point2PointViewTask(pointFile, idField, zEye, objFile, objId, zDest, 
                outValue, agreg, bounds, project, Config.getProgressBar("ToObject"));
        ExecutorService.execute(task);
        
        Map result = task.getResult();
        
        if(agreg == null) {
            List<DefaultFeature> eyePoints = IOFeature.loadFeatures(pointFile, idField);
            List<DefaultFeature> objPoints = IOFeature.loadFeatures(objFile, objId);
            File f = new File(resDir, resname + ".csv");
            try (CSVWriter w = new CSVWriter(new FileWriter(f))) {
                w.writeNext(new String[]{"IdEye", "IdObj", "View", "Dist", "Azimuth"});
                for(Feature eye : eyePoints) {
                    if(result.containsKey(eye.getId())) {
                        Map objs = (Map)result.get(eye.getId());
                        for(Feature obj : objPoints) {
                            if(objs.containsKey(obj.getId())) {
                                double view = (double) objs.get(obj.getId());
                                Coordinate c0 = eye.getGeometry().getCoordinate();
                                Coordinate c1 = obj.getGeometry().getCoordinate();
                                w.writeNext(new String[]{eye.getId().toString(), obj.getId().toString(), String.valueOf(view),
                                    String.valueOf(c0.distance(c1)), 
                                    String.valueOf(Bounds.rad2deg(Math.atan2(c0.y-c1.y, c1.x-c0.x)))});
                            }
                        }
                    }
                }
            }
        } else {
            List<DefaultFeature> points = agreg == Agreg.EYE ? (List)IOFeature.loadFeatures(pointFile, idField) : 
                    IOFeature.loadFeatures(objFile, objId);
            DefaultFeature.addAttribute("TotView", points, 0.0);
            for(DefaultFeature point : points) {
                point.setAttribute("TotView", result.get(point.getId()));
            }
            File f = new File(resDir, resname + ".gpkg");
            IOFeature.saveFeatures(points, f);
        }
    }
    
    private void landmod(final List<String> args) throws IOException, SchemaException {
        File fileZone = new File(args.remove(0).split("=")[1]);
        String idZoneField = args.remove(0).split("=")[1];
        String codeField = args.remove(0).split("=")[1];
        File dsmFile = null;
        String heightField = null;
        String arg = args.remove(0);
        if(arg.startsWith("height=")) {
            heightField = arg.split("=")[1];
        } else {
            dsmFile = new File(arg.split("=")[1]);
        }
        List<String> selIds = null;
        if(!args.isEmpty() && args.get(0).startsWith("selid=")) {
            selIds = Arrays.asList(args.remove(0).split("=")[1].split(","));
        }
        
        LandModTask task = heightField != null ? new LandModTask(project, fileZone, idZoneField, codeField, heightField, selIds, args)
                : new LandModTask(project, fileZone, idZoneField, codeField, dsmFile, selIds, args);
        ExecutorService.executeSequential(task);

        args.clear();
    }
    
    
    private Project createProject(List<String> args) throws IOException {
        String name = args.remove(0);
        File dtm = new File(args.remove(0));
        Map<String, String> params = extractAndCheckParams(args, Collections.EMPTY_LIST, Arrays.asList("landuse", "dsm", "dir"));
        File dir = new File(params.containsKey("dir") ? params.get("dir") : name);
        
        GridCoverage2D dtmCov = IOImage.loadCoverage(dtm);
        Project prj = new Project(name, dir, dtmCov, 1);
        if(params.containsKey("dsm")) {
            prj.setDSM(IOImage.loadCoverage(new File(params.get("dsm"))));
        }
        if(params.containsKey("landuse")) {
            prj.setLandUse(IOImage.loadCoverage(new File(params.get("landuse"))));
        }
        
        return prj;
    }
    
    private void addScale(List<String> args) throws IOException {
        File dtm = new File(args.remove(0));
        Map<String, String> params = extractAndCheckParams(args, Collections.EMPTY_LIST, Arrays.asList("landuse", "dsm"));
        GridCoverage2D dtmCov = IOImage.loadCoverage(dtm);
        Raster land = null, dsm = null;
        if(params.containsKey("dsm")) {
            dsm = IOImage.loadCoverage(new File(params.get("dsm"))).getRenderedImage().getData();
        }
        if(params.containsKey("landuse")) {
            land = IOImage.loadCoverage(new File(params.get("landuse"))).getRenderedImage().getData();
        }
        
        project.addScaleData(new ScaleData(dtmCov, land, dsm));
    }

     
    private void showMetrics() {
        System.out.println("===== Metrics =====");
        for(Metric indice : Project.getMetrics(Metric.class)) {
            System.out.println(indice.toString());
        }
       
    }
    
    private static Map<String, String> extractAndCheckParams(List<String> args, List<String> mandatoryParams, List<String> optionalParams) {
        Map<String, String> params = new LinkedHashMap<>();
                
        while(!args.isEmpty() && !args.get(0).startsWith("-")) {
            String arg = args.remove(0);
            if(arg.contains("=")) {
                String[] tok = arg.split("=");
                params.put(tok[0], tok[1]);
            } else {
                params.put(arg, null);
            }
        }
        
        // check mandatory parameters
        if(!params.keySet().containsAll(mandatoryParams)) {
            HashSet<String> set = new HashSet<>(mandatoryParams);
            set.removeAll(params.keySet());
            throw new IllegalArgumentException("Mandatory parameters are missing : " + Arrays.deepToString(set.toArray()));
        }
        
        // check unknown parameters if optionalParams is set
        if(optionalParams != null) {
            HashSet<String> set = new HashSet<>(params.keySet());
            set.removeAll(mandatoryParams);
            set.removeAll(optionalParams);
            if(!set.isEmpty()) {
                throw new IllegalArgumentException("Unknown parameters : " + Arrays.deepToString(set.toArray()));
            }
        }
        
        return params;
    }

}
