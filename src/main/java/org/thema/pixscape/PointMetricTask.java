/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import org.thema.pixscape.view.ComputeView;
import com.vividsolutions.jts.geom.Coordinate;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.ProgressBar;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 *
 * @author gvuidel
 */
public class PointMetricTask extends AbstractParallelTask<Map<Feature, List<Double[]>>, Map<Feature, List<Double[]>>> implements Serializable {
    
    private final double startZ;
    
    // options for viewshed
    private double destZ;
    private boolean direct;
    
    // option for tangential view
    private final double anglePrec;
    
    private final Bounds bounds;
    
    private final List<? extends Metric> metrics;
    
    // sampling
    private final File pointFile;
    
    private final File resDir;
    
    private transient ComputeView compute;
    private transient List<DefaultFeature> points;
    private transient Map<Feature, List<Double[]>> result;

    public PointMetricTask(double startZ, double destZ, boolean direct, Bounds bounds, List<ViewShedMetric> metrics, File pointFile, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        this.metrics = metrics;
        this.pointFile = pointFile;
        this.resDir = resDir;
        this.anglePrec = Double.NaN;
    }
    
    public PointMetricTask(double startZ, double anglePrec, Bounds bounds, List<ViewTanMetric> metrics, File pointFile, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.anglePrec = anglePrec;
        this.bounds = bounds;
        this.metrics = metrics;
        this.pointFile = pointFile;
        this.resDir = resDir;
    }

    @Override
    public void init() {
        try {
            // needed for getSplitRange
            points = DefaultFeature.loadFeatures(pointFile);
        } catch (IOException ex) {
            Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.init(); 
        compute = Project.getProject().getComputeView();
    }
    
    private boolean isTanView() {
        return !Double.isNaN(anglePrec);
    }
    
    public boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<Feature, List<Double[]>> execute(int start, int end) {
        Map<Feature, List<Double[]>> map = new HashMap<>();
        
        for(int i = start; i < end; i++) {
            if(isCanceled()) {
                break;
            }
            Feature p = points.get(i);
            Coordinate c = p.getGeometry().getCoordinate();
            GridCoordinates2D gc = null;
            try {
                gc = Project.getProject().getDtmCov().getGridGeometry().worldToGrid(new DirectPosition2D(c.x, c.y));
            } catch (InvalidGridGeometryException | TransformException ex) {
                Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            }
            List<Double[]> values;          
            Bounds b = getBounds(p);
            if(isTanView()) {
                values = compute.aggrViewTan(gc, startZ, anglePrec, b, (List) metrics);
            } else {
                values = compute.aggrViewShed(gc, startZ, destZ, direct, b, (List) metrics);
            }
            map.put(points.get(i), values);
            incProgress(1);
        }
        
        return map;
    }

    @Override
    public int getSplitRange() {
        return points.size();
    }
    
    @Override
    public Map<Feature, List<Double[]>> getResult() {
        return result;
    }

    @Override
    public void gather(Map<Feature, List<Double[]>> map) {
        if(result == null)
            result = new HashMap<>();
        result.putAll(map);
    }
    
    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            try {
                for(DefaultFeature point : points) {
                    List<Double[]> values = result.get(point);
                    for(int i = 0; i < metrics.size(); i++) {
                        int j = 0;
                        for(String resName : metrics.get(i).getResultNames())
                            point.addAttribute(resName, values.get(i)[j++]);
                    }
                }
                DefaultFeature.saveFeatures(points, getResultFile());
            } catch (IOException | SchemaException ex) {
                Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
    
    public File getResultFile() {
        if(isTanView()) {
            return new File(resDir, "metrics-aprec" + anglePrec + "-" + pointFile.getName());
        } else {
            return new File(resDir, "metrics-" + (direct ? "direct" : "indirect") + "-" + pointFile.getName());
        }
    }

    private Bounds getBounds(Feature p) {
        Bounds b = new Bounds(bounds);
        if(p.getAttributeNames().contains("zmin")) {
            b.setZMin(((Number)p.getAttribute("zmin")).doubleValue());
        }
        if(p.getAttributeNames().contains("zmax")) {
            b.setZMax(((Number)p.getAttribute("zmax")).doubleValue());
        }
        if(p.getAttributeNames().contains("dmin")) {
            b.setDmin(((Number)p.getAttribute("dmin")).doubleValue());
        }
        if(p.getAttributeNames().contains("dmax")) {
            b.setDmax(((Number)p.getAttribute("dmax")).doubleValue());
        }
        if(p.getAttributeNames().contains("orien")) {
            if(p.getAttributeNames().contains("amp")) {
                b = b.createBounds(((Number)p.getAttribute("orien")).doubleValue(), ((Number)p.getAttribute("amp")).doubleValue());
            } else {
                b = b.createBounds(((Number)p.getAttribute("orien")).doubleValue());
            }
        }
        return b;
    }
}
