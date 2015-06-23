/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thema.pixscape.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.thema.pixscape.view.ViewResult;
import org.thema.pixscape.view.ViewShedResult;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public abstract class AbstractDistMetric extends AbstractMetric {

    private SortedSet<Double> distances;
    
    public AbstractDistMetric(boolean codeSupport) {
        super(codeSupport);
        distances = new TreeSet<>();
    }
    
    protected Double [] calcMetric(ViewResult result) {
        if(distances.isEmpty()) {
            return new Double[] {calcMetric(result, 0, Double.POSITIVE_INFINITY)};
        }
        List<Double> results = new ArrayList<>(distances.size());
        Iterator<Double> it = distances.iterator();
        double d1 = it.next();
        while(it.hasNext()) {
            double d2 = it.next();
            results.add(calcMetric(result, d1, d2));
            d1 = d2;
        }
        return results.toArray(new Double[results.size()]);
    }
    
    protected abstract double calcMetric(ViewResult result, double dmin, double dmax);
    
    
    public SortedSet<Double> getDistances() {
        return distances;
    }

    public void setDistances(SortedSet<Double> distances) {
        if(distances == null) {
            this.distances = new TreeSet<>();
        } else {
            if(distances.size() == 1) {
                throw new IllegalArgumentException("Requires at least 2 distances.");
            }
            this.distances = distances;
        }
    }

    @Override
    public final String[] getResultNames() {
        if(distances.isEmpty()) {
            return super.getResultNames();
        }
        List<String> names = new ArrayList<>();
        Iterator<Double> it = distances.iterator();
        double dist = it.next();
        while(it.hasNext()) {
            double d2 = it.next();
            names.add(getCodeName() + "_" + dist + "-" + d2);
            dist = d2;
        }
        return names.toArray(new String[names.size()]);
    }
    
    @Override
    public String toString() {
        String s =  super.toString();
        if(!distances.isEmpty()) {
            s += " " + Arrays.deepToString(distances.toArray());
        }
        return s;
    }
}
