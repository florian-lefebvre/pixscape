/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.thema.common.collection.TreeMapList;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class ShanDistMetric extends AbstractMetric implements ViewTanMetric {

    private static final TreeSet<Double> DIST_BREAKS = new TreeSet<>(Arrays.asList(0.0, 10.0, 100.0, 1000.0, 10000.0));
    
    public ShanDistMetric() {
        super(false);
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        TreeMapList<Double, Double> distances = new TreeMapList<>();
        for(int t = 0; t < result.getThetaWidth(); t++) {
            double dist = result.getMaxDistance(t);
            distances.putValue(DIST_BREAKS.floor(dist), dist);
        }
        if(distances.size() < 2) {
            return new Double[]{ 0.0 };
        }
        double shannon = 0;
        final double tot = result.getThetaWidth();
        for(List<Double> lst : distances.values()) {
            int nb = lst.size();
            shannon += - nb/tot * Math.log(nb/tot);
        }

        return new Double[]{ shannon/Math.log(distances.size()) };
    }

    @Override
    public String getShortName() {
        return "SD";
    }

}
