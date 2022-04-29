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
package org.thema.pixscape.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Puntal;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.linearref.LinearIterator;
import org.locationtech.jts.linearref.LinearLocation;
import org.thema.common.JTS;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.pixscape.ScaleData;

/**
 *
 * @author gvuidel
 */
public class Interpolate {
    
    private static final List<String> ATTR_NAMES = Arrays.asList("featureId");
    
    private ScaleData grid;
    
    public Interpolate(ScaleData grid) {
        this.grid = grid;
    } 
    
    public List<Feature> interpolate(List<? extends Feature> features) {
        return interpolate(features, 1);
    }
    
    public List<Feature> interpolate(List<? extends Feature> features, int ratio) {
        GeometryFactory factory = new GeometryFactory();
        List<Feature> points = new ArrayList<>();
        List<String> attrNames = features.get(0).getAttributeNames();
        int id = 1;
        for(Feature feature : features) {
            Geometry geom = JTS.flattenGeometryCollection(feature.getGeometry()).copy();
            geom.apply(grid.getWorld2Grid());
            for(int i = 0; i < geom.getNumGeometries(); i++) {
                Geometry g = geom.getGeometryN(i);
                
                if(g instanceof Puntal) {
                    g.apply(filter);
                    points.add(new DefaultFeature(id++, g, attrNames, feature.getAttributes()));
                } else if(g instanceof Lineal) {
                    HashSet<Coordinate> coords = new HashSet<>();
                    LengthIndexedLine indexedLine = new LengthIndexedLine(g);
                    for(int ind = 0; ind < g.getLength(); ind+=ratio) {
                        Coordinate c = indexedLine.extractPoint(ind);
                        filter.filter(c);
                        coords.add(c);
                    }
                    for(Coordinate c : coords) {
                        points.add(new DefaultFeature(id++, factory.createPoint(c), attrNames, feature.getAttributes()));
                    }
                } else {
                    Envelope gEnv = g.getEnvelopeInternal();
                    Coordinate c = new Coordinate();
                    for(c.y = ((int)gEnv.getMinY())+0.5; c.y < gEnv.getMaxY()+1; c.y+=ratio) {
                        for(c.x = ((int)gEnv.getMinX())+0.5; c.x < gEnv.getMaxX()+1; c.x+=ratio) {
                            Point p = factory.createPoint(new Coordinate(c));
                            if(g.intersects(p)) {
                                p.apply(filter);
                                points.add(new DefaultFeature(id++, p, attrNames, feature.getAttributes()));
                            }
                        }
                    }
                }
            }
        }
        
        return points;
    }
    
    
    CoordinateFilter filter = new CoordinateFilter() {
        @Override
        public void filter(Coordinate c) {
            c.x = ((int)c.x) + 0.5;
            c.y = ((int)c.y) + 0.5;
            grid.getGrid2World().transform(c, c);
        }
    };
}
