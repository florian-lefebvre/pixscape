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

package org.thema.pixscape.view;

import org.geotools.geometry.DirectPosition2D;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.TestTools;

/**
 *
 * @author Gilles Vuidel
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
