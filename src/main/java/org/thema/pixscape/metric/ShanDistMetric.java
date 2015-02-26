/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.List;
import org.thema.common.collection.TreeMapList;
import org.thema.pixscape.view.ViewTanResult;

/**
 *
 * @author gvuidel
 */
public class ShanDistMetric extends AbstractMetric implements ViewTanMetric {

    public ShanDistMetric() {
        super(false);
    }

    @Override
    public Double[] calcMetric(ViewTanResult result) {
        TreeMapList<Integer, Integer> distances = new TreeMapList<>();
        for(int t = 0; t < result.getThetaWidth(); t++) {
            int dist = (int) result.getMaxDistance(t);
            distances.putValue(dist, dist);
        }
        double shannon = 0;
        double sum = 0;
        for(List<Integer> lst : distances.values()) {
            sum += lst.size();
        }
        for(List<Integer> lst : distances.values()) {
            int nb = lst.size();
            shannon += - nb/sum * Math.log(nb/sum);
        }
        return new Double[]{ shannon/Math.log(distances.size()) };
    }

    @Override
    public String getShortName() {
        return "SD";
    }


}
