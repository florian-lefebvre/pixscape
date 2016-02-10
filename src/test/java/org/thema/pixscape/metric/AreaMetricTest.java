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

import java.util.Arrays;
import java.util.TreeSet;
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
public class AreaMetricTest {
    
    private static final int NLAND = 10;
    
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND), 1, false, 0);
        
    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(5, 5), 2, 0, true, new Bounds());
        AreaMetric aMetric = new AreaMetric();
        aMetric.setDistances(new TreeSet<>(Arrays.asList(0.0, 2.0, 4.0)));
        Double[] result = aMetric.calcMetric(view);
        assertEquals("A_0-2 plan", 9, result[0], 0.0);
        assertEquals("A_2-4 plan", 36, result[1], 0.0);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("A[5]_0-2 plan", 3, result[0], 0.0);
        assertEquals("A[5]_2-4 plan", 4, result[1], 0.0);
        
        aMetric = new AreaMetric();
        aMetric.setDistances(new TreeSet<>(Arrays.asList(0.0, 2.1)));
        result = aMetric.calcMetric(view);
        assertEquals("A_0-2.1 plan", 13, result[0], 0.0);

    }

    /**
     * Test of calcMetric method, of class AreaMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        AreaMetric aMetric = new AreaMetric();
        aMetric.setDistances(new TreeSet<>(Arrays.asList(0.0, 2.0, 4.0)));
        Double[] result = aMetric.calcMetric(view);
        assertEquals("A_0-2 tan", 14188, result[0], 0.0);
        assertEquals("A_2-4 tan", 8284, result[1], 0.0);
        
        aMetric.addCode(5);
        result = aMetric.calcMetric(view);
        assertEquals("A[5]_0-2 tan", 7996, result[0], 0.0);
        assertEquals("A[5]_2-4 tan", 1344, result[1], 0.0);
        
        aMetric = new AreaMetric();
        aMetric.setDistances(new TreeSet<>(Arrays.asList(0.0, 2.1)));
        result = aMetric.calcMetric(view);
        assertEquals("A_0-2.1 tan", 16408, result[0], 0.0);
    }

    
}
