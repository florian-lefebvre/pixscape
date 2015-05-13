package org.thema.pixscape;

import com.thoughtworks.xstream.XStream;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import java.awt.Color;
import java.awt.event.ActionEvent;
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
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.thema.drawshape.image.CoverageShape;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.DefaultGroupLayer;
import org.thema.drawshape.layer.LayerListener;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.style.SimpleStyle;
import org.thema.drawshape.style.table.ColorRamp;
import org.thema.drawshape.style.table.UniqueColorTable;
import org.thema.pixscape.metric.AreaMetric;
import org.thema.pixscape.metric.CONTAGMetric;
import org.thema.pixscape.metric.CompactMetric;
import org.thema.pixscape.metric.DistMetric;
import org.thema.pixscape.metric.FractalDimMetric;
import org.thema.pixscape.metric.IJIMetric;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.PerimeterMetric;
import org.thema.pixscape.metric.RasterMetric;
import org.thema.pixscape.metric.ShanDistMetric;
import org.thema.pixscape.metric.ShannonMetric;
import org.thema.pixscape.metric.SkyLineMetric;
import org.thema.pixscape.view.ComputeView;
import org.thema.pixscape.view.ComputeViewJava;
import org.thema.pixscape.view.MultiComputeViewJava;
import org.thema.pixscape.view.SimpleComputeView;
import org.thema.pixscape.view.cuda.ComputeViewCUDA;

/**
 *
 * @author gvuidel
 */
public final class Project {


    public enum Aggregate {NONE, SUM, SHANNON};
    
    private static Project project;
    
    private transient DefaultGroupLayer layers;
    private transient File dir;
    private transient SimpleComputeView simpleComputeView;
    
    private TreeMap<Double, ScaleData> scaleDatas;
    
    private String name;
    
    private String wktCRS;
    private TreeSet<Integer> codes;
    private TreeMap<Double, Color> colors;
    
    // options
    private int nbGPU = 0;
    private double startZ = 1.8;
    private double aPrec = 0.1;
    private double minDistMS = -1;

    public Project(String name, File prjPath, GridCoverage2D demCov, double resZ) throws IOException {
        this.name = name;
        this.dir = prjPath;
        this.scaleDatas = new TreeMap<>();
        ScaleData scaleData = new ScaleData(demCov, null, null, resZ);
        scaleDatas.put(scaleData.getResolution(), scaleData);
        
        new GeoTiffWriter(new File(prjPath, "dtm-" + scaleData.getResolution() + ".tif")).write(scaleData.getDtmCov(), null);
        
        CoordinateReferenceSystem crs = demCov.getCoordinateReferenceSystem2D();
        if(crs != null) {
            wktCRS = crs.toWKT();
        }
        
        project = this;
        save();
    }

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
        
        ScaleData newData = new ScaleData(getDtmCov(), land, getDefaultScale().getDsm());
        scaleDatas.put(newData.getResolution(), newData);
        simpleComputeView = null;
        
        codes = new TreeSet<>(newData.getCodes());
        
        colors = new TreeMap<>();
        for(Integer code : codes) {
            colors.put(code.doubleValue(), SimpleStyle.randomColor().brighter());
        }
        
