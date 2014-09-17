package org.thema.pixscape;

import com.thoughtworks.xstream.XStream;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EventObject;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.thema.data.IOImage;
import org.thema.drawshape.image.CoverageShape;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.DefaultGroupLayer;
import org.thema.drawshape.layer.LayerListener;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.style.SimpleStyle;
import org.thema.drawshape.style.table.ColorRamp;
import org.thema.drawshape.style.table.UniqueColorTable;

/**
 *
 * @author gvuidel
 */
public final class Project {


    public enum Aggregate {NONE, SUM, SHANNON};
    
    private static Project project;
    
    private transient GridCoverage2D dtmCov;
    private transient Raster dtm;
    private transient Raster land, dsm;
    private transient DefaultGroupLayer layers;
    private transient File dir;
    private transient ComputeView computeView;
    
    private String name;
    /** resolution of the grid dtm in meter */
    private double res2D;
    /** resolution of altitude Z in meter */
    private double resZ;
    private String wktCRS;
    private TreeSet<Integer> codes;
    private TreeMap<Double, Color> colors;
    private AffineTransformation grid2space;
    
    private int nbGPU = 0;

    public Project(String name, File prjPath, GridCoverage2D demCov, double resZ) throws IOException {
        this.name = name;
        this.dir = prjPath;
        this.dtmCov = demCov;
        
        new GeoTiffWriter(new File(prjPath, "dtm.tif")).write(dtmCov, null);
        
        this.resZ = resZ;
        this.res2D = demCov.getEnvelope2D().getWidth() / demCov.getGridGeometry().getGridRange2D().getWidth();
        CoordinateReferenceSystem crs = demCov.getCoordinateReferenceSystem2D();
        if(crs != null)
            wktCRS = crs.toWKT();
        Envelope2D zone = demCov.getEnvelope2D();
        GridEnvelope2D range = demCov.getGridGeometry().getGridRange2D();
        grid2space = new AffineTransformation(zone.getWidth() / range.getWidth(), 0,
                    zone.getMinX() - zone.getWidth() / range.getWidth(),
                0, -zone.getHeight() / range.getHeight(),
                    zone.getMaxY() + zone.getHeight() / range.getHeight());
        
        project = this;
        save();
    }

    public void setLandUse(GridCoverage2D landCov) throws IOException {
        if(!dtmCov.getEnvelope2D().boundsEquals(landCov.getEnvelope2D(), 0, 1, 0.1))
            throw new IllegalArgumentException("Land use bounds does not correspond to DTM bounds");
        if(landCov.getRenderedImage() instanceof BufferedImage)
            this.land = ((BufferedImage)landCov.getRenderedImage()).getRaster();
        else
            this.land = landCov.getRenderedImage().getData();
        if(land.getWidth() != getDtm().getWidth() || land.getHeight() != getDtm().getHeight())
            throw new IllegalArgumentException("Land use raster size does not correspond to DTM raster size");
        int type = land.getSampleModel().getDataType();
        if(type == DataBuffer.TYPE_FLOAT || type == DataBuffer.TYPE_DOUBLE)
            throw new IllegalArgumentException("Float types are not supported for land use");
        codes = new TreeSet<>();
        for(int yi = 0; yi < land.getHeight(); yi++)
            for(int xi = 0; xi < land.getWidth(); xi++)
                codes.add(land.getSample(xi, yi, 0));
        colors = new TreeMap<>();
        for(Integer code : codes)
            colors.put(code.doubleValue(), SimpleStyle.randomColor().brighter());
        
        new GeoTiffWriter(new File(dir, "land.tif")).write(landCov, null);
        save();
    }
    
    public void setDSM(GridCoverage2D dsmCov) throws IOException {
        if(!dtmCov.getEnvelope2D().boundsEquals(dsmCov.getEnvelope2D(), 0, 1, 0.1))
            throw new IllegalArgumentException("DSM bounds does not correspond to DTM bounds");
        if(dsmCov.getRenderedImage() instanceof BufferedImage)
            dsm = ((BufferedImage)dsmCov.getRenderedImage()).getRaster();
        else
            dsm = dsmCov.getRenderedImage().getData();
        if(dsm.getWidth() != getDtm().getWidth() || dsm.getHeight() != getDtm().getHeight())
            throw new IllegalArgumentException("DSM raster size does not correspond to DTM raster size");
        
        new GeoTiffWriter(new File(dir, "dsm.tif")).write(dsmCov, null);
    }
    
    public WritableRaster calcViewShed(DirectPosition2D c, double startZ, double destZ, boolean direct, Bounds bounds) throws InvalidGridGeometryException, TransformException {
        return getComputeView().calcViewShed(dtmCov.getGridGeometry().worldToGrid(c), startZ, destZ, direct, bounds);
    }
    
