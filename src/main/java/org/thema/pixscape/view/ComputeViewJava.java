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
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
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
     * @param earthCurv true for taking into account earth curvature
     * @param coefRefraction refraction correction coefficient, 0 for no correction
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
    public double calcRay(final GridCoordinates2D c0, final double startZ, final GridCoordinates2D c1, 
            final double destZ, Bounds bounds, boolean area) {
        return calcRay(c0, startZ, c1, destZ, bounds, area, 1);
    }
    
    public double calcRay(final GridCoordinates2D c0, final double startZ, final GridCoordinates2D c1, 
            final double destZ, Bounds bounds, boolean area, int dd) {
        
        if(bounds.isOrienBounded() && !bounds.isTheta1Included(Math.atan2(c0.y-c1.y, c1.x-c0.x))) {
            return 0;
        }
        final double res = getData().getResolution();
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
        
        if(ind == ind1 && bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (res/2), bounds.getSlopemax());
            if(bounds.getSlopemin() < si) {
                final double a1 = Math.atan(si);
                final double a2 = Math.atan(bounds.getSlopemin());
                return area ? rad2deg2(Math.pow(2*(a1-a2), 2)) : rad2deg(2*(a1-a2));
            }
        }
        double maxSlope = Math.max(-startZ / (res/2), bounds.getSlopemin());
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
                return 0;
            }
            
            // distance 
            final double dist = res * Math.sqrt(xx*xx + yy*yy);
            if(dist >= bounds.getDmax()) {
                return 0;
            }
            
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * dist*dist / EARTH_DIAM;
            }
            final double zSurf = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double zView = destZ == -1 ? zSurf : (z + destZ);

            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf / (dist-dd*Math.signum(zzSurf)*res/2);
            if(slopeSurf > bounds.getSlopemax()) {
                return 0;
            }

            if(ind == ind1 && dist >= bounds.getDmin()) {
                final double zzView = (zView - z0);
                final double slopeView = zzView / (dist-dd*Math.signum(zzView)*res/2);
                if(slopeView > maxSlope) {
                    final double z1 = Math.atan(Math.min(slopeView, bounds.getSlopemax()));
                    final double z2 = Math.atan(maxSlope);
                    return Math.abs(area ? rad2deg2((z1-z2) * 2*Math.atan((res/2) / dist)) : rad2deg(z1-z2));
                }
                
            }
            if(slopeSurf > maxSlope) {
                maxSlope = slopeSurf;
            }
        }
        
        return 0;
   
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
    
    /**
     * Calculate the viewshed from cg and stores the visible surface of each pixel in squared degree
     * 
     * @param cg the point of view if direct=true, the observed point otherwise. cg is in world coordinate
     * @param startZ the height of the eye of the observer
     * @param destZ the height of the observed points, -1 if not used
     * @param inverse if false, observer is on cg, else observed point is on cg
     * @param bounds the limits of the viewshed
     * @return the resulting viewshed in squared degree
     */
    @Override
    public ViewShedResult calcViewShedDeg(DirectPosition2D p, double startZ, double destZ, boolean inverse, Bounds bounds, boolean area)  {
        return calcViewShedDeg(p, startZ, destZ, inverse, bounds, area, 1);
    }
    
    public ViewShedResult calcViewShedDeg(DirectPosition2D p, double startZ, double destZ, boolean inverse, Bounds bounds, boolean area, int dd)  {
        long time = System.currentTimeMillis();
        WritableRaster view = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_DOUBLE, dtm.getWidth(), dtm.getHeight(), 1), null);
        double [] viewBuf = ((DataBufferDouble)view.getDataBuffer()).getData();
        GridCoordinates2D cg = getWorld2Grid(p);
        GridCoordinates2D c = new GridCoordinates2D();
        for(c.x = 0; c.x < dtm.getWidth(); c.x++) {
            c.y = 0;
            calcRayDeg(inverse, cg, c, startZ, destZ, bounds, viewBuf, area, dd);
            c.y = dtm.getHeight()-1;
            calcRayDeg(inverse, cg, c, startZ, destZ, bounds, viewBuf, area, dd);
        }
        for(c.y = 1; c.y < dtm.getHeight()-1; c.y++) {
            c.x = 0;
            calcRayDeg(inverse, cg, c, startZ, destZ, bounds, viewBuf, area, dd);
            c.x = dtm.getWidth()-1;
            calcRayDeg(inverse, cg, c, startZ, destZ, bounds, viewBuf, area, dd);
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
     * @param a the horizontal angle of the ray in (0-2PI(
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
        
        double ddy = Math.round(Math.tan(a) * (c0.x-x1));
        double y = c0.y + ddy;   
        if(y >= 0 && y < h) {
            y1 = (int)y;   
        } else {
            int ddx = (int) Math.round((c0.y-y1) / Math.tan(a));
            x1 = c0.x + ddx;
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
     * Set view to the surface visible in squared degree when the pixel is seen from the point of view (direct) or sees the observed point (indirect).
     * @param inverse
     * @param c0 the point of view if direct = true, the observed point otherwise, in grid coordinate
     * @param c1 the end point of the ray, in grid coordinate
     * @param startZ the height of the eye
     * @param destZ the eight of the observed point or -1
     * @param bounds the limits of the view
     * @param view the resulting viewshed in squared degree (buffer of the size of dtm data)
     */
    private void calcRayDeg(final boolean inverse, final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double startZ, final double destZ, Bounds bounds, final double[] view, boolean area, int dd) {
        if(!bounds.isOrienBounded() || bounds.isTheta1Included(Math.atan2(c0.y-c1.y, c1.x-c0.x))) {
            if(inverse) {
                calcRayIndirectDeg(c0, c1, startZ, destZ, bounds, view, area, dd);
            } else {
                calcRayDirectDeg(c0, c1, startZ, destZ, bounds, view, area, dd);
            }
        }
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Set view to the surface visible in squared degree when the pixel is seen from the point of view c0.
     * @param c0 the point of view, starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the eye
     * @param destZ the height of observed point or -1
     * @param bounds the limits of the view
     * @param view the result view (buffer of the size of dtm data)
     */
    private void calcRayDirectDeg(final GridCoordinates2D c0, final GridCoordinates2D c1, final double startZ, 
            final double destZ, Bounds bounds, final double[] view, boolean area, int dd) {
        final double res = getData().getResolution();
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
        
        if(view[ind] == 0 && bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (res/2), bounds.getSlopemax());
            if(bounds.getSlopemin() < si) {
                final double a1 = Math.atan(si);
                final double a2 = Math.atan(bounds.getSlopemin());
                view[ind] = area ? rad2deg2(Math.pow(2*(a1-a2), 2)) : rad2deg(2*(a1-a2));
            }
        }
        double maxSlope = Math.max(-startZ / (res/2), bounds.getSlopemin());
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

            // distance
            final double dist = res * Math.sqrt(xx*xx + yy*yy);
            if(dist >= bounds.getDmax()) {
                return;
            }
            
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * dist*dist / EARTH_DIAM;
            }
            
            final double zSurf = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double zView = destZ == -1 ? zSurf : (z + destZ);
            
            if(maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
                continue;
            }
            
            if(view[ind] == 0 && dist >= bounds.getDmin()) {
                final double slopeView = (zView - z0) / (dist-dd*Math.signum(zView - z0)*res/2);
                if(slopeView > maxSlope) {
                    final double z2 = Math.atan(maxSlope);
                    final double z1 = Math.atan(Math.min(bounds.getSlopemax(), slopeView));
                    view[ind] = Math.abs(area ? rad2deg2((z2-z1) * 2*Math.atan((res/2) / dist)) : rad2deg(z2-z1));
                }
            }
            final double slopeSurf = (zSurf - z0) / (dist-dd*Math.signum(zSurf - z0)*res/2);
            if(slopeSurf > bounds.getSlopemax()) {
                return;
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
     * Set view to the surface visible in squared degree when the pixel sees the observed point c0.
     * @param c0 the observed point, starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the point of view
     * @param destZ the height of observed point or -1
     * @param bounds the limits of the view
     * @param view the result view (buffer of the size of dtm data)
     */
    private void calcRayIndirectDeg(final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double startZ, double destZ, Bounds bounds, final double[] view, boolean area, int dd) {
        final double dsmZ = (getData().getDsm()!= null ? getData().getDsm().getSampleDouble(c0.x, c0.y, 0) : 0);
        if(destZ != -1 && destZ < dsmZ) {
            return;
        }
        final double res = getData().getResolution();
        double zBase = dtm.getSampleDouble(c0.x, c0.y, 0);
        final double zTop = zBase + (destZ != -1 ? destZ : dsmZ);
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
        
        if(view[ind] == 0 && bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (res/2), bounds.getSlopemax());
            if(bounds.getSlopemin() < si) {
                final double a1 = Math.atan(si);
                final double a2 = Math.atan(bounds.getSlopemin());
                view[ind] = area ? rad2deg2(Math.pow(2*(a1-a2), 2)) : rad2deg(2*(a1-a2));
            }
        }
        double dBase = res/2;
        
        double maxSlope = -Double.MAX_VALUE;//Math.max(-startZ / (res/2), bounds.getSlopemin());
        double maxSlopeBase = maxSlope;
        double maxZ = -Double.MAX_VALUE;
        boolean first = true;
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
            
            if(first) {
                zBase = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
                if(zBase > zTop) {
                    zBase = zTop;
                }
                int x = ind % w;
                int y = ind / w;
                if(Math.abs(x-c0.x) + Math.abs(y-c0.y) == 2) { // si on a progressé en diagonale
                    dBase += res * (Math.sqrt(2) - 1); 
                }
                first = false;
            }
            
            final double dist = res * Math.sqrt(xx*xx + yy*yy);
            if(dist >= bounds.getDmax()) {
                return;
            }
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * dist*dist / EARTH_DIAM;
            }
            if(maxSlope >= 0 && z+startZ <= maxZ) {
                continue;
            }
            final double zEye = z + startZ;
            final double slopeEye = (zEye - zTop) / (dist+dd*Math.signum(zEye - zTop)*res/2);
            if(view[ind] == 0 && slopeEye > maxSlope && dist >= bounds.getDmin()) {
                final double slopeEyeBase = (zEye - zBase) / (dist-dBase);
                if(slopeEyeBase >= maxSlopeBase) { // on voit l'élément en entier
                    final double z2 = Math.atan(slopeEyeBase);
                    final double z1 = Math.atan(Math.min(bounds.getSlopemax(), slopeEye));
                    view[ind] = Math.abs(area ? rad2deg2((z2-z1) * 2*Math.atan((res/2) / dist)) : rad2deg(z2-z1));
                } else {
                    final double zb = zTop + maxSlope * dist;
                    final double zh = zBase + maxSlopeBase * dist;
                    final double zi = (zEye - zb) / (zh-zb) * (zTop-zBase);
                    if(zi < 0 || zi > (zTop - zBase)) {
                        throw new IllegalStateException("bad calculation !!");
                    }
                    final double zzi = zEye - (zi+zBase);
                    final double z2 = Math.atan(zzi / (dist+dd*Math.signum(zzi)*res/2));
                    final double z1 = Math.atan(Math.min(bounds.getSlopemax(), slopeEye));
                    view[ind] = Math.abs(area ? rad2deg2((z2-z1) * 2*Math.atan((res/2) / dist)) : rad2deg(z2-z1));
                }
            } 
            final double zDsm = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double slope = (zDsm - zTop) / (dist+dd*Math.signum(zDsm - zTop)*res/2);
            if(slope > maxSlope) {
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax()) {
                return;
            }
            if(zDsm > maxZ) {
                maxZ = zDsm;
            }
            
            final double slopeBase = (zBase - zDsm) / dist;
            if(slopeBase > maxSlopeBase) {
                maxSlopeBase = slopeBase;
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
    
    public static double rad2deg2(double rad2) {
        return rad2*Math.pow(180/Math.PI, 2);
    }
    
    public static double rad2deg(double rad) {
        return rad*180/Math.PI;
    }
}
