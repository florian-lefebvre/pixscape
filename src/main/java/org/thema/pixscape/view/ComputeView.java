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


package org.thema.pixscape.view;

import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;

/**
 * Base class for all visibility computing.
 * 
 * @author Gilles Vuidel
 */
public abstract class ComputeView {
    
    public static final double EARTH_DIAM = 12740000;
    
    /**
     * Alpha precision in radian
     */
    private double aPrec;

    private boolean earthCurv;
    private double coefRefraction;
    
    /**
     * Creates a new instance of ComputeView
     * @param aPrec the precision in degree for tangential view
     * @param earthCurv take into account earth curvature ?
     * @param coefRefraction refraction correction, 0 for no correction
     */
    public ComputeView(double aPrec, boolean earthCurv, double coefRefraction) {
        setaPrec(aPrec);
        this.earthCurv = earthCurv;
        this.coefRefraction = coefRefraction;
    }
    
    /**
     * Calculate the viewshed from cg and calculate all the metrics on this viewshed.
     * 
     * @param cg the point of view if direct=true, the observed point otherwise. cg is in world coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param inverse if false, observer is on cg, else observed point is on cg
     * @param bounds the limits of the viewshed
     * @param metrics the metrics to calculate
     * @return the results of the metrics
     */
    public List<Double[]> aggrViewShed(DirectPosition2D cg, double startZ, double destZ, boolean inverse, Bounds bounds, List<? extends ViewShedMetric> metrics) {
        
        ViewShedResult view = calcViewShed(cg, startZ, destZ, inverse, bounds); 
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewShedMetric m : metrics) {
            results.add(m.calcMetric(view));
        }
        return results;
    }
    
    /**
     * Calculate the tangential view from cg and calculate all the metrics on this view.
     * 
     * @param cg the point of view in world coordinate
     * @param startZ the height of the eye of the observer
     * @param bounds the limits of the viewshed
     * @param metrics the metrics to calculate
     * @return the results of the metrics
     */
    public List<Double[]> aggrViewTan(DirectPosition2D cg, double startZ, Bounds bounds, List<? extends ViewTanMetric> metrics) {
        
        ViewTanResult view = calcViewTan(cg, startZ, bounds);
        List<Double[]> results = new ArrayList<>(metrics.size());
        for(ViewTanMetric m : metrics) {
            results.add(m.calcMetric(view));
        }
        return results;
    }
    
    /**
     * Calculate the tangential view from cg.
     * 
     * @param cg the point of view in world coordinate
     * @param startZ the height of the eye of the observer
     * @param bounds the limits of the viewshed
     * @return the resulting tangential view
     */
    public abstract ViewTanResult calcViewTan(DirectPosition2D cg, double startZ, Bounds bounds) ;
    
    
    /**
     * Calculate the viewshed from cg.
     * 
     * @param cg the point of view if direct=true, the observed point otherwise. cg is in world coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param inverse if false, observer is on cg, else observed point is on cg
     * @param bounds the limits of the viewshed
     * @return the resulting viewshed
     */
    public abstract ViewShedResult calcViewShed(DirectPosition2D cg, double startZ, double destZ, boolean inverse, Bounds bounds) ;
    
    
    /**
     * Default implementation does nothing
     */
    public void dispose() {
        
    }

    /**
     * Alpha precision in radian for tangential view.
     * 
     * @return the precision in radian for tangential view
     */
    protected double getRadaPrec() {
        return aPrec;
    }
    
    /**
     * Alpha precision in degree for tangential view.
     * 
     * @return the precision in degree for tangential view
     */
    public double getaPrec() {
        return aPrec * 180 / Math.PI;
    }

    /**
     * Set the precision in degree for tangential view.
     * @param aPrec alpha precision in degree
     */
    public final void setaPrec(double aPrec) {
        this.aPrec = aPrec * Math.PI / 180;
    }

    /**
     * @return true if taking into account earth curvature
     */
    final public boolean isEarthCurv() {
        return earthCurv;
    }

    /**
     * Sets earth curvature option
     * @param earthCurv take into account earth curvature ?
     */
    public void setEarthCurv(boolean earthCurv) {
        this.earthCurv = earthCurv;
    }

    /**
     * @return the current refraction correction coefficient (default 0.13), 0 for no correction
     */
    final public double getCoefRefraction() {
        return coefRefraction;
    }

    /**
     * Sets the refraction correction coefficient.
     * Set to 0 for no correction, default is 0.13
     * @param coefRefraction the new refraction correction coefficient
     */
    public void setCoefRefraction(double coefRefraction) {
        this.coefRefraction = coefRefraction;
    }
    
}
