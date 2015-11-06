
package org.thema.pixscape.metric;

import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;


/**
 * Base Metric interface.
 * 
 * @author Gilles Vuidel
 */
public interface Metric extends Serializable {
    
    /**
     * @return true if the metric use landuse codes
     */
    boolean isCodeSupported();
    
    /**
     * Adds one code, and creates a singleton group with this code.
     * Does nothing if {@link #isCodeSupported() } returns false.
     * @param code the landuse code to add
     */
    void addCode(int code);
    
    /**
     * Adds a set of codes, and creates a group with these codes.
     * Does nothing if {@link #isCodeSupported() } returns false.
     * @param codes the landuse codes to add in one group
     */
    void addCodes(Set<Integer> codes);
    
    /**
     * If {@link #addCode } or {@link #addCodes } have not been called, returns all landuse codes.
     * @return landuse codes used for this metric
     */
    SortedSet<Integer> getCodes();
    
    /**
     * @return the short name of the metric 
     */
    String getShortName();
    
    /**
     * @return the full name of the metric 
     */
    String getName();
    
    /**
     * Returns the results name.
     * If the metric has only one result, returns the metric short name
     * @return the results names
     */
    String [] getResultNames();
}
