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

import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.thema.data.IOImage;
import org.thema.drawshape.style.SimpleStyle;
import org.thema.pixscape.metric.AbstractDistMetric;
import org.thema.pixscape.metric.AggregationMetric;
import org.thema.pixscape.metric.AreaMetric;
import org.thema.pixscape.metric.CONTAGMetric;
import org.thema.pixscape.metric.CompactMetric;
import org.thema.pixscape.metric.DepthLineMetric;
import org.thema.pixscape.metric.DistMetric;
import org.thema.pixscape.metric.EdgeDensityMetric;
import org.thema.pixscape.metric.FractalDimMetric;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.PatchDensityMetric;
import org.thema.pixscape.metric.PatchMeanSizeMetric;
import org.thema.pixscape.metric.PerimeterMetric;
import org.thema.pixscape.metric.ShanDistMetric;
import org.thema.pixscape.metric.ShannonMetric;
import org.thema.pixscape.metric.SkyLineMetric;
import org.thema.pixscape.view.ComputeView;
import org.thema.pixscape.view.ComputeViewJava;
import org.thema.pixscape.view.MultiComputeViewJava;
import org.thema.pixscape.view.SimpleComputeView;
import org.thema.pixscape.view.cuda.ComputeViewCUDA;

/**
 * Contains all the data of a project.
 * - all data scale 
 * - the coordinate reference system
 * - codes and colors for landuse
 * - and global options
 * The class is serialized into the xml project file.
 * This class creates new project, loads and saves whole project.
 * 
 * @author Gilles Vuidel
 */
public final class Project {

    private transient File dir;
    private transient SimpleComputeView simpleComputeView;
    
    private TreeMap<Double, ScaleData> scaleDatas;
    
    private String name;
    
    private String wktCRS;
    private TreeSet<Integer> codes;
    private TreeMap<Double, Color> colors;
    
    // options
    private int nbGPU = 0;
    private double startZ = 1.7;
    private double aPrec = 0.1;
    private boolean earthCurv = true;
    private double coefRefraction = 0.13;
    private double minDistMS = -1;

    
    private Project() {
        
    }
    
    /**
     * Creates a new project and saves it in prjPath
     * @param name the name of the project
     * @param prjPath the project directory, it will be created if it does not exist
     * @param dtmCov the DTM coverage
     * @param resZ the elevation resolution int meter of dtmCov
     * @throws IOException 
     */
    public Project(String name, File prjPath, GridCoverage2D dtmCov, double resZ) throws IOException {
        this.name = name;
        this.dir = prjPath;
        this.scaleDatas = new TreeMap<>();
        ScaleData scaleData = new ScaleData(dtmCov, null, null, resZ);
        scaleDatas.put(scaleData.getResolution(), scaleData);
        prjPath.mkdirs();
        IOImage.saveTiffCoverage(new File(prjPath, "dtm-" + scaleData.getResolution() + ".tif"), scaleData.getDtmCov());
        
        CoordinateReferenceSystem crs = dtmCov.getCoordinateReferenceSystem2D();
        if(crs != null) {
            wktCRS = crs.toWKT();
        }

        save();
    }
    
    /**
     * Creates a new project and saves it in prjPath
     * @param name the name of the project
     * @param prjPath the project directory, it will be created if it does not exist
     * @param scaleData the default scale data containing dtm, dsm and landuse for the finest 2D resolution
     * @throws IOException 
     */
    public Project(String name, File prjPath, ScaleData scaleData) throws IOException {
        this.name = name;
        this.dir = prjPath;
        this.scaleDatas = new TreeMap<>();
        scaleDatas.put(scaleData.getResolution(), scaleData);
        prjPath.mkdirs();
        scaleData.save(prjPath);
        
        CoordinateReferenceSystem crs = scaleData.getDtmCov().getCoordinateReferenceSystem2D();
        if(crs != null) {
            wktCRS = crs.toWKT();
        }

        save();
    }
    
    /**
     * Creates a new project based on this one and saves it in prjPath
     * @param name the name of the project
     * @param prjPath the project directory, it will be created if it does not exist
     * @param scaleData the default scale data containing dtm, dsm and landuse for the finest 2D resolution
     * @throws IOException 
     */
    public Project dupProject(String name, File prjPath, ScaleData scaleData) throws IOException {
        Project prj = new Project();
        prj.name = name;
        prj.wktCRS = wktCRS;
        prj.codes = codes;
        prj.colors = colors;
        prj.nbGPU = nbGPU;
        prj.startZ = startZ;
        prj.aPrec = aPrec;
        prj.earthCurv = earthCurv;
        prj.coefRefraction = coefRefraction;
        prj.minDistMS = -1;
        
        prj.dir = prjPath;
        prj.scaleDatas = new TreeMap<>();
        prj.scaleDatas.put(scaleData.getResolution(), scaleData);
        prjPath.mkdirs();
        scaleData.save(prjPath);
        
        CoordinateReferenceSystem crs = scaleData.getDtmCov().getCoordinateReferenceSystem2D();
        if(crs != null) {
            prj.wktCRS = crs.toWKT();
        }

        prj.save();
        return prj;
    }

