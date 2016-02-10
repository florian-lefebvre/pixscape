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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thema.common.collection.HashMapList;
import org.thema.pixscape.view.ViewResult;

/**
 * Base class for Metric implementation.
 * 
 * @author Gilles Vuidel
 */
public abstract class AbstractMetric implements Metric {
    
    private final boolean codeSupport;
    private SortedMap<Integer, Integer> codes;

    /**
     * Creates a new metric
     * @param codeSupport is the metric support landuse code ?
     */
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

    /**
     * {@inheritDoc }
     * @throws IllegalStateException if codes are not supported by this metric
     */
    @Override
    public SortedSet<Integer> getCodes() {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return new TreeSet<>(codes.keySet());
    }
    
    /**
     * @return true if some landuse codes are grouped
     */
    public boolean hasCodeGroup() {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return codes.size() > new HashSet(codes.values()).size();
    }
    
    /**
     * @return a mapping containing for each group id (key) the list of landuse codes
     * @throws IllegalStateException if codes are not supported by this metric
     */
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
    
    /**
     * If {@link #getCodes() } is empty returns all codes from {@link ViewResult#getCodes() }
     * @param result the view result 
     * @return the landuse codes to be used for calculating this metric
     * @throws IllegalStateException if codes are not supported by this metric
     */
    protected SortedSet<Integer> getCodes(ViewResult result) {
        if(!isCodeSupported()) {
            throw new IllegalStateException("Codes are not supported for metric : " + this);
        }
        return getCodes().isEmpty() ? result.getCodes() : getCodes();
    }
    
    /**
     * The full name of the metric is retrieved from 
     * org/thema/pixscape/metric/Bundle properties file.
     * The key is the short name metric.
     * If the entry does not exist in the Bundle file, return only the short name with codes if any.
     * @return the full name of the metric
     */
    @Override
    public String getName() {
        try {
            return getCodeName() + " - " + java.util.ResourceBundle.getBundle("org/thema/pixscape/metric/Bundle").getString(getShortName());
        } catch(MissingResourceException e) {
            Logger.getLogger(AbstractMetric.class.getName()).log(Level.INFO, "No name for metric " + getShortName(), e);
            return getCodeName();
        }
    }
    
    /**
     * Example : A1,2,3,4-6 
     * @return a String containing the metric short name followed by the codes list if any
     */
    protected String getCodeName() {
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

    /**
     * @return the full name of the metric
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * {@inheritDoc }
     * Default implementation returns the short name with codes.
     */
    @Override
    public String[] getResultNames() {
        return new String[] { getCodeName() };
    }
    
}
