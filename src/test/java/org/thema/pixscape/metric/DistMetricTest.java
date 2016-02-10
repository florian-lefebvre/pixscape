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

import org.geotools.geometry.DirectPosition2D;
import org.junit.AfterClass;
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
public class DistMetricTest {
   
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(10, 1), 1, false, 0); 
    }

    /**
     * Test of calcMetric method, of class DistMetric.
     */
    @Test
    public void testCalcMetric_ViewShedResult() {
        ViewShedResult view = compute.calcViewShed(new DirectPosition2D(5, 5), 2, 0, true, new Bounds());
        DistMetric metric = new DistMetric();
        Double[] result = metric.calcMetric(view);
        assertArrayEquals(new Double[]{100.0, 385.3057600688064, 3.8530576006880644, 0.0, 7.0710678118654755}, result);
    }

    /**
     * Test of calcMetric method, of class DistMetric.
     */
    @Test
    public void testCalcMetric_ViewTanResult() {
        ViewTanResult view = compute.calcViewTan(new DirectPosition2D(5, 5), 0, new Bounds());
        DistMetric metric = new DistMetric();
        Double[] result = metric.calcMetric(view);
        assertArrayEquals(new Double[]{32400.0, 0.0, 0.0, 0.0, 0.0}, result);
        
        view = compute.calcViewTan(new DirectPosition2D(5, 5), 2, new Bounds());
        result = metric.calcMetric(view);
        assertArrayEquals(new Double[]{25401.0, 45679.61848164024, 1.798339375679707, 0.0, 7.0710678118654755}, result);
    }
    
}
