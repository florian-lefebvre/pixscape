/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thema.pixscape.view;

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
public class SimpleViewTanResultTest {
        
    private static SimpleViewTanResult tanResult;
    
    @BeforeClass
    public static void setUpClass() {
        tanResult = (SimpleViewTanResult) new ComputeViewJava(TestTools.createRandomData(10), 0.1, false, 0).calcViewTan(new DirectPosition2D(2, 2), 2, new Bounds());
    }
    

    /**
     * Test of getMaxDistance method, of class SimpleViewTanResult.
     */
    @Test
    public void testGetMaxDistance() {
        System.out.println("getMaxDistance");
        
        for(int theta1 = 0; theta1 < tanResult.getThetaWidth(); theta1++) {
            double max = 0;
            for(int y = 0; y < tanResult.getThetaHeight(); y++) {
                double d = tanResult.getDistance(theta1, y);
                if(d > max) {
                    max = d;
                }
            }

            double result = tanResult.getMaxDistance(theta1);
            assertEquals(max, result, 0.0);
        }
    }
    
}
