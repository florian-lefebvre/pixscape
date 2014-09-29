package org.thema.pixscape.view;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.thema.pixscape.Bounds;

/**
 *
 * 
 * @author gvuidel
 */
public class ComputeViewJava extends ComputeView {
    
    private final DataBuffer dtmBuf;
    private DataBuffer dsmBuf;
    
    /**
     * 
     * @param dtm
     * @param resZ
     * @param res2D
     * @param land can be null
     * @param dsm  can be null
     */
    public ComputeViewJava(Raster dtm, double resZ, double res2D, Raster land, SortedSet<Integer> codes, Raster dsm) {
        super(dtm, resZ, res2D, land, codes, dsm);
        this.dtmBuf = dtm.getDataBuffer();
        if(dsm != null) {
            dsmBuf = dsm.getDataBuffer();
        }
    }

    @Override
    public double aggrViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds) {
//        long t1 = System.currentTimeMillis();
        byte[] view = ((DataBufferByte)calcViewShed(cg, startZ, destZ, direct, bounds).getView().getDataBuffer()).getData();
        int sum = 0;
        for(int i = 0; i < view.length; i++) {
            sum += view[i];
        }
//        long t2 = System.currentTimeMillis();
//        System.out.println("View " + (t2-t1) + " ms");
        return sum;
    }
    
    @Override
    public double[] aggrViewShedLand(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds, SortedSet<Integer> codes) {
        byte[] view = ((DataBufferByte)calcViewShed(cg, startZ, destZ, direct, bounds).getView().getDataBuffer()).getData();
        final double [] landuse = new double[codes.last()+1];
        final int w = dtm.getWidth();
        for(int i = 0; i < view.length; i++) {
            if(view[i] == 1) {
                final int os = land.getSample(i%w, i/w, 0);
                if(os < landuse.length)
                    landuse[os]++;  
            }
        }
        return landuse;
    }

    @Override
    public double aggrViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds) {
        int[] view = ((DataBufferInt)calcViewTan(cg, startZ, ares, bounds).getView().getDataBuffer()).getData();
        int sum = 0;
        for(int i = 0; i < view.length; i++) {
            if(view[i] > -1)
                sum++;
        }
        return sum * Math.pow(ares*180/Math.PI, 2);
    }

    @Override
    public double[] aggrViewTanLand(GridCoordinates2D cg, double startZ, double ares, Bounds bounds, SortedSet<Integer> codes) {
        int[] view = ((DataBufferInt)calcViewTan(cg, startZ, ares, bounds).getView().getDataBuffer()).getData();
        final int w = dtm.getWidth();
        double [] sum = new double[codes.last()+1];
        for(int i = 0; i < view.length; i++) {
            if(view[i] > -1) {
                final int ind = view[i];
                sum[land.getSample(ind%w, ind/w, 0)]++;
            }
        }
        for(int i = 0; i < sum.length; i++)
            sum[i] = sum[i] * Math.pow(ares*180/Math.PI, 2);
        
        return sum;
    }
    
    @Override
    public ViewShedResult calcViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds)  {
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, dtm.getWidth(), dtm.getHeight(), 1, null);
        byte [] viewBuf = ((DataBufferByte)view.getDataBuffer()).getData();
        
//        long time = System.currentTimeMillis();
        for(int x = 0; x < dtm.getWidth(); x++) {
            if(bounds.isAlphaIncluded(Math.atan2(cg.y, x-cg.x)))
                calcRay(direct, cg.x, cg.y, x, 0, startZ, destZ, bounds, viewBuf);
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-(dtm.getHeight()-1), x-cg.x)))
                calcRay(direct, cg.x, cg.y, x, dtm.getHeight()-1, startZ, destZ, bounds, viewBuf);
        }
        for(int y = 1; y < dtm.getHeight()-1; y++) {
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-y, -cg.x)))
                calcRay(direct, cg.x, cg.y, 0, y, startZ, destZ, bounds, viewBuf);
            if(bounds.isAlphaIncluded(Math.atan2(cg.y-y, dtm.getWidth()-1-cg.x)))
                calcRay(direct, cg.x, cg.y, dtm.getWidth()-1, y, startZ, destZ, bounds, viewBuf);
        }
