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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.thema.common.ProgressBar;
import org.thema.data.GlobalDataStore;
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
public class PointMetricTask extends AbstractParallelTask<List<DefaultFeature>, Map<DefaultFeature, List<Double[]>>> implements Serializable {
    
    boolean isTan;
    
    private final double startZ;
    
    // options for viewshed
    private double destZ;
    private boolean direct;
    
    private final Bounds bounds;
    
    private final List<? extends Metric> metrics;
    
    // sampling
    private final File pointFile;
    private final String idField;
    
    private final File resDir;
    
    private transient ComputeView compute;
    private transient List<DefaultFeature> points;
    private transient Map<DefaultFeature, List<Double[]>> result;

    public PointMetricTask(double startZ, double destZ, boolean direct, Bounds bounds, List<ViewShedMetric> metrics, File pointFile, String idField, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.destZ = destZ;
        this.direct = direct;
        this.bounds = bounds;
        this.metrics = metrics;
        this.pointFile = pointFile;
        this.idField = idField;
        this.resDir = resDir;
        this.isTan = false;
    }
    
    public PointMetricTask(double startZ, Bounds bounds, List<ViewTanMetric> metrics, File pointFile, String idField, File resDir, ProgressBar monitor) {
        super(monitor);
        this.startZ = startZ;
        this.bounds = bounds;
        this.metrics = metrics;
        this.pointFile = pointFile;
        this.idField = idField;
        this.resDir = resDir;
        this.isTan = true;
    }

    @Override
    public void init() {
        try {
            // needed for getSplitRange
            points = GlobalDataStore.getFeatures(pointFile, idField, null);
        } catch (IOException ex) {
            Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.init(); 
        compute = Project.getProject().getDefaultComputeView();
    }

    public boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<DefaultFeature, List<Double[]>> execute(int start, int end) {
        Map<DefaultFeature, List<Double[]>> map = new HashMap<>();
        
        for(int i = start; i < end; i++) {
            if(isCanceled()) {
                break;
            }
            Feature p = points.get(i);
            Coordinate c = p.getGeometry().getCoordinate();
            DirectPosition2D gc = new DirectPosition2D(c.x, c.y);
            List<Double[]> values;          
            Bounds b = bounds.updateBounds(p);
            if(isTan) {
                values = compute.aggrViewTan(gc, startZ, b, (List) metrics);
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
    public List<DefaultFeature> getResult() {
        List<DefaultFeature> resPoints = new ArrayList<>();
        for(DefaultFeature point : points) {
            DefaultFeature p = bounds.updateBounds(point).createFeatureWithBoundAttr(point.getId(), point.getGeometry());
            List<Double[]> values = result.get(point);
            for(int i = 0; i < metrics.size(); i++) {
                int j = 0;
                for(String resName : metrics.get(i).getResultNames()) {
                    p.addAttribute(resName, values.get(i)[j++]);
                }
            }
            resPoints.add(p);
        }
        return resPoints;
    }

    @Override
    public void gather(Map<DefaultFeature, List<Double[]>> map) {
        if(result == null) {
            result = new HashMap<>();
        }
        result.putAll(map);
    }
    
    @Override
    public void finish() {
        super.finish(); 
        if(isSaved()) {
            try {
                DefaultFeature.saveFeatures(getResult(), getResultFile());
            } catch (IOException | SchemaException ex) {
                Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
    
    public File getResultFile() {
        if(isTan) {
            return new File(resDir, "metrics-" + pointFile.getName());
        } else {
            return new File(resDir, "metrics-" + (direct ? "direct" : "indirect") + "-" + pointFile.getName());
        }
    }

    
}
