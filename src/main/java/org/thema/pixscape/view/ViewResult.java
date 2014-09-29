/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 *
 * @author gvuidel
 */
public abstract class ViewResult {
    protected GridCoordinates2D coord;
    protected Raster view;
    protected int count = -1;
    
    private final ComputeView compute;

    public ViewResult(GridCoordinates2D cg, Raster view, ComputeView compute) {
        this.compute = compute;
        this.coord = cg;
        this.view = view;
    }

    public Raster getView() {
        return view;
    }

    public final Raster getLanduse() {
        return compute.land;
    }
    
    public final Raster getDtm() {
        return compute.dtm;
    }

    public GridCoordinates2D getCoord() {
        return coord;
    }

    public synchronized int getCount() {
        if (count == -1) {
            DataBuffer buf = view.getDataBuffer();
            int nb = 0;
            for (int i = 0; i < buf.getSize(); i++) {
                if (buf.getElem(i) == 1) {
                    nb++;
                }
            }
            count = nb;
        }
        return count;
    }

    public final double getRes2D() {
        return compute.res2D;
    }

    public final SortedSet<Integer> getCodes() {
        return compute.codes;
    }

    public abstract int[] getCountLand();
    
}
