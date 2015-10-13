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
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class DepthLineMetricTest {
    
    private static final int SIZE = 11;
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(SIZE, 1), 45);
    }
    
    /**
     * Test of calcMetric method, of class DepthLineMetric.
     */
    @Test
    public void testCalcMetric() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(SIZE/2, SIZE/2), 2, new Bounds());
        Double[] result = new DepthLineMetric().calcMetric(view);
        assertEquals("DL", 2/Math.sqrt(Math.PI), result[0], 0.0);
        
        view = compute.calcViewTan(new DirectPosition2D(SIZE/2, SIZE/2), 2, new Bounds().createBounds(0, 180));
        result = new DepthLineMetric().calcMetric(view);
        assertEquals("DL", 2/Math.sqrt(Math.PI), result[0], 0.0);
    }
    
}
