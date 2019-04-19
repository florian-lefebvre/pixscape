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
package org.thema.pixscape;

import java.awt.image.BandedSampleModel;
import org.locationtech.jts.geom.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.DirectPosition2D;
import org.thema.common.ProgressBar;
import org.thema.data.IOImage;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.parallel.AbstractParallelTask;
import org.thema.pixscape.view.ViewShedResult;

/**
 *
 * @author gvuidel
 */
public class MultiViewshedTask extends AbstractParallelTask<Object, Object> {
    
    private Project project;
    
    private List<Feature> points;
    
    /** Is from observer eye or to ? */
    private boolean inverse;
    /** The height of the observed points, -1 if not used */
    private double zDest;
    /** 
     * The default 3D limits of the sight. This limits are overriden by shapefile attributes if present
     */
    private Bounds bounds;
    
    /** Is vector output or raster ? */
    private boolean vectorOutput;
    /** for raster output, is degree or count ? */
    private boolean degree;
    
    /** Results */
    private List<DefaultFeature> viewSheds;
    private WritableRaster viewshedRast;

    public MultiViewshedTask(List<Feature> points, Project project, boolean inverse, double zDest, Bounds bounds, boolean vectorOutput, boolean degree, ProgressBar monitor) {
        super(monitor);
        this.points = points;
        this.project = project;
        this.inverse = inverse;
        this.zDest = zDest;
        this.bounds = bounds;
        this.vectorOutput = vectorOutput;
        this.degree = degree;
    }
    
    
    @Override
    public void init() {
        super.init();
        viewSheds = vectorOutput ? new ArrayList<DefaultFeature>() : null;
        viewshedRast = !vectorOutput ? Raster.createWritableRaster(new BandedSampleModel(degree ? DataBuffer.TYPE_DOUBLE : DataBuffer.TYPE_INT, project.getDtm().getWidth(), project.getDtm().getHeight(), 1), null) : null;
    }


    @Override
    public Object getResult() {
        return vectorOutput ? viewSheds : viewshedRast;
    }

    @Override
    public Object execute(int start, int end) {
        List<DefaultFeature> viewsheds = new ArrayList<>();
        WritableRaster sumViewshed = null;
        if(!vectorOutput) {
            int type;
            if(degree) {
                type = DataBuffer.TYPE_DOUBLE;
            } else if(end-start < 255) {
                type = DataBuffer.TYPE_BYTE;
            } else if(end-start < 65535) {
                type = DataBuffer.TYPE_USHORT;
            } else {
                type = DataBuffer.TYPE_INT;
            }
            sumViewshed = Raster.createWritableRaster(new BandedSampleModel(type, project.getDtm().getWidth(), project.getDtm().getHeight(), 1), null);
        }
        for(Feature point : points.subList(start, end)) {
            Point p = point.getGeometry().getCentroid();
            Bounds b = bounds.updateBounds(point);
            double zOrig = project.getStartZ();
            double zDest = this.zDest;
            if(point.getAttributeNames().contains("height")) {
                double h = ((Number)point.getAttribute("height")).doubleValue();
                if(inverse) {
                    zDest = h;
                } else {
                    zOrig = h;
                }
            }
            ViewShedResult viewshed = degree ? 
                    project.getDefaultComputeView().calcViewShedDeg(new DirectPosition2D(p.getX(), p.getY()), zOrig, zDest, inverse, b) :
                    project.getDefaultComputeView().calcViewShed(new DirectPosition2D(p.getX(), p.getY()), zOrig, zDest, inverse, b);
            if(vectorOutput) {
                viewsheds.add(b.createFeatureWithBoundAttr(point.getId(), viewshed.getPolygon()));
            } else {
                DataBuffer sumBuf = sumViewshed.getDataBuffer();
                DataBuffer viewBuf = viewshed.getView().getDataBuffer();
                for(int i = 0; i < viewBuf.getSize(); i++) {
                    sumBuf.setElemDouble(i, sumBuf.getElemDouble(i)+viewBuf.getElemDouble(i));
                }
            }
            incProgress(1);
        }
        
        if(vectorOutput) {
            return viewsheds;
        } else {
            return sumViewshed;
        }
    }

    @Override
    public void gather(Object result) {
        if(vectorOutput) {
            viewSheds.addAll((List)result);
        } else if(degree) {
            double[] viewTot = ((DataBufferDouble)viewshedRast.getDataBuffer()).getData();
            DataBuffer sumBuf = ((Raster)result).getDataBuffer();
            for(int i = 0; i < sumBuf.getSize(); i++) {
                 viewTot[i] += sumBuf.getElemDouble(i);
            }
        } else {
            int[] viewTot = ((DataBufferInt)viewshedRast.getDataBuffer()).getData();
            DataBuffer sumBuf = ((Raster)result).getDataBuffer();
            for(int i = 0; i < sumBuf.getSize(); i++) {
                 viewTot[i] += sumBuf.getElem(i);
            }
        }
    }

    @Override
    public int getSplitRange() {
        return points.size();
    }
    
    public void saveResult(File dir, String name) throws IOException {
        if(dir == null) {
            dir = project.getDirectory();
        }
        if(name == null) {
            name = "multiviewshed" + (inverse ? "-inverse" : "") + (degree ? "-deg" : "");
        }
        if(vectorOutput) {
            DefaultFeature.saveFeatures(viewSheds, new File(dir, name + ".shp"), project.getCRS());
        } else {
            IOImage.saveTiffCoverage(new File(dir, name + ".tif"),
                new GridCoverageFactory().create("view", viewshedRast, project.getDtmCov().getEnvelope2D()));
        }
    }
}