//        System.out.println((System.currentTimeMillis()-time) + " ms");
        return new ViewShedResult(cg, view, this);
    }
    
    @Override
    public ViewTanResult calcViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds)  {
        int n = (int)Math.ceil(2*Math.PI/ares);
        WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, n, (int)Math.ceil(Math.PI/ares), 1, null);
        int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
        Arrays.fill(viewBuf, -1);
        long time = System.currentTimeMillis();
        // démarre au sud dans le sens des aiguilles d'une montre
        for(int ax = 0; ax < n; ax++) {
            if(bounds.isAlphaIncluded(1.5*Math.PI-(ax*ares)))
                calcRayTan(cg.x, cg.y, startZ, bounds, viewBuf, ax, ares);
        }
        System.out.println((System.currentTimeMillis()-time) + " ms");
        return new ViewTanResult(ares, cg, view, this);
    }
    
    private void calcRayTan(final int x0, final int y0, final double startZ, Bounds bounds, final int[] view, final int ax, final double ares) {
        
        final int w = dtm.getWidth();
        final int h = dtm.getHeight();
        double a = 1.5*Math.PI - ax*ares;
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
        
        final double z0 = dtm.getSample(x0, y0, 0) * resZ + startZ;
        
        final int wa = (int) Math.ceil(2*Math.PI / ares);
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
            final double si = Math.min(-startZ / (res2D/2), bounds.getSlopemax());
            final int zi1 = (int) ((Math.PI/2 - Math.atan(si)) / ares);
            final int zi2 = (int) ((Math.PI/2 - Math.atan(bounds.getSlopemin())) / ares);
            for(int yz = zi1; yz < zi2; yz++) {
                view[yz*wa + ax] = (int) ind;
            }
        }
        double maxSlope = Math.max(-startZ / (res2D/2), bounds.getSlopemin());
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
//            if(Math.abs(xx) > dx || Math.abs(yy) > dy) 
//                return;
            final double z = dtmBuf.getElemDouble(ind) * resZ + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(maxSlope >= 0 && z <= maxZ)
                continue;
            final double dist = res2D * Math.sqrt(xx*xx + yy*yy) - Math.signum(z-z0)*res2D/2;
            if(dist > bounds.getDmax())
                break;
            final double slope = (z - z0) / (dist);
            if(slope > maxSlope) {
                if(dist >= bounds.getDmin()) {
                    final double s2 = Math.min(bounds.getSlopemax(), slope);
                    // tester Math.round à la place de ceil
                    final int z2 = (int) Math.round((Math.PI/2 - Math.atan2(maxSlope*dist, dist)) / ares);
                    final int z1 = (int) ((Math.PI/2 - Math.atan2(s2*dist, dist)) / ares);

                    for(int yz = z1; yz < z2; yz++) {
                        final int i = yz*wa + ax;
                        if(view[i] == -1)
                            view[i] = (int) ind;
                    }
                }   
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax())
                break;
            if(z > maxZ)
                maxZ = z;
        }
   
    }
    
    private void calcRay(final boolean direct, final int x0, final int y0, final int x1, final int y1, final double startZ, final double destZ, Bounds bounds, final byte[] view) {
        if(direct) {
            calcRayDirect(x0, y0, x1, y1, startZ, destZ, bounds, view);
        } else {
            calcRayIndirect(x0, y0, x1, y1, startZ, destZ, bounds, view);
        }
    }
    
    private void calcRayDirect(final int x0, final int y0, final int x1, final int y1, final double startZ, Bounds bounds, final byte[] view) {
        final double res2D2 = res2D*res2D;
        final double z0 = dtm.getSampleDouble(x0, y0, 0) * resZ + startZ;
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
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0)
            view[ind] = 1;
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

            final double z = dtmBuf.getElemDouble(ind) * resZ + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            
            if(maxSlope >= 0 && z <= maxZ)
                continue;

            final double zz = (z - z0);
//            final double slope = zz / (res2D * Math.sqrt(xx*xx + yy*yy));
            final double d2 = res2D2 * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2())
                break;
            final double slope = zz*Math.abs(zz) / d2;
            if(slope > bounds.getSlopemax2())
                break;
            if(slope > maxSlope) {
                if(d2 >= bounds.getDmin2())
                    view[ind] = 1;
                maxSlope = slope;
            }
            if(z > maxZ)
                maxZ = z;
        }
   
    }
    
    private void calcRayDirect(final int x0, final int y0, final int x1, final int y1, final double startZ, final double destZ, Bounds bounds, final byte[] view) {
        final double res2D2 = res2D*res2D;
        final double z0 = dtm.getSampleDouble(x0, y0, 0) * resZ + startZ;
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
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0)
            view[ind] = 1;
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

            final double zSurf = dtmBuf.getElemDouble(ind) * resZ + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            final double zView = destZ == -1 ? zSurf : (dtmBuf.getElemDouble(ind) * resZ + destZ);
            
            if(maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ)
                continue;
            
            // distance au carré
            final double d2 = res2D2 * (xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2())
                break;
            
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf*Math.abs(zzSurf) / d2;
            if(slopeSurf > bounds.getSlopemax2())
                break;

            if(d2 >= bounds.getDmin2() && zView >= zSurf) {
                if(zView == zSurf) {
                    if(slopeSurf > maxSlope)
                        view[ind] = 1;
                } else {
                    final double zzView = (zView - z0);
                    final double slopeView = zzView*Math.abs(zzView) / d2;
                    if(slopeView > maxSlope)
                        view[ind] = 1;
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
        final double dsmZ = (dsm != null ? dsm.getSampleDouble(x0, y0, 0) : 0);
        if(destZ != -1 && destZ < dsmZ)
            return;
        final double z0 = dtm.getSampleDouble(x0, y0, 0) * resZ + (destZ != -1 ? destZ : dsmZ);
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
        
        if(bounds.getSlopemin() == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0)
            view[ind] = 1;
        
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
            final double z = dtmBuf.getElemDouble(ind) * resZ;
            if(maxSlope >= 0 && z+startZ <= maxZ)
                continue;

            final double d2 = (res2D*res2D * xx*xx + yy*yy);
            if(d2 >= bounds.getDmax2())
                break;
            double zz = z + startZ - z0;
            final double slopeEye = zz*Math.abs(zz) / d2;
            if(slopeEye > maxSlope) {
                if(d2 >= bounds.getDmin2() && slopeEye <= bounds.getSlopemax2())
                    view[ind] = 1;
            } 
            final double ztot = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            zz = ztot - z0;
            final double slope = zz*Math.abs(zz) / d2;
            if(slope > maxSlope)
                maxSlope = slope;
            if(maxSlope > bounds.getSlopemax2())
                break;
            if(ztot > maxZ)
                maxZ = ztot;
        }
   
    }

    @Override
    public void dispose() {
    }

}
