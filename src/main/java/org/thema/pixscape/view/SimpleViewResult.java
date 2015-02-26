/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.view;

import java.awt.image.Raster;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.thema.pixscape.ScaleData;

/**
 *
 * @author gvuidel
 */
public abstract class SimpleViewResult extends AbstractViewResult {
    
    private final SimpleComputeView compute;
    private final int w;
    
    protected Raster view;
    
    public SimpleViewResult(GridCoordinates2D cg, Raster view, SimpleComputeView compute) {
        super(cg);
        this.compute = compute;
        this.view = view;
        w = getGrid().getGridRange2D().width;
    }

    @Override
    public Raster getView() {
        return view;
    }

    @Override
    public final Raster getLanduse() {
        return compute.getData().getLand();
    }

    @Override
    public final GridGeometry2D getGrid() {
        return compute.getData().getGridGeometry();
    }

    @Override
    public final double getRes2D() {
        return compute.getData().getResolution();
    }

    @Override
    public final SortedSet<Integer> getCodes() {
        return compute.getData().getCodes();
    }
    
    public final ScaleData getData() {
        return compute.getData();
    }
    
    protected final boolean isInside(int x, int y, double dmin, double dmax) {
        double d2 = getRes2D()*getRes2D() * (Math.pow(x-getCoord().x, 2) + Math.pow(y-getCoord().y, 2));
        return d2 >= dmin*dmin && d2 < dmax*dmax;
    }
    
    protected final int getW() {
        return w;
    }

}