        new GeoTiffWriter(new File(dir, "land-" + newData.getResolution() + ".tif")).write(landCov, null);
        save();
    }
    
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
        
        new GeoTiffWriter(new File(dir, "dsm-" + newData.getResolution() + ".tif")).write(dsmCov, null);
        save();
    }
    
    public void addScaleData(ScaleData data) throws IOException {
        if(!data.getGridGeometry().getEnvelope2D().contains((Rectangle2D)getDefaultScale().getGridGeometry().getEnvelope2D())) {
            throw new IllegalArgumentException("The data must cover the first scale zone.");
        }
        if(hasLandUse() != data.hasLandUse()) {
            throw new IllegalArgumentException("Land use must be present for all scales or any.");
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
        layers = null;
    }
    
    public void removeScaleData() throws IOException {
        scaleDatas = new TreeMap<>(scaleDatas.headMap(scaleDatas.firstKey(), true));
        save();
        layers = null;
    }
    
    private void removeScaleData(double res) throws IOException {
        scaleDatas.remove(res);
        save();
    }

    public AffineTransformation getGrid2space() {
        return getDefaultScale().getGrid2Space();
    }
    
    public GridCoverage2D getDtmCov() {
        return getDefaultScale().getDtmCov();
    }

    public Raster getDtm() {
        return getDefaultScale().getDtm();
    }

    public ScaleData getDefaultScale() {
        return scaleDatas.firstEntry().getValue();
    }
    
    public boolean hasMultiScale() {
        return scaleDatas.size() > 1;
    }
    
    public TreeMap<Double, ScaleData> getScaleDatas() {
        return scaleDatas;
    }

    public boolean hasLandUse() {
        return getDefaultScale().hasLandUse();
    }

    public Raster getLandUse() {
        return getDefaultScale().getLand();
    }

    public SortedSet<Integer> getCodes() {
        return codes;
    }
    
    public Map<Double, Color> getLandColors() {
        return colors;
    }

    public boolean isUseCUDA() {
        return nbGPU > 0;
    }

    public synchronized void setUseCUDA(int nbGPU) {
        this.nbGPU = nbGPU;
        if(simpleComputeView != null) {
            simpleComputeView.dispose();
        }
        simpleComputeView = null;
    }

    public double getStartZ() {
        return startZ;
    }

    public void setStartZ(double startZ) {
        this.startZ = startZ;
    }

    public double getaPrec() {
        return aPrec;
    }

    public void setaPrec(double aPrec) {
        this.aPrec = aPrec;
        if(simpleComputeView != null) {
            simpleComputeView.setaPrec(aPrec);
        }
    }

    public double getMinDistMS() {
        return minDistMS;
    }

    public void setMinDistMS(double minDistMS) {
        this.minDistMS = minDistMS;
    }
    
    public synchronized ComputeView getDefaultComputeView() {
        if(minDistMS > 0) {
            return getMultiComputeView(minDistMS);
        } else {
            return getSimpleComputeView();
        }
    }
    
    public synchronized SimpleComputeView getSimpleComputeView() {
        if(simpleComputeView == null) {
            if(isUseCUDA()) {
                try {
                    simpleComputeView = new ComputeViewCUDA(getDefaultScale(), aPrec, nbGPU);
                } catch (Exception ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.WARNING, null, ex);
                    Logger.getLogger(Project.class.getName()).info("CUDA is not available, continue in Java mode");
                    nbGPU = 0;
                }
            } 
            if(simpleComputeView == null) {
                simpleComputeView = new ComputeViewJava(getDefaultScale(), aPrec);
            }
        }
        
        return simpleComputeView;
    }
    
    public MultiComputeViewJava getMultiComputeView(double distMin) {
        if(!hasMultiScale()) {
            throw new IllegalStateException("Project has no multi scale data.");
        }
        
        MultiComputeViewJava compute = new MultiComputeViewJava(scaleDatas, 
            (int) Math.ceil(distMin/getDefaultScale().getResolution()), aPrec);

        return compute;
    }
    
    public static synchronized Project loadProject(File file) throws IOException {       
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
        project = prj;
        return prj;
    }
    
    public void save() throws IOException {
        XStream xstream = new XStream();
        xstream.alias("Project", Project.class);
        try (FileWriter fw = new FileWriter(getProjectFile())) {
            xstream.toXML(this, fw);
        }
    }
    
    public File getProjectFile() {
        return new File(dir, name + ".xml");
    }

    public File getDirectory() {
        return dir;
    }
    
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

    public synchronized DefaultGroupLayer getLayers() {
        if(layers == null) {
            createLayers();
        }
        return layers;
    }
    
    private void createLayers() {
        layers = createScaleDataLayers(getDefaultScale());
        layers.setName(name);
        layers.setExpanded(true);
        
        layers.getLayerFirst().setVisible(true);
        
        if(hasMultiScale()) {
            DefaultGroupLayer gl = new DefaultGroupLayer("Other scales", false);
            for(ScaleData data : scaleDatas.values()) {
                if(data == getDefaultScale()) {
                    continue;
                }
                gl.addLayerLast(createScaleDataLayers(data));
            }
            layers.addLayerLast(gl);
        }
        
        
    } 

    private DefaultGroupLayer createScaleDataLayers(final ScaleData data) {
        DefaultGroupLayer gl = new DefaultGroupLayer(""+data.getResolution(), false) {

            @Override
            public JPopupMenu getContextMenu() {
                if(data == project.getDefaultScale()) {
                    return null;
                }
                JPopupMenu menu = new JPopupMenu();
                menu.add(new AbstractAction("Remove...") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int res = JOptionPane.showConfirmDialog(null, "Do you want to remove the scale " + getName() + " ?",
                        "Suppression...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if(res == JOptionPane.YES_OPTION) {
                            try {
                                Project.this.removeScaleData(Double.parseDouble(getName()));
                                getParent().removeLayer(getParent().getLayer(getName()));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                });
                return menu;
            }
            
        };
        
        gl.addLayerFirst(new RasterLayer("DTM", new CoverageShape(data.getDtmCov(), new RasterStyle(ColorRamp.RAMP_DEM))));
        if(data.getDsm() != null) {
            gl.addLayerFirst(new RasterLayer("DSM", new RasterShape(data.getDsm(), data.getDtmCov().getEnvelope2D(), 
                    new RasterStyle(ColorRamp.RAMP_TEMP), true)));
        }
        if(hasLandUse()) {
            final UniqueColorTable colorTable = new UniqueColorTable((Map)colors);
            RasterLayer l = new RasterLayer("Land use", new RasterShape(data.getLand(), data.getDtmCov().getEnvelope2D(), 
                    new RasterStyle(colorTable), true));
            l.addLayerListener(new LayerListener() {
                @Override
                public void layerVisibilityChanged(EventObject e) {                    
                }
                @Override
                public void layerStyleChanged(EventObject e) {
                    for(Double code : colors.keySet()) {
                        colors.put(code, colorTable.getColor(code));
                    }
                    
                }
            });
            gl.addLayerFirst(l);
        }
        gl.setLayersVisible(false);
        return gl;
    } 
    
    public static Project getProject() {
        return project;
    }

    public void close() {
        try {
            save();
            if(simpleComputeView != null) {
                simpleComputeView.dispose();
            }
        } catch (IOException ex) {
            Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public static <U extends Metric> List<U> getMetrics(Class<U> cls) {
        List<U> metrics = new ArrayList<>();
        for(Metric m : METRICS) {
            if(cls.isAssignableFrom(m.getClass())) {
                try {
                    metrics.add((U) m.getClass().newInstance());
                } catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return metrics;
    }
    
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
            throw new RuntimeException("Error while instanciate " + shortName);
        }
    }
    
    private static final List<Metric> METRICS;
    static {
        METRICS = new ArrayList(Arrays.asList(new AreaMetric(), new PerimeterMetric(), 
                new CompactMetric(), new ShannonMetric(), new FractalDimMetric(),
                new IJIMetric(), new CONTAGMetric(), new DistMetric(), new SkyLineMetric(), 
                new ShanDistMetric(), new RasterMetric()));
    }
    
    
    public static void loadPluginMetric() throws Exception {
        URL url = Project.class.getProtectionDomain().getCodeSource().getLocation();
        File dir = new File(url.toURI()).getParentFile();
        File loc = new File(dir, "plugins");

        if(!loc.exists()) {
            return;
        }
        
        File[] flist = loc.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {return file.getPath().toLowerCase().endsWith(".jar");}
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
    
    public static void loadPluginMetric(ClassLoader loader) throws Exception {
        ServiceLoader<Metric> sl = ServiceLoader.load(Metric.class, loader);
        Iterator<Metric> it = sl.iterator();
        while (it.hasNext()) {
            Metric ind = it.next();
            METRICS.add(ind);
        }
    }
}
