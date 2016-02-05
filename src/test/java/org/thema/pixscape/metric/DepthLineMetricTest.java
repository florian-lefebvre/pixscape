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
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author Gilles Vuidel
 */
public class DepthLineMetricTest {
    
    private static final int SIZE = 11;
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(SIZE, 1), 45, false, 0);
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
