/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author gvuidel
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