    /**
     * Sets the land use coverage for the default scale and saves the project.
     * @param landCov the land use coverage
     * @throws IOException 
     * @throws IllegalStateException if the project contains multiscale data
     * @throws IllegalArgumentException if the envelope of landCov does not equals to the dtm envelope
     */
    public void setLandUse(GridCoverage2D landCov) throws IOException {
        if(hasMultiScale()) {
            throw new IllegalStateException("Cannot set land use with multi scale database");
        }
        if(!getDtmCov().getEnvelope2D().boundsEquals(landCov.getEnvelope2D(), 0, 1, 0.1)) {
            throw new IllegalArgumentException("Land use bounds does not correspond to DTM bounds");
        }
        Raster land;
        if(landCov.getRenderedImage() instanceof BufferedImage) {
            land = ((BufferedImage)landCov.getRenderedImage()).getRaster();
        } else {
            land = landCov.getRenderedImage().getData();
        }
        
        ScaleData newData = new ScaleData(getDtmCov(), land, getDefaultScaleData().getDsm());
        scaleDatas.put(newData.getResolution(), newData);
        simpleComputeView = null;
        
//        ColorModel colorModel = landCov.getRenderedImage().getColorModel();
        codes = new TreeSet<>(newData.getCodes());
        colors = new TreeMap<>();
        for(Integer code : codes) {
//            try {
//                colors.put(code.doubleValue(), new Color(colorModel.getRGB(code.intValue())));
//            } catch(IllegalArgumentException ex) {
                colors.put(code.doubleValue(), SimpleStyle.randomColor().brighter());
//            }
        }
        
        newData.save(dir);
        save();
    }
    
    /**
     * Sets the DSM coverage for the default scale and saves the project.
     * @param dsmCov the DSM coverage
     * @throws IOException 
     * @throws IllegalStateException if the project contains multiscale data
     * @throws IllegalArgumentException if the envelope of landCov does not equals to the dtm envelope
     */
    public void setDSM(GridCoverage2D dsmCov) throws IOException {
        if(hasMultiScale()) {
            throw new IllegalStateException("Cannot set land use with multi scale database");
        }
        
        if(!getDtmCov().getEnvelope2D().boundsEquals(dsmCov.getEnvelope2D(), 0, 1, 0.1)) {
            throw new IllegalArgumentException("DSM bounds does not correspond to DTM bounds");
        }
        Raster dsm;
        if(dsmCov.getRenderedImage() instanceof BufferedImage) {
            dsm = ((BufferedImage)dsmCov.getRenderedImage()).getRaster();
        } else {
            dsm = dsmCov.getRenderedImage().getData();
        }
        
        ScaleData newData = new ScaleData(getDtmCov(), getLandUse(), dsm);
        scaleDatas.put(newData.getResolution(), newData);
        simpleComputeView = null;
        
        newData.save(dir);
        save();
    }
    
    /**
     * Add data at coarser resolution for speeding up execution with multiscale algorithms.
     * Saves the project.
     * @param data the data at coarser resolution ie. resolution &gt; defaultscale resolution
     * @throws IOException 
     * @throws IllegalArgumentException if the data resolution is less than the defaultscale
     * @throws IllegalArgumentException if the data does not cover the the defaultscale data
     * @throws IllegalArgumentException if the data does not contain landuse while defaultscale so or inverse 
     */
    public void addScaleData(ScaleData data) throws IOException {
        if(data.getResolution() <= getDefaultScaleData().getResolution()) {
            throw new IllegalArgumentException("The data resolution must must be coarser.");
        }
        if(!data.getGridGeometry().getEnvelope2D().contains((Rectangle2D)getDefaultScaleData().getGridGeometry().getEnvelope2D())) {
            throw new IllegalArgumentException("The data must cover the first scale zone.");
        }
        if(hasLandUse() != data.hasLandUse()) {
            throw new IllegalArgumentException("Land use must be present for all scales or none.");
        }
        scaleDatas.put(data.getResolution(), data);
        data.save(dir);
        if(hasLandUse()) {
            codes.addAll(data.getCodes());
            for(int code : data.getCodes()) {
                if(!colors.containsKey((double)code)) {
                    colors.put((double)code, SimpleStyle.randomColor().brighter());
                }
            }
        }
        save();
    }
    
