/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jpac.vioss.iec61131_3;

import org.jpac.vioss.InvalidAddressException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author berndschuster
 */
public class AbsoluteAddressTest {
    
    public AbsoluteAddressTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setType method, of class AbsoluteAddress.
     */
    @Test
    public void testConstructor() {
        //invalid address specifications
        try{
            AbsoluteAddress instance = new AbsoluteAddress("IX10.0");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%V");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%IV");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%IXV");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%I9.16");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%IX9");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%I9");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MB9.3");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MB9v");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MX9.v");
            assert(false);
        }
        catch(InvalidAddressException exc){
            System.out.println(exc);
            assert(true);
        }
        //valid address specifications
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%IX9.3");
            assert(instance.getArea() == AbsoluteAddress.Area.INPUT && 
                   instance.getType() == AbsoluteAddress.Type.BIT && 
                   instance.getWordAddress() == 9 && 
                   instance.getBitAddress() == 3);
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%I9.3");
            assert(instance.getArea() == AbsoluteAddress.Area.INPUT && 
                   instance.getType() == AbsoluteAddress.Type.BIT && 
                   instance.getWordAddress() == 9 && 
                   instance.getBitAddress() == 3);
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%OX9.3");
            assert(instance.getArea() == AbsoluteAddress.Area.OUTPUT && 
                   instance.getType() == AbsoluteAddress.Type.BIT && 
                   instance.getWordAddress() == 9 && 
                   instance.getBitAddress() == 3);
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%O9.3");
            assert(instance.getArea() == AbsoluteAddress.Area.OUTPUT && 
                   instance.getType() == AbsoluteAddress.Type.BIT && 
                   instance.getWordAddress() == 9 && 
                   instance.getBitAddress() == 3);
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MX9.3");
            assert(instance.getArea() == AbsoluteAddress.Area.MERKER && 
                   instance.getType() == AbsoluteAddress.Type.BIT && 
                   instance.getWordAddress() == 9 && 
                   instance.getBitAddress() == 3);
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MB9");
            assert(instance.getArea() == AbsoluteAddress.Area.MERKER && 
                   instance.getType() == AbsoluteAddress.Type.BYTE && 
                   instance.getByteAddress() == 9 && 
                   instance.getWordAddress() == 4 && 
                   instance.getDoubleWordAddress() == 2); 
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MW9");
            assert(instance.getArea() == AbsoluteAddress.Area.MERKER && 
                   instance.getType() == AbsoluteAddress.Type.WORD && 
                   instance.getByteAddress() == 18 && 
                   instance.getWordAddress() == 9 && 
                   instance.getDoubleWordAddress() == 4); 
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
        try{
            AbsoluteAddress instance = new AbsoluteAddress("%MD9");
            assert(instance.getArea() == AbsoluteAddress.Area.MERKER && 
                   instance.getType() == AbsoluteAddress.Type.DOUBLEWORD && 
                   instance.getByteAddress() == 36 && 
                   instance.getWordAddress() == 18 && 
                   instance.getDoubleWordAddress() == 9); 
        }
        catch(InvalidAddressException exc){
            assert(false);
        }
    }
    
    /**
     * Test of getAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testGetAddress() {
    }

    /**
     * Test of setAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testSetAddress() {
    }

    /**
     * Test of getWordAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testGetWordAddress() {
    }

    /**
     * Test of setWordAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testSetWordAddress() {
    }

    /**
     * Test of getBitAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testGetBitAddress() {
    }

    /**
     * Test of setBitAddress method, of class AbsoluteAddress.
     */
    @Test
    public void testSetBitAddress() {
    }

    /**
     * Test of getArea method, of class AbsoluteAddress.
     */
    @Test
    public void testGetArea() {
    }

    /**
     * Test of setArea method, of class AbsoluteAddress.
     */
    @Test
    public void testSetArea() {
    }

    /**
     * Test of getType method, of class AbsoluteAddress.
     */
    @Test
    public void testGetType() {
    }

    /**
     * Test of setType method, of class AbsoluteAddress.
     */
    @Test
    public void testSetType() {
    }
    
}
