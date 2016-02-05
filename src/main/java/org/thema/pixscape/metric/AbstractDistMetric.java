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


package org.thema.pixscape.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.thema.pixscape.view.ViewResult;

/**
 * Base class for metric with distance ranges support.
 * 
 * @author Gilles Vuidel
 */
public abstract class AbstractDistMetric extends AbstractMetric {

    private SortedSet<Double> distances;
    
    /**
     * Creates a new metric
     * @param codeSupport is the metric support landuse code ?
     */
    public AbstractDistMetric(boolean codeSupport) {
        super(codeSupport);
        distances = new TreeSet<>();
    }
    
    /**
     * Default implementation which calls {@link #calcMetric(org.thema.pixscape.view.ViewResult, double, double) } for each distance range.
     * If no range is defined, the calculation is done for one full range [0 - Double.POSITIVE_INFINITY]
     * @param result the view result
     * @return the result of the metric for each distance range
     */
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
    
    /**
     * Calculates the metric for the given distance range [dmin-dmax[
     * @param result the result view
     * @param dmin the min distance inclusive
     * @param dmax the max distance exclusive
     * @return the result of the metric for the given distance range
     */
    protected abstract double calcMetric(ViewResult result, double dmin, double dmax);
    
    /**
     * The set can be empty if no range is defined.
     * 
     * @return set of distances used for distance ranges
     */
    public SortedSet<Double> getDistances() {
        return distances;
    }

    /**
     * Sets the distance ranges for this metric.
     * The ranges are contiguous : the set {0, 10, 100}  corresponds to 2 ranges [0-10[ and [10-100[
     * Distances must be in range [0 - Double.POSITIVE_INFINITY]
     * @param distances the set of distances, can be null for reseting to default range [0 - Double.POSITIVE_INFINITY]
     * @throws IllegalArgumentException if the distance set size < 2
     */
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

    /**
     * If no distance is set, return de default result name.
     * @return a result name for each distance range
     */
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
    
    /**
     * {@inheritDoc }
     * Adds the distance ranges if any
     */
    @Override
    public String toString() {
        String s =  super.toString();
        if(!distances.isEmpty()) {
            s += " " + Arrays.deepToString(distances.toArray());
        }
        return s;
    }
}
