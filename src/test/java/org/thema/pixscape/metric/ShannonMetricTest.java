/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
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
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.TestTools;
import org.thema.pixscape.view.ComputeViewJava;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author Gilles Vuidel
 */
public class ShannonMetricTest {
    
    private static final int SIZE = 10;
    private static final int NLAND = 10;
    private static final int NLAND_AGGR = 2;
    
    private static ComputeViewJava compute;
    private static ComputeViewJava computeAggr;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(SIZE, NLAND), 1, false, 0);
        computeAggr = new ComputeViewJava(TestTools.createFlatDataWithLand(SIZE, NLAND_AGGR), 1, false, 0);
    }

    /**
     * Test of calcMetric method, of class ShannonMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(SIZE/2, SIZE/2), 2, 0, true, new Bounds());
        Double[] result = new ShannonMetric().calcMetric(view);
        assertEquals("S plan", 1, result[0], 1e-14);
        
        ViewShedResult view2 = computeAggr.calcViewShed(new DirectPosition2D(SIZE/2, SIZE/2), 2, 0, true, new Bounds());
        result = new ShannonMetric().calcMetric(view2);
        assertEquals("S 2 plan", 1, result[0], 0);
        
        ShannonMetric metricAggr = new ShannonMetric();
        TestTools.setMetricCodes(metricAggr, NLAND, NLAND_AGGR);
        
        assertArrayEquals(result, metricAggr.calcMetric(view));
    }

    /**
     * Test of calcMetric method, of class ShannonMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(SIZE/2, SIZE/2), 2,new Bounds());
        Double[] result = new ShannonMetric().calcMetric(view);
        assertEquals("S tan", 0.8004499386300504, result[0], 0);
        
        ViewTanResult view2 = computeAggr.calcViewTan(new DirectPosition2D(SIZE/2, SIZE/2), 2, new Bounds());
        result = new ShannonMetric().calcMetric(view2);
        assertEquals("S 2 tan", 0.9029061000910822, result[0], 0);
        
        ShannonMetric metricAggr = new ShannonMetric();
        TestTools.setMetricCodes(metricAggr, NLAND, NLAND_AGGR);
        
        assertEquals(result[0], metricAggr.calcMetric(view)[0], 0);
    }
    
}
