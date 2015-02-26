package org.thema.pixscape.view;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 *
 * 
 * @author gvuidel
 */
public class ComputeViewJava extends SimpleComputeView {
    
    private final float[] dtmBuf;
    private DataBuffer dsmBuf;
    private Raster dtm;
    
    public ComputeViewJava(ScaleData data, double aPrec) {
        super(data, aPrec);
        this.dtm = data.getDtm();
        this.dtmBuf = ((DataBufferFloat)dtm.getDataBuffer()).getData();
        if(data.getDsm() != null) {
            dsmBuf = data.getDsm().getDataBuffer();
        }
    }
    
    @Override
    public ViewShedResult calcViewShed(DirectPosition2D p, double startZ, double destZ, boolean direct, Bounds bounds)  {
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
        byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
        GridCoordinates2D cg = getWorld2Grid(p);
//        long time = System.currentTimeMillis();
        for(int x = 0; x < dtm.getWidth(); x++) {
            if(bounds.isAlphaIncluded(Math.atan2(cg.y, x-cg.x))) {
                calcRay(direct, cg.x, cg.y, x, 0, startZ, destZ, bounds, viewBuf);
            }
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-(dtm.getHeight()-1), x-cg.x))) {
                calcRay(direct, cg.x, cg.y, x, dtm.getHeight()-1, startZ, destZ, bounds, viewBuf);
            }
        }
        for(int y = 1; y < dtm.getHeight()-1; y++) {
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-y, -cg.x))) {
                calcRay(direct, cg.x, cg.y, 0, y, startZ, destZ, bounds, viewBuf);
            }
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-y, dtm.getWidth()-1-cg.x))) {
                calcRay(direct, cg.x, cg.y, dtm.getWidth()-1, y, startZ, destZ, bounds, viewBuf);
            }
        }
