/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public class IJIMetricTest {
    
    private static final int NLAND = 10;
    private static final int NLAND_AGGR = 5;
    
    private static ComputeViewJava compute;
    private static ComputeViewJava computeAggr;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND), 0.1, false, 0);
        computeAggr = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND_AGGR), 0.1, false, 0);
    }
    
   
    /**
     * Test of calcMetric method, of class IJIMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(2, 2), 2, 0, true, new Bounds());
        Double[] result = new IJIMetric().calcMetric(view);
        assertEquals("IJI plan", 57.72049881597967, result[0], 0.0);
        
        ViewShedResult view5 = computeAggr.calcViewShed(new DirectPosition2D(2, 2), 2, 0, true, new Bounds());
        result = new IJIMetric().calcMetric(view5);
        
        IJIMetric ijiMetric = new IJIMetric();
        TestTools.setMetricCodes(ijiMetric, NLAND, NLAND_AGGR);
        
        assertArrayEquals(result, ijiMetric.calcMetric(view));
    }

    /**
     * Test of calcMetric method, of class IJIMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(2, 2), 2, new Bounds());
        Double[] result = new IJIMetric().calcMetric(view);
        assertEquals("IJI tan", 54.33523859661044, result[0], 0.0);
        
        ViewTanResult view5 = computeAggr.calcViewTan(new DirectPosition2D(2, 2), 2, new Bounds());
        result = new IJIMetric().calcMetric(view5);
        
        IJIMetric ijiMetric = new IJIMetric();
        TestTools.setMetricCodes(ijiMetric, NLAND, NLAND_AGGR);
        
        assertArrayEquals(result, ijiMetric.calcMetric(view));
    }
    
}
