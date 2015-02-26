/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 *
 * @author gvuidel
 */
public abstract class ComputeView {
    
    /**
     * Alpha precision in radian
     */
    protected double aPrec;

    public ComputeView(double aPrec) {
        setaPrec(aPrec);
    }
    
    public List<Double[]> aggrViewShed(DirectPosition2D cg, double startZ, double destZ, boolean direct, Bounds bounds, List<? extends ViewShedMetric> metrics) {
        try {
            ViewShedResult view = calcViewShed(cg, startZ, destZ, direct, bounds);
            
            List<Double[]> results = new ArrayList<>(metrics.size());
            for(ViewShedMetric m : metrics) {
                results.add(m.calcMetric(view));
            }
            return results;
        } catch (TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    public List<Double[]> aggrViewTan(DirectPosition2D cg, double startZ, Bounds bounds, List<? extends ViewTanMetric> metrics) {
        try {
            ViewTanResult view = calcViewTan(cg, startZ, bounds);
            
            List<Double[]> results = new ArrayList<>(metrics.size());
            for(ViewTanMetric m : metrics) {
                results.add(m.calcMetric(view));
            }
            return results;
        } catch (TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    public abstract ViewTanResult calcViewTan(DirectPosition2D cg, double startZ, Bounds bounds) throws TransformException;
    public abstract ViewShedResult calcViewShed(DirectPosition2D cg, double startZ, double destZ, boolean direct, Bounds bounds) throws TransformException;
    
    
    /**
     * Default implementation does nothing
     */
    public void dispose() {
        
    }

    /**
     * Alpha precision in degree
     * @return 
     */
    public double getaPrec() {
        return aPrec * 180 / Math.PI;
    }

    /**
     * 
     * @param aPrec alpha precision in degree
     */
    public final void setaPrec(double aPrec) {
        this.aPrec = aPrec * Math.PI / 180;
    }
    
    
}