//        System.out.println((System.currentTimeMillis()-time) + " ms");
        return new SimpleViewShedResult(cg, view, this);
    }
    
    @Override
    public ViewTanResult calcViewTan(DirectPosition2D p, double startZ, Bounds bounds)  {
        int n = (int)Math.ceil(bounds.getAmplitudeRad()/aPrec);
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, n, (int)Math.ceil(Math.PI/aPrec), 1, null);
        int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
        Arrays.fill(viewBuf, -1);
        GridCoordinates2D cg = getWorld2Grid(p);
        double aStart = bounds.getAlphaleft();
        long time = System.currentTimeMillis();
        for(int ax = 0; ax < n; ax++) {
            double a = (aStart - ax*aPrec + 2*Math.PI) % (2*Math.PI);
            if(bounds.isAlphaIncluded(a)) {
                calcRayTan(cg.x, cg.y, startZ, bounds, viewBuf, a, n, ax, aPrec);
            }
        }
        System.out.println((System.currentTimeMillis()-time) + " ms");
        return new SimpleViewTanResult(aPrec, cg, view, this);
    }
    
    private void calcRayTan(final int x0, final int y0, final double startZ, Bounds bounds, final int[] view, final double a, final int wa, final int ax, final double ares) {
        
        final int w = dtm.getWidth();
        final int h = dtm.getHeight();
        int y1 = a >= 0 && a < Math.PI ? 0 : h-1; // haut ou bas ?
        int x1 = a >= Math.PI/2 && a < 1.5*Math.PI ? 0 : w-1; // droite ou gauche ?
        int sens = x1 == 0 ? -1 : +1;

        int ddy = (int) -Math.round(Math.tan(a) * Math.abs(x1-x0));
        int y = y0 + sens * ddy;   
        if(y >= 0 && y < h) {
            y1 = y;   
        } else {
            int ddx = (int) Math.abs(Math.round(Math.tan(a+Math.PI/2) * Math.abs(y1-y0)));
            x1 = x0 + sens * ddx;
        }        
        
        final double z0 = dtm.getSample(x0, y0, 0) + startZ;
        
        final int dx = Math.abs(x1-x0);
        final int dy = Math.abs(y1-y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = x0 + y0*w;
        final int ind1 = x1 + y1*w;
        
        if(bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (getData().getResolution()/2), bounds.getSlopemax());
            final int zi1 = (int) ((Math.PI/2 - Math.atan(si)) / ares);
            final int zi2 = (int) ((Math.PI/2 - Math.atan(bounds.getSlopemin())) / ares);
            for(int yz = zi1; yz < zi2; yz++) {
                view[yz*wa + ax] = (int) ind;
            }
        }
        double maxSlope = Math.max(-startZ / (getData().getResolution()/2), bounds.getSlopemin());
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

            final double z = dtmBuf[ind] + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(maxSlope >= 0 && z <= maxZ) {
                continue;
            }
            final double dist = getData().getResolution() * Math.sqrt(xx*xx + yy*yy) - Math.signum(z-z0)*getData().getResolution()/2;
            if(dist > bounds.getDmax()) {
                break;
            }
            final double slope = (z - z0) / (dist);
            if(slope > maxSlope) {
                if(dist >= bounds.getDmin()) {
                    final double s2 = Math.min(bounds.getSlopemax(), slope);
                    // tester Math.round à la place de ceil
                    final int z2 = (int) Math.round((Math.PI/2 - Math.atan2(maxSlope*dist, dist)) / ares);
                    final int z1 = (int) ((Math.PI/2 - Math.atan2(s2*dist, dist)) / ares);

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
                break;
            }
            if(z > maxZ) {
                maxZ = z;
            }
        }
   
    }
    
    private void calcRay(final boolean direct, final int x0, final int y0, final int x1, final int y1, final double startZ, final double destZ, Bounds bounds, final byte[] view) {
        if(direct) {
            calcRayDirect(x0, y0, x1, y1, startZ, destZ, bounds, view);
        } else {
            calcRayIndirect(x0, y0, x1, y1, startZ, destZ, bounds, view);
        }
    }
    
    private void calcRayDirect(final int x0, final int y0, final int x1, final int y1, final double startZ, final double destZ, Bounds bounds, final byte[] view) {
        final double res2D2 = getData().getResolution()*getData().getResolution();
        final double z0 = dtm.getSampleDouble(x0, y0, 0) + startZ;
        final int w = dtm.getWidth();
        final int dx = Math.abs(x1-x0);
        final int dy = Math.abs(y1-y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = x0 + y0*w;
        final int ind1 = x1 + y1*w;
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[ind] = 1;
        }
        double maxSlope = bounds.getSlopemin2();
        double maxZ = -Double.MAX_VALUE;
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

            final double zSurf = dtmBuf[ind] + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double zView = destZ == -1 ? zSurf : (dtmBuf[ind] + destZ);
            
            if(maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
                continue;
            }
            
            // distance au carré
            final double d2 = res2D2 * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2()) {
                break;
            }
            
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf*Math.abs(zzSurf) / d2;
            if(slopeSurf > bounds.getSlopemax2()) {
                break;
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
    
    private void calcRayIndirect(final int x0, final int y0, final int x1, final int y1, final double startZ, double destZ, Bounds bounds, final byte[] view) {
        final double dsmZ = (getData().getDsm()!= null ? getData().getDsm().getSampleDouble(x0, y0, 0) : 0);
        if(destZ != -1 && destZ < dsmZ) {
            return;
        }
        final double z0 = dtm.getSampleDouble(x0, y0, 0) + (destZ != -1 ? destZ : dsmZ);
        final int w = dtm.getWidth();
        final int dx = Math.abs(x1-x0);
        final int dy = Math.abs(y1-y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = x0 + y0*w;
        final int ind1 = x1 + y1*w;
        
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
            final double z = dtmBuf[ind];
            if(maxSlope >= 0 && z+startZ <= maxZ) {
                continue;
            }

            final double d2 = getData().getResolution()*getData().getResolution() * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2()) {
                break;
            }
            double zz = z + startZ - z0;
            final double slopeEye = zz*Math.abs(zz) / d2;
            if(slopeEye > maxSlope) {
                if(d2 >= bounds.getDmin2() && slopeEye <= bounds.getSlopemax2()) {
                    view[ind] = 1;
                }
            } 
            final double ztot = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            zz = ztot - z0;
            final double slope = zz*Math.abs(zz) / d2;
            if(slope > maxSlope) {
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax2()) {
                break;
            }
            if(ztot > maxZ) {
                maxZ = ztot;
            }
        }
   
    }

    @Override
    public void dispose() {
    }

}
