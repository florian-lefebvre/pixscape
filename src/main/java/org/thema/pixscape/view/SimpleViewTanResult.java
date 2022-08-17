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

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.Bounds;

/**
 * ViewTanResult implementation for monoscale computation.
 * 
 * @author Gilles Vuidel
 */
public class SimpleViewTanResult extends SimpleViewResult implements ViewTanResult {
       
    private WritableRaster elevation, distance;

    /**
     * Creates a new SimpleViewTanResult.
     * This constructor is used by MultiViewTanResult to delegates most methods to this class 
     * @param cg the point of view in grid coordinate
     * @param view the resulting view, may be null
     * @param land the landuse view, may be null
     * @param compute the compute view used
     */
    SimpleViewTanResult(GridCoordinates2D cg, Raster view, Raster land, SimpleComputeView compute) {
        super(cg, view, compute);
        this.landuse = land;
    }
    
    /**
     * Creates a new SimpleViewTanResult
     * @param cg the point of view in grid coordinate
     * @param view the resulting view, may be null
     * @param compute the compute view used
     */
    public SimpleViewTanResult(GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg, view, compute);
    }

    @Override
    public final double getAres() {
        return compute.getRadaPrec();
    }

    protected double calcAreaUnbounded() {
        int[] buf = ((DataBufferInt) getView().getDataBuffer()).getData();
        int nb = 0;
        for(int ind : buf) {
            if(ind > -1) {
                nb++;
            }
        }
        return nb * Math.pow(getAres()*180/Math.PI, 2);
    }

    protected double[] calcAreaLandUnbounded() {
        final double res = Math.pow(getAres()*180/Math.PI, 2);
        final double[] count = new double[256];
        
        short[] buf = ((DataBufferShort) getLanduseView().getDataBuffer()).getData();
        for(int i = 0; i < buf.length; i++) {
            if(buf[i] != -1) {
                count[buf[i]] += res;
            }
        }
        return count;
    }
    
    @Override
    public double getArea(double dmin, double dmax) {
        if(Bounds.isUnboundedDistance(dmin, dmax)) {
            return calcAreaUnbounded();
        }
        int[] buf = ((DataBufferInt) getView().getDataBuffer()).getData();
        int nb = 0;
        for(int ind : buf) {
            if(ind > -1 && isInside(ind % getW(), ind / getW(), dmin, dmax)) {
                nb++;
            }
        }
        return nb * Math.pow(getAres()*180/Math.PI, 2);
    }

    @Override
    protected double[] calcAreaLand(double dmin, double dmax) {
        if(Bounds.isUnboundedDistance(dmin, dmax)) {
            return calcAreaLandUnbounded();
        }
        final double res = Math.pow(getAres()*180/Math.PI, 2);
        final double[] count = new double[256];
        
        int[] buf = ((DataBufferInt) getView().getDataBuffer()).getData();
        short[] landBuf = ((DataBufferShort) getLanduseView().getDataBuffer()).getData();
        for(int i = 0; i < buf.length; i++) {
            final int ind = buf[i];
            final int x = ind % getW();
            final int y = ind / getW();
            if(ind > -1 && isInside(x, y, dmin, dmax)) {
                count[landBuf[i]] += res;
            }
        }
        return count;
    }

    @Override
    public final double getDistance(int theta1, int theta2) {
        final int ind = getView().getSample(theta1, theta2, 0);
        if(ind == -1) {
            return Double.NaN;
        }
        return getRes2D() * Math.sqrt(Math.pow(getCoord().x-(ind%getW()), 2) + Math.pow(getCoord().y-(ind/getW()), 2));
    }

    @Override
    public final double getElevation(int theta1, int theta2) {
        final int ind = getView().getSample(theta1, theta2, 0);
        if(ind == -1) {
            return Double.NaN;
        }
        return getData().getZ(ind%getW(), ind/getW());
    }
    
    @Override
    public final int getLand(int theta1, int theta2) {
        final int ind = getView().getSample(theta1, theta2, 0);
        if(ind == -1) {
            return -1;
        }
        return compute.getData().getLandRaster().getSample(ind%getW(), ind/getW(), 0) & 0xff;
    }

    @Override
    public synchronized Raster getElevationView() {
        if(elevation == null) {
            fillViews();
        }
        return elevation;
    }

    @Override
    public synchronized Raster getDistanceView() {
        if(distance == null) {
            fillViews();
        }
        return distance;
    }
    
    private void fillViews() {
        final int w = getThetaWidth();
        final int h = getThetaHeight();
        elevation = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        distance = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                elevation.setSample(x, y, 0, getElevation(x, y));
                distance.setSample(x, y, 0, getDistance(x, y));
            }
        }
    }

    @Override
    public int getThetaWidth() {
        return getView().getWidth();
    }

    @Override
    public int getThetaHeight() {
        return getView().getHeight();
    }

    @Override
    public double getMaxDistance(int theta1) {
        final int h = getThetaHeight();
        int y = 0;
        while(y < h && getView().getSample(theta1, y, 0) == -1) {
            y++;
        } 
        return y == h ? 0 : getDistance(theta1, y);
    }

    @Override
    public boolean isView360() {
        return getAres() * getThetaWidth() >= 2*Math.PI;
    }

    @Override
    public boolean isCyclic() {
        return isView360();
    }
    
}
