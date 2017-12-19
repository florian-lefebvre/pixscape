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
public class AggregationMetricTest {
    
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
        AggregationMetric aMetric = new AggregationMetric();
        Double[] result = aMetric.calcMetric(view);
        assertEquals("AG plan", 9.0 / 13 * 100, result[0], 1e-10);
        
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("AG[1] plan", 9.0 / 13 * 100, result[0], 1e-10);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("AG[1,5] plan", 9.0 / 13 * 100, result[0], 1e-10);

    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute1land.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        AggregationMetric aMetric = new AggregationMetric();
        aMetric.addCode(1);
        Double[] result = aMetric.calcMetric(view);
        assertEquals("AG[1] tan", (2*15*360-360) / (double)(2*73*72+2*71-1) * 100, result[0], 1e-10);

        view = compute.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        aMetric = new AggregationMetric();
        result = aMetric.calcMetric(view);
        assertEquals("AG tan", 97.74244064708614, result[0], 1e-10);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("AG[5] tan", 98.55962719762762, result[0], 1e-10);
        
        aMetric = new AggregationMetric();
        aMetric.addCode(0);
        result = aMetric.calcMetric(view);
        assertEquals("AG[0] tan", 89.03654485049833, result[0], 1e-10);
        
        aMetric = new AggregationMetric();
        aMetric.addCode(1);
        result = aMetric.calcMetric(view);
        assertEquals("AG[1] tan", 92.93785310734465, result[0], 1e-10);
        
        aMetric = new AggregationMetric();
        aMetric.addCode(9);
        result = aMetric.calcMetric(view);
        assertEquals("AG[9] tan", 93.42222222222222, result[0], 1e-10);
    }

    
}
