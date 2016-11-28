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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * Default implementation of SimpleComputeView in Java.
 * 
 * @author Gilles Vuidel
 */
public final class ComputeViewJava extends SimpleComputeView {
    
    private final float[] dtmBuf;
    private DataBuffer dsmBuf;
    private Raster dtm;
    
    /**
     * Creates a new ComputeViewJava.
     * @param data the data for this resolution
     * @param aPrec the precision in degree for tangential view
     */
    public ComputeViewJava(ScaleData data, double aPrec, boolean earthCurv, double coefRefraction) {
        super(data, aPrec, earthCurv, coefRefraction);
        this.dtm = data.getDtm();
        this.dtmBuf = ((DataBufferFloat)dtm.getDataBuffer()).getData();
        if(data.getDsm() != null) {
            dsmBuf = data.getDsm().getDataBuffer();
        }
    }
    
    @Override
    public ViewShedResult calcViewShed(DirectPosition2D p, double startZ, double destZ, boolean inverse, Bounds bounds)  {
        long time = System.currentTimeMillis();
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
        byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
        GridCoordinates2D cg = getWorld2Grid(p);
        GridCoordinates2D c = new GridCoordinates2D();
        for(c.x = 0; c.x < dtm.getWidth(); c.x++) {
            c.y = 0;
            calcRay(inverse, cg, c, startZ, destZ, bounds, viewBuf);
            c.y = dtm.getHeight()-1;
            calcRay(inverse, cg, c, startZ, destZ, bounds, viewBuf);
        }
        for(c.y = 1; c.y < dtm.getHeight()-1; c.y++) {
            c.x = 0;
            calcRay(inverse, cg, c, startZ, destZ, bounds, viewBuf);
            c.x = dtm.getWidth()-1;
            calcRay(inverse, cg, c, startZ, destZ, bounds, viewBuf);
        }
        Logger.getLogger(ComputeViewJava.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
        return new SimpleViewShedResult(cg, view, this);
    }
    
    @Override
    public ViewTanResult calcViewTan(DirectPosition2D p, double startZ, Bounds bounds)  {
        long time = System.currentTimeMillis();
        int n = (int)Math.ceil(bounds.getAmplitudeRad() / getRadaPrec());
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, n, (int)Math.ceil(Math.PI/getRadaPrec()), 1, null);
        int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
        Arrays.fill(viewBuf, -1);
        GridCoordinates2D cg = getWorld2Grid(p);
        double aStart = bounds.getTheta1Left();
        for(int ax = 0; ax < n; ax++) {
            double a = (aStart - ax*getRadaPrec() + 2*Math.PI) % (2*Math.PI);
            if(bounds.isTheta1Included(a)) {
                calcRayTan(cg, startZ, bounds, viewBuf, a, n, ax, getRadaPrec());
            }
        }
        Logger.getLogger(ComputeViewJava.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
        return new SimpleViewTanResult(cg, view, this);
    }
    
    /**
     * Calculates the ray from c0 with angle a.
     * Set the pixel index in view when the pixel is viewed from c0
     * @param c0 the point of view
     * @param startZ the height of the eye
     * @param bounds the limits of the view
     * @param view the resulting tangential view
     * @param a the horizontal angle of the ray
     * @param wa the width of the view buffer
     * @param ax the x index in the view buffer for this ray
     * @param ares the resolution (in degree) of the view
     */
    private void calcRayTan(final GridCoordinates2D c0, final double startZ, final Bounds bounds, final int[] view, 
            final double a, final int wa, final int ax, final double ares) {
        final int w = dtm.getWidth();
        final int h = dtm.getHeight();
        final double res = getData().getResolution();
        int y1 = a >= 0 && a < Math.PI ? 0 : h-1; // haut ou bas ?
        int x1 = a >= Math.PI/2 && a < 1.5*Math.PI ? 0 : w-1; // droite ou gauche ?
        int sens = x1 == 0 ? -1 : +1;

        int ddy = (int) -Math.round(Math.tan(a) * Math.abs(x1-c0.x));
        int y = c0.y + sens * ddy;   
        if(y >= 0 && y < h) {
            y1 = y;   
        } else {
            int ddx = (int) Math.abs(Math.round(Math.tan(a+Math.PI/2) * Math.abs(y1-c0.y)));
            x1 = c0.x + sens * ddx;
        }        
        
        final double z0 = dtm.getSample(c0.x, c0.y, 0) + startZ;
        
        final int dx = Math.abs(x1-c0.x);
        final int dy = Math.abs(y1-c0.y);
        final int sx = c0.x < x1 ? 1 : -1;
        final int sy = c0.y < y1 ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        final int ind1 = x1 + y1*w;
        
        if(bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (res/2), bounds.getSlopemax());
            final int zi1 = (int) ((Math.PI/2 - Math.atan(si)) / ares);
            final int zi2 = (int) ((Math.PI/2 - Math.atan(bounds.getSlopemin())) / ares);
            for(int yz = zi1; yz < zi2; yz++) {
                view[yz*wa + ax] = (int) ind;
            }
        }
        double maxSlope = Math.max(-startZ / (res/2), bounds.getSlopemin());
        double maxZ = Double.NEGATIVE_INFINITY;
        while(ind != ind1) {
            final int e2 = err * 2;
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
            }

            double z = dtmBuf[ind] + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(Double.isNaN(z)) {
                return;
            }
            if(maxSlope >= 0 && z <= maxZ) {
                continue;
            }
            final double dist = res * Math.sqrt(xx*xx + yy*yy) - Math.signum(z-z0)*res/2;
            if(dist >= bounds.getDmax()) {
                return;
            }
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * dist*dist / EARTH_DIAM;
            }
            final double slope = (z - z0) / dist;
            if(slope > maxSlope) {
                if(dist >= bounds.getDmin()) {
                    final double s2 = Math.min(bounds.getSlopemax(), slope);
                    // tester Math.round à la place de ceil
                    final int z2 = (int) Math.round((Math.PI/2 - Math.atan(maxSlope)) / ares);
                    final int z1 = (int) ((Math.PI/2 - Math.atan(s2)) / ares);

                    for(int yz = z1; yz < z2; yz++) {
                        final int i = yz*wa + ax;
                        if(view[i] == -1) {
                            view[i] = (int) ind;
                        }
                    }
                }   
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax()) {
                return;
            }
            if(z > maxZ) {
                maxZ = z;
            }
        }
   
    }
    
    /**
     * Calculates the ray starting from c0 to c1.
     * Set view to 1 when the pixel is seen from the point of view (direct) or sees the observed point (indirect).
     * @param inverse
     * @param c0 the point of view if direct = true, the observed point otherwise, in grid coordinate
     * @param c1 the end point of the ray, in grid coordinate
     * @param startZ the height of the eye
     * @param destZ the eight of the observed point or -1
     * @param bounds the limits of the view
     * @param view the resulting viewshed (buffer of the size of dtm data)
     */
    private void calcRay(final boolean inverse, final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double startZ, final double destZ, Bounds bounds, final byte[] view) {
        if(bounds.isUnbounded() && !isEarthCurv() && !inverse && destZ == -1) {
            calcRayDirectUnbound(c0, c1, startZ, view);
        } else {
            if(!bounds.isOrienBounded() || bounds.isTheta1Included(Math.atan2(c0.y-c1.y, c1.x-c0.x))) {
                if(inverse) {
                    calcRayIndirect(c0, c1, startZ, destZ, bounds, view);
                } else {
                    calcRayDirect(c0, c1, startZ, destZ, bounds, view);
                }
            }
        }
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Set view to 1 when the pixel is seen from the point of view c0.
     * @param c0 the point of view, starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the eye
     * @param destZ the height of observed point or -1
     * @param bounds the limits of the view
     * @param view the result view (buffer of the size of dtm data)
     */
    private void calcRayDirect(final GridCoordinates2D c0, final GridCoordinates2D c1, final double startZ, 
            final double destZ, Bounds bounds, final byte[] view) {
        final double res2D2 = getData().getResolution()*getData().getResolution();
        final int w = dtm.getWidth();
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        final int ind1 = c1.x + c1.y*w;
        final double z0 = dtmBuf[ind] + startZ;
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[ind] = 1;
        }
        double maxSlope = bounds.getSlopemin2();
        double maxZ = -Double.MAX_VALUE;
        while(ind != ind1) {           
            final int e2 = (err << 1);
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
            }
            
            double z = dtmBuf[ind];
            if(Double.isNaN(z)) {
                return;
            }
            
            // distance au carré
            final double d2 = res2D2 * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2()) {
                return;
            }
            
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * d2 / EARTH_DIAM;
            }
            final double zSurf = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double zView = destZ == -1 ? zSurf : (z + destZ);
            
            if(maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
                continue;
            }
            
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf*Math.abs(zzSurf) / d2;
            if(slopeSurf > bounds.getSlopemax2()) {
                return;
            }

            if(d2 >= bounds.getDmin2() && zView >= zSurf) {
                if(zView == zSurf) {
                    if(slopeSurf > maxSlope) {
                        view[ind] = 1;
                    }
                } else {
                    final double zzView = (zView - z0);
                    final double slopeView = zzView*Math.abs(zzView) / d2;
                    if(slopeView > maxSlope) {
                        view[ind] = 1;
                    }
                }
            }
            if(slopeSurf > maxSlope) {
                maxSlope = slopeSurf;
            }
            if(zSurf > maxZ) {
                maxZ = zSurf;
            }
        }
   
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Optimized version without bounds checking and without earth curvature 
     * Set view to 1 when the pixel is seen from the point of view c0.
     * @param c0 the point of view, starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the eye
     * @param view the result view (buffer of the size of dtm data)
     */
    private void calcRayDirectUnbound(final GridCoordinates2D c0, final GridCoordinates2D c1, final double startZ, 
            final byte[] view) {
        final double res2D2 = getData().getResolution()*getData().getResolution();
        final int w = dtm.getWidth();
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        final int ind1 = c1.x + c1.y*w;
        final double z0 = dtmBuf[ind] + startZ;
        
        view[ind] = 1;
        
        double maxSlope = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        while(ind != ind1) {           
            final int e2 = (err << 1);
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
            }
            
            final double zSurf = dtmBuf[ind] + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(Double.isNaN(zSurf)) {
                return;
            }
            
            if(maxSlope >= 0 && zSurf <= maxZ) {
                continue;
            }
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf*Math.abs(zzSurf) / (res2D2 * (xx*xx + yy*yy));
            if(slopeSurf > maxSlope) {
                view[ind] = 1;
                maxSlope = slopeSurf;
            }

            if(zSurf > maxZ) {
                maxZ = zSurf;
            }
        }
   
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Set view to 1 when the pixel sees the observed point c0.
     * @param c0 the observed point, starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the point of view
     * @param destZ the height of observed point or -1
     * @param bounds the limits of the view
     * @param view the result view (buffer of the size of dtm data)
     */
    private void calcRayIndirect(final GridCoordinates2D c0, final GridCoordinates2D c1, final double startZ, double destZ, Bounds bounds, final byte[] view) {
        final double dsmZ = (getData().getDsm()!= null ? getData().getDsm().getSampleDouble(c0.x, c0.y, 0) : 0);
        if(destZ != -1 && destZ < dsmZ) {
            return;
        }
        final double z0 = dtm.getSampleDouble(c0.x, c0.y, 0) + (destZ != -1 ? destZ : dsmZ);
        final int w = dtm.getWidth();
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        final int ind1 = c1.x + c1.y*w;
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[ind] = 1;
        }
        
        double maxSlope = bounds.getSlopemin2();
        double maxZ = -Double.MAX_VALUE;
        while(ind != ind1) {
            final int e2 = err << 1;
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
            }
            
            double z = dtmBuf[ind];
            if(Double.isNaN(z)) {
                return;
            }
            
            final double d2 = getData().getResolution()*getData().getResolution() * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2()) {
                return;
            }
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * d2 / EARTH_DIAM;
            }
            if(maxSlope >= 0 && z+startZ <= maxZ) {
                continue;
            }
                        
            final double zz = z + startZ - z0;
            final double slopeEye = zz*Math.abs(zz) / d2;
            if(slopeEye > maxSlope) {
                if(d2 >= bounds.getDmin2() && slopeEye <= bounds.getSlopemax2()) {
                    view[ind] = 1;
                }
            } 
            final double ztot = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double dz = ztot - z0;
            final double slope = dz*Math.abs(dz) / d2;
            if(slope > maxSlope) {
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax2()) {
                return;
            }
            if(ztot > maxZ) {
                maxZ = ztot;
            }
        }
   
    }
}
