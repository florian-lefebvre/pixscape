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
package org.thema.pixscape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.geometry.DirectPosition2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.thema.common.collection.TreeMapList;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.pixscape.view.SimpleComputeView;

/**
 *
 * @author gvuidel
 */
public class Saturation {
    private Project project;
    
    private DefaultFeature freeSector;
    private List<DefaultFeature> sectors;
    private List<DefaultFeature> objectSeen;

    public Saturation(Project project) {
        this.project = project;
    }
    
    public void calc(Coordinate c0, double maxDist, List<DefaultFeature> objects, String idField) {
        SimpleComputeView compute = project.getSimpleComputeView();
        double zOrig = project.getStartZ();
        GridCoordinates2D orig = compute.getData().getWorld2Grid(new DirectPosition2D(c0.getX(), c0.getY()));
        
        Map<Feature, Double> angleHeight = new HashMap<>();
        TreeMapList<Double, Feature> angleHoriz = new TreeMapList<>();
        Set groupIds = new HashSet();
        objectSeen = new ArrayList<>();
        List<String> attrNames = new ArrayList<>(objects.get(0).getAttributeNames());
        attrNames.add("zh");
        for(Feature obj : objects) {
            groupIds.add(obj.getAttribute(idField));
            Coordinate c1 = obj.getGeometry().getCoordinate();
            double dist = c0.distance(c1);
            if(dist > maxDist) {
                continue;
            }
            double zDest = -1;
            if(obj.getAttributeNames().contains("height")) {
                zDest = ((Number)obj.getAttribute("height")).doubleValue();
            }
            GridCoordinates2D dest = compute.getData().getWorld2Grid(new DirectPosition2D(c1.getX(), c1.getY()));
            double zh = compute.calcRay(orig, zOrig, dest, zDest, new Bounds(), false);
            if(zh == 0) {
                continue;
            }
            List attrs = obj.getAttributes();
            attrs.add(zh);
            objectSeen.add(new DefaultFeature(obj.getId(), obj.getGeometry(), attrNames, attrs));
            angleHeight.put(obj, zh);
            angleHoriz.putValue(Math.atan2(c1.y-c0.y, c1.x-c0.x), obj);
        }
        groupIds.add(null);
        sectors = new ArrayList<>();
        freeSector = null;
        for(Object id : groupIds) {
            double first = Double.NaN, prec = Double.NaN;
            double maxAngle = 0, hMax = 0;
            double aMin = Double.NaN, aMax = Double.NaN;
            double dist = 0;
            int nbObj = 0;
            for(double angle : angleHoriz.keySet()) {
                for(Feature obj : angleHoriz.get(angle)) {
                    if(id != null && !id.equals(obj.getAttribute(idField))) {
                        continue;
                    }
                    nbObj++;
                    double h = angleHeight.get(obj);
                    if(h > hMax) {
                        hMax = h;
                    }
                    double dd = c0.distance(obj.getGeometry().getCoordinate());
                    if(dd > dist) {
                        dist = dd;
                    }
                    if(!Double.isNaN(first)) {
                        double d = angle - prec;
                        if(d > maxAngle) {
                            maxAngle = d;
                            aMin = prec;
                            aMax = angle;
                        }
                    } else {
                        first = angle;
                    }
                    prec = angle;
                }
            }
            double d = (first - prec + 2*Math.PI) % (2*Math.PI);
            if(d > maxAngle) {
                maxAngle = d;
                aMin = prec;
                aMax = first;
            }
            if(!Double.isNaN(aMin) && aMin != aMax) {
                GeometricShapeFactory factory = new GeometricShapeFactory();
                factory.setCentre(c0);
                if(id == null) {
                    factory.setSize(maxDist*2);
                    double amp = (aMax-aMin + 2*Math.PI) % (2*Math.PI);
                    Geometry geom = factory.createArcPolygon(aMin, amp);
                    freeSector = new DefaultFeature("free", geom, Arrays.asList("amplitude", "aleft", "aright"), Arrays.asList(amp*180/Math.PI, Bounds.rad2deg(aMax), Bounds.rad2deg(aMin)));
                } else {
                    factory.setSize(dist*2);
                    double amp = (aMin-aMax + 2*Math.PI) % (2*Math.PI);
                    Geometry geom = factory.createArcPolygon(aMax, amp);
                    sectors.add(new DefaultFeature(id, geom, Arrays.asList("nbobj", "zhmax", "amplitude", "aleft", "aright"), Arrays.asList(nbObj, hMax, amp*180/Math.PI, Bounds.rad2deg(aMin), Bounds.rad2deg(aMax))));
                }
                
            }
        }
        
        if(freeSector == null) {
            GeometricShapeFactory factory = new GeometricShapeFactory();
            factory.setCentre(c0);
            factory.setSize(maxDist*2);
            freeSector = new DefaultFeature("free", factory.createCircle(), Arrays.asList("amplitude", "aleft", "aright"), Arrays.asList(360, 0, 0));
        }
    }

    public DefaultFeature getFreeSector() {
        return freeSector;
    }

    public List<DefaultFeature> getSectors() {
        return sectors;
    }

    public List<DefaultFeature> getObjectSeen() {
        return objectSeen;
    }
    
    
}
