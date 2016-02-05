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

package org.thema.pixscape;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.thema.pixscape.metric.AreaMetric;
import org.thema.pixscape.metric.Metric;

/**
 *
 * @author Gilles Vuidel
 */
public class ProjectTest {
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test of getMetric method, of class Project.
     */
    @Test
    public void testGetMetric() {
        Metric A = Project.getMetric("A");
        assertEquals("A", A.getShortName());
        
        thrown.expect(IllegalArgumentException.class);
        Project.getMetric("X");
        
    }

    /**
     * Test of getMetricWithParams method, of class Project.
     */
    @Test
    public void testGetMetricWithParams() {
        Metric m = Project.getMetricWithParams("A");
        assertEquals("A", m.getShortName());

        AreaMetric A = (AreaMetric) Project.getMetricWithParams("A[1]");
        assertEquals(Collections.singleton(1), A.getCodes());
        
        A = (AreaMetric) Project.getMetricWithParams("A[1,5]");
        assertEquals(new TreeSet<>(Arrays.asList(1, 5)), A.getCodes());
        
        A = (AreaMetric) Project.getMetricWithParams("A[1-5,2]");
        assertEquals(new TreeSet<>(Arrays.asList(1, 2, 5)), A.getCodes());
        assertEquals(Arrays.asList(1, 5), A.getCodeGroups().get(0));
        assertEquals(Arrays.asList(2), A.getCodeGroups().get(1));
        
        A = (AreaMetric) Project.getMetricWithParams("A_0,10,+Infinity");
        assertEquals(new TreeSet<>(Arrays.asList(0.0, 10.0, Double.POSITIVE_INFINITY)), A.getDistances());
        
        A = (AreaMetric) Project.getMetricWithParams("A[1]_0,10,100");
        assertEquals(Collections.singleton(1), A.getCodes());
        assertEquals(new TreeSet<>(Arrays.asList(0.0, 10.0, 100.0)), A.getDistances());
        
    }
    
}
