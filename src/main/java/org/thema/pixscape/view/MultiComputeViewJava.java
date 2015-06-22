package org.thema.pixscape.view;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.TreeMap;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * ComputeView using multiscale datas in Java.
 * 
 * @author Gilles Vuidel
 */
public class MultiComputeViewJava extends ComputeView {
    
    private final TreeMap<Double, ScaleData> datas;
    private int distMin;
    
    /**
     * Creates a new MultiComputeViewJava
     * @param datas the data at each scale
     * @param distMin the minimal distance (in pixel) before changing scale
     * @param aPrec the precision in degree for tangential view
     */
    public MultiComputeViewJava(TreeMap<Double, ScaleData> datas, int distMin, double aPrec) {
        super(aPrec);
        this.datas = datas;
        this.distMin = distMin;
    }

    /**
     * @return the minimal distance (in pixel) before changing scale
     */
    public int getDistMin() {
        return distMin;
    }

    /**
     * Sets the minimal distance (in pixel) before changing scale
     * @param distMin the minimal distance before changing scale
     */
    public void setDistMin(int distMin) {
        this.distMin = distMin;
    }

    /**
     * @return the data at each scale
     */
    public TreeMap<Double, ScaleData> getDatas() {
        return datas;
    }

    @Override
    public MultiViewShedResult calcViewShed(DirectPosition2D c, double startZ, double destZ, boolean direct, Bounds bounds)  {
//        long t1 = System.currentTimeMillis();
        TreeMap<Double, byte[]> viewBufs = new TreeMap<>();
        TreeMap<Double, Raster> viewRasters = new TreeMap<>();
        try {
            Rectangle largestZone = new Rectangle(0, 0, -1, -1);
            TreeMap<Double, GridEnvelope2D> viewZones = new TreeMap<>();
            for(ScaleData data : datas.values()) {
                GridGeometry2D grid = data.getGridGeometry();
                GridCoordinates2D c0 = grid.worldToGrid(c);
//                Point2D c0 = grid.getCRSToGrid2D().transform((Point2D)c, null);
                Rectangle rect = data.getDtm().getBounds();
                if(data != datas.lastEntry().getValue()) {
                    GridEnvelope2D rLim = new GridEnvelope2D(new Rectangle(c0.x-distMin, c0.y-distMin, 2*distMin+1, 2*distMin+1).intersection(rect));
                    // adapt the envelope to match pixel border of upper scale
                    // TODO  increase the size when rect is truncate
                    GridGeometry2D gridUpper = datas.higherEntry(data.getResolution()).getValue().getDtmCov().getGridGeometry();
                    rLim = grid.worldToGrid(gridUpper.gridToWorld(gridUpper.worldToGrid(grid.gridToWorld(rLim))));
                    rect = rect.intersection(rLim);
                    rect.add(c0); // ensure point is in zone
                }
                largestZone = largestZone.union(new Rectangle(rect.x-c0.x, rect.y-c0.y, rect.width, rect.height));
                viewZones.put(data.getResolution(), new GridEnvelope2D(rect));
            }
            
            for(ScaleData data : datas.values()) {
                GridEnvelope2D env = viewZones.get(data.getResolution());
                WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, env.width, env.height, 1, new Point(env.x, env.y));
                viewRasters.put(data.getResolution(), view);
                byte[] buf = ((DataBufferByte)view.getDataBuffer()).getData();
                viewBufs.put(data.getResolution(), buf);
                Arrays.fill(buf, (byte)-1);
            }
//            long t3 = System.currentTimeMillis();
            for(int x = largestZone.x; x < largestZone.getMaxX(); x++) {
                double a = Math.atan2(-largestZone.getMinY(), x);
                if(bounds.isAlphaIncluded(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
                a = Math.atan2(-(largestZone.getMaxY()-1), x);
                if(bounds.isAlphaIncluded(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
            }
            for(int y = largestZone.y+1; y < largestZone.getMaxY()-1; y++) {
                double a = Math.atan2(-y, largestZone.getMinX());
                if(bounds.isAlphaIncluded(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
                a = Math.atan2(-y, largestZone.getMaxX()-1);
                if(bounds.isAlphaIncluded(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
            }
//            long t4 = System.currentTimeMillis();
//            System.out.println("init " + (t3-t1) + " rays " + (t4-t3));
//            Logger.getLogger(MultiComputeViewJava.class.getName()).fine((t3-t1) + " ms");
            return new MultiViewShedResult(datas.firstEntry().getValue().getGridGeometry().worldToGrid(c), viewRasters, viewZones, this);
        } catch(TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * Calculates the ray starting from p0 with angle a, at several scales.
     * Set view to 1 when the pixel is seen from the point of view (direct) or sees the observed point (indirect).
     * @param direct
     * @param p0 the point of view if direct = true, the observed point otherwise, in world coordinate
     * @param startZ the height of the eye
     * @param destZ the eight of the observed point or -1
     * @param bounds the limits of the view
     * @param views the resulting viewsheds at every scale
     * @param a the angle of the ray
     * @param zones the boundary of the view for each scale
     * @throws TransformException 
     */
    private void calcRay(final boolean direct, final DirectPosition2D p0, final double startZ, 
            final double destZ, Bounds bounds, final TreeMap<Double, byte[]> views, double a, 
            final TreeMap<Double, GridEnvelope2D> zones) throws TransformException {
//        long t1 = System.nanoTime();
//        long totRay = 0;
        ScaleData dataInit = datas.firstEntry().getValue();
        GridCoordinates2D c0 = dataInit.getDtmCov().getGridGeometry().worldToGrid(p0);
        double z0 = dataInit.getDtm().getSampleDouble(c0.x, c0.y, 0);
        if(direct) {
             z0 += startZ;
        } else {
            // for indirect
            double dsmZ = (dataInit.getDsm() != null ? dataInit.getDsm().getSampleDouble(c0.x, c0.y, 0) : 0);
            if(destZ != -1 && destZ < dsmZ) {
                return;
            }
            z0 += (destZ != -1 ? destZ : dsmZ);
        }
        final GridCoordinates2D c1 = new GridCoordinates2D();
        final GridCoordinates2D c = new GridCoordinates2D();
        double slopeMin2 = bounds.getSlopemin2();
        Envelope2D precEnv = null;
        for(ScaleData data : datas.values()) {
            GridGeometry2D grid = data.getDtmCov().getGridGeometry();
            c0 = grid.worldToGrid(p0);
//            Point2D d0 = grid.getCRSToGrid2D().transform((Point2D)p0, null);
            GridEnvelope2D rect = zones.get(data.getResolution());
            calcIntersects(c0, a, rect, c1);
            double dist = 0;
            if(precEnv != null) {
                Rectangle startRect = rect.intersection(data.getDtmCov().getGridGeometry().worldToGrid(precEnv));
                startRect.add(c0); // ensure point is in zone
                calcIntersects(c0, a, startRect, c);
                dist = data.getResolution() * c.distance(c0);
                c0.x = c.x;
                c0.y = c.y;
            }
//            long t2 = System.nanoTime();
            if(direct) {
                slopeMin2 = calcRayDirect(c0, c1, destZ, bounds, data, views.get(data.getResolution()), rect, z0, dist, slopeMin2);
            } else {
                slopeMin2 = calcRayIndirect(c0, c1, startZ, bounds, data, views.get(data.getResolution()), rect, z0, dist, slopeMin2);
            }
            if(Double.isNaN(slopeMin2)) {
                break;
            }
//            totRay += System.nanoTime()-t2;
            precEnv = grid.gridToWorld(rect);
        }
//        long tot = System.nanoTime();
//        System.out.println("Time : " + (tot-t1) + " ns - Ray : " + totRay + " ns");
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Set view to 1 when the pixel is seen from the point of view.
     * @param c0 the starting point of the ray
     * @param c1 the ending point of the ray
     * @param destZ the height of observed point or -1
     * @param bounds the limits of the view
     * @param data the data of the current scale
     * @param view the result view (buffer of the size of dtm data)
     * @param z0 the elevation of the point of view
     * @param dist the distance between the point of view and c0
     * @param slope2 the current max slope^2
     * @return the new slope^2
     */
    private double calcRayDirect(final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double destZ, Bounds bounds, ScaleData data, final byte[] view, final Rectangle viewRect, double z0, double dist, double slope2) {
        final Raster dtm = data.getDtm();
        final DataBuffer dtmBuf = dtm.getDataBuffer();
        DataBuffer dsmBuf = null;
        if(data.getDsm() != null) {
            dsmBuf = data.getDsm().getDataBuffer();
        }
        final double res2D2 = data.getResolution()*data.getResolution();
        
        final int w = dtm.getWidth();
        final int wv = viewRect.width;
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        int indv = c0.x - viewRect.x + (c0.y - viewRect.y)*wv;
        final int ind1 = c1.x + c1.y*w;
        
        if(slope2 == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[indv] = 1;
        } else if(dist == 0) {
            view[indv] = 0;
        }
        
        double maxSlope = slope2;
        double maxZ = -Double.MAX_VALUE;
        while(ind != ind1) {           
            final int e2 = err * 2;
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
                indv += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
                indv += sy*wv;
            }
            
            final double zSurf = dtmBuf.getElemDouble(ind) + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(Double.isNaN(zSurf)) {
                return Double.NaN;
            }
            final double zView = destZ == -1 ? zSurf : (dtmBuf.getElemDouble(ind) + destZ);
            if(view[indv] == -1) {
                view[indv] = 0;
            }
            if(maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
                continue;
            }
            
            // distance au carré
            double d2 = res2D2 * (xx*xx + yy*yy);
            if(dist > 0) {
                d2 = Math.pow(Math.sqrt(d2) + dist, 2);
            }
            if(d2 >= bounds.getDmax2()) {
                return Double.NaN;
            }
            
            final double zzSurf = (zSurf - z0);
            final double slopeSurf = zzSurf*Math.abs(zzSurf) / d2;
            if(slopeSurf > bounds.getSlopemax2()) {
                return Double.NaN;
            }

            if(d2 >= bounds.getDmin2() && zView >= zSurf) {
                if(zView == zSurf) {
                    if(slopeSurf > maxSlope) {
                        view[indv] = 1;
                    }
                } else {
                    final double zzView = (zView - z0);
                    final double slopeView = zzView*Math.abs(zzView) / d2;
                    if(slopeView > maxSlope) {
                        view[indv] = 1;
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
   
        return maxSlope;
    }
    
    /**
     * Calculates the ray from c0 to c1.
     * Set view to 1 when the pixel sees the observed point.
     * @param c0 the starting point of the ray
     * @param c1 the ending point of the ray
     * @param startZ the height of the point of view
     * @param bounds the limits of the view
     * @param data the data of the current scale
     * @param view the result view (buffer of the size of dtm data)
     * @param z0 the elevation of the observed point
     * @param dist the distance between the observed point and c0
     * @param slope2 the current max slope^2
     * @return the new slope^2
     */
    private double calcRayIndirect(final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double startZ, Bounds bounds, ScaleData data, final byte[] view, final Rectangle viewRect, double z0, double dist, double slope2) {
        final Raster dtm = data.getDtm();
        final DataBuffer dtmBuf = dtm.getDataBuffer();
        DataBuffer dsmBuf = null;
        if(data.getDsm() != null) {
            dsmBuf = data.getDsm().getDataBuffer();
        }
        final double res2D = data.getResolution();
        
        final int w = dtm.getWidth();
        final int wv = viewRect.width;        
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        int indv = c0.x - viewRect.x + (c0.y - viewRect.y)*wv;
        final int ind1 = c1.x + c1.y*w;
        
        if(slope2 == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[indv] = 1;
        } else if(dist == 0) {
            view[indv] = 0;
        }
        
        double maxSlope = slope2;
        double maxZ = -Double.MAX_VALUE;
        while(ind != ind1) {
            final int e2 = err << 1;
            if(e2 > -dy) {
                err -= dy;
                xx += sx;
                ind += sx;
                indv += sx;
            }
            if(e2 < dx) {
                err += dx;
                yy += sy;
                ind += sy*w;
                indv += sy*wv;
            }
            final double z = dtmBuf.getElemDouble(ind);
            if(Double.isNaN(z)) {
                return Double.NaN;
            }
            if(view[indv] == -1) {
                view[indv] = 0;
            }
            if(maxSlope >= 0 && z+startZ <= maxZ) {
                continue;
            }

            final double d2 = Math.pow(res2D * Math.sqrt(xx*xx + yy*yy) + dist, 2);
            if(d2 >= bounds.getDmax2()) {
                return Double.NaN;
            }
            double zz = z + startZ - z0;
            final double slopeEye = zz*Math.abs(zz) / d2;
            if(slopeEye > maxSlope) {
                if(d2 >= bounds.getDmin2() && slopeEye <= bounds.getSlopemax2()) {
                    view[indv] = 1;
                }
            } 
            final double ztot = z + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            zz = ztot - z0;
            final double slope = zz*Math.abs(zz) / d2;
            if(slope > maxSlope) {
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax2()) {
                return Double.NaN;
            }
            if(ztot > maxZ) {
                maxZ = ztot;
            }
        }
   
        return maxSlope;
    }

    /**
     * Calculates the intersection point between the rectangle and the half straight defined by p0 and the angle a.
     * p0 must be within rect.
     * @param p0 the start point of the half straight
     * @param a the angle of the half straight
     * @param rect the rectangle
     * @param res the result point, can be null
     * @return the point intersecting rect with the half straight
     */
    private static GridCoordinates2D calcIntersects(Point2D p0, double a, Rectangle rect, GridCoordinates2D res) {
        if(res == null) {
            res = new GridCoordinates2D();
        }
        if(a == Math.PI/2) {
            res.x = (int) p0.getX();
            res.y = (int) rect.getMinY();
            return res;
        } else if(a == -Math.PI/2) {
            res.x = (int) p0.getX();
            res.y = (int) rect.getMaxY()-1;
            return res;
        } 
        
        if(a < 0) {
            a += 2*Math.PI;
        }
        
        int y1 = (int)(a >= 0 && a < Math.PI ? rect.getMinY() : rect.getMaxY()-1); // haut ou bas ?
        int x1 = (int)(a >= Math.PI/2 && a < 1.5*Math.PI ? rect.getMinX() : rect.getMaxX()-1); // droite ou gauche ?
        int sens = x1 == rect.getMinX() ? -1 : +1;

        double ddy = -(Math.tan(a) * Math.abs(x1-p0.getX()));
        double y = Math.round(p0.getY() + sens * ddy);   
        if(y >= rect.getMinY() && y < rect.getMaxY()) {
            y1 = (int)y;   
        } else {
            double ddx = Math.abs((Math.tan(a+Math.PI/2) * Math.abs(y1-p0.getY())));
            x1 = (int)Math.round(p0.getX() + sens * ddx);
        }    
        res.x = x1;
        res.y = y1;
        return res;
    }

    @Override
    public ViewTanResult calcViewTan(DirectPosition2D cg, double startZ, Bounds bounds)  {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
