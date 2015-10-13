/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.thema.common.collection.HashMapList;
import org.thema.pixscape.view.ViewResult;

/**
 *
 * @author gvuidel
 */
public abstract class AbstractMetric implements Metric {
    private final boolean codeSupport;
    private SortedMap<Integer, Integer> codes;

    public AbstractMetric(boolean codeSupport) {
        this.codeSupport = codeSupport;
        if(codeSupport) {
            codes = new TreeMap<>();
        }
    }

    @Override
    public boolean isCodeSupported() {
        return codeSupport;
    }

    @Override
    public void addCode(int code) {
        if(!isCodeSupported()) {
            return;
        }
        if(codes.isEmpty()) {
            codes.put(code, 0);
        } else {
            codes.put(code, Collections.max(codes.values())+1);
        }
    }

    @Override
    public void addCodes(Set<Integer> set) {
        if(!isCodeSupported()) {
            return;
        }
        int group = codes.isEmpty() ? 0 : (Collections.max(codes.values())+1);
        for(Integer code : set) {
            codes.put(code, group);
        }
    }

    @Override
    public SortedSet<Integer> getCodes() {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return new TreeSet<>(codes.keySet());
    }
    
    public boolean hasCodeGroup() {
        return codes.size() > new HashSet(codes.values()).size();
    }
    
    public HashMapList<Integer, Integer> getCodeGroups() {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        HashMapList<Integer, Integer> groups = new HashMapList<>();
        for(Integer code : codes.keySet()) {
            groups.putValue(codes.get(code), code);
        }
        return groups;
    }
    
    
    protected SortedSet<Integer> getCodes(ViewResult result) {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return getCodes().isEmpty() ? result.getCodes() : getCodes();
    }
    
    public String getName() {
        try {
            return getCodeName() + " - " + java.util.ResourceBundle.getBundle("org/thema/pixscape/metric/Bundle").getString(getShortName());
        } catch(MissingResourceException e) {
            Logger.getLogger(AbstractMetric.class.getName()).info("No name for metric " + getShortName());
            return getCodeName();
        }
    }
    
    public String getCodeName() {
        String s = getShortName();
        if(isCodeSupported() && !codes.isEmpty()) {
            for(List<Integer> group : getCodeGroups().values()) {
                Iterator<Integer> it = group.iterator();
                s += it.next();
                while(it.hasNext()) {
                    s += "-" + it.next();
                }
                s += ",";
            }
            s = s.substring(0, s.length()-1);
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
