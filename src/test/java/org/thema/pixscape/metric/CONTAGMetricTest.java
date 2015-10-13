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
public class CONTAGMetricTest {

    private static final int NLAND = 10;
    private static final int NLAND_AGGR = 5;
    
    private static ComputeViewJava compute;
    private static ComputeViewJava computeAggr;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND), 0.1);
        computeAggr = new ComputeViewJava(TestTools.createFlatDataWithLand(10, NLAND_AGGR), 0.1);
    }
    

    /**
     * Test of calcMetric method, of class CONTAGMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(2, 2), 2, 0, true, new Bounds());
        Double[] result = new CONTAGMetric().calcMetric(view);
        assertEquals("CONTAG plan", 28.814855556669748, result[0], 0.0);
        
        ViewShedResult view5 = computeAggr.calcViewShed(new DirectPosition2D(2, 2), 2, 0, true, new Bounds());
        result = new CONTAGMetric().calcMetric(view5);
//        assertEquals("CONTAG 5 plan", 30.57159953673201, result[0], 0.0);
        
        CONTAGMetric contagAggr = new CONTAGMetric();
        TestTools.setMetricCodes(contagAggr, NLAND, NLAND_AGGR);
        
        assertArrayEquals(result, contagAggr.calcMetric(view));
    }

    /**
     * Test of calcMetric method, of class CONTAGMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(2, 2), 2, new Bounds());
        Double[] result = new CONTAGMetric().calcMetric(view);
        assertEquals("CONTAG 10 tan", 65.23151238552525, result[0], 0.0);
        
        ViewTanResult view5 = computeAggr.calcViewTan(new DirectPosition2D(2, 2), 2, new Bounds());
        result = new CONTAGMetric().calcMetric(view5);
//        assertEquals("CONTAG 5 tan", 69.56641474615853, result[0], 0.0);
        
        CONTAGMetric contagAggr = new CONTAGMetric();
        TestTools.setMetricCodes(contagAggr, NLAND, NLAND_AGGR);
        
        assertArrayEquals(result, contagAggr.calcMetric(view));
    }
    
}
