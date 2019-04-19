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

import org.locationtech.jts.geom.Geometry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;

/**
 * Class for defining limits of the sight in 3D polar coordinates (theta1, theta2, r).
 * theta1 is the angle in the x,y plan [0-360°[
 * theta2 is the angle in z  [-90° - +90°]
 * r is the distance [0-infinity[
 * 
 * The bounds are defined by 3 intervals :
 * - r interval : [dmin dmax]
 * - theta2 interval : [zmin zmax]
 * - theta1 interval : [orien-amp/2 orien+amp/2] or in radian [alpharight alphaleft]
 * 
 * For theta1 :
 * angle in radian starts east in inverse CCW.
 * angle in degre starts north in CCW.
 * @author Gilles Vuidel
 */
public final class Bounds implements Serializable {
    
    /** Minimum distance parameter name */
    public static final String DMIN = "dmin";
    /** Maximum distance parameter name */
    public static final String DMAX = "dmax";
    /** Minimum z angle (theta2) parameter name */
    public static final String ZMIN = "zmin";
    /** Maximum z angle (theta2) parameter name */
    public static final String ZMAX = "zmax";
    /** Orientation in (x,y) plan (theta1) parameter name */
    public static final String ORIEN = "orien";
    /** Amplitude (theta1) parameter name */
    public static final String AMP = "amp";
    
    /** Attributes name for creating feature with bounds parameters */
    public static final List<String> ATTRIBUTES = Arrays.asList(
        DMIN, DMAX, ORIEN, AMP, ZMIN, ZMAX
    );
    
    private double dmin, dmax;
    private final double orientation, amplitude;
    private final double alphaleft, alpharight;
    private double slopemin, slopemax;

    /**
     * Creates a default bounds unbounded !
     */
    public Bounds() {
        this.dmin = 0;
        this.dmax = Double.POSITIVE_INFINITY;
        this.orientation = Math.PI/2; // Default to North
        this.amplitude = 2*Math.PI; // 360°
        this.alphaleft = 3*Math.PI/2; // Default to South
        this.alpharight = 3*Math.PI/2; // Default to South
        this.slopemin = Double.NEGATIVE_INFINITY;
        this.slopemax = Double.POSITIVE_INFINITY;
    }
    
    /**
     * Creates a copy of a bounds
     * @param bounds the bounds to dupplicate
     */
    public Bounds(Bounds bounds) {
        this.dmin = bounds.dmin;
        this.dmax = bounds.dmax;
        this.orientation = bounds.orientation;
        this.amplitude = bounds.amplitude;
        this.alphaleft = bounds.alphaleft;
        this.alpharight = bounds.alpharight;
        this.slopemin = bounds.slopemin;
        this.slopemax = bounds.slopemax;
    }
    
    /**
     * Create a new bounds
     * @param dmin minimum distance [0 infinity[
     * @param dmax maximum distance [0 infinity[
     * @param orientation center horizontal orientation in degree [0 360[
     * @param amplitude size of the horizontal angle in degree [0 360]
     * @param zmin minimum vertical angle in degree [-90 +90]
     * @param zmax maximum vertical angle in degree [-90 +90]
     */
    public Bounds(double dmin, double dmax, double orientation, double amplitude, double zmin, double zmax) {
        if(amplitude > 360) {
            throw new IllegalArgumentException("Amplitude cannot be greater than 360°");
        }
        this.dmin = dmin;
        this.dmax = dmax;
        this.orientation = deg2rad(orientation);
        this.amplitude = amplitude * Math.PI / 180;
        this.alphaleft = (this.orientation + this.amplitude/2) % (2*Math.PI);
        this.alpharight = (this.orientation - this.amplitude/2 + 2*Math.PI) % (2*Math.PI);
        this.slopemin = zmin == -90 ? Double.NEGATIVE_INFINITY : Math.tan(zmin * Math.PI / 180);
        this.slopemax = zmax == 90 ? Double.POSITIVE_INFINITY : Math.tan(zmax * Math.PI / 180);
    }