    public WritableRaster calcViewTan(DirectPosition2D c, double startZ, double ares, Bounds bounds) throws InvalidGridGeometryException, TransformException {
        return getComputeView().calcViewTan(dtmCov.getGridGeometry().worldToGrid(c), startZ, ares, bounds);
    }

//    public WritableRaster calcVisibility(double startZ, double destZ, boolean direct, Bounds bounds, boolean saved, ProgressBar progressBar) throws IOException {
//        GlobalViewTask task = new GlobalViewTask(startZ, destZ, direct, bounds, saved, progressBar);
////        if(isUseCUDA()) {
////            WritableRaster r = ((ComputeViewCUDA)getComputeView()).calcVisibility(startZ, destZ, direct, bounds, progressBar);
////            if(saved) {
////                new GeoTiffWriter(task.getResultFile()).write(
////                        new GridCoverageFactory().create("view", r, getDtmCov().getEnvelope2D()), null);
////                return null;
////            } else
////                return r;
////        } else {
//            ExecutorService.execute(task);
//            return task.getResult();
////        }
//    }
//    
//    public Map<Integer, WritableRaster> calcVisibility(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode,  Set<Integer> toCode, 
//            boolean saved, ProgressBar progressBar) throws IOException {
//        GlobalViewLandUseTask task = new GlobalViewLandUseTask(startZ, destZ, direct, bounds, fromCode, toCode, saved, progressBar);
////        if(isUseCUDA()) {
////            Map<Integer, WritableRaster> views = ((ComputeViewCUDA)getComputeView()).calcVisibility(startZ, destZ, direct, bounds, fromCode, toCode, progressBar);
////            if(saved) {
////                for(Integer code : views.keySet()) {
////                    new GeoTiffWriter(task.getResultFile(code)).write(
////                        new GridCoverageFactory().create("view", views.get(code), getDtmCov().getEnvelope2D()), null);
////                }
////                return null;
////            } else
////                return views;
////        } else {
//            ExecutorService.execute(task);
//            return task.getResult();
////        }
//    }
//    
//    public WritableRaster calcVisibility(double startZ, double destZ, boolean direct, Bounds bounds, Set<Integer> fromCode, Set<Integer> toCode, 
//            Aggregate aggr, boolean saved, ProgressBar progressBar) throws IOException {
//        GlobalViewTask task = new GlobalViewTask(startZ, destZ, direct, bounds, fromCode, toCode, aggr, saved, progressBar);
////        if(isUseCUDA()) {
////            Map<Integer, WritableRaster> views = ((ComputeViewCUDA)getComputeView()).calcVisibility(startZ, destZ, direct, bounds, fromCode, toCode, progressBar);
////            WritableRaster view = null;
////            if(progressBar != null)
////                progressBar.setIndeterminate(true);
////            if(aggr == Aggregate.SUM) {
////                view = views.values().iterator().next().createCompatibleWritableRaster();
////                for(int yi = 0; yi < view.getHeight(); yi++) {
////                    for(int xi = 0; xi < view.getWidth(); xi++) {
////                        int sum = 0;
////                        for(WritableRaster v : views.values())
////                            sum += v.getSample(xi, yi, 0);
////                        if(sum < 0)
////                            sum = -1;
////                        view.setSample(xi, yi, 0, sum);
////                    }
////                }
////            } else if(aggr == Aggregate.SHANNON) {
////                view = Raster.createWritableRaster(
////                        new BandedSampleModel(DataBuffer.TYPE_FLOAT, getDtm().getWidth(), getDtm().getHeight(), 1), null);
////                for(int yi = 0; yi < view.getHeight(); yi++)
////                    for(int xi = 0; xi < view.getWidth(); xi++) {
////                        int sum = 0;
////                        for(WritableRaster v : views.values()) {
////                            final int val = v.getSample(xi, yi, 0);
////                            if(val != 0) {
////                                sum += val;
////                            }
////                        }
////                        if(sum < 0) {
////                            view.setSample(xi, yi, 0, -1);
////                            continue;
////                        }
////                        double shannon = 0;
////                        for(WritableRaster v : views.values()) {
////                            double val = v.getSample(xi, yi, 0);
////                            if(val > 0) {
////                                shannon += - val/sum * Math.log(val/sum);
////                            }
////                        }
////                        view.setSample(xi, yi, 0, shannon/Math.log(views.size()));
////                    }
////            } 
////            if(saved) {
////                new GeoTiffWriter(task.getResultFile()).write(
////                        new GridCoverageFactory().create("view", view, getDtmCov().getEnvelope2D()), null);
////                return null;
////            } else
////                return view;
////        } else {
//            ExecutorService.execute(task);
//            return task.getResult();
////        }
//    }
    
    public final double getZ(int x, int y) {
        double z = dtm.getSample(x, y, 0) * resZ;
        if(dsm != null)
            z += dsm.getSample(x, y, 0);
        return z;
    }

    public AffineTransformation getGrid2space() {
        return grid2space;
    }
    
    public GridCoverage2D getDtmCov() {
        return dtmCov;
    }

