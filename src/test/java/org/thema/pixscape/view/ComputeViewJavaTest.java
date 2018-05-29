/*
 * Copyright (C) 2018 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
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
package org.thema.pixscape.view;

import java.awt.image.Raster;
import org.geotools.geometry.DirectPosition2D;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.TestTools;

/**
 *
 * @author gvuidel
 */
public class ComputeViewJavaTest {
    
    private static ComputeViewJava compute;
    
    @BeforeClass
    public static void setUpClass() {
        compute = new ComputeViewJava(TestTools.createFlatData(30, 20), 0.1, false, 0);
    }

    /**
     * Test of calcViewTan method, of class ComputeViewJava.
     */
    @Test
    public void testCalcViewTan() {
        System.out.println("calcViewTan");
        // check if all pixels at image border are seen and their contiguity at 360°
        ViewTanResult tanResult = compute.calcViewTan(new DirectPosition2D(10, 10), 2, new Bounds());
        Raster view = tanResult.getView();
        final int w = view.getWidth();
        final int h = view.getHeight();
        final int wData = tanResult.getGrid().getGridRange2D().width;
        int y = 0;
        while(y < h && view.getSample(0, y, 0) == -1) {
            y++;
        }
        int firstInd = view.getSample(0, y, 0);
        int precInd = view.getSample(0, y, 0);
        for(int x = 1; x < w; x++) {
            y = 0;
            while(y < h && view.getSample(x, y, 0) == -1) {
                y++;
            } 
            int ind = view.getSample(x, y, 0);
            assertTrue("Pixel border are not contiguous", Math.abs((ind%wData) - (precInd%wData)) + Math.abs(ind/wData - precInd/wData) <= 1);
            precInd = ind;
        }
        
        assertTrue("Pixel border are not contiguous", Math.abs((firstInd%wData) - (precInd%wData)) + Math.abs(firstInd/wData - precInd/wData) <= 1);
    }
    
}
