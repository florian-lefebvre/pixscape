/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import java.awt.image.WritableRaster;
import java.util.SortedSet;
import org.geotools.coverage.grid.GridCoordinates2D;

/**
 *
 * @author gvuidel
 */
public interface ComputeView {
    
    public WritableRaster calcViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds);
    public WritableRaster calcViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds);
    
    public double aggrViewShed(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds);
    public double[] aggrViewShedLand(GridCoordinates2D cg, double startZ, double destZ, boolean direct, Bounds bounds, SortedSet<Integer> codes) ;
    
    public double aggrViewTan(GridCoordinates2D cg, double startZ, double ares, Bounds bounds);
    public double[] aggrViewTanLand(GridCoordinates2D cg, double startZ, double ares, Bounds bounds, SortedSet<Integer> codes);
    
    public void dispose();
}
