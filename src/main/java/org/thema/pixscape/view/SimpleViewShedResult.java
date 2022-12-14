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

import org.locationtech.jts.geom.Geometry;
import java.awt.Rectangle;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.thema.pixscape.Bounds;
import org.thema.process.Vectorizer;

/**
 * ViewShedResult implementation for monoscale computation.
 * 
 * @author Gilles Vuidel
 */
public class SimpleViewShedResult extends SimpleViewResult implements ViewShedResult {
    
    private double perim = -1;

    /**
     * Creates a new SimpleViewShedResult
     * @param cg the point of view or observed point in grid coordinate
     * @param view the resulting viewshed, may be null
     * @param compute the compute view used
     */
    public SimpleViewShedResult(GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg, view, compute);
    }
    
    @Override
    public final int getLand(int x, int y) {
        final int ind = getView().getSample(x, y, 0);
        if(ind == 0) {
            return -1;
        }
        return getData().getLandRaster().getSample(x, y, 0) & 0xff;
    }
    
    @Override
    public synchronized double getPerimeter() {
        if (perim == -1) {
            perim = calcPerimeter(getView()) * getRes2D();
        }
        return perim;
    }

    protected double calcAreaUnbounded() {
        final byte[] buf = ((DataBufferByte) getView().getDataBuffer()).getData();
        int nb = 0;
        for (int v : buf) {
            if (v == 1) {
                nb++;
            }
        }
        return nb * getRes2D()*getRes2D();
    }

    protected double[] calcAreaLandUnbounded() {
        final double res2D2 = getRes2D()*getRes2D();
        final double[] count = new double[256];
        final byte[] buf = ((DataBufferByte) getView().getDataBuffer()).getData();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] == 1) {
                count[getData().getLandRaster().getSample(i % getW(), i / getW(), 0)] += res2D2;
            }
        }
        return count;
    }

    @Override
    protected double[] calcAreaLand(double dmin, double dmax) {
        if(Bounds.isUnboundedDistance(dmin, dmax)) {
            return calcAreaLandUnbounded();
        }
        final Raster view = getView();
        final int size = (int) Math.ceil(dmax / getRes2D());
        final Rectangle r = Double.isInfinite(dmax) ? view.getBounds() :
                new GridEnvelope2D(getCoord().x-size, getCoord().y-size, 2*size+1, 2*size+1).intersection(view.getBounds());
        final double res2D2 = getRes2D()*getRes2D();
        final double[] count = new double[256];
        for(int y = (int) r.getMinY(); y < r.getMaxY(); y++) {
            for(int x = (int) r.getMinX(); x < r.getMaxX(); x++) {
                if(view.getSample(x, y, 0) == 1 && isInside(x, y, dmin, dmax)) {
                    count[getData().getLandRaster().getSample(x, y, 0)] += res2D2;
                }
            }
        }
        
        return count;
    }

    @Override
    public double getArea(double dmin, double dmax) {
        if(Bounds.isUnboundedDistance(dmin, dmax)) {
            return calcAreaUnbounded();
        }
        final Raster view = getView();
        final int size = (int) Math.ceil(dmax / getRes2D());
        final Rectangle r = Double.isInfinite(dmax) ? view.getBounds() :
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

    @Override
    public Geometry getPolygon() {
        Geometry poly = Vectorizer.vectorize(getView(), 1);
        poly.apply(getData().getGrid2World());
        return poly;
    }

    /**
     * Calculates the full perimeter (including holes) of the viewshed in pixel unit
     * @param view the viewshed
     * @return the full perimeter in pixel unit
     */
    public static double calcPerimeter(Raster view) {
        double p = 0;
        final int w = view.getWidth();
        final int h = view.getHeight();
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                if(view.getSample(x, y, 0) != 1) {
                    continue;
                }
                if(x == 0 || view.getSample(x-1, y, 0) != 1) {
                    p++;
                }
                if(y == 0 || view.getSample(x, y-1, 0) != 1) {
                    p++;
                }
                if(x == w-1 || view.getSample(x+1, y, 0) != 1) {
                    p++;
                }
                if(y == h-1 || view.getSample(x, y+1, 0) != 1) {
                    p++;
                }
            }
        }
        return p;
    }
}
