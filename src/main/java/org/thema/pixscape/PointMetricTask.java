/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

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
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.feature.SchemaException;
import org.thema.common.ProgressBar;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.ComputeView.ViewShedResult;
import org.thema.pixscape.ComputeView.ViewTanResult;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 *
 * @author gvuidel
 */
public class PointMetricTask extends AbstractParallelTask<Map<Feature, List<Double>>, Map<Feature, List<Double>>> implements Serializable {
    
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
    private transient Map<Feature, List<Double>> result;

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
    public Map<Feature, List<Double>> execute(int start, int end) {
        Map<Feature, List<Double>> map = new HashMap<>();
        
        for(int i = start; i < end; i++) {
            if(isCanceled())
                break;
            Coordinate c = points.get(i).getGeometry().getCoordinate();
            Coordinate pc = Project.getProject().getSpace2Grid().transform(c, new Coordinate());
            GridCoordinates2D gc = new GridCoordinates2D((int)pc.x, (int)pc.y);
            List<Double> values;
            if(isTanView()) {
                values = compute.aggrViewTan(gc, startZ, anglePrec, bounds, (List) metrics);
            } else {
                values = compute.aggrViewShed(gc, startZ, destZ, direct, bounds, (List) metrics);
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
    public Map<Feature, List<Double>> getResult() {
        return result;
    }

    @Override
    public void gather(Map<Feature, List<Double>> map) {
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
                    List<Double> values = result.get(point);
                    for(int i = 0; i < metrics.size(); i++)
                        point.addAttribute(metrics.get(i).toString(), values.get(i));
                }
                DefaultFeature.saveFeatures(points, getResultFile());
            } catch (IOException | SchemaException ex) {
                Logger.getLogger(PointMetricTask.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
    
    public File getResultFile() {
        if(isTanView())
            return new File(resDir, "metrics-aprec" + anglePrec + "-" + bounds + ".shp");
        else
            return new File(resDir, "metrics-" + (direct ? "direct" : "indirect") + "-" + bounds + ".shp");
    }
}
