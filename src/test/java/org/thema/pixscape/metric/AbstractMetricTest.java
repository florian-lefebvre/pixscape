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

import java.util.Arrays;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author gvuidel
 */
public class AbstractMetricTest {
    
    private AbstractMetric metric, metricCode;
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Before
    public void setUpTest() {
        metric = new AbstractMetric(false) {
            @Override
            public String getShortName() {
                return "TEST";
            }
        };
        
        metricCode = new AbstractMetric(true) {
            @Override
            public String getShortName() {
                return "TEST";
            }
        };
        
    }
    /**
     * Test of isCodeSupported method, of class AbstractMetric.
     */
    @Test
    public void testIsCodeSupported() {
        assertTrue(metricCode.isCodeSupported());
        assertFalse(metric.isCodeSupported());
    }

    /**
     * Test of addCode method, of class AbstractMetric.
     */
    @Test
    public void testAddCode() {
        metricCode.addCode(0);
        assertEquals(1, metricCode.getCodes().size());
        assertEquals(0, (int)metricCode.getCodes().first());
    }

    /**
     * Test of addCodes method, of class AbstractMetric.
     */
    @Test
    public void testAddCodes() {
        metricCode.addCodes(new TreeSet<>(Arrays.asList(1, 2, 3)));
        assertEquals(new TreeSet<>(Arrays.asList(1, 2, 3)), metricCode.getCodes());
    }

    /**
     * Test of getCodes method, of class AbstractMetric.
     */
    @Test
    public void testGetCodes() {
        assertTrue(metricCode.getCodes().isEmpty());
        
        thrown.expect(IllegalStateException.class);
        metric.getCodes();
    }

    /**
     * Test of hasCodeGroup method, of class AbstractMetric.
     */
    @Test
    public void testHasCodeGroup() {
        assertFalse(metricCode.hasCodeGroup());
        
        thrown.expect(IllegalStateException.class);
        metric.hasCodeGroup();
    }

    /**
     * Test of getCodeGroups method, of class AbstractMetric.
     */
    @Test
    public void testGetCodeGroups() {
        assertTrue(metricCode.getCodeGroups().isEmpty());
        metricCode.addCode(0);
        assertEquals(1, metricCode.getCodeGroups().size());
        metricCode.addCodes(new TreeSet<>(Arrays.asList(1, 2, 3)));
        assertEquals(2, metricCode.getCodeGroups().size());
        
        thrown.expect(IllegalStateException.class);
        metric.getCodeGroups();
    }

    /**
     * Test of getName method, of class AbstractMetric.
     */
    @Test
    public void testGetName() {
        assertEquals(metric.getShortName(), metric.getName());
        assertEquals(metricCode.getShortName(), metricCode.getName());
        metricCode.addCode(0);
        assertEquals(metricCode.getShortName()+"0", metricCode.getName());
        metricCode.addCodes(new TreeSet<>(Arrays.asList(1, 2, 3)));
        assertEquals(metricCode.getShortName()+"0,1-2-3", metricCode.getName());
    }

    /**
     * Test of toString method, of class AbstractMetric.
     */
    @Test
    public void testToString() {
        assertEquals(metric.getName(), metric.toString());
    }

    /**
     * Test of getResultNames method, of class AbstractMetric.
     */
    @Test
    public void testGetResultNames() {
        assertEquals(metric.getCodeName(), metric.getResultNames()[0]);
    }
    
}
