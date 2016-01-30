package org.thema.pixscape.view;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
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
     * @param earthCurv take into account earth curvature ?
     * @param coefRefraction refraction correction, 0 for no correction
     */
    public MultiComputeViewJava(TreeMap<Double, ScaleData> datas, int distMin, double aPrec, boolean earthCurv, double coefRefraction) {
        super(aPrec, earthCurv, coefRefraction);
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

    private Rectangle calcZones(DirectPosition2D c, SortedMap<Double, GridEnvelope2D> viewZones) throws TransformException {
        Rectangle largestZone = new Rectangle(0, 0, -1, -1);
        for(ScaleData data : datas.values()) {
            GridGeometry2D grid = data.getGridGeometry();
            GridCoordinates2D c0 = grid.worldToGrid(c);
//            Point2D c0 = grid.getCRSToGrid2D().transform((Point2D)c, null);
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
        return largestZone;
    }
    
    @Override
    public MultiViewShedResult calcViewShed(DirectPosition2D c, double startZ, double destZ, boolean direct, Bounds bounds)  {
//        long t1 = System.currentTimeMillis();
        TreeMap<Double, byte[]> viewBufs = new TreeMap<>();
        TreeMap<Double, Raster> viewRasters = new TreeMap<>();
        try {
            TreeMap<Double, GridEnvelope2D> viewZones = new TreeMap<>();
            Rectangle largestZone = calcZones(c, viewZones);
            
            System.out.println(largestZone);
            
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
                if(bounds.isTheta1Included(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
                a = Math.atan2(-(largestZone.getMaxY()-1), x);
                if(bounds.isTheta1Included(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
            }
            for(int y = largestZone.y+1; y < largestZone.getMaxY()-1; y++) {
                double a = Math.atan2(-y, largestZone.getMinX());
                if(bounds.isTheta1Included(a)) {
                    calcRay(direct, c, startZ, destZ, bounds, viewBufs, a, viewZones);
                }
                a = Math.atan2(-y, largestZone.getMaxX()-1);
                if(bounds.isTheta1Included(a)) {
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
     * @param startDist the distance between the point of view and c0
     * @param startSlope2 the current max slope^2
     * @return the new slope^2
     */
    private double calcRayDirect(final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double destZ, Bounds bounds, ScaleData data, final byte[] view, final Rectangle viewRect, double z0, double startDist, double startSlope2) {
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
        
        if(startSlope2 == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[indv] = 1;
        } else if(startDist == 0) {
            view[indv] = 0;
        }
        
        double maxSlope = startSlope2;
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
            
            double z = dtmBuf.getElemDouble(ind);
            if(Double.isNaN(z)) {
                return Double.NaN;
            }
            
            if(view[indv] == -1) {
                view[indv] = 0;
            }

            // distance au carré
            double d2 = res2D2 * (xx*xx + yy*yy);
            if(startDist > 0) {
                d2 = Math.pow(Math.sqrt(d2) + startDist, 2);
            }
            if(d2 >= bounds.getDmax2()) {
                return Double.NaN;
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
     * @param startDist the distance between the observed point and c0
     * @param startSlope2 the current max slope^2
     * @return the new slope^2
     */
    private double calcRayIndirect(final GridCoordinates2D c0, final GridCoordinates2D c1, 
            final double startZ, Bounds bounds, ScaleData data, final byte[] view, final Rectangle viewRect, double z0, double startDist, double startSlope2) {
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
        
        if(startSlope2 == Double.NEGATIVE_INFINITY && bounds.getDmin() == 0) {
            view[indv] = 1;
        } else if(startDist == 0) {
            view[indv] = 0;
        }
        
        double maxSlope = startSlope2;
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
            
            double z = dtmBuf.getElemDouble(ind);
            if(Double.isNaN(z)) {
                return Double.NaN;
            }
            if(view[indv] == -1) {
                view[indv] = 0;
            }

            final double d2 = Math.pow(res2D * Math.sqrt(xx*xx + yy*yy) + startDist, 2);
            if(d2 >= bounds.getDmax2()) {
                return Double.NaN;
            }
            if(isEarthCurv()) {
                z -= (1 - getCoefRefraction()) * d2 / EARTH_DIAM;
            }
            if(maxSlope >= 0 && z+startZ <= maxZ) {
                continue;
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
    public ViewTanResult calcViewTan(DirectPosition2D p, double startZ, Bounds bounds)  {
        try {
            long time = System.currentTimeMillis();
            
            TreeMap<Double, GridEnvelope2D> viewZones = new TreeMap<>();
            calcZones(p, viewZones);

            int n = (int)Math.ceil(bounds.getAmplitudeRad()/getRadaPrec());
            WritableRaster view = Raster.createBandedRaster(DataBuffer.TYPE_INT, n, (int)Math.ceil(Math.PI/getRadaPrec()), 1, null);
            int [] viewBuf = ((DataBufferInt)view.getDataBuffer()).getData();
            Arrays.fill(viewBuf, -1);
            WritableRaster scale = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, n, (int)Math.ceil(Math.PI/getRadaPrec()), 1, null);
            byte [] scaleBuf = ((DataBufferByte)scale.getDataBuffer()).getData();
            Arrays.fill(scaleBuf, (byte)-1);

            double aStart = bounds.getTheta1Left();
            for(int ax = 0; ax < n; ax++) {
                double a = (aStart - ax*getRadaPrec() + 2*Math.PI) % (2*Math.PI);
                if(bounds.isTheta1Included(a)) {
                    calcRayTan(p, startZ, bounds, viewBuf, scaleBuf, a, n, ax, getRadaPrec(), viewZones);
                }
            }

            Logger.getLogger(ComputeViewJava.class.getName()).fine((System.currentTimeMillis()-time) + " ms");
        
            return new MultiViewTanResult(datas.firstEntry().getValue().getGridGeometry().worldToGrid(p), view, scale, viewZones, this);
        } catch(TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * Calculates the ray starting from p0 with angle a, at several scales.

     * @param p0 the point of view in world coordinate
     * @param startZ the height of the eye
     * @param bounds the limits of the view
     * @param view the resulting view
     * @param scale the scale for each pixel of view
     * @param a the angle of the ray
     * @param wa the width of the view buffer
     * @param ax the x index in the view buffer for this ray
     * @param ares the resolution (in degree) of the view
     * @param zones the boundary of the view for each scale
     * @throws TransformException 
     */
    private void calcRayTan(final DirectPosition2D p0, final double startZ, Bounds bounds, int[] view, byte[] scale, double a, 
            final int wa, final int ax, final double ares, final TreeMap<Double, GridEnvelope2D> zones) throws TransformException {

        ScaleData dataInit = datas.firstEntry().getValue();
        GridCoordinates2D c0 = dataInit.getDtmCov().getGridGeometry().worldToGrid(p0);
        double z0 = dataInit.getDtm().getSampleDouble(c0.x, c0.y, 0) + startZ;
        
        if(bounds.getDmin() == 0) {
            final double si = Math.min(-startZ / (dataInit.getResolution()/2), bounds.getSlopemax());
            final int zi1 = (int) ((Math.PI/2 - Math.atan(si)) / ares);
            final int zi2 = (int) ((Math.PI/2 - Math.atan(bounds.getSlopemin())) / ares);
            final int ind = c0.x + c0.y*dataInit.getDtm().getWidth();
            for(int yz = zi1; yz < zi2; yz++) {
                view[yz*wa + ax] = (int) ind;
                scale[yz*wa + ax] = 0; // first scale : 0
            }
        }
        
        final GridCoordinates2D c1 = new GridCoordinates2D();
        final GridCoordinates2D c = new GridCoordinates2D();
        double slopeMin = bounds.getSlopemin();
        Envelope2D precEnv = null;
        byte indScale = 0;
        for(ScaleData data : datas.values()) {
            GridGeometry2D grid = data.getDtmCov().getGridGeometry();
            c0 = grid.worldToGrid(p0);
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

            slopeMin = calcRayTan(c0, c1, bounds, data, view, scale, indScale, wa, ax, ares, z0, dist, slopeMin);
            
            if(Double.isNaN(slopeMin)) {
                break;
            }
            precEnv = grid.gridToWorld(rect);
            indScale++;
        }
    }
    
     /**
     * Calculates the ray from c0 to c1 in the scale data
     * Set the pixel index in view when the pixel is viewed from the point of view
     * @param c0 the starting point of the ray
     * @param c1 the ending point of the ray
     * @param bounds the limits of the view
     * @param data the data of the current scale
     * @param view the resulting tangential view
     * @param scale the scale for each pixel
     * @param indScale the current scale index
     * @param wa the width of the view buffer
     * @param ax the x index in the view buffer for this ray
     * @param ares the resolution (in degree) of the view
     * @param z0 the elevation of the point of view
     * @param startDist the distance between the point of view and c0
     * @param startSlope the current max slope
     * @return the new slope
     */
    private double calcRayTan(final GridCoordinates2D c0, final GridCoordinates2D c1, final Bounds bounds, final ScaleData data, final int[] view, 
            final byte[] scale, final byte indScale, final int wa, final int ax, final double ares, final double z0, final double startDist, final double startSlope) {

        final DataBuffer dtmBuf = data.getDtm().getDataBuffer();
        DataBuffer dsmBuf = null;
        if(data.getDsm() != null) {
            dsmBuf = data.getDsm().getDataBuffer();
        }
        final double res = data.getResolution();
        final int w = data.getDtm().getWidth();
        final int dx = Math.abs(c1.x-c0.x);
        final int dy = Math.abs(c1.y-c0.y);
        final int sx = c0.x < c1.x ? 1 : -1;
        final int sy = c0.y < c1.y ? 1 : -1;
        int err = dx-dy;
        int xx = 0;
        int yy = 0;
        int ind = c0.x + c0.y*w;
        final int ind1 = c1.x + c1.y*w;

        double maxSlope = startSlope;
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

            double z = dtmBuf.getElemDouble(ind) + (dsmBuf != null ? dsmBuf.getElemDouble(ind) : 0);
            if(Double.isNaN(z)) {
                return Double.NaN;
            }
            if(maxSlope >= 0 && z <= maxZ) {
                continue;
            }
            final double dist = startDist + res * Math.sqrt(xx*xx + yy*yy) - Math.signum(z-z0)*res/2;
            if(dist >= bounds.getDmax()) {
                return Double.NaN;
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
                            scale[i] = indScale;
                        }
                    }
                }   
                maxSlope = slope;
            }
            if(maxSlope > bounds.getSlopemax()) {
                return Double.NaN;
            }
            if(z > maxZ) {
                maxZ = z;
            }
        }
   
        return maxSlope;
    }
}