    /**
     * Removes all data scales but the finest (default scale) and saves the project.
     * @throws IOException 
     */
    public void removeMultiScaleData() throws IOException {
        scaleDatas = new TreeMap<>(scaleDatas.headMap(scaleDatas.firstKey(), true));
        save();
    }
    
    /**
     * Removes one scale data and saves the project.
     * @param res the scale data resolution to remove
     * @throws IOException 
     */
    public void removeScaleData(double res) throws IOException {
        scaleDatas.remove(res);
        save();
    }
    
    /**
     * @return the DTM coverage at the finest resolution
     */
    public GridCoverage2D getDtmCov() {
        return getDefaultScaleData().getDtmCov();
    }

    /**
     * @return the DTM raster at the finest resolution
     */
    public Raster getDtm() {
        return getDefaultScaleData().getDtm();
    }

    /**
     * @return does the project contain land use data ?
     */
    public boolean hasLandUse() {
        return getDefaultScaleData().hasLandUse();
    }

    /**
     * @return the land use raster at the finest resolution
     */
    public Raster getLandUse() {
        return getDefaultScaleData().getLand();
    }
    
    /**
     * @return the scale data at the finest resolution
     */
    public ScaleData getDefaultScaleData() {
        return scaleDatas.firstEntry().getValue();
    }
    
    /**
     * @param resolution the 2D resolution ie. pixel size
     * @return the scale data at the given resolution
     */
    public ScaleData getScaleData(double resolution) {
        return scaleDatas.get(resolution);
    }
    
    /**
     * @return all the scale datas
     */
    public Collection<ScaleData> getScaleDatas() {
        return scaleDatas.values();
    }
    
    /**
     * @return true if the project contains data at several resolutions
     */
    public boolean hasMultiScale() {
        return scaleDatas.size() > 1;
    }

    /**
     * @return all land use codes or null if the land use is not present
     */
    public SortedSet<Integer> getCodes() {
        return codes;
    }
    
    /**
     * @return the color associated with each land use code
     */
    public Map<Double, Color> getLandColors() {
        return colors;
    }

    //
    // GLOBAL OPTIONS
    //
    
    /**
     * 
     * @return true if pixscape try to use CUDA (GPU) if present
     */
    public boolean isUseCUDA() {
        return nbGPU > 0;
    }

    /**
     * Sets the number of CUDA device (GPU) to use if present.
     * @param nbGPU the number of GPU device to use
     */
    public synchronized void setUseCUDA(int nbGPU) {
        this.nbGPU = nbGPU;
        if(simpleComputeView != null) {
            simpleComputeView.dispose();
        }
        simpleComputeView = null;
    }

    /**
     * @return the default height of the observer eye
     */
    public double getStartZ() {
        return startZ;
    }

    /**
     * Sets the default height of the observer eye.
     * @param startZ the new default height of the eye
     */
    public void setStartZ(double startZ) {
        this.startZ = startZ;
    }

    /**
     * @return the angle precision in degree (for tangential view only)
     */
    public double getAlphaPrec() {
        return aPrec;
    }

    /**
     * Sets the angle precision in degree (for tangential view only)
     * @param aPrec the new angle precision in degree
     */
    public void setAlphaPrec(double aPrec) {
        this.aPrec = aPrec;
        if(simpleComputeView != null) {
            simpleComputeView.setaPrec(aPrec);
        }
    }

    /**
     * @return true if taking into account earth curvature
     */
    public boolean isEarthCurv() {
        return earthCurv;
    }

    /**
     * Sets earth curvature option
     * @param earthCurv take into account earth curvature ?
     */
    public void setEarthCurv(boolean earthCurv) {
        this.earthCurv = earthCurv;
        if(simpleComputeView != null) {
            simpleComputeView.setEarthCurv(earthCurv);
        }
    }

    /**
     * @return the current refraction correction coefficient (default 0.13), 0 for no correction
     */
    public double getCoefRefraction() {
        return coefRefraction;
    }

    /**
     * Sets the refraction correction coefficient.
     * Set to 0 for no correction, default is 0.13
     * @param coefRefraction the new refraction correction coefficient
     */
    public void setCoefRefraction(double coefRefraction) {
        this.coefRefraction = coefRefraction;
        if(simpleComputeView != null) {
            simpleComputeView.setCoefRefraction(coefRefraction);
        }
    }

