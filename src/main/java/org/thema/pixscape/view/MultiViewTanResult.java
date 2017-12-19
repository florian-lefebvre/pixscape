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


package org.thema.pixscape.view;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.TreeMap;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.ScaleData;

/**
 * ViewTanResult implementation for multiscale computation.
 * Creates the view with index for the first scale and creates the corresponding partial planimetric landuse. 
 * Based on these 2 rasters, delegates all methods to SimpleViewTanResult.
 * 
 * @author Gilles Vuidel
 */
public final class MultiViewTanResult extends MultiViewResult implements ViewTanResult {

    private Raster multiView, scale;
    
    private SimpleViewTanResult resultDelegate;
    
    /**
     * Creates a new MultiViewTanResult.
     * Creates the view with all indices at the first scale and creates the corresponding partial planimetric landuse. 
     * 
     * @param cg the point of view or observed point in grid coordinate
     * @param multiView the tangential view using several scales
     * @param scale scale used for each pixel
     * @param zones the zone where viewshed has been calculated for each scale
     * @param compute the compute view used
     */
    MultiViewTanResult(GridCoordinates2D cg, Raster multiView, Raster scale, TreeMap<Double, GridEnvelope2D> zones, MultiComputeViewJava compute) {
        super(cg, zones, compute);
        this.multiView = multiView;
        this.scale = scale;
        calcViewLand();
        resultDelegate = new SimpleViewTanResult(cg, view, landuse, new ComputeViewJava(getDatas().firstEntry().getValue(), 
                    compute.getaPrec(), compute.isEarthCurv(), compute.getCoefRefraction()));
    }

    @Override
    public final double getAres() {
        return resultDelegate.getAres();
    }

    protected double calcAreaUnbounded() {
        return resultDelegate.calcAreaUnbounded();
    }

    protected double[] calcAreaLandUnbounded() {
        return resultDelegate.calcAreaLandUnbounded();
    }

    @Override
    public double getArea(double dmin, double dmax) {
        return resultDelegate.getArea(dmin, dmax);
    }

    @Override
    protected double[] calcAreaLand(double dmin, double dmax) {
        return resultDelegate.calcAreaLand(dmin, dmax);
    }

    @Override
    public final double getDistance(int theta1, int theta2) {
        return resultDelegate.getDistance(theta1, theta2);
    }

    @Override
    public final double getElevation(int theta1, int theta2) {
        return resultDelegate.getElevation(theta1, theta2);
    }

    @Override
    public Raster getElevationView() {
        return resultDelegate.getElevationView();
    }

    @Override
    public Raster getDistanceView() {
        return resultDelegate.getDistanceView();
    }

    @Override
    public int getThetaWidth() {
        return resultDelegate.getThetaWidth();
    }

    @Override
    public int getThetaHeight() {
        return resultDelegate.getThetaHeight();
    }

    @Override
    public double getMaxDistance(int theta1) {
        return resultDelegate.getMaxDistance(theta1);
    }

    @Override
    public boolean isView360() {
        return resultDelegate.isView360();
    }
    
    @Override
    protected synchronized void calcViewLand() {
        final ScaleData first = getDatas().firstEntry().getValue();
        final GridGeometry2D firstGrid = first.getGridGeometry();
        final int firstW = first.getDtm().getWidth();
        view = multiView.createCompatibleWritableRaster();
        WritableRaster land = null;
        if(first.hasLandUse()) {
            land = Raster.createWritableRaster(new BandedSampleModel(DataBuffer.TYPE_SHORT, view.getWidth(), view.getHeight(), 1), null);
        }
        
        ScaleData[] datas = getDatas().values().toArray(new ScaleData[getDatas().size()]);
        
        for(int theta1 = 0; theta1 < view.getWidth(); theta1++) {
            for(int theta2 = 0; theta2 < view.getHeight(); theta2++) {
                int ind = multiView.getSample(theta1, theta2, 0);
                if(ind == -1) {
                    view.setSample(theta1, theta2, 0, -1);
                    if(first.hasLandUse()) {
                        land.setSample(theta1, theta2, 0, -1);
                    }
                    continue;
                }
                ScaleData data = datas[scale.getSample(theta1, theta2, 0)];
                final int w = data.getDtm().getWidth();
                final int x = ind % w;
                final int y = ind / w;
                if(data == first) {
                    view.setSample(theta1, theta2, 0, ind);
                    if(data.hasLandUse()) {
                        land.setSample(theta1, theta2, 0, data.getLand().getSample(x, y, 0));
                    }
                } else {
                    try {
                        final GridCoordinates2D p = firstGrid.worldToGrid(data.getGridGeometry().gridToWorld(new GridCoordinates2D(x, y)));
                        view.setSample(theta1, theta2, 0, p.y*firstW+p.x);
                        if(land != null) {
                            land.setSample(theta1, theta2, 0, data.getLand().getSample(x, y, 0));
                        }

                    } catch (TransformException ex) {
                       throw new IllegalArgumentException(ex);
                    }
                }
            }
        }
        
        landuse = land;
        
    }

}
