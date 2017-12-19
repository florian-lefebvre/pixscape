/*
 * Copyright (C) 2016 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
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

import org.geotools.geometry.DirectPosition2D;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.TestTools;
import org.thema.pixscape.view.ComputeViewJava;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class PatchMeanSizeMetricTest {
    
    private static final int NLAND = 10;
    
    private static ComputeViewJava compute, compute1land;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND), 1, false, 0);
        compute1land = new ComputeViewJava(TestTools.createFlatDataWith1CentreLand(10), 1, false, 0);
        
    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(5, 5), 2, 0, true, new Bounds());
        PatchMeanSizeMetric aMetric = new PatchMeanSizeMetric();
        Double[] result = aMetric.calcMetric(view);
        assertEquals("PMS plan", 10, result[0], 1e-10);
        
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[1] plan", 10, result[0], 1e-10);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[1,5] plan", 10, result[0], 1e-10);

    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute1land.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        PatchMeanSizeMetric aMetric = new PatchMeanSizeMetric();
        aMetric.addCode(1);
        Double[] result = aMetric.calcMetric(view);
        assertEquals("PMS[1] tan", view.getAreaLand()[1], result[0], 1e-10);

        view = compute.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        aMetric = new PatchMeanSizeMetric();
        result = aMetric.calcMetric(view);
        assertEquals("PMS tan", view.getArea() / 10, result[0], 1e-10);
        
        aMetric.addCode(0);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0] tan", view.getAreaLand()[0], result[0], 1e-10);
        
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0,1] tan", view.getAreaLandCodes(aMetric.getCodes()) / 2, result[0], 1e-10);
        
        aMetric.addCode(2);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0,1,2] tan", view.getAreaLandCodes(aMetric.getCodes()) / 3, result[0], 1e-10);
        
        aMetric.addCode(3);
        aMetric.addCode(4);
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0,1,2,3,4,5] tan", view.getAreaLandCodes(aMetric.getCodes()) / 6, result[0], 1e-10);

        aMetric.addCode(6);
        aMetric.addCode(7);
        aMetric.addCode(8);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0,1,2,3,4,5,6,7,8] tan", view.getAreaLandCodes(aMetric.getCodes()) / 9, result[0], 1e-10);
        
        aMetric.addCode(9);
        result = aMetric.calcMetric(view);
        assertEquals("PMS[0,1,2,3,4,5,6,7,8,9] tan", view.getArea() / 10, result[0], 1e-10);
    }

    
}
