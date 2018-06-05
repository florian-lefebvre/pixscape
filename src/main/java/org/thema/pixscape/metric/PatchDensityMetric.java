/*
 * Copyright (C) 2017 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thema.pixscape.metric;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class PatchDensityMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {
    
    
    public PatchDensityMetric() {
        super(true);
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("PD does not support land category groups");
        }
        return new Double[] {getNbPatch(result, getCodes(), false) / result.getArea()};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("PD does not support land category groups");
        }
        return new Double[] {getNbPatch(result, getCodes(), result.isView360()) / result.getArea()};
    }

    @Override
    public String getShortName() {
        return "PD";
    }
    
    public static int getNbPatch(ViewResult view, Set<Integer> codes, boolean cylinder) {
        Raster land = view.getLanduseView();
        final int h = land.getHeight();
        final int w = land.getWidth();
        int k = 0;
        WritableRaster clust = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_INT, w, h, 1), null);
        TreeSet<Integer> set = new TreeSet<>();
        ArrayList<Integer> idClust = new ArrayList<>();

        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                int val = land.getSample(i, j, 0);
                if(val != -1 && (codes.isEmpty() || codes.contains(val))) {
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
            if(cylinder) {
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
        
        return ids.size();
    }
    
    private static int getMinId(List<Integer> ids, int id) {
        while(ids.get(id-1) != id) {
            id = ids.get(id-1);
        }
        return id;
    }  
}
