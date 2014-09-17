/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import org.thema.pixscape.ComputeViewJava;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ComputeView;
import org.thema.pixscape.ComputeViewCUDA;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.TreeSet;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void testCalcViewShedJava() throws IOException {
        System.out.println("calcViewShedJava static mnt only");
        testViewShed(false);
    }
    
    @Test
    public void testCalcViewShedCUDA() throws IOException {
        System.out.println("calcViewShedCUDA static mnt only");
        testViewShed(true);
    }
    
    /**
     * Test of calcViewTan method, of class ComputeViewJava.
     */
    @Test
    public void testCalcViewTanJava() throws IOException {
        testViewTan(false);
    }
    
    /**
     * Test of calcViewTan method, of class ComputeViewCUDA.
     */
    @Test
    public void testCalcViewTanCUDA() throws IOException {
        testViewTan(true);
    }
    
    /**
     * Test of calcViewShed method, of class ComputeView.
     */
    @Test
    public void testCalcViewShed() throws IOException {   
        int size = 5;
        System.out.println("calcViewShed random mnt + mne");
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, size, size, 1), null);
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                mnt.setSample(x, y, 0, (float)(Math.random()*5));
                mne.setSample(x, y, 0, (float)(Math.random()*2));
                land.setSample(x, y, 0, (int)(Math.random()*5+1));
            }
        }
        System.out.println("mnt");
        printArray(((DataBufferFloat)mnt.getDataBuffer()).getData());
        System.out.println("mne");
        printArray(((DataBufferFloat)mne.getDataBuffer()).getData());
        ComputeViewCUDA cuda = new ComputeViewCUDA(mnt, 1, 1, land, mne, 1);
        ComputeViewJava java = new ComputeViewJava(mnt, 1, 1, land, mne);
        double startZ = 2;
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                for(Double destZ : Arrays.asList(-1.0, 1.0, 3.0)) {
                    GridCoordinates2D p = new GridCoordinates2D(x, y);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " direct");
                    compJavaCUDA(p, startZ, destZ, true, cuda, java);
                    System.out.println("x : " + x + ", y : " + y +  ", destZ : " + destZ + " indirect");
                    compJavaCUDA(p, startZ, destZ, false, cuda, java);
                }
            }
        }
        cuda.dispose();
        java.dispose();
    }
    
    private void compJavaCUDA(GridCoordinates2D p, double startZ, double destZ, boolean direct, 
            ComputeViewCUDA cuda, ComputeViewJava java) {
        WritableRaster resCuda = cuda.calcViewShed(p, startZ, destZ, direct, new Bounds());
        WritableRaster resJava = java.calcViewShed(p, startZ, destZ, direct, new Bounds());
        
        TreeSet<Integer> to = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
                System.out.println("java");
                printArray(((DataBufferByte)resJava.getDataBuffer()).getData());
                System.out.println("CUDA");
                printArray(((DataBufferByte)resCuda.getDataBuffer()).getData());
        Assert.assertArrayEquals(((DataBufferByte)resCuda.getDataBuffer()).getData(), ((DataBufferByte)resJava.getDataBuffer()).getData());
        
        Assert.assertEquals(java.aggrViewShed(p, startZ, startZ, true, new Bounds()), cuda.aggrViewShed(p, startZ, startZ, true, new Bounds()), 0);
        
        Assert.assertArrayEquals(java.aggrViewShedLand(p, startZ, startZ, true, new Bounds(), to), 
                cuda.aggrViewShedLand(p, startZ, startZ, true, new Bounds(), to), 0);
    }
    
    private void testViewShed(boolean cuda) throws IOException {
        ComputeView compute = null;
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 5, 5, 1), null);
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
                    if(compute != null)
                        compute.dispose();
                    System.out.println("Load mnt");
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++)
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                    }
                    compute = cuda ? new ComputeViewCUDA(mnt, 1, 1, null, null, 1) : new ComputeViewJava(mnt, 1, 1, null, null);
                } else {
                    System.out.println("Test with : " + line);
                    String[] tokens = line.split(" ");
                    GridCoordinates2D p = new GridCoordinates2D(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                    double startZ = Double.parseDouble(tokens[2]);
                    double destZ = Double.parseDouble(tokens[3]);
                    boolean direct = true;
                    if(!tokens[4].equals("?"))
                        direct = Boolean.parseBoolean(tokens[4]);
                    Bounds bounds = new Bounds();
                    if(tokens.length > 5)
                        bounds = new Bounds(Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6]), 
                                Double.parseDouble(tokens[7]), Double.parseDouble(tokens[8]), 
                                Double.parseDouble(tokens[9]), Double.parseDouble(tokens[10]));           
                    byte [] test = new byte[25];
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++)
                            test[i*5+j] = Byte.parseByte(values[j]);
                    }
                    WritableRaster result = compute.calcViewShed(p, startZ, destZ, direct, bounds);    
                    Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                    if(destZ == -1) {
                        result = compute.calcViewShed(p, startZ, 0, direct, bounds); 
                        Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                    }
                    if(tokens[4].equals("?")) {
                        System.out.println("Test indirect");
                        direct = false;
                        result = compute.calcViewShed(p, startZ, destZ, direct, bounds);  
                        Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                        if(destZ == -1) {
                            result = compute.calcViewShed(p, startZ, 0, direct, bounds);  
                            Assert.assertArrayEquals(test, ((DataBufferByte)result.getDataBuffer()).getData());
                        }
                    }
                }
                
                line = r.readLine();
            }
        }
        if(compute != null)
            compute.dispose();
    }
    
    private void testViewTan(boolean cuda) throws IOException {
        ComputeView compute = null;
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, 5, 5, 1), null);
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
                    if(compute != null)
                        compute.dispose();
                    System.out.println("Load mnt");
                    for(int i = 0; i < 5; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < 5; j++)
                            mnt.setSample(j, i, 0, Double.parseDouble(values[j]));
                    }
                    compute = cuda ? new ComputeViewCUDA(mnt, 1, 1, null, null, 1) : new ComputeViewJava(mnt, 1, 1, null, null);
                } else {
                    System.out.println("Test with : " + line);
                    String[] tokens = line.split(" ");
                    GridCoordinates2D p = new GridCoordinates2D(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                    double startZ = Double.parseDouble(tokens[2]);
                    double ares = Double.parseDouble(tokens[3]);
                    Bounds bounds = new Bounds();
                    if(tokens.length > 4)
                        bounds = new Bounds(Double.parseDouble(tokens[4]), Double.parseDouble(tokens[5]), 
                                Double.parseDouble(tokens[6]), Double.parseDouble(tokens[7]), 
                                Double.parseDouble(tokens[8]), Double.parseDouble(tokens[9]));           
                    int n = (int)(360/ares);
                    int [] test = new int[n*n/2];
                    for(int i = 0; i < n/2; i++) {
                        line = r.readLine().trim();
                        String[] values = line.split(" ");
                        for(int j = 0; j < n; j++)
                            test[i*n+j] = Integer.parseInt(values[j]);
                    }
                    WritableRaster result = compute.calcViewTan(p, startZ, ares*Math.PI/180, bounds);    
                    printArray(((DataBufferInt)result.getDataBuffer()).getData(), n, n/2);
                    Assert.assertArrayEquals(test, ((DataBufferInt)result.getDataBuffer()).getData());
                    
                }
                
                line = r.readLine();
            }
        }
        if(compute != null)
            compute.dispose();
    }

    public static void printArray(Object array) {
        printArray(array, 5, 5);
    }
    
    public static void printArray(Object array, int w, int h) {
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                System.out.print(Array.get(array, i*w+j) + " ");
            }
            System.out.println();
        }
    }
    
    
}
