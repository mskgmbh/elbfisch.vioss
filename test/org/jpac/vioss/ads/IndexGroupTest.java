/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpac.vioss.ads;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author berndschuster
 */
public class IndexGroupTest {
    
    public IndexGroupTest() {
    }

    /**
     * Test of equals method, of class IndexGroup.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        IndexGroup ig = IndexGroup.ADSIGRP_IOIMAGE_RISIZE;
        IndexGroup instance = IndexGroup.ADSIGRP_IOIMAGE_RISIZE;
        boolean expResult = true;
        boolean result = instance.equals(ig);
        assertEquals(expResult, result);
        
        instance  = IndexGroup.ADSIGRP_SYM_HNDBYNAME;
        expResult = false;
        result    = instance.equals(ig);
        assertEquals(expResult, result);
        System.out.println("toString : " + instance);
    }
    
}