    /**
     * @return minimum distance in meter to change to coarser resolution in multiscale computation
     */
    public double getMinDistMS() {
        return minDistMS;
    }

    /**
     * Sets the minimum distance in meter to change to coarser resolution in multiscale computation.
     * Sets 0 to disable multiscale computation.
     * @param minDistMS th new minimum distance or zero to disable multiscale computation.
     */
    public void setMinDistMS(double minDistMS) {
        this.minDistMS = minDistMS;
    }
    
    
    //
    // Computation
    // 
    
    /**
     * @return true if multi scale computation is enabled
     */
    public boolean isMSComputation() {
        return minDistMS > 0 && hasMultiScale();
    }
    
    /**
     * @return the current computation class
     */
    public synchronized ComputeView getDefaultComputeView() {
        if(isMSComputation()) {
            return getMultiComputeView(minDistMS);
        } else {
            return getSimpleComputeView();
        }
    }
    
    /**
     * Creates, if not already creates, and returns the mono scale computation class.
     * Can be CUDA or Java depending of global options and CUDA GPU presence.
     * @return the current mono scale computation class
     */
    public synchronized SimpleComputeView getSimpleComputeView() {
        if(simpleComputeView == null) {
            if(isUseCUDA()) {
                try {
                    simpleComputeView = new ComputeViewCUDA(getDefaultScaleData(), aPrec, earthCurv, coefRefraction, nbGPU);
                } catch (Exception ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.WARNING, null, ex);
                    Logger.getLogger(Project.class.getName()).info("CUDA is not available, continue in Java mode");
                    nbGPU = 0;
                }
            } 
            if(simpleComputeView == null) {
                simpleComputeView = new ComputeViewJava(getDefaultScaleData(), aPrec, earthCurv, coefRefraction);
            }
        }
        
