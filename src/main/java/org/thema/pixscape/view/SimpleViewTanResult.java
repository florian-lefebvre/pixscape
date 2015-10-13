
package org.thema.pixscape.view;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
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
    private final double ares;
    
    private WritableRaster elevation, distance, landuse;

    
    /**
     * Creates a new SimpleViewTanResult
     * @param ares the angular resolution in radian
     * @param cg the point of view in grid coordinate
     * @param view the resulting view, may be null
     * @param compute the compute view used
     */
    public SimpleViewTanResult(double ares, GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg, view, compute);
        this.ares = ares;
    }

    @Override
    public final double getAres() {
        return ares;
    }

    protected double calcAreaUnbounded() {
        int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
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
        
        int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
        for(int ind : buf) {
            if(ind > -1) {
                final int x = ind % getW();
                final int y = ind / getW();
                count[getLanduse().getSample(x, y, 0)] += res;
            }
        }
        return count;
    }
    
    @Override
    public double getArea(double dmin, double dmax) {
        if(Bounds.isUnboundedDistance(dmin, dmax)) {
            return calcAreaUnbounded();
        }
        int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
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
        
        int[] buf = ((DataBufferInt) view.getDataBuffer()).getData();
        for(int ind : buf) {
            final int x = ind % getW();
            final int y = ind / getW();
            if(ind > -1 && isInside(x, y, dmin, dmax)) {
                count[getLanduse().getSample(x, y, 0)] += res;
            }
        }
        return count;
    }

    @Override
    public final double getDistance(int theta1, int theta2) {
        final int ind = view.getSample(theta1, theta2, 0);
        if(ind == -1) {
            return Double.NaN;
        }
        return getRes2D() * Math.sqrt(Math.pow(getCoord().x-(ind%getW()), 2) + Math.pow(getCoord().y-(ind/getW()), 2));
    }

    @Override
    public final int getLandUse(int theta1, int theta2) {
        final int ind = view.getSample(theta1, theta2, 0);
        if(ind == -1) {
            return -1;
        }
        return getLanduse().getSample(ind%getW(), ind/getW(), 0);
    }

    @Override
    public final double getElevation(int theta1, int theta2) {
        final int ind = view.getSample(theta1, theta2, 0);
        if(ind == -1) {
            return Double.NaN;
        }
        return getData().getZ(ind%getW(), ind/getW());
    }

    @Override
    public synchronized WritableRaster getElevationView() {
        if(elevation == null) {
            fillViews();
        }
        return elevation;
    }

    @Override
    public synchronized WritableRaster getDistanceView() {
        if(distance == null) {
            fillViews();
        }
        return distance;
    }

    @Override
    public synchronized WritableRaster getLanduseView() {
        if(landuse == null) {
            fillViews();
        }
        return landuse;
    }
    
    private void fillViews() {
        elevation = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 
                view.getWidth(), view.getHeight(), 1), null);
        distance = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 
                view.getWidth(), view.getHeight(), 1), null);
        landuse = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, 
                view.getWidth(), view.getHeight(), 1), null);
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                elevation.setSample(x, y, 0, getElevation(x, y));
                distance.setSample(x, y, 0, getDistance(x, y));
                if(getLanduse() != null) {
                    landuse.setSample(x, y, 0, getLandUse(x, y));
                }
            }
        }
    }

    @Override
    public int getThetaWidth() {
        return view.getWidth();
    }

    @Override
    public int getThetaHeight() {
        return view.getHeight();
    }

    @Override
    public double getMaxDistance(int theta1) {
        final int h = getThetaHeight();
        int y = 0;
        while(y < h && view.getSample(theta1, y, 0) == -1) {
            y++;
        } 
        return y == h ? 0 : getDistance(theta1, y);
    }
}
