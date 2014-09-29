/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 *
 * @author gvuidel
 */
public abstract class ComputeView {
    
    protected final Raster dtm, land, dsm;
    protected final SortedSet<Integer> codes;
    
    /** resolution of the grid dtm in meter */
    protected final double res2D;
    /** resolution of altitude Z in meter */
    protected final double resZ;

    /**
     * 
     * @param dtm
     * @param resZ
     * @param res2D
     * @param land can be null
     * @param dsm  can be null
     */
    public ComputeView(Raster dtm, double resZ, double res2D, Raster land, SortedSet<Integer> codes,  Raster dsm) {
        this.dtm = dtm;
        this.resZ = resZ;
        this.res2D = res2D;
        this.land = land;
        this.codes = codes;
        this.dsm = dsm;
    }
    
    public List<Double[]> aggrViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds, List<? extends ViewShedMetric> metrics) {
        ViewShedResult view = calcViewShed(cg, startZ, destZ, direct, bounds);
        
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewShedMetric m : metrics)
            results.add(m.calcMetric(view));
        return results;
    }
    
    public List<Double[]> aggrViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds, List<? extends ViewTanMetric> metrics) {
        ViewTanResult view = calcViewTan(cg, startZ, ares, bounds);
        
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewTanMetric m : metrics)
            results.add(m.calcMetric(view));
        return results;
    }
    
    public abstract ViewTanResult calcViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds);
    public abstract ViewShedResult calcViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds);
    
    
    public abstract double aggrViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds);
    public abstract double[] aggrViewShedLand(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds, SortedSet<Integer> codes) ;
    
    public abstract double aggrViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds);
    public abstract double[] aggrViewTanLand(GridCoordinates2D cg, double startZ, double ares, Bounds bounds, SortedSet<Integer> codes);
    
    public abstract void dispose();
    
}
