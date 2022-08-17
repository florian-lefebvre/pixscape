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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RookIter;
import javax.media.jai.iterator.RookIterFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * Implementation of SimpleComputeView in Java without loading rasters in memory
 * For testing, the calculations are very slow
 * 
 * @author Gilles Vuidel
 */
public final class ComputeViewLargeJava extends SimpleComputeView {
    
    private RenderedImage dtm, dsm;
    
    /**
     * Creates a new ComputeViewJava.
     * @param data the data for this resolution
     * @param aPrec the precision in degree for tangential view
     * @param earthCurv true for taking into account earth curvature
     * @param coefRefraction refraction correction coefficient, 0 for no correction
     */
    public ComputeViewLargeJava(ScaleData data, double aPrec, boolean earthCurv, double coefRefraction) {
        super(data, aPrec, earthCurv, coefRefraction);
        this.dtm = data.getDtm();
        this.dsm = data.getDsm() != null ? data.getDsm() : null;
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
        Rectangle r = new Rectangle(new Point(c0.x, c0.y));
        r.add(new Point(c1.x+1, c1.y+1));
        RookIter rDtm = RookIterFactory.create(dtm, r);
        RookIter rDsm = dsm != null ? RookIterFactory.create(dsm, r) : null;
        
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
        rDtm.jumpLines(c0.y-r.y); 
        rDtm.jumpPixels(c0.x-r.x);
        if(rDsm != null) {
            rDsm.jumpLines(c0.y-r.y); 
            rDsm.jumpPixels(c0.x-r.x);
        }
        final double z0 = rDtm.getSampleDouble() + startZ;
        
        if(ind == ind1 && bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (res/2), bounds.getSlopemax());
            if(bounds.getSlopemin() < si) {
                final double a1 = Math.atan(si);
                final double a2 = Math.atan(bounds.getSlopemin());
                return area ? rad2deg2(Math.pow(2*(a1-a2), 2)) : rad2deg(2*(a1-a2));
            }
        }

        double maxSlopeBound = bounds.getSlopemax();
        
        double maxSlope = Math.max(-startZ / (res/2), bounds.getSlopemin());
        while(ind != ind1) {   
            if(maxSlope > maxSlopeBound) {
                return 0;
            }
            final int e2 = (err << 1);
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
                rDtm.jumpPixels(sx);
                if(rDsm != null) {
                    rDsm.jumpPixels(sx);
                }
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
                rDtm.jumpLines(sy);
                if(rDsm != null) {
                    rDsm.jumpLines(sy); 
                }
            }
            
            double z = rDtm.getSampleDouble();
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
            final double zSurf = z + (rDsm != null ? rDsm.getSampleDouble() : 0);

            if(ind == ind1 && dist >= bounds.getDmin()) {
                final double zView = destZ == -1 ? zSurf : (z + destZ);
                final double zzView = (zView - z0);
                final double slopeView = zzView / (dist-dd*Math.signum(zzView)*res/2);
                if(slopeView > maxSlope) {
                    final double z1 = Math.atan(Math.min(slopeView, bounds.getSlopemax()));
                    final double z2 = Math.atan(maxSlope);
                    return Math.abs(area ? rad2deg2((z1-z2) * 2*Math.atan((res/2) / dist)) : rad2deg(z1-z2));
                }
                
            }
            
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf / (dist-dd*Math.signum(zzSurf)*res/2);
            if(slopeSurf > maxSlope) {
                maxSlope = slopeSurf;
            }
        }
        
        return 0;
   
    }

    @Override
    public ViewTanResult calcViewTan(DirectPosition2D cg, double startZ, Bounds bounds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ViewShedResult calcViewShed(DirectPosition2D cg, double startZ, double destZ, boolean inverse, Bounds bounds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ViewShedResult calcViewShedDeg(DirectPosition2D cg, double startZ, double destZ, boolean inverse, Bounds bounds, boolean area) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
