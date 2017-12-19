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
import java.util.SortedSet;
import java.util.TreeSet;
import org.thema.common.collection.HashMapList;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Contagion index from Fragstat for planimetric and tangential view.
 * 
 * Calculates the contagion index on the landuse seen.
 * Support landuse codes and landuse code groups.
 * 
 * @author Gilles Vuidel
 */
public class CONTAGMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {

    /**
     * Creates a new CONTAGMetric
     */
    public CONTAGMetric() {
        super(true);
    }

    @Override
    public String getShortName() {
        return "CONTAG";
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {calcCONTAG(result, false)};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        return new Double[] {calcCONTAG(result, result.isView360())};
    }
    
    private double calcCONTAG(ViewResult result, boolean cylinder) {
        int [] count = new int[256];
        int[][] border = new int[256][256];
        Raster land = result.getLanduseView();

        for(int y = 0; y < land.getHeight(); y++) {
            for(int x = 0; x < land.getWidth(); x++) {
                final int l = land.getSample(x, y, 0);
                if(l == -1) {
                    continue;
                }
                count[l]++;
                if(x < land.getWidth()-1) {
                    final int l1 = land.getSample(x+1, y, 0);
                    if(l1 != -1) {
                        border[l][l1]++;
                        border[l1][l]++;
                    }
                } else if(cylinder) {
                    final int l1 = land.getSample(0, y, 0);
                    if(l1 != -1) {
                        border[l][l1]++;
                        border[l1][l]++;
                    }
                }
                if(y < land.getHeight()-1) {
                    final int l1 = land.getSample(x, y+1, 0);
                    if(l1 != -1) {
                        border[l][l1]++;
                        border[l1][l]++;
                    }
                }
            }
        }
        
        SortedSet<Integer> codes;
        if(hasCodeGroup()) {
            HashMapList<Integer, Integer> groups = getCodeGroups();
            int[] c = new int[groups.size()];
            for(int g : groups.keySet()) {
                for(int code : groups.get(g)) {
                    c[g] += count[code];
                }
            }
            count = c;
            int[][] b = new int[groups.size()][groups.size()];
            for(int g1 : groups.keySet()) {
                for(int g2 : groups.keySet()) {
                    for(int c1 : groups.get(g1)) {
                        for(int c2 : groups.get(g2)) {
                            int n = border[c1][c2];
                            b[g1][g2] += n;
                        }
                    }
                }
            }
            border = b;
            codes = new TreeSet<>(groups.keySet());
        } else {
            codes = getCodes(result);
        }
        
        final int m = codes.size();
        if(m < 2) {
            return 100;
        }
        
        double tot = 0;
        for(int nb : count) {
            tot += nb;
        }
        
        int[] sumBorder = new int[256];
        for(int c1 : codes) {
            int sum = 0;
            int [] b = border[c1];
            for(int c2 : codes) {
                sum += b[c2];
            }
            sumBorder[c1] = sum;
        }
        
        double sum = 0;
        for(int c1 : codes) {
            for(int c2 : codes) {
                double val = count[c1]/tot * border[c1][c2];
                if(val > 0) {
                    sum += (val / sumBorder[c1]) * Math.log(val / sumBorder[c1]);
                }
            }
        }
        
        return (1 + sum / (2*Math.log(m))) * 100;
    }
}
