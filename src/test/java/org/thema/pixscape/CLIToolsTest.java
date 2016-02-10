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

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.junit.Assert;
import org.junit.Test;
import org.thema.data.IOImage;
import static org.thema.pixscape.TestTools.printArray;
import org.thema.pixscape.view.cuda.ComputeViewCUDA;

/**
 *
 * @author Gilles Vuidel
 */
public class CLIToolsTest {
    
    /**
     * Test of execute method, of class CLITools.
     */
    @Test
    public void testViewshedJava() throws Exception {
        testViewshed(Arrays.asList("-proc", "1"), Collections.EMPTY_LIST);
    }
    
    @Test
    public void testViewshedMulti() throws Exception {
        testViewshed(Collections.EMPTY_LIST, Arrays.asList("-multi", "dmin=100"));
    }
    
    @Test
    public void testViewshedCUDA() throws Exception {
        if(ComputeViewCUDA.isCUDAAvailable()) {
            testViewshed(Arrays.asList("-cuda", "1"), Collections.EMPTY_LIST);
        }
    }
    
    private void testViewshed(List<String> opt1, List<String> opt2) throws Exception {
        Project project = null;
        int size = 5;
        
        try (BufferedReader r = new BufferedReader(new FileReader(new File("target/test-classes/test-viewshed-cli.txt")))) {
            String line = r.readLine();
            while(line != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue;
                }
                // read mnt
                if(line.startsWith("--")) {
                    if(project != null) {
                        project.close();
                    }
                    System.out.println("Load mnt");
                    size = Integer.parseInt(r.readLine().trim());
                    WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
                    GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
                    for(int i = 0; i < size; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < size; j++) {
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                        }
                    }
                    ScaleData data = new ScaleData(mntCov, null, null, 1);
                    project = new Project("test", Files.createTempDirectory("pixscape").toFile(), data);
                } else {
                    System.out.println("Test command line : " + line);
                    String[] tokens = line.split("\\s+");
    
                    byte [] test = new byte[size*size];
                    for(int i = 0; i < size; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < size; j++) {
                            test[i*size+j] = Byte.parseByte(values[j]);
                        }
                    }
                    ArrayList<String> params = new ArrayList<>(opt1);
                    params.addAll(Arrays.asList("--project", project.getProjectFile().getAbsolutePath()));
                    params.addAll(opt2);
                    params.addAll(Arrays.asList(tokens));
                    params.add("resfile=viewshed.tif");
                    new CLITools().execute(params.toArray(new String[0]));
                    
                    // compare
                    byte[] result = ((DataBufferByte)IOImage.loadTiff(new File(project.getDirectory(), "viewshed.tif")).getRenderedImage().getData().getDataBuffer()).getData();

                    Assert.assertArrayEquals(test, result);
                }
                
