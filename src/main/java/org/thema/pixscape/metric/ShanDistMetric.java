
package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.thema.common.collection.TreeMapList;
import org.thema.pixscape.view.ViewTanResult;

/**
 * Shannon entropy of the distance of the pixels on the skyline in tangential view.
 * The distances are grouped in 5 classes defined by {@link #DIST_BREAKS }
 * Does not support codes nor distance ranges.
 * @author Gilles Vuidel
 */
public class ShanDistMetric extends AbstractMetric implements ViewTanMetric {

    /**
     * Distance classes 
     */
    public static final TreeSet<Double> DIST_BREAKS = new TreeSet<>(Arrays.asList(0.0, 10.0, 100.0, 1000.0, 10000.0));
    
    /**
     * Creates a new ShanDistMetric
     */
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