    public synchronized Raster getDtm() {
        if(dtm == null) {
            if(dtmCov.getRenderedImage() instanceof BufferedImage)
                this.dtm = ((BufferedImage)dtmCov.getRenderedImage()).getRaster();
            else
                this.dtm = dtmCov.getRenderedImage().getData();
        }
        return dtm;
    }

    public double getRes2D() {
        return res2D;
    }

    public double getResZ() {
        return resZ;
    }

    public boolean hasLandUse() {
        return land != null;
    }

    public Raster getLandUse() {
        return land;
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
        if(computeView != null)
            computeView.dispose();
        computeView = null;
    }
    
    public synchronized ComputeView getComputeView() {
        if(computeView == null) {
            if(isUseCUDA()) {
                try {
                    computeView = new ComputeViewCUDA(getDtm(), resZ, res2D, land, dsm, nbGPU);
                } catch (Throwable ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                    Logger.getLogger(Project.class.getName()).info("CUDA not available, continue in Java mode");
                    nbGPU = 0;
                }
            } 
            if(computeView == null) {
                computeView = new ComputeViewJava(getDtm(), resZ, res2D, land, dsm);
            }
        }
        
        return computeView;
    }
    
    public void fillViewTan(GridCoordinates2D c, Raster viewTan, WritableRaster viewTanZ, WritableRaster viewTanDist, WritableRaster viewTanLand) {
        int w = getDtm().getWidth();
        for(int y = 0; y < viewTan.getHeight(); y++)
            for(int x = 0; x < viewTan.getWidth(); x++) {
                int ind = viewTan.getSample(x, y, 0);
                if(ind == -1) {
                    viewTanZ.setSample(x, y, 0, -1000);
                    viewTanDist.setSample(x, y, 0, -1);
                    viewTanLand.setSample(x, y, 0, 255);
                } else {
                    viewTanZ.setSample(x, y, 0, getZ(ind%w, ind/w));
                    viewTanDist.setSample(x, y, 0, getRes2D() * Math.sqrt(Math.pow(c.x - (ind%w), 2) +  Math.pow(c.y - (ind/w), 2)));
                    if(hasLandUse())
                        viewTanLand.setSample(x, y, 0, getLandUse().getSample(ind%w, ind/w, 0));
                }
            }
    }
    
    public static synchronized Project loadProject(File file) throws IOException {       
        XStream xstream = new XStream();
        xstream.alias("Project", Project.class);
        
        Project prj;
        try (FileReader fr = new FileReader(file)) {
            prj = (Project) xstream.fromXML(fr);
            prj.dir = file.getAbsoluteFile().getParentFile();
        }

        prj.dtmCov = IOImage.loadTiffWithoutCRS(new File(prj.dir, "dtm.tif"));
        
        File dsmFile = new File(prj.dir, "dsm.tif");
        if(dsmFile.exists()) {
            GridCoverage2D cov = IOImage.loadTiffWithoutCRS(dsmFile);
            if(cov.getRenderedImage() instanceof BufferedImage)
                prj.dsm = ((BufferedImage)cov.getRenderedImage()).getRaster();
            else
                prj.dsm = cov.getRenderedImage().getData();
        }
        
        File luFile = new File(prj.dir, "land.tif");
        if(luFile.exists()) {
            GridCoverage2D cov = IOImage.loadTiffWithoutCRS(luFile);
            if(cov.getRenderedImage() instanceof BufferedImage)
                prj.land = ((BufferedImage)cov.getRenderedImage()).getRaster();
            else
                prj.land = cov.getRenderedImage().getData();
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
        if(wktCRS != null && !wktCRS.isEmpty())
            try {
                return CRS.parseWKT(wktCRS);
            } catch (FactoryException ex) {
                Logger.getLogger(Project.class.getName()).log(Level.WARNING, null, ex);
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
        layers = new DefaultGroupLayer(name, true);
        layers.addLayerFirst(new RasterLayer("DTM", new CoverageShape(dtmCov, new RasterStyle(ColorRamp.RAMP_DEM))));
        if(dsm != null) {
            layers.addLayerFirst(new RasterLayer("DSM", new RasterShape(dsm, dtmCov.getEnvelope2D(), 
                    new RasterStyle(ColorRamp.RAMP_TEMP), true)));
        }
        if(land != null) {
            final UniqueColorTable colorTable = new UniqueColorTable((Map)colors);
            RasterLayer l = new RasterLayer("Land use", new RasterShape(land, dtmCov.getEnvelope2D(), 
                    new RasterStyle(colorTable), true));
            l.addLayerListener(new LayerListener() {
                @Override
                public void layerVisibilityChanged(EventObject e) {                    
                }
                @Override
                public void layerStyleChanged(EventObject e) {
                    for(Double code : colors.keySet())
                        colors.put(code, colorTable.getColor(code));
                    
                }
            });
            layers.addLayerFirst(l);
        }
    } 

    public static Project getProject() {
        return project;
    }

    public void close() {
        try {
            save();
            if(computeView != null)
                computeView.dispose();
        } catch (IOException ex) {
            Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
