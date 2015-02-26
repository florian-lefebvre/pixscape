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

/**
 *
 * @author gvuidel
 */
public interface ViewResult {

    SortedSet<Integer> getCodes();

    GridCoordinates2D getCoord();

    double getArea();

    double[] getAreaLand();
    
    double getArea(double dmin, double dmax);

    double[] getAreaLand(double dmin, double dmax);

    GridGeometry2D getGrid();

    Raster getLanduse();

    double getRes2D();

    Raster getView();
    
}
