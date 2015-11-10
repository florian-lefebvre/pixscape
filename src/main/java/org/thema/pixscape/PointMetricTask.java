
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
import org.thema.pixscape.view.ComputeView;

/**
 * Parallel task for calculating metrics on a point shapefile sampling.
 * 
 * @author Gilles Vuidel
 */
public class PointMetricTask extends AbstractParallelTask<List<DefaultFeature>, Map<Object, List<Double[]>>> implements Serializable {
    
    /** project file for loading project for MPI mode only */
    private File prjFile;
    
    private boolean isTan;
    
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
    
    private transient Project project;
    private transient ComputeView compute;
    private transient List<DefaultFeature> points;
    private transient Map<Object, List<Double[]>> result;

    /**
     * Creates a new PointMetricTask for viewshed metric.
     * 
     * @param project the project (must be saved for MPI mode)
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param direct if true the starting point is the observer, else the starting point is the observed point
     * @param bounds the 3D limits of the sight 
     * @param metrics the metrics to calculate
     * @param pointFile the sampling point shapefile
     * @param idField the identifier name field in the shapefile
     * @param resDir the directory for storing result file, may be null for not saving the results
     * @param monitor the progress monitor, may be null
     */
    public PointMetricTask(Project project, double startZ, double destZ, boolean direct, Bounds bounds, List<ViewShedMetric> metrics, File pointFile, String idField, File resDir, ProgressBar monitor) {
        super(monitor);
        this.project = project;
        this.prjFile = project.getProjectFile();
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
    
    /**
     * Creates a new PointMetricTask for tanential metric.
     * 
     * @param project the project (must be saved for MPI mode)
     * @param startZ the height of the eye of the observer
     * @param bounds the 3D limits of the sight 
     * @param metrics the metrics to calculate
     * @param pointFile the sampling point shapefile
     * @param idField the identifier name field in the shapefile
     * @param resDir the directory for storing result file, may be null for not saving the results
     * @param monitor the progress monitor, may be null
     */
    public PointMetricTask(Project project, double startZ, Bounds bounds, List<ViewTanMetric> metrics, File pointFile, String idField, File resDir, ProgressBar monitor) {
        super(monitor);
        this.project = project;
        this.prjFile = project.getProjectFile();
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
            super.init(); 
            // useful for MPI only, because project is not serializable
            if(project == null) {
                project = Project.load(prjFile);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        compute = project.getDefaultComputeView();
    }

    private boolean isSaved() {
        return resDir != null;
    }
    
    @Override
    public Map<Object, List<Double[]>> execute(int start, int end) {
        Map<Object, List<Double[]>> map = new HashMap<>();
        
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
            map.put(points.get(i).getId(), values);
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
            List<Double[]> values = result.get(point.getId());
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
    public void gather(Map<Object, List<Double[]>> map) {
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
    
    private File getResultFile() {
        if(isTan) {
            return new File(resDir, "metrics-" + pointFile.getName());
        } else {
            return new File(resDir, "metrics-" + (direct ? "direct" : "indirect") + "-" + pointFile.getName());
        }
    }

    
}
