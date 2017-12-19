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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 * Base class for ViewResult.
 * 
 * @author Gilles Vuidel
 */
public abstract class AbstractViewResult implements ViewResult {
    
    private static final Pair UNBOUND = new Pair(0, Double.POSITIVE_INFINITY);
    
    private final GridCoordinates2D coord;
    private double area = -1;
    private Map<Pair, double []> areaLand = null;
    
    protected Raster landuse;

    /**
     * Creates a new Viewresult
     * @param coord the coordinate of the point of view or observed point in grid coordinate
     */
    public AbstractViewResult(GridCoordinates2D coord) {
        this.coord = coord;
        areaLand = new HashMap<>();
    }
    
    @Override
    public final GridCoordinates2D getCoord() {
        return coord;
    }
    
    @Override
    public synchronized double getArea() {
        if(area == -1) {
            area = getArea(UNBOUND.min, UNBOUND.max);
        }
        return area;
    }
    
    @Override
    public double getAreaLandCodes(Set<Integer> codes) {
        if(codes.isEmpty()) {
            return getArea();
        } else {
            double a = 0;
            double[] areaLand = getAreaLand();
            for(int code : codes) {
                a += areaLand[code];
            }
            return a;
        }
    }

    @Override
    public double[] getAreaLand() {
        return getAreaLand(UNBOUND.min, UNBOUND.max);
    }

    @Override
    public double[] getAreaLand(double dmin, double dmax) {
        final Pair pair = new Pair(dmin, dmax);
        synchronized(this) {
            if(!areaLand.containsKey(pair)) {
                areaLand.put(pair, calcAreaLand(dmin, dmax));
            }
        }
        return areaLand.get(pair);
    }
    
    protected abstract double[] calcAreaLand(double dmin, double dmax);
    
    @Override
    public synchronized Raster getLanduseView() {
        if(landuse == null && getData().hasLandUse()) {
            final int w = getView().getWidth();
            final int h = getView().getHeight();
            WritableRaster landview = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_SHORT, w, h, 1), null);
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    landview.setSample(x, y, 0, getLand(x, y));
                }
            }
            this.landuse = landview;
        }
        return landuse;
    }

    private static class Pair {
        private double min, max;

        private Pair(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.min) ^ (Double.doubleToLongBits(this.min) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.max) ^ (Double.doubleToLongBits(this.max) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Pair other = (Pair) obj;
            return Double.doubleToLongBits(this.min) == Double.doubleToLongBits(other.min) &&
                Double.doubleToLongBits(this.max) == Double.doubleToLongBits(other.max);
        }
        
    }
}
