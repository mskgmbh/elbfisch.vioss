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
public class AmsNetIdTest {
    
    public AmsNetIdTest() {
    }

    /**
     * Test of write method, of class AmsNetId.
     */
    @Test
    public void testNetIdToBytes() {
        try{
            System.out.println("netIdToBytes");
            Connection connection = null;
            AmsNetId instance = new AmsNetId("1.2.3.4.5.6");
            instance.netIdToBytes();
            for (int i = 0; i < 6; i++){
                if (instance.getNetIdBytes()[i] != i+1){
                    fail();
                }
            }
            instance.bytesToNetId();
            assertEquals("1.2.3.4.5.6", instance.getNetId());
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail();
        }
    }

    /**
     * Test of toString method, of class AmsNetId.
     */
    @Test
    public void testToString() {
        try{
            System.out.println("toString");
            AmsNetId instance = new AmsNetId("1.2.3.4.5.6");
            String expResult = "AmsNetId(1.2.3.4.5.6)";
            String result = instance.toString();
            assertEquals(expResult, result);
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail();
        }
    }
    
}
