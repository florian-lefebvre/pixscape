/*
 * Copyright (C) 2018 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
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
package org.thema.pixscape.view;

import java.awt.image.Raster;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.TestTools;

/**
 *
 * @author gvuidel
 */
public class ComputeViewJavaTest {
    
    private static ComputeViewJava compute, computeDsm;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatData(30, 20), 0.1, false, 0);
        computeDsm = new ComputeViewJava(TestTools.createFlatDataWithDsm(30, 20, 2), 0.1, false, 0);
    }

    @Test
    public void testCalcRay() {
        assertEquals(90*90, compute.calcRay(new GridCoordinates2D(0, 0), 0.5, new GridCoordinates2D(0, 0), 0, new Bounds(), true), 1e-10);
        assertEquals(90*90, compute.calcRay(new GridCoordinates2D(0, 0), 0.5, new GridCoordinates2D(0, 0), 1, new Bounds(), true), 1e-10);
        
        assertEquals(0.0, compute.calcRay(new GridCoordinates2D(0, 0), 0, new GridCoordinates2D(0, 1), 0, new Bounds(), true), 1e-10);
        
        assertEquals(ComputeViewJava.rad2deg2((Math.atan(-1/1.5)-Math.atan(-1/0.5))*2*Math.atan(0.5/1)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 0, new Bounds(), true), 1e-10);
        assertEquals(ComputeViewJava.rad2deg2((Math.atan(0/1)-Math.atan(-1/0.5))*2*Math.atan(0.5/1)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 1, new Bounds(), true), 1e-10);
        assertEquals(ComputeViewJava.rad2deg2((Math.atan(1/0.5)-Math.atan(-1/0.5))*2*Math.atan(0.5/1)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 2, new Bounds(), true), 1e-10);
        
        assertEquals(90, compute.calcRay(new GridCoordinates2D(0, 0), 0.5, new GridCoordinates2D(0, 0), 0, new Bounds(), false), 1e-10);
        assertEquals(90, compute.calcRay(new GridCoordinates2D(0, 0), 0.5, new GridCoordinates2D(0, 0), 1, new Bounds(), false), 1e-10);
        
        assertEquals(0.0, compute.calcRay(new GridCoordinates2D(0, 0), 0, new GridCoordinates2D(0, 1), 0, new Bounds(), false), 1e-10);
        
        assertEquals(ComputeViewJava.rad2deg(Math.atan(-1/1.5)-Math.atan(-1/0.5)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 0, new Bounds(), false), 1e-10);
        assertEquals(ComputeViewJava.rad2deg(Math.atan(0/1)-Math.atan(-1/0.5)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 1, new Bounds(), false), 1e-10);
        assertEquals(ComputeViewJava.rad2deg(Math.atan(1/0.5)-Math.atan(-1/0.5)), 
                compute.calcRay(new GridCoordinates2D(0, 0), 1, new GridCoordinates2D(0, 1), 2, new Bounds(), false), 1e-10);
    }
    
    @Test
    public void testCalcViewShedDeg() {
        DirectPosition2D p = new DirectPosition2D(14.5, 9.5);
        ViewShedResult result = compute.calcViewShedDeg(p, 2, -1, false, new Bounds(), false);
        checkRay(compute, result, 2, -1);
        result = compute.calcViewShedDeg(p, 2, 1, false, new Bounds(), false);
        checkRay(compute, result, 2, 1);
        result = compute.calcViewShedDeg(p, 2, 2, false, new Bounds(), false);
        checkRay(compute, result, 2, 2);
        result = compute.calcViewShedDeg(p, 0, 1, false, new Bounds(), false);
        checkRay(compute, result, 0, 1);
        result = computeDsm.calcViewShedDeg(p, 0, -1, false, new Bounds(), false);
        checkRay(computeDsm, result, 0, -1);
        result = computeDsm.calcViewShedDeg(p, 3, -1, false, new Bounds(), false);
        checkRay(computeDsm, result, 3, -1);
        result = computeDsm.calcViewShedDeg(p, 3, 3, false, new Bounds(), false);
        checkRay(computeDsm, result, 3, 3);
        
        result = compute.calcViewShedDeg(p, 2, -1, true, new Bounds(), false);
        checkRayInv(result, 2, -1);
        result = compute.calcViewShedDeg(p, 2, 1, true, new Bounds(), false);
        checkRayInv(result, 2, 1);
        result = compute.calcViewShedDeg(p, 2, 2, true, new Bounds(), false);
        checkRayInv(result, 2, 2);
        result = compute.calcViewShedDeg(p, 0, 1, true, new Bounds(), false);
        checkRayInv(result, 0, 1);
    }
 
    private void checkRay(ComputeViewJava compute, ViewShedResult result, double zEye, double zDest) {
        GridCoordinates2D c = result.getCoord();
        Raster r = result.getView();
        for(int y = 0; y < r.getHeight(); y++) {
            for(int x = 0; x < r.getWidth(); x++) {
                double d = c.distance(x, y);
                double expect = compute.calcRay(c, zEye, new GridCoordinates2D(x, y), zDest, new Bounds(), false);
                if(x == 0 || y == 0 || x == r.getWidth()-1 || y == r.getHeight()-1 || x == c.x || y == c.y || d <= 2) {
                    assertEquals("Ray error at "+x+"-"+y, expect, 
                            r.getSampleDouble(x, y, 0), 1e-10);
                } else {
                    assertEquals("Ray error at "+x+"-"+y, expect, 
                            r.getSampleDouble(x, y, 0), expect*0.6);
                }
            }
        }
    }
    
    private void checkRayInv(ViewShedResult result, double zEye, double zDest) {
        GridCoordinates2D c = result.getCoord();
        Raster r = result.getView();
        for(int y = 0; y < r.getHeight(); y++) {
            for(int x = 0; x < r.getWidth(); x++) {
                double expect = compute.calcRay(new GridCoordinates2D(x, y), zEye, c, zDest, new Bounds(), false);
                assertEquals("Ray inv error at "+x+"-"+y, expect, 
                        r.getSampleDouble(x, y, 0), expect*0.3);      
            }
        }
    }
    
    
    /**
     * Test of calcViewTan method, of class ComputeViewJava.
     */
    @Test
    public void testCalcViewTan() {
        System.out.println("calcViewTan");
        // check if all pixels at image border are seen and their contiguity at 360°
        ViewTanResult tanResult = compute.calcViewTan(new DirectPosition2D(10, 10), 2, new Bounds());
        Raster view = tanResult.getView();
        final int w = view.getWidth();
        final int h = view.getHeight();
        final int wData = tanResult.getGrid().getGridRange2D().width;
        int y = 0;
        while(y < h && view.getSample(0, y, 0) == -1) {
            y++;
        }
        int firstInd = view.getSample(0, y, 0);
        int precInd = view.getSample(0, y, 0);
        for(int x = 1; x < w; x++) {
            y = 0;
            while(y < h && view.getSample(x, y, 0) == -1) {
                y++;
            } 
            int ind = view.getSample(x, y, 0);
            assertTrue("Pixel border are not contiguous", Math.abs((ind%wData) - (precInd%wData)) + Math.abs(ind/wData - precInd/wData) <= 1);
            precInd = ind;
        }
        
        assertTrue("Pixel border are not contiguous", Math.abs((firstInd%wData) - (precInd%wData)) + Math.abs(firstInd/wData - precInd/wData) <= 1);
    }
    
}
