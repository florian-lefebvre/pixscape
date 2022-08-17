/*
 * Copyright (C) 2022 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.SortedMap;
import java.util.TreeMap;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.ScaleData;

/**
 * Base class for ComputeView using multiscale datas.
 * 
 * @author Gilles Vuidel
 */
public abstract class MultiComputeView extends ComputeView {
    
    private final TreeMap<Double, ScaleData> datas;
    private int distMin;
    
    public MultiComputeView(TreeMap<Double, ScaleData> datas, int distMin, double aPrec, boolean earthCurv, double coefRefraction) {
        super(aPrec, earthCurv, coefRefraction);
        this.datas = datas;
        this.distMin = distMin;
    }

    /**
     * @return the minimal distance (in pixel) before changing scale
     */
    public int getDistMin() {
        return distMin;
    }

    /**
     * Sets the minimal distance (in pixel) before changing scale
     * @param distMin the minimal distance before changing scale
     */
    public void setDistMin(int distMin) {
        this.distMin = distMin;
    }

    /**
     * @return the data at each scale
     */
    public TreeMap<Double, ScaleData> getDatas() {
        return datas;
    }
    
    protected Rectangle calcZones(DirectPosition2D c, SortedMap<Double, GridEnvelope2D> viewZones) throws TransformException {
        Rectangle largestZone = new Rectangle(0, 0, -1, -1);
        for(ScaleData data : datas.values()) {
            GridGeometry2D grid = data.getGridGeometry();
            GridCoordinates2D c0 = grid.worldToGrid(c);
            Rectangle rect = new Rectangle(data.getDtm().getWidth(), data.getDtm().getHeight());
            if(data != datas.lastEntry().getValue()) {
                GridEnvelope2D rLim = new GridEnvelope2D(new Rectangle(c0.x-distMin, c0.y-distMin, 2*distMin+1, 2*distMin+1).intersection(rect));
                // adapt the envelope to match pixel border of upper scale
                // TODO  increase the size when rect is truncate
                GridGeometry2D gridUpper = datas.higherEntry(data.getResolution()).getValue().getDtmCov().getGridGeometry();
                rLim = grid.worldToGrid(gridUpper.gridToWorld(gridUpper.worldToGrid(grid.gridToWorld(rLim))));
                rect = rect.intersection(rLim);
                rect.add(new Rectangle(c0, new Dimension(1, 1))); // ensure point is in zone
            }
            largestZone = largestZone.union(new Rectangle(rect.x-c0.x, rect.y-c0.y, rect.width, rect.height));
            viewZones.put(data.getResolution(), new GridEnvelope2D(rect));
        }
        return largestZone;
    }
    
    /**
     * Calculates the intersection point between the rectangle and the half straight defined by p0 and the angle a.
     * p0 must be within rect.
     * @param p0 the start point of the half straight
     * @param a the angle of the half straight
     * @param rect the rectangle
     * @param res the result point, can be null
     * @return the point intersecting rect with the half straight
     */
    protected GridCoordinates2D calcIntersects(Point2D p0, double a, Rectangle rect, GridCoordinates2D res) {
        if(res == null) {
            res = new GridCoordinates2D();
        }
        if(a == Math.PI/2) {
            res.x = (int) p0.getX();
            res.y = (int) rect.getMinY();
            return res;
        } else if(a == -Math.PI/2) {
            res.x = (int) p0.getX();
            res.y = (int) rect.getMaxY()-1;
            return res;
        } 
        
        if(a < 0) {
            a += 2*Math.PI;
        }
        
        int y1 = (int)(a >= 0 && a < Math.PI ? rect.getMinY() : rect.getMaxY()-1); // haut ou bas ?
        int x1 = (int)(a >= Math.PI/2 && a < 1.5*Math.PI ? rect.getMinX() : rect.getMaxX()-1); // droite ou gauche ?
        
        double ddy = Math.tan(a) * (p0.getX()-x1);
        double y = Math.round(p0.getY() + ddy);   
        if(y >= rect.getMinY() && y < rect.getMaxY()) {
            y1 = (int)y;   
        } else {
            double ddx = (p0.getY()-y1) / Math.tan(a);
            x1 = (int)Math.round(p0.getX() + ddx);
        }
        res.x = x1;
        res.y = y1;
        return res;
    }

    public abstract MultiViewShedResult calcViewShed(DirectPosition2D c, double startZ, double destZ, boolean inverse, Bounds bounds);
}