    /**
     * @return true if this bounds is unbounded ie. no limit in the 3 dimension
     */
    public boolean isUnbounded() {
        return dmin == 0 && dmax == Double.POSITIVE_INFINITY 
                && !isOrienBounded()
                && slopemin == Double.NEGATIVE_INFINITY && slopemax == Double.POSITIVE_INFINITY;
    }
    
    /**
     * @return true if theta1 is limited ie. amplitude &lt; 360°
     */
    public boolean isOrienBounded() {
        return amplitude < 2*Math.PI;
    }
    
    /**
     * 
     * @param alpha angle in trigonometric convention (radian, start east, inverse ccw)
     * @return true if alpha is included in theta1 interval
     */
    public boolean isTheta1Included(double alpha) {
        if(!isOrienBounded()) {
            return true;
        }
        final double a = (alpha + 2*Math.PI) % (2*Math.PI);
        if(alpharight < alphaleft) {
            return a >= alpharight && a <= alphaleft;
        } else {
            return a >= alpharight || a <= alphaleft;
        }
    }

    /**
     * @return the "left" side of the theta1 interval in radian
     */
    public double getTheta1Left() {
        return alphaleft;
    }

    /**
     * @return the "right" side of the theta1 interval in radian
     */
    public double getTheta1Right() {
        return alpharight;
    }

    /**
     * @return the minimum distance, default is 0
     */
    public double getDmin() {
        return dmin;
    }

    /**
     * @return the maxnimum distance, default is +Infinity
     */
    public double getDmax() {
        return dmax;
    }

    /**
     * Sets the minimum distance
     * @param dmin the new minimum distance
     */
    public void setDmin(double dmin) {
        this.dmin = dmin;
    }

    /**
     * Sets the maximum distance
     * @param dmax the new maximum distance
     */
    public void setDmax(double dmax) {
        this.dmax = dmax;
    }

    /**
     * @return the squared minimum distance
     */
    public double getDmin2() {
        return dmin*dmin;
    }

    /**
     * @return the squared maximum distance
     */
    public double getDmax2() {
        return dmax*dmax;
    }

    /**
     * Returns the slope corresponding to the Zmin angle (theta2), default is -Infinity
     * @return the minimum slope
     */
    public double getSlopemin() {
        return slopemin;
    }

    /**
     * Returns the slope corresponding to the Zmax angle (theta2), default is +Infinity
     * @return the minimum slope
     */
    public double getSlopemax() {
        return slopemax;
    }
    
    /**
     * @return the signed squared minimum slope
     */
    public double getSlopemin2() {
        return slopemin*Math.abs(slopemin);
    }

    /**
     * @return the signed squared maximum slope
     */
    public double getSlopemax2() {
        return slopemax*Math.abs(slopemax);
    }

    /**
     * @return the orientation of theta1 in degree, default is 0
     */
    public double getOrientation() {
        return rad2deg(orientation);
    }

    /**
     * @return the amplitude of theta1 in degree, default is 360
     */
    public double getAmplitude() {
        return amplitude * 180 / Math.PI;
    }
 
    /**
     * @return the amplitude of theta1 in radian, default is 2*Pi
     */
    public double getAmplitudeRad() {
        return amplitude;
    }
    
    /**
     * @return minimum Z angle (theta2) in degree, default is -90
     */
    public double getZMin() {
        return Math.atan(slopemin) * 180 / Math.PI;
    }
    
    /**
     * @return maximum Z angle (theta2) in degree, default is +90
     */
    public double getZMax() {
        return Math.atan(slopemax) * 180 / Math.PI;
    }

    /**
     * Sets the minimum Z angle (theta2) in degree
     * @param zmin the new minimum Z angle
     */
    public void setZMin(double zmin) {
        slopemin = zmin == -90 ? Double.NEGATIVE_INFINITY : Math.tan(zmin * Math.PI / 180);
    }

    /**
     * Sets the maximum Z angle (theta2) in degree
     * @param zmax the new maximum Z angle
     */
    public void setZMax(double zmax) {
        slopemax = zmax == 90 ? Double.POSITIVE_INFINITY : Math.tan(zmax * Math.PI / 180);
    }
    
    /**
     * Creates new Bounds with the same parameters but the orientation
     * @param orientation the new orientation angle (theta1) in degree
     * @return new Bounds with another orientation
     */
    public Bounds createBounds(double orientation) {
        return new Bounds(dmin, dmax, orientation, getAmplitude(), getZMin(), getZMax());
    }
    
