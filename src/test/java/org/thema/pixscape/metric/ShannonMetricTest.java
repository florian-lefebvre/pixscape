/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author gvuidel
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