                line = r.readLine();
            }
        }

    }
    
    @Test
    public void testViewtanJava() throws Exception {
        testViewtan(Arrays.asList("-proc", "1"), Collections.EMPTY_LIST);
    }
    
    @Test
    public void testViewtanMulti() throws Exception {
        testViewtan(Collections.EMPTY_LIST, Arrays.asList("-multi", "dmin=100"));
    }
    
    @Test
    public void testViewtanCUDA() throws Exception {
        if(ComputeViewCUDA.isCUDAAvailable()) {
            testViewtan(Arrays.asList("-cuda", "1"), Collections.EMPTY_LIST);
        }
    }
    
    private void testViewtan(List<String> opt1, List<String> opt2) throws Exception {
        Project project = null;
        int size = 5;
        
        try (BufferedReader r = new BufferedReader(new FileReader(new File("target/test-classes/test-viewtan-cli.txt")))) {
            String line = r.readLine();
            while(line != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue;
                }
                // read mnt
                if(line.startsWith("--")) {
                    if(project != null) {
                        project.close();
                    }
                    System.out.println("Load mnt");
                    size = Integer.parseInt(r.readLine().trim());
                    WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
                    GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
                    for(int i = 0; i < size; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < size; j++) {
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                        }
                    }
                    ScaleData data = new ScaleData(mntCov, null, null, 1);
                    project = new Project("test", Files.createTempDirectory("pixscape").toFile(), data);
                } else {
                    System.out.println("Test command line : " + line);
                    String[] tokens = line.split("\\s+");
                    String[] sizes = r.readLine().trim().split("\\s+");
                    int w = Integer.parseInt(sizes[0]);
                    int h = Integer.parseInt(sizes[1]);
                    float [] dist = new float[w*h];
                    for(int i = 0; i < h; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < w; j++) {
                            dist[i*w+j] = Float.parseFloat(values[j]);
                        }
                    }
                    float [] elev = new float[w*h];
                    for(int i = 0; i < h; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < w; j++) {
                            elev[i*w+j] = Float.parseFloat(values[j]);
                        }
                    }
                    ArrayList<String> params = new ArrayList<>(opt1);
                    params.addAll(Arrays.asList("--project", project.getProjectFile().getAbsolutePath()));
                    params.addAll(opt2);
                    params.addAll(Arrays.asList(tokens));
                    params.add("resname=viewtan");
                    new CLITools().execute(params.toArray(new String[0]));
                    
                    // compare
                    float[] result = ((DataBufferFloat)IOImage.loadTiff(new File(project.getDirectory(), "viewtan-dist.tif")).getRenderedImage().getData().getDataBuffer()).getData();
                    printArray(result, w, h);
                    Assert.assertArrayEquals(dist, result, 0);
                    
                    result = ((DataBufferFloat)IOImage.loadTiff(new File(project.getDirectory(), "viewtan-elev.tif")).getRenderedImage().getData().getDataBuffer()).getData();
                    printArray(result, w, h);
                    Assert.assertArrayEquals(elev, result, 0);
                }
                
                line = r.readLine();
            }
        }

    }
    
    @Test
    public void testMetricJava() throws Exception {
        testMetric(Arrays.asList("-proc", "2"), Collections.EMPTY_LIST);
    }
    
    @Test
    public void testMetricCUDA() throws Exception {
        if(ComputeViewCUDA.isCUDAAvailable()) {
            testMetric(Arrays.asList("-cuda", "1"), Collections.EMPTY_LIST);
        }
    }
    
    @Test
    public void testMetricMulti() throws Exception {
        testMetric(Collections.EMPTY_LIST, Arrays.asList("-multi", "dmin=100"));
    }
    
    private void testMetric(List<String> opt1, List<String> opt2) throws Exception {
        Project project = null;
        int size = 5;
        
        try (BufferedReader r = new BufferedReader(new FileReader(new File("target/test-classes/test-metric-cli.txt")))) {
            String line = r.readLine();
            while(line != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue;
                }
                // read mnt
                if(line.startsWith("--")) {
                    if(project != null) {
                        project.close();
                    }
                    System.out.println("Load mnt");
                    size = Integer.parseInt(r.readLine().trim());
                    WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
                    GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
                    for(int i = 0; i < size; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < size; j++) {
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                        }
                    }
                    ScaleData data = new ScaleData(mntCov, null, null, 1);
                    project = new Project("test", Files.createTempDirectory("pixscape").toFile(), data);
                } else {
                    System.out.println("Test command line : " + line);
                    String[] tokens = line.split("\\s+");
                    
                    ArrayList<String> params = new ArrayList<>(opt1);
                    params.addAll(Arrays.asList("--project", project.getProjectFile().getAbsolutePath()));
                    params.addAll(opt2);
                    params.addAll(Arrays.asList(tokens));
                    new CLITools().execute(params.toArray(new String[0]));
                    
                    String resultFile = r.readLine();
                    while(resultFile != null && !resultFile.trim().isEmpty()) {
                        float [] test = new float[size*size];
                        for(int i = 0; i < size; i++) {
                            line = r.readLine().trim();
                            String[] values = line.split(" ");
                            for(int j = 0; j < size; j++) {
                                test[i*size+j] = Float.parseFloat(values[j]);
                            }
                        }
                        // compare
                        float[] result = ((DataBufferFloat)IOImage.loadTiff(new File(project.getDirectory(), resultFile)).getRenderedImage().getData().getDataBuffer()).getData();

                        Assert.assertArrayEquals(test, result, 0);
                        
                        resultFile = r.readLine();
                    }
                }
                
                line = r.readLine();
            }
        }

    }
    
}
