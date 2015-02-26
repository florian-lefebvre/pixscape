/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thema.pixscape.view;

import org.geotools.coverage.grid.GridCoordinates2D;

/**
 *
 * @author gvuidel
 */
public abstract class AbstractViewResult implements ViewResult {
    private final GridCoordinates2D coord;
    private double area = -1;
    private double [] areaLand = null;

    public AbstractViewResult(GridCoordinates2D coord) {
        this.coord = coord;
    }
    
    @Override
    public final GridCoordinates2D getCoord() {
        return coord;
    }
    
    @Override
    public synchronized double getArea() {
        if(area == -1) {
            area = getArea(0, Double.POSITIVE_INFINITY);
        }
        return area;
    }

    @Override
    public synchronized double[] getAreaLand() {
        if(areaLand == null) {
            areaLand = getAreaLand(0, Double.POSITIVE_INFINITY);
        }
        return areaLand;
    }
    
    protected final boolean isUnboundedDistance(double dmin, double dmax) {
        return dmin == 0 && dmax == Double.POSITIVE_INFINITY;
    }

}
