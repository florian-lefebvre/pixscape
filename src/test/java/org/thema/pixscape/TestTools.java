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
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.thema.common.RasterImage;
import org.thema.pixscape.metric.Metric;

/**
 *
 * @author Gilles Vuidel
 */
public class TestTools {
    
    
    public static void setMetricCodes(Metric metric, int nLand, int nLandGroup) {
        int land = 0;
        for(int i = 0; i < nLandGroup; i++) {
            List<Integer> codes = new ArrayList<>();
            for(int j = 0; j < nLand/nLandGroup; j++) {
                codes.add(land++);
            }
            metric.addCodes(new HashSet<>(codes));
        }
    }
    
    public static ScaleData createFlatDataWith1CentreLand(int size) {
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, size, size, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
        land.setSample(5, 5, 0, 1);
        
        return new ScaleData(mntCov, new RasterImage(land), new RasterImage(mne), 1);
    }
    
    public static ScaleData createFlatDataWithLand(int size, int nLand) {
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, size, size, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                land.setSample(x, y, 0, y*nLand/size);
            }
        }
        return new ScaleData(mntCov, new RasterImage(land), new RasterImage(mne), 1);
    }
    
    public static ScaleData createFlatData(int w, int h) {
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, w, h));
        return new ScaleData(mntCov, null, new RasterImage(mne), 1);
    }
    
    public static ScaleData createFlatDataWithDsm(int w, int h, int dsmHeight) {
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, w, h));
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                mne.setSample(x, y, 0, dsmHeight);
            }
        }
        return new ScaleData(mntCov, null, new RasterImage(mne), 1);
    }
    
    public static ScaleData createRandomData(int size) {
        WritableRaster mnt = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster mne = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_FLOAT, size, size, 1), null);
        WritableRaster land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_BYTE, size, size, 1), null);
        GridCoverage2D mntCov = new GridCoverageFactory().create("", mnt, new Envelope2D(null, 0, 0, size, size));
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
        return new ScaleData(mntCov, new RasterImage(land), new RasterImage(mne), 1);
    }
    
    
    public static void printArray(Object array) {
        int size = (int) Math.sqrt(Array.getLength(array));
        printArray(array, size, size);
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
