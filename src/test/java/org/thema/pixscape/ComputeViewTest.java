/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.metric.AreaMetric;
import org.thema.pixscape.metric.ShannonMetric;
import org.thema.pixscape.metric.ViewShedMetric;
import org.thema.pixscape.metric.ViewTanMetric;
import org.thema.pixscape.view.ComputeView;
import org.thema.pixscape.view.ComputeViewJava;
import org.thema.pixscape.view.MultiComputeViewJava;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;
import org.thema.pixscape.view.cuda.ComputeViewCUDA;
import static org.thema.pixscape.TestTools.printArray;

/**
 *
 * @author gvuidel
 */
public class ComputeViewTest {
    
    public ComputeViewTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testCalcViewShedJava() throws IOException, TransformException {
        System.out.println("calcViewShedJava static mnt only");
        testViewShed(0);
    }
    
    @Test
    public void testCalcViewShedJavaMulti() throws IOException, TransformException {
        System.out.println("calcViewShedJava static mnt only");
        testViewShed(1);
    }
    
    @Test
    public void testCalcViewShedCUDA() throws IOException, TransformException {
        if(ComputeViewCUDA.isCUDAAvailable()) {
            System.out.println("calcViewShedCUDA static mnt only");
            testViewShed(2);
        }
    }
    
    /**
     * Test of calcViewTan method, of class ComputeViewJava.
     */
    @Test
    public void testCalcViewTanJava() throws IOException, TransformException {
        testViewTan(0);
    }
    
    /**
     * Test of calcViewTan method, of class MultiComputeViewJava.
     */
    @Test
    public void testCalcViewTanMulti() throws IOException, TransformException {
        testViewTan(1);
    }
    
    /**
     * Test of calcViewTan method, of class ComputeViewCUDA.
     */
    @Test
    public void testCalcViewTanCUDA() throws IOException, TransformException {
        if(ComputeViewCUDA.isCUDAAvailable()) {
            testViewTan(2);
        }
    }
    
