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


package org.thema.pixscape.metric;

import java.awt.image.Raster;
import java.util.TreeSet;
import org.thema.common.collection.HashMapList;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Interspersion index from Fragstat for planimetric and tangential view.
 * 
 * Calculates the interspersion index on the landuse seen.
 * Support landuse codes and landuse code groups.
 * @author Gilles Vuidel
 */
public class IJIMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new IJIMetric
     */
    public IJIMetric() {
        super(true);
    }

    @Override
    public String getShortName() {
        return "IJI";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        int[][] border = new int[256][256];
        Raster view = result.getView();
        Raster land = result.getLanduse();
        int tot = 0;
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                if(view.getSample(x, y, 0) != 1) {
                    continue;
                }
                final int l = land.getSample(x, y, 0) & 0xff;
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) == 1) {
                    final int l1 = land.getSample(x+1, y, 0) & 0xff;
                    if(l != l1) {
                        border[l][l1]++;
                        border[l1][l]++;
                        tot++;
                    }
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) == 1) {
                    final int l1 = land.getSample(x, y+1, 0) & 0xff;
                    if(l != l1) {
                        border[l][l1]++;
                        border[l1][l]++;
                        tot++;
                    }
                }
            }
        }
        
        return new Double[] {calcIJI(result, border, tot)};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        int[][] border = new int[256][256];
        Raster view = result.getView();
        Raster land = result.getLanduseView();
        int tot = 0;
        for(int y = 0; y < view.getHeight(); y++) {
            for(int x = 0; x < view.getWidth(); x++) {
                final int ind = view.getSample(x, y, 0);
                if(ind == -1) {
                    continue;
                }
                final int l = land.getSample(x, y, 0) & 0xff;
                if(x < view.getWidth()-1 && view.getSample(x+1, y, 0) != -1) {
                    final int l1 = land.getSample(x+1, y, 0) & 0xff;
                    if(l != l1) {
                        border[l][l1]++;
                        border[l1][l]++;
                        tot++;
                    }
                }
                if(y < view.getHeight()-1 && view.getSample(x, y+1, 0) != -1) {
                    final int l1 = land.getSample(x, y+1, 0) & 0xff;
                    if(l != l1) {
                        border[l][l1]++;
                        border[l1][l]++;
                        tot++;
                    }
                }
            }
        }
        
        return new Double[] {calcIJI(result, border, tot)};
    }
    
    private double calcIJI(ViewResult result, int[][] border, int tot) {
        TreeSet<Integer> codes;
        if(hasCodeGroup()) {
            tot = 0;
            HashMapList<Integer, Integer> groups = getCodeGroups();
            TreeSet<Integer> codeGroups = new TreeSet<>(groups.keySet());
            int[][] b = new int[groups.size()][groups.size()];
            for(int g1 : codeGroups) {
                for(int g2 : codeGroups.tailSet(g1, false)) {
                    for(int c1 : groups.get(g1)) {
                        for(int c2 : groups.get(g2)) {
                            int n = border[c1][c2];
                            b[g1][g2] += n;
                            tot += n;
                        }
                    }
                }
            }
            border = b;
            codes = new TreeSet<>(groups.keySet());
        } else {
            codes = new TreeSet<>(getCodes(result));
        }
        int m = codes.size();
        if(tot == 0 || m < 3) {
            return 0;
        }
        
        double sum = 0;
        for(int c1 : codes) {
            for(int c2 : codes.tailSet(c1, false)) {
                double val = border[c1][c2];
                if(val > 0) {
                    sum += -(val / tot) * Math.log(val / tot);
                }
            }
        }
        
        return sum / Math.log(0.5*m*(m-1)) * 100;
    }
}
