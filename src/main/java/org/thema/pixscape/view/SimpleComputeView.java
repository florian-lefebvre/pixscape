/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.ScaleData;

/**
 *
 * @author gvuidel
 */
public abstract class SimpleComputeView extends ComputeView {
    
    private final ScaleData data;

    public SimpleComputeView(ScaleData data, double aPrec) {
        super(aPrec);
        this.data = data;
    }

    public final ScaleData getData() {
        return data;
    }
    
    protected final GridCoordinates2D getWorld2Grid(DirectPosition2D p) {
        try {
            return data.getGridGeometry().worldToGrid(p);
        } catch (TransformException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
