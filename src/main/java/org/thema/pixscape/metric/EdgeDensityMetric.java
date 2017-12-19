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

import java.awt.image.Raster;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class EdgeDensityMetric extends AbstractMetric implements ViewShedMetric, ViewTanMetric {
    
    
    public EdgeDensityMetric() {
        super(true);
    }

    @Override
    public Double[] calcMetric(ViewShedResult result) {
        return new Double[] {calcNbEdges(result, false) * result.getRes2D() / result.getArea()};
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        return new Double[] {calcNbEdges(result, result.isView360()) * result.getAres()*180/Math.PI / result.getArea()};
    }
    
    private int calcNbEdges(ViewResult result, boolean cylinder) {
        if(hasCodeGroup()) {
            throw new IllegalArgumentException("ED does not support land category groups");
        }
        
        int nbEdges = 0;
        Raster land = result.getLanduseView();
        
        final boolean allCodes = getCodes().isEmpty();
        
        for(int y = 0; y < land.getHeight(); y++) {
            for(int x = 0; x < land.getWidth(); x++) {
                final int l = land.getSample(x, y, 0);
                if(l == -1) {
                    continue;
                }
                if(x < land.getWidth()-1) {
                    final int l1 = land.getSample(x+1, y, 0);
                    if(l1 != -1 && l != l1 && (allCodes || getCodes().contains(l) || getCodes().contains(l1))) {
                        nbEdges++;
                    }
                } else if(cylinder) {
                    final int l1 = land.getSample(0, y, 0);
                    if(l1 != -1 && l != l1 && (allCodes || getCodes().contains(l) || getCodes().contains(l1))) {
                        nbEdges++;
                    }
                }
                if(y < land.getHeight()-1) {
                    final int l1 = land.getSample(x, y+1, 0);
                    if(l1 != -1 && l != l1 && (allCodes || getCodes().contains(l) || getCodes().contains(l1))) {
                        nbEdges++;
                    }
                }
            }
        }
        
        return nbEdges;
    }
    
    @Override
    public String getShortName() {
        return "ED";
    }
  
}
