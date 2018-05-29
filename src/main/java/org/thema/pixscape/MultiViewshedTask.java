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

import com.vividsolutions.jts.geom.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.DirectPosition2D;
import org.thema.common.ProgressBar;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.parallel.SimpleParallelTask;
import org.thema.pixscape.view.ViewShedResult;

/**
 *
 * @author gvuidel
 */
public class MultiViewshedTask extends SimpleParallelTask<Feature, Object> {
    
    private Project project;
    
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
    
    /** Results */
    private List<DefaultFeature> viewSheds;
    private Raster viewshedRast;

    public MultiViewshedTask(List<Feature> points, Project project, boolean inverse, double zDest, Bounds bounds, boolean vectorOutput, ProgressBar monitor) {
        super(points, monitor);
        this.project = project;
        this.inverse = inverse;
        this.zDest = zDest;
        this.bounds = bounds;
        this.vectorOutput = vectorOutput;
    }
    
    
    @Override
    public void init() {
        super.init();
        viewSheds = vectorOutput ? new ArrayList<DefaultFeature>() : null;
        viewshedRast = !vectorOutput ? Raster.createBandedRaster(DataBuffer.TYPE_INT, project.getDtm().getWidth(), project.getDtm().getHeight(), 1, null) : null;
    }


    @Override
    public Object getResult() {
        return vectorOutput ? viewSheds : viewshedRast;
    }

    @Override
    protected Object executeOne(Feature point) {
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
        ViewShedResult viewshed = project.getDefaultComputeView().calcViewShed(
                new DirectPosition2D(p.getX(), p.getY()), zOrig, zDest, inverse, b);
        if(vectorOutput) {
            return b.createFeatureWithBoundAttr(point.getId(), viewshed.getPolygon());
        } else {
            return viewshed.getView().getDataBuffer();
        }
    }

    @Override
    public void gather(List results) {
        if(vectorOutput) {
            viewSheds.addAll(results);
        } else {
            int[] viewTot = ((DataBufferInt)viewshedRast.getDataBuffer()).getData();
            for(DataBuffer viewBuf : (List<DataBuffer>)results) {
                for(int i = 0; i < viewBuf.getSize(); i++) {
                    if(viewBuf.getElem(i) > 0) {
                        viewTot[i]++;
                    }
                }
            }
        }
    }
    
}
