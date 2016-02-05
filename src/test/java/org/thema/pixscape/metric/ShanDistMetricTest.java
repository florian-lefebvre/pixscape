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
public class ShanDistMetricTest {
    
    private static final int SIZE = 19;
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(SIZE, 1), 0.1, false, 0);
    }
    
    /**
     * Test of calcMetric method, of class DepthLineMetric.
     */
    @Test
    public void testCalcMetric() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(SIZE/2, SIZE/2), 2, new Bounds());
        Double[] result = new ShanDistMetric().calcMetric(view);
        assertEquals("SD", 1, result[0], 1e-3);
    }

    
}
