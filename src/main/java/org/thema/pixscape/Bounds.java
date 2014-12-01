/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import java.io.Serializable;

/**
 *
 * @author gvuidel
 */
public final class Bounds implements Serializable {
    private double dmin, dmax;
    private final double orientation, amplitude;
    private final double alphaleft, alpharight;
    private double slopemin, slopemax;

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
    
    public Bounds(double dmin, double dmax, double orientation, double amplitude, double zmin, double zmax) {
        this.dmin = dmin;
        this.dmax = dmax;
        this.orientation = deg2rad(orientation);
        this.amplitude = amplitude * Math.PI / 180;
        this.alphaleft = (this.orientation + this.amplitude/2) % (2*Math.PI);
        this.alpharight = (this.orientation - this.amplitude/2 + 2*Math.PI) % (2*Math.PI);
        this.slopemin = zmin == -90 ? Double.NEGATIVE_INFINITY : Math.tan(zmin * Math.PI / 180);
        this.slopemax = zmax == 90 ? Double.POSITIVE_INFINITY : Math.tan(zmax * Math.PI / 180);
    }

    public boolean isUnbounded() {
        return dmin == 0 && dmax == Double.POSITIVE_INFINITY 
                && !isOrienBounded()
                && slopemin == Double.NEGATIVE_INFINITY && slopemax == Double.POSITIVE_INFINITY;
    }
    
    public boolean isOrienBounded() {
        return amplitude < 2*Math.PI;
    }
    /**
     * 
     * @param alpha angle in trigonometric convention (radian, start east, inverse ccw)
     * @return true if alpha is in bounds
     */
    public boolean isAlphaIncluded(double alpha) {
        if(!isOrienBounded())
            return true;
        final double a = (alpha + 2*Math.PI) % (2*Math.PI);
        if(alpharight < alphaleft)
            return a >= alpharight && a <= alphaleft;
        else
            return a >= alpharight || a <= alphaleft;
    }

    public double getAlphaleft() {
        return alphaleft;
    }

    public double getAlpharight() {
        return alpharight;
    }

    public double getDmin() {
        return dmin;
    }

    public double getDmax() {
        return dmax;
    }

    public void setDmin(double dmin) {
        this.dmin = dmin;
    }

    public void setDmax(double dmax) {
        this.dmax = dmax;
    }

    public double getDmin2() {
        return dmin*dmin;
    }

    public double getDmax2() {
        return dmax*dmax;
    }

    public double getSlopemin() {
        return slopemin;
    }

    public double getSlopemax() {
        return slopemax;
    }
    
    public double getSlopemin2() {
        return slopemin*Math.abs(slopemin);
    }

    public double getSlopemax2() {
        return slopemax*Math.abs(slopemax);
    }

    public double getOrientation() {
        return rad2deg(orientation);
    }

    public double getAmplitude() {
        return amplitude * 180 / Math.PI;
    }
 
    public double getAmplitudeRad() {
        return amplitude;
    }
    
    public double getZMin() {
        return Math.atan(slopemin) * 180 / Math.PI;
    }
    
    public double getZMax() {
        return Math.atan(slopemax) * 180 / Math.PI;
    }

    public void setZMin(double zmin) {
        slopemin = zmin == -90 ? Double.NEGATIVE_INFINITY : Math.tan(zmin * Math.PI / 180);
    }

    public void setZMax(double zmax) {
        slopemax = zmax == 90 ? Double.POSITIVE_INFINITY : Math.tan(zmax * Math.PI / 180);
    }
    
    public Bounds createBounds(double orientation) {
        return new Bounds(dmin, dmax, orientation, getAmplitude(), getZMin(), getZMax());
    }
    
    public Bounds createBounds(double orientation, double amplitude) {
        return new Bounds(dmin, dmax, orientation, amplitude, getZMin(), getZMax());
    }
    
    public static double deg2rad(double a) {
        return ((360 - a) * Math.PI / 180 + Math.PI/2) % (2*Math.PI);
    }
    
    public static double rad2deg(double a) {
        return (360 - ((a - Math.PI/2) * 180 / Math.PI)) % 360;
    }
    
    @Override
    public String toString() {
        String s = "";
        if(dmin != 0)
            s += "dmin"+dmin;
        if(dmax != Double.POSITIVE_INFINITY)
            s += "dmax"+dmax;
        if(getOrientation() != 0)
            s += "orien"+getOrientation();
        if(getAmplitude() != 360)
            s += "amp"+getAmplitude();
        if(getZMin() != -90)
            s += "zmin"+getZMin();
        if(getZMax() != 90)
            s += "zmax"+getZMax();
        return s;
    }
}
