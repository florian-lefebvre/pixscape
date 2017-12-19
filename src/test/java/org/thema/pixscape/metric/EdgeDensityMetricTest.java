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
public class EdgeDensityMetricTest {
    
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
        EdgeDensityMetric aMetric = new EdgeDensityMetric();
        Double[] result = aMetric.calcMetric(view);
        assertEquals("ED plan", 90 / 100.0, result[0], 1e-10);
        
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("ED[1] plan", 20 / 100.0, result[0], 1e-10);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("ED[1,5] plan", 40 / 100.0, result[0], 1e-10);
        
        view = compute1land.calcViewShed(new DirectPosition2D(5, 5), 2, 0, true, new Bounds());
        aMetric = new EdgeDensityMetric();
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("ED[1] plan", 4 / 100.0, result[0], 1e-10);
    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute1land.calcViewTan(new DirectPosition2D(5, 5), 0.01, new Bounds());
        EdgeDensityMetric aMetric = new EdgeDensityMetric();
        aMetric.addCode(1);
        Double[] result = aMetric.calcMetric(view);
        assertEquals("ED[1] tan", 360.0 / (360*90), result[0], 1e-10);

        view = compute.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        aMetric = new EdgeDensityMetric();
        result = aMetric.calcMetric(view);
        assertEquals("ED tan", 0.06436754458485887, result[0], 1e-10);
        
        for(int i = 0; i < NLAND; i++) {
            aMetric.addCode(i);
        }
        result = aMetric.calcMetric(view);
        assertEquals("ED tan all", 0.06436754458485887, result[0], 1e-10);
        double sum = 0;
        for(int i = 0; i < NLAND; i++) {
            aMetric = new EdgeDensityMetric();
            aMetric.addCode(i);
            sum += aMetric.calcMetric(view)[0];
        }
        assertEquals("ED sum all", sum/2, result[0], 1e-10);
        
        aMetric = new EdgeDensityMetric();
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("ED[5] tan", 0.021652690838943348, result[0], 1e-10);
        
        aMetric = new EdgeDensityMetric();
        aMetric.addCode(0);
        result = aMetric.calcMetric(view);
        assertEquals("ED[0] tan", 0.004054958466202119, result[0], 1e-10);
        
        aMetric = new EdgeDensityMetric();
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("ED[1] tan", 0.00901539309476005, result[0], 1e-10);
    }

    
}
