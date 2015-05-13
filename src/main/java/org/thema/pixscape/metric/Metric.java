/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape.metric;

import java.io.Serializable;
import java.util.SortedSet;


/**
 *
 * @author gvuidel
 */
public interface Metric extends Serializable {
    public boolean isCodeSupported();
    public void setCodes(SortedSet<Integer> codes);
    public SortedSet<Integer> getCodes();
    public String getShortName();
    public String [] getResultNames();
}
