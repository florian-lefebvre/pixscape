/*
 * Copyright (C) 2019 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
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
package org.thema.pixscape;

import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import org.geotools.geometry.DirectPosition2D;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.thema.common.swing.TaskMonitor;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.parallel.ExecutorService;
import org.thema.pixscape.MultiViewshedTask.RasterValue;
import org.thema.pixscape.view.ViewShedResult;

/**
 *
 * @author gvuidel
 */
public class MultiViewshedTaskTest {
    
    private static Project project;
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        project = new Project("MultiViewshedTaskTest", Files.createTempDirectory("pixscape").toFile(), TestTools.createFlatData(50, 50));
    }
   

    /**
     * Test of getResult method, of class MultiViewshedTask.
     */
    @Test
    public void testGetResult() {
        System.out.println("getResult");
        
        Feature f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)));
        MultiViewshedTask task = new MultiViewshedTask(Collections.singletonList(f), project, false, -1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        ViewShedResult resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), project.getStartZ(), -1, false, new Bounds());
        assertEquals(resView.getArea(), 50*50, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(1));
        task = new MultiViewshedTask(Collections.singletonList(f), project, false, -1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), 1, -1, false, new Bounds());
        assertEquals(resView.getArea(), 50*50, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(0));
        task = new MultiViewshedTask(Collections.singletonList(f), project, false, -1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), 0, -1, false, new Bounds());
        assertEquals(resView.getArea(), 4, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(0));
        task = new MultiViewshedTask(Collections.singletonList(f), project, false, 1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), 0, 1, false, new Bounds());
        assertEquals(resView.getArea(), 50*50, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(-1.5));
        task = new MultiViewshedTask(Collections.singletonList(f), project, false, 5, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), -1.5, 5, false, new Bounds());
        assertEquals(resView.getArea(), 27, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(-1.5));
        task = new MultiViewshedTask(Collections.singletonList(f), project, true, 1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.execute(task);
        resView = project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), project.getStartZ(), -1.5, true, new Bounds());
        assertEquals(resView.getArea(), 0, 0);
        cmpRaster(resView.getView(), (Raster)task.getResult(), 1);
        
        
        f = new DefaultFeature("p", new GeometryFactory().createPoint(new Coordinate(0, 0)), Arrays.asList("height"), Arrays.asList(1));
        task = new MultiViewshedTask(Collections.nCopies(300, f), project, false, -1, new Bounds(), false, RasterValue.COUNT, new TaskMonitor.EmptyMonitor());
        ExecutorService.executeSequential(task);
        cmpRaster(project.getDefaultComputeView().calcViewShed(new DirectPosition2D(0, 0), 1, -1, false, new Bounds()).getView(),
                (Raster)task.getResult(), 300);
        
    }
    
    private void cmpRaster(Raster r1, Raster r2, int n) {
        int w = r1.getWidth();
        int h = r1.getHeight();
        assertEquals(w, r2.getWidth());
        assertEquals(h, r2.getHeight());
        
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                assertEquals(n*r1.getSample(j, i, 0), r2.getSample(j, i, 0));
            }
        }
    }
    
}
