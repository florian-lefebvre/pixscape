/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.Rectangle;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;

/**
 *
 * @author gvuidel
 */
public class SimpleViewShedResult extends SimpleViewResult implements ViewShedResult {
    
    private double perim = -1;

    public SimpleViewShedResult(GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg, view, compute);
    }
    
    @Override
    public synchronized double getPerimeter() {
        if (perim == -1) {
            double p = 0;
            Raster view = getView();
            final int w = view.getWidth();
            final int h = view.getHeight();
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    if(view.getSample(x, y, 0) != 1) {
                        continue;
                    }
                    if(        x == 0 || view.getSample(x-1, y, 0) != 1
                            || y == 0 || view.getSample(x, y-1, 0) != 1
                            || x == w-1 || view.getSample(x+1, y, 0) != 1
                            || y == h-1 || view.getSample(x, y+1, 0) != 1) {
                        p++;
                    }
                }
            }
            perim = p * getRes2D();
        }
        return perim;
    }

    protected double getAreaUnbounded() {
        byte[] buf = ((DataBufferByte) view.getDataBuffer()).getData();
        int nb = 0;
        for (int v : buf) {
            if (v == 1) {
                nb++;
            }
        }
        return nb * getRes2D()*getRes2D();
    }

    protected double[] getAreaLandUnbounded() {
        final double res2D2 = getRes2D()*getRes2D();
        final double[] count = new double[256];
        byte[] buf = ((DataBufferByte) view.getDataBuffer()).getData();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] == 1) {
                count[getLanduse().getSample(i % getW(), i / getW(), 0)] += res2D2;
            }
        }
        return count;
    }

    @Override
    public double[] getAreaLand(double dmin, double dmax) {
        if(isUnboundedDistance(dmin, dmax)) {
            return getAreaLandUnbounded();
        }
        Raster view = getView();
        int size = (int) Math.ceil(dmax / getRes2D());
        Rectangle r = Double.isInfinite(dmax) ? view.getBounds() :
                new GridEnvelope2D(getCoord().x-size, getCoord().y-size, 2*size+1, 2*size+1).intersection(view.getBounds());
        final double res2D2 = getRes2D()*getRes2D();
        final double[] count = new double[256];
        for(int y = (int) r.getMinY(); y < r.getMaxY(); y++) {
            for(int x = (int) r.getMinX(); x < r.getMaxX(); x++) {
                if(view.getSample(x, y, 0) == 1 && isInside(x, y, dmin, dmax)) {
                    count[getLanduse().getSample(x, y, 0)] += res2D2;
                }
            }
        }
        
        return count;
    }

    @Override
    public double getArea(double dmin, double dmax) {
        if(isUnboundedDistance(dmin, dmax)) {
            return getAreaUnbounded();
        }
        Raster view = getView();
        int size = (int) Math.ceil(dmax / getRes2D());
        Rectangle r = Double.isInfinite(dmax) ? view.getBounds() :
                new GridEnvelope2D(getCoord().x-size, getCoord().y-size, 2*size+1, 2*size+1).intersection(view.getBounds());
        int nb = 0;
        for(int y = (int) r.getMinY(); y < r.getMaxY(); y++) {
            for(int x = (int) r.getMinX(); x < r.getMaxX(); x++) {
                if(view.getSample(x, y, 0) == 1 && isInside(x, y, dmin, dmax)) {
                    nb++;
                }
            }
        }
        
        return nb * getRes2D()*getRes2D();
    }

}