    /**
     * Creates new Bounds with the same parameters but the orientation and the amplitude
     * @param orientation the new orientation angle (theta1) in degree
     * @param amplitude the new amplitude of theta1 in degree
     * @return new Bounds with another orientation and amplitude
     */
    public Bounds createBounds(double orientation, double amplitude) {
        return new Bounds(dmin, dmax, orientation, amplitude, getZMin(), getZMax());
    }
    
    /**
     * Convert an angle in degree to radian for orientation angle (theta1).
     * Change origin from north to east and inverse direction
     * @param a angle in degree
     * @return the angle in radian
     */
    public static double deg2rad(double a) {
        return ((360 - a) * Math.PI / 180 + Math.PI/2) % (2*Math.PI);
    }
    
    /**
     * Convert an angle in radian to degree for orientation angle (theta1).
     * Change origin from east to north and inverse direction
     * @param a angle in radian
     * @return the angle in degree
     */
    public static double rad2deg(double a) {
        return (360 - ((a - Math.PI/2) * 180 / Math.PI)) % 360;
    }
    
    /**
     * Returns a string representation of this bounds.
     * If the bounds is unbounded the string is empty.
     * Returns parameter name and value only for parameter which does not have the default value
     * @return a string representation of this bounds.
     */
    @Override
    public String toString() {
        String s = "";
        if(dmin != 0) {
            s += DMIN+dmin;
        }
        if(dmax != Double.POSITIVE_INFINITY) {
            s += DMAX+dmax;
        }
        if(getOrientation() != 0) {
            s += ORIEN+getOrientation();
        }
        if(getAmplitude() != 360) {
            s += AMP+getAmplitude();
        }
        if(getZMin() != -90) {
            s += ZMIN+getZMin();
        }
        if(getZMax() != 90) {
            s += ZMAX+getZMax();
        }
        return s;
    }
    
    /**
     * Creates a new Bound from this bounds and update values from attribute of the feature for those existing
     * @param f the feature which may contain boundary attributes
     * @return a new updated bounds
     */
    public Bounds updateBounds(Feature f) {
        Bounds b = new Bounds(this);
        if(f.getAttributeNames().contains(ZMIN)) {
            b.setZMin(((Number)f.getAttribute(ZMIN)).doubleValue());
        }
        if(f.getAttributeNames().contains(ZMAX)) {
            b.setZMax(((Number)f.getAttribute(ZMAX)).doubleValue());
        }
        if(f.getAttributeNames().contains(DMIN)) {
            b.setDmin(((Number)f.getAttribute(DMIN)).doubleValue());
        }
        if(f.getAttributeNames().contains(DMAX)) {
            b.setDmax(((Number)f.getAttribute(DMAX)).doubleValue());
        }
        if(f.getAttributeNames().contains(ORIEN)) {
            if(f.getAttributeNames().contains(AMP)) {
                b = b.createBounds(((Number)f.getAttribute(ORIEN)).doubleValue(), ((Number)f.getAttribute(AMP)).doubleValue());
            } else {
                b = b.createBounds(((Number)f.getAttribute(ORIEN)).doubleValue());
            }
        }
        return b;
    }
    
    /**
     * Creates a features containing all the parameters in the attributes feature
     * @param id the feature identifier
     * @param geom the geometry of the feature
     * @return a new feature containing the parameter values of this bounds
     */
    public DefaultFeature createFeatureWithBoundAttr(Object id, Geometry geom) {
        return new DefaultFeature(id, geom, new ArrayList<>(ATTRIBUTES), new ArrayList<>(Arrays.asList(
                getDmin(), getDmax(), getOrientation(), getAmplitude(), getZMin(), getZMax())));
    }
    
    /**
     * Tests if distance interval is unbounded.
     * @param dmin the minimum distance
     * @param dmax the maximum distance
     * @return true if dmin == 0 and dmax == +inf
     */
    public static final boolean isUnboundedDistance(double dmin, double dmax) {
        return dmin == 0 && dmax == Double.POSITIVE_INFINITY;
    }
}
