/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import org.thema.pixscape.ComputeView;

/**
 *
 * @author gvuidel
 */
public abstract class AbstractMetric implements Metric {
    private SortedSet<Integer> codes = new TreeSet<>();

    @Override
    public void setCodes(SortedSet<Integer> codes) {
        if(codes == null)
            this.codes = new TreeSet<>();
        else
            this.codes = codes;
    }

    @Override
    public SortedSet<Integer> getCodes() {
        return codes;
    }
    
    protected SortedSet<Integer> getCodes(ComputeView.ViewResult result) {
        return getCodes().isEmpty() ? result.getCodes() : getCodes();
    }
    
    @Override
    public String toString() {
        String s = getShortName();
        if(!codes.isEmpty()) {
            s += Arrays.deepToString(codes.toArray()).replace(",", "");
        }
        return s;
    }
    
    
}
