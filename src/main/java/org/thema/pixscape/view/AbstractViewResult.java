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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    private int[] patches = null;
    
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
    
    @Override
    public int getNbPatch(Set<Integer> codes) {
        if(patches == null) {
            calcPatches();
        }
        
        int nb = 0;
        if(codes.isEmpty()) {
            for(int n : patches) {
                nb += n;
            }
        } else {
            for(int code : codes) {
                nb += patches[code];
            }
        }
        return nb;
    }

    @Override
    public boolean isCyclic() {
        return false;
    }
    
    private void calcPatches() {
        Raster land = getLanduseView();
        final int h = land.getHeight();
        final int w = land.getWidth();
        int k = 0;
        WritableRaster clust = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_INT, w, h, 1), null);
        TreeSet<Integer> set = new TreeSet<>();
        ArrayList<Integer> idClust = new ArrayList<>();
        ArrayList<Integer> landClust = new ArrayList<>();

        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                int val = land.getSample(i, j, 0);
                if(val != -1) {
                    if(i > 0 && land.getSample(i-1, j, 0) == val) {
                        set.add(clust.getSample(i-1, j, 0));
                    }
                    if(j > 0 && land.getSample(i, j-1, 0) == val) {
                        set.add(clust.getSample(i, j-1, 0));
                    }
                    set.remove(0);
                    if(set.isEmpty()) {
                        k++;
                        clust.setSample(i, j, 0, k);
                        landClust.add(val);
                        idClust.add(k);
                    } else if(set.size() == 1) {
                        int id = set.iterator().next();
                        clust.setSample(i, j, 0, idClust.get(id-1));
                    } else {
                        int minId = Integer.MAX_VALUE;
                        for(Integer id : set) {
                            int min = getMinId(idClust, id);
                            if(min < minId) {
                                minId = min;
                            }
                        }

                        for(Integer id : set) {
                            idClust.set(getMinId(idClust, id)-1, minId);
                        }

                        clust.setSample(i, j, 0, minId);
                    }
                    set.clear();
                } 
            }
            if(isCyclic()) {
                set.add(clust.getSample(0, j, 0));
                set.add(clust.getSample(w-1, j, 0));
                set.remove(0);
                if(set.size() == 2 && land.getSample(0, j, 0) == land.getSample(w-1, j, 0)) {
                    int minId = Integer.MAX_VALUE;
                    for(Integer id : set) {
                        int min = getMinId(idClust, id);
                        if(min < minId) {
                            minId = min;
                        }
                    }

                    for(Integer id : set) {
                        idClust.set(getMinId(idClust, id)-1, minId);
                    }
                }
                set.clear();
            }
        }

        for(int i = 0; i < idClust.size(); i++) {
            int m = i+1;
            while(idClust.get(m-1) != m) {
                m = idClust.get(m-1);
            }
            idClust.set(i, m);
        }

        HashSet<Integer> ids = new HashSet<>();
        for(int id : idClust) {
            ids.add(id);
        }
        
        patches = new int[256];
        for(int id : ids) {
            patches[landClust.get(id-1)]++;
        }
        
    }
    
    private int getMinId(List<Integer> ids, int id) {
        while(ids.get(id-1) != id) {
            id = ids.get(id-1);
        }
        return id;
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