    /**
     * Test of calcViewShed method, of class ComputeView.
     */
    @Test
    public void testViewShedJavaCUDA() throws IOException, TransformException, TransformException {  
        if(!ComputeViewCUDA.isCUDAAvailable()) {
            return;
        }
        
        int size = 10;
        ScaleData data = TestTools.createRandomData(size);
        ComputeViewCUDA cuda = new ComputeViewCUDA(data, 0, false, 0, 1);
        ComputeViewJava java = new ComputeViewJava(data, 0, false, 0);
        double startZ = 2;
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                for(Double destZ : Arrays.asList(-1.0, 1.0, 3.0)) {
                    DirectPosition2D p = new DirectPosition2D(x, y);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " direct");
                    compComputeView(p, startZ, destZ, true, cuda, java);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " indirect");
                    compComputeView(p, startZ, destZ, false, cuda, java);
                }
            }
        }
        cuda.dispose();
        java.dispose();
    }
    
    /**
     * Test of calcViewShed method, of class MultiComputeView.
     * Randomly results are not equals...
     */
    public void testViewShedJavaMulti() throws IOException, TransformException {  
        int size = 5;
        ScaleData data = TestTools.createRandomData(size);
        MultiComputeViewJava multi = new MultiComputeViewJava(new TreeMap<>(Collections.singletonMap(data.getResolution(), data)), 100, 0, false, 0);
        ComputeViewJava java = new ComputeViewJava(data, 0, false, 0);
        double startZ = 2;
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                for(Double destZ : Arrays.asList(-1.0, 1.0, 3.0)) {
                    DirectPosition2D p = new DirectPosition2D(x, y);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " direct");
                    compComputeView(p, startZ, destZ, true, multi, java);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " indirect");
                    compComputeView(p, startZ, destZ, false, multi, java);
                }
            }
        }
        multi.dispose();
        java.dispose();
    }
    
    /**
     * Test of viewshed with earh curvature and refraction of classes ComputeViewJava and MultiComputeViewJava.
     */
    @Test
    public void testEarthCurvature() throws IOException, TransformException {  
        ComputeView compute = new ComputeViewJava(TestTools.createFlatDataWithLand(100, 1), 0.1, true, 0);
        double area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, true, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
        Assert.assertEquals(4153, area, 0); // aproximative radius = 36 = round(sqrt(0.0001 * 12 740 000))

        compute = new ComputeViewJava(TestTools.createFlatDataWithLand(100, 1), 0.1, true, 0.5);
        area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, true, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
        Assert.assertEquals(8139, area, 0); 
        area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, false, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
        Assert.assertEquals(8139, area, 0); 
        
        if(ComputeViewCUDA.isCUDAAvailable()) {
            compute = new ComputeViewCUDA(TestTools.createFlatDataWithLand(100, 1), 0.1, true, 0.5, 1);
            area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, true, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
            Assert.assertEquals(8139, area, 0); 
            area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, false, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
            Assert.assertEquals(8139, area, 0); 
        }
        
        compute = new MultiComputeViewJava(new TreeMap<>(Collections.singletonMap(1.0, TestTools.createFlatDataWithLand(100, 1))), 1000, 0.1, true, 0.5);
        area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, true, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
        Assert.assertEquals(8139, area, 0); 
        area = compute.aggrViewShed(new DirectPosition2D(50, 50), 0.0001, -1, false, new Bounds(), Arrays.asList(new AreaMetric())).get(0)[0];
        Assert.assertEquals(8139, area, 0); 
    }
    
    private void compComputeView(DirectPosition2D p, double startZ, double destZ, boolean direct, 
            ComputeView cuda, ComputeView java) throws TransformException {
        Raster resCuda = cuda.calcViewShed(p, startZ, destZ, direct, new Bounds()).getView();
        Raster resJava = java.calcViewShed(p, startZ, destZ, direct, new Bounds()).getView();
        
        System.out.println("java");
        printArray(((DataBufferByte)resJava.getDataBuffer()).getData());
        System.out.println("CUDA");
        printArray(((DataBufferByte)resCuda.getDataBuffer()).getData());
                
        Assert.assertArrayEquals(((DataBufferByte)resCuda.getDataBuffer()).getData(), ((DataBufferByte)resJava.getDataBuffer()).getData());
        
        AreaMetric sum = new AreaMetric();
        Assert.assertEquals(java.aggrViewShed(p, startZ, destZ, direct, new Bounds(), Arrays.asList(sum)).get(0)[0], 
                cuda.aggrViewShed(p, startZ, destZ, direct, new Bounds(), Arrays.asList(sum)).get(0)[0]);
        
        List<AreaMetric> metrics = Arrays.asList(new AreaMetric(1), new AreaMetric(2), new AreaMetric(3), new AreaMetric(4), new AreaMetric(5));
        Assert.assertArrayEquals(java.aggrViewShed(p, startZ, destZ, direct, new Bounds(), metrics).toArray(), 
                cuda.aggrViewShed(p, startZ, destZ, direct, new Bounds(), metrics).toArray());
        
        ShannonMetric sh = new ShannonMetric();
        Assert.assertEquals(java.aggrViewShed(p, startZ, destZ, direct, new Bounds(), Arrays.asList(sh)).get(0)[0], 
                cuda.aggrViewShed(p, startZ, destZ, direct, new Bounds(), Arrays.asList(sh)).get(0)[0]);
    }
    

    private void testViewShed(int type) throws IOException, TransformException {
        ComputeView compute = null;
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 5, 5, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, 5, 5));
        try (BufferedReader r = new BufferedReader(new FileReader(new File("target/test-classes/test-viewshed.txt")))) {
            String line = r.readLine();
            while(line != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue;
                }
                // read mnt
                if(line.startsWith("--")) {
                    if(compute != null) {
                        compute.dispose();
                    }
                    System.out.println("Load mnt");
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++) {
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                        }
                    }
                    ScaleData data = new ScaleData(mntCov, null, null, 1);
                    compute = type == 2 ? new ComputeViewCUDA(data, 0, false, 0, 1) : (
                            type == 1 ? new MultiComputeViewJava(new TreeMap<>(Collections.singletonMap(data.getResolution(), data)), 100, 0, false, 0)
                            : new ComputeViewJava(data, 0, false, 0));
                } else {
                    System.out.println("Test with : " + line);
                    String[] tokens = line.split(" ");
                    GridCoordinates2D c = new GridCoordinates2D(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                    DirectPosition2D p = (DirectPosition2D) mntCov.getGridGeometry().gridToWorld(c);
                    double startZ = Double.parseDouble(tokens[2]);
                    double destZ = Double.parseDouble(tokens[3]);
                    boolean direct = true;
                    if(!tokens[4].equals("?")) {
                        direct = Boolean.parseBoolean(tokens[4]);
                    }
                    Bounds bounds = new Bounds();
                    if(tokens.length > 5) {
                        bounds = new Bounds(Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6]),
                                Double.parseDouble(tokens[7]), Double.parseDouble(tokens[8]),
                                Double.parseDouble(tokens[9]), Double.parseDouble(tokens[10]));
                    }           
                    byte [] test = new byte[25];
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++) {
                            test[i*5+j] = Byte.parseByte(values[j]);
                        }
                    }
                    ViewShedResult viewshedResult = compute.calcViewShed(p, startZ, destZ, direct, bounds); 
                    Raster result = viewshedResult.getView();
                    printArray(((DataBufferByte)result.getDataBuffer()).getData());
                    Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                    if(destZ == -1) {
                        result = compute.calcViewShed(p, startZ, 0, direct, bounds).getView(); 
                        Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                    }
                    if(tokens[4].equals("?")) {
                        System.out.println("Test indirect");
                        direct = false;
                        result = compute.calcViewShed(p, startZ, destZ, direct, bounds).getView();  
                        Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                        if(destZ == -1) {
                            result = compute.calcViewShed(p, startZ, 0, direct, bounds).getView();  
                            Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                        }
                    }
                    //test metrics
                    line = r.readLine();
                    while(line != null && !line.trim().isEmpty()) {
                        tokens = line.split("=");
                        double res = ((ViewShedMetric)Project.getMetric(tokens[0])).calcMetric(viewshedResult)[0];
                        Assert.assertEquals(tokens[0], Double.parseDouble(tokens[1]), res, 1e-10);
                        line = r.readLine();
                    }
                }
                
                line = r.readLine();
            }
        }
        if(compute != null) {
            compute.dispose();
        }
    }
    
    private void testViewTan(int type) throws IOException, TransformException {
        ComputeView compute = null;
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 5, 5, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, 5, 5));
        try (BufferedReader r = new BufferedReader(new FileReader(new File("target/test-classes/test-viewtan.txt")))) {
            String line = r.readLine();
            while(line != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue;
                }
                // read mnt
                if(line.startsWith("--")) {
                    if(compute != null) {
                        compute.dispose();
                    }
                    System.out.println("Load mnt");
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++) {
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                        }
                    }
                    
                    ScaleData data = new ScaleData(mntCov, null, null, 1);
                    compute = type == 2 ? new ComputeViewCUDA(data, 0, false, 0, 1) 
                            : type == 1 ? new MultiComputeViewJava(new TreeMap<>(Collections.singletonMap(data.getResolution(), data)), 100, 0, false, 0) : new ComputeViewJava(data, 0, false, 0);
                } else {
                    System.out.println("Test with : " + line);
                    String[] tokens = line.split(" ");
                    GridCoordinates2D c = new GridCoordinates2D(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                    DirectPosition2D p = (DirectPosition2D) mntCov.getGridGeometry().gridToWorld(c);
                    double startZ = Double.parseDouble(tokens[2]);
                    double ares = Double.parseDouble(tokens[3]);
                    Bounds bounds = new Bounds();
                    if(tokens.length > 4) {
                        bounds = new Bounds(Double.parseDouble(tokens[4]), Double.parseDouble(tokens[5]),
                                Double.parseDouble(tokens[6]), Double.parseDouble(tokens[7]),
                                Double.parseDouble(tokens[8]), Double.parseDouble(tokens[9]));
                    }           
                    int n = (int)(360/ares);
                    int [] test = new int[n*n/2];
                    for(int i = 0; i < n/2; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < n; j++) {
                            test[i*n+j] = Integer.parseInt(values[j]);
                        }
                    }
                    compute.setaPrec(ares);
                    ViewTanResult viewtanResult = compute.calcViewTan(p, startZ, bounds);    
                    Raster result = viewtanResult.getView();
                    printArray(((DataBufferInt)result.getDataBuffer()).getData(), n, n/2);
                    Assert.assertArrayEquals(test, ((DataBufferInt)result.getDataBuffer()).getData());
                    
                    //test metrics
                    line = r.readLine();
                    while(line != null && !line.trim().isEmpty()) {
                        tokens = line.split("=");
                        double res = ((ViewTanMetric)Project.getMetric(tokens[0])).calcMetric(viewtanResult)[0];
                        Assert.assertEquals(tokens[0], Double.parseDouble(tokens[1]), res, 1e-10);
                        line = r.readLine();
                    }
                }
                
                line = r.readLine();
            }
        }
        if(compute != null) {
            compute.dispose();
        }
    }

}
