/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.SortedSet;
import java.util.TreeSet;
import org.thema.pixscape.view.ViewResult;

/**
 *
 * @author gvuidel
 */
public abstract class AbstractMetric implements Metric {
    private final boolean codeSupport;
    private SortedSet<Integer> codes;

    public AbstractMetric(boolean codeSupport) {
        this.codeSupport = codeSupport;
        if(codeSupport) {
            codes = new TreeSet<>();
        }
    }

    @Override
    public boolean isCodeSupported() {
        return codeSupport;
    }

    @Override
    public void setCodes(SortedSet<Integer> codes) {
        if(!isCodeSupported()) {
            return;
        }
        if(codes == null) {
            this.codes = new TreeSet<>();
        } else {
            this.codes = codes;
        }
    }

    @Override
    public SortedSet<Integer> getCodes() {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return codes;
    }
    
    protected SortedSet<Integer> getCodes(ViewResult result) {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return getCodes().isEmpty() ? result.getCodes() : getCodes();
    }
    
    public String getName() {
        try {
            return java.util.ResourceBundle.getBundle("org/thema/pixscape/metric/Bundle").getString(getShortName())
                    + " -" + getCodeName();
        } catch(MissingResourceException e) {
            return getCodeName();
        }
    }
    
    public String getCodeName() {
        String s = getShortName();
        if(isCodeSupported() && !codes.isEmpty()) {
            s += Arrays.deepToString(codes.toArray()).replace(",", "");
        }
        return s;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String[] getResultNames() {
        return new String[] { getCodeName() };
    }
    
}