        return simpleComputeView;
    }
    
    /**
     * Creates and return a new multi scale computation class
     * @param distMin the minimum distance in meter to change to a coarser scale, must be &gt; 0
     * @return a new multi scale computation class
     * @throws IllegalStateException if the project does not contain muti scale data
     */
    public MultiComputeViewJava getMultiComputeView(double distMin) {
        if(!hasMultiScale()) {
            throw new IllegalStateException("Project has no multi scale data.");
        }
        
        MultiComputeViewJava compute = new MultiComputeViewJava(scaleDatas, 
            (int) Math.ceil(distMin/getDefaultScaleData().getResolution()), aPrec, earthCurv, coefRefraction);

        return compute;
    }
    
    
    //
    // Global project management
    //
    
    /**
     * Loads and return a project from a xml project file.
     * Loads the project class and all scale data
     * @param file the project file to load
     * @return the loaded project
     * @throws IOException 
     */
    public static synchronized Project load(File file) throws IOException {       
        XStream xstream = new XStream();
        xstream.alias("Project", Project.class);
        
        Project prj;
        try (FileReader fr = new FileReader(file)) {
            prj = (Project) xstream.fromXML(fr);
            prj.dir = file.getAbsoluteFile().getParentFile();
        }

        for(ScaleData data : prj.scaleDatas.values()) {
            data.load(prj.dir);
        }

        return prj;
    }
    
    /**
     * Saves the xml project file.
     * 
     * @throws IOException 
     */
    public void save() throws IOException {
        XStream xstream = new XStream();
        xstream.alias("Project", Project.class);
        try (FileWriter fw = new FileWriter(getProjectFile())) {
            xstream.toXML(this, fw);
        }
    }
    
    /**
     * @return the xml project file
     */
    public File getProjectFile() {
        return new File(dir, name + ".xml");
    }

    /**
     * @return the project directory
     */
    public File getDirectory() {
        return dir;
    }

    /**
     * @return the project name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the CRS of the data or null if no CRS found in DTM coverage
     */
    public CoordinateReferenceSystem getCRS() {
        if(wktCRS != null && !wktCRS.isEmpty()) {
            try {
                return CRS.parseWKT(wktCRS);
            } catch (FactoryException ex) {
                Logger.getLogger(Project.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        return null;
    }

    /**
     * Saves the project and release computation ressources.
     */
    public void close() {
        try {
            save();
        } catch (IOException ex) {
            Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
        }
        dispose();
    }
    
    /**
     *  Releases computation ressources.
     */
    public void dispose() {
        if(simpleComputeView != null) {
            simpleComputeView.dispose();
        }
    }

    //
    // METRIC MANAGEMENT
    //
    
    /**
     * @param <U> the type of the metrics to retrieve
     * @param cls the type of the metrics to retrieve
     * @return new instances of metrics of type or subtype cls
     */
    public static <U extends Metric> List<U> getMetrics(Class<U> cls) {
        List<U> metrics = new ArrayList<>();
        for(Metric m : METRICS) {
            if(cls.isAssignableFrom(m.getClass())) {
                try {
                    metrics.add((U) m.getClass().newInstance());
                } catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        return metrics;
    }
    
    /**
     * @param shortName the short name of the metric
     * @return a new instance of the metric with given shortName
     * @throws IllegalArgumentException if no metric corresponds to the given shortName
     */
    public static Metric getMetric(String shortName) {
        try {
            for(Metric ind : METRICS) {
                if(ind.getShortName().equals(shortName)) {
                    return ind.getClass().newInstance();
                }
            }
            throw new IllegalArgumentException("Unknown metric " + shortName);
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Error while instantiate " + shortName);
        }
    }
    
    /**
     * @param name the short name of the metric with optionally landuse codes and distance intervals
     * @return a new instance of the metric with given name and parameters set if any
     * @throws IllegalArgumentException if no metric corresponds to the given name
     * @throws IllegalArgumentException if the metric does not support codes and the name contains landuse code
     * @throws IllegalArgumentException if the metric does not support distance intervals and the name contains them
     */
    public static Metric getMetricWithParams(String name) {
        String shortName;
        if(name.contains("[")) {
            shortName = name.split("\\[")[0];
        } else if(name.contains("_")) {
            shortName = name.split("_")[0];
        } else {
            shortName = name;
        }
        Metric m = Project.getMetric(shortName);
        
        if(name.contains("[")) {
            if(!m.isCodeSupported()) {
                throw new IllegalArgumentException("Metric " + shortName + " does not support codes.");
            }
            
            String lst = name.split("\\[")[1].split("\\]")[0];
            String [] tokens = lst.split(",");
            for(String code : tokens) {
                code = code.trim();
                if(code.isEmpty()) {
                    continue;
                }
                if(code.contains("-")) {
                    TreeSet<Integer> codes = new TreeSet<>();
                    String [] toks = code.split("-");
                    for(String c : toks) {
                        codes.add(Integer.parseInt(c));
                    }
                    m.addCodes(codes);
                } else {
                    m.addCode(Integer.parseInt(code));
                }
            }
        }
        
        if(name.contains("_")) {
            if(!(m instanceof AbstractDistMetric)) {
                throw new IllegalArgumentException("Metric " + shortName + " does not support distances.");
            }
            TreeSet<Double> dists = new TreeSet<>();
            String lst = name.split("_")[1];
            String [] tokens = lst.split(",");
            for(String dist : tokens) {
                dists.add(Double.parseDouble(dist));
            }
            ((AbstractDistMetric)m).setDistances(dists);
        }
        
        return m;
    }
    
    private static final List<Metric> METRICS;
    static {
        METRICS = new ArrayList(Arrays.asList(new AreaMetric(),new ShannonMetric(),
                new PerimeterMetric(), new CompactMetric(), new FractalDimMetric(),
                new AggregationMetric(), new CONTAGMetric(), new EdgeDensityMetric(), 
                new PatchDensityMetric(), new PatchMeanSizeMetric(), 
                new DistMetric(), new SkyLineMetric(), 
                new ShanDistMetric(), new DepthLineMetric()));
    }
    
    /**
     * Loads additional metrics available in jar files contained in the subdirectory named plugins of the directory where is the pixscape jar file.
     * 
     * @throws Exception 
     */
    public static void loadPluginMetric() throws Exception {
        URL url = Project.class.getProtectionDomain().getCodeSource().getLocation();
        File dir = new File(url.toURI()).getParentFile();
        File loc = new File(dir, "plugins");

        if(!loc.exists()) {
            return;
        }
        
        File[] flist = loc.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getPath().toLowerCase().endsWith(".jar");
            }
        });
        if(flist == null || flist.length == 0) {
            return;
        }
        URL[] urls = new URL[flist.length];
        for (int i = 0; i < flist.length; i++) {
            urls[i] = flist[i].toURI().toURL();
        }
        URLClassLoader ucl = new URLClassLoader(urls);

        loadPluginMetric(ucl);
    }
    
    /**
     * Loads other metrics given a specific class loader
     * @param loader the class loader
     */
    public static void loadPluginMetric(ClassLoader loader) throws Exception {
        ServiceLoader<Metric> sl = ServiceLoader.load(Metric.class, loader);
        Iterator<Metric> it = sl.iterator();
        while (it.hasNext()) {
            Metric ind = it.next();
            METRICS.add(ind);
        }
    }
}
