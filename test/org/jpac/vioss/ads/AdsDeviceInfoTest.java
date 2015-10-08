/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpac.vioss.ads;

import java.io.IOException;
import java.net.InetAddress;
import org.jpac.plc.Data;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author berndschuster
 */
public class AdsDeviceInfoTest {
    
    public AdsDeviceInfoTest() {
    }

    @Test
    public void testAdsDeviceInfo() throws IOException {
         Connection conn = null;
        try{
            System.out.println("test AdsDeviceInfo");
            System.out.println(InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsDeviceInfo instance = new AdsDeviceInfo();
            instance.transact(conn);
            System.out.println("Device name     :" + instance.getDeviceName());
            System.out.println("major version   :" + instance.getMajorVersion());
            System.out.println("minor version   :" + instance.getMinorVersion());
            System.out.println("build           :" + instance.getVersionBuild());
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }
    
//    @Test
//    public void testAdsRead() throws IOException {
//        Connection conn = null;
//        try{
//            System.out.println("test AdsRead");
//            System.out.println(InetAddress.getLocalHost());
//            System.out.println(InetAddress.getLocalHost().getHostAddress());
////            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
//            AdsRead instance = new AdsRead(IndexGroup.ADSIGRP_IOIMAGE_CLEARI,321,123);
//            instance.transact(conn);
//        }
//        catch(Exception exc){
//            exc.printStackTrace();
//            fail("exception thrown");
//        }
//        finally{
//            if (conn != null) conn.close();
//            System.out.println("connection closed");
//        }
//    }

//    @Test
//    public void testAdsWrite() throws IOException {
//        Connection conn = null;
//        byte[] b = {0x01,0x02};
//        try{
//            System.out.println("test AdsWrite");
//            System.out.println(InetAddress.getLocalHost());
//            System.out.println(InetAddress.getLocalHost().getHostAddress());
////            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.0.52.1.1"), AmsPortNr.TwinCat3Plc);
//            AdsWrite instance = new AdsWrite(IndexGroup.ADSIGRP_IOIMAGE_CLEARI,111,b.length,new Data(b));
//            instance.transact(conn);
//        }
//        catch(Exception exc){
//            exc.printStackTrace();
//            fail("exception thrown");
//        }
//        finally{
//            if (conn != null) conn.close();
//            System.out.println("connection closed");
//        }
//    }
    @Test
    public void testAdsGetSymbolHandleByName() throws IOException {
        Connection conn = null;
        try{
            System.out.println("test AdsGetSymbolHandleByName");
            System.out.println(InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName instance = new AdsGetSymbolHandleByName("MAIN.countMain");
            instance.transact(conn);
            System.out.println("Handle: " + instance.getHandle());
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }
    
    @Test
    public void testAdsReleaseHandle() throws IOException {
        Connection conn = null;
        try{
            System.out.println("test AdsReleaseHandle");
            System.out.println(InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName instance = new AdsGetSymbolHandleByName("MAIN.countMain");
            instance.transact(conn);
            System.out.println("Handle: " + instance.getHandle());
            AdsReleaseHandle rinstance = new AdsReleaseHandle(instance.getHandle());
            rinstance.transact(conn);
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }
        
    @Test
    public void testAdsReadVariableByHandle() throws IOException {
        Connection conn = null;
        try{
            System.out.println("test AdsReadVariableByHandle");
            System.out.println(InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName handleCmd = new AdsGetSymbolHandleByName("MAIN.countMain");
            handleCmd.transact(conn);
            System.out.println("handle = " + handleCmd.getHandle());
            AdsReadVariableByHandle instance = new AdsReadVariableByHandle(handleCmd.getHandle(), 4);
            instance.transact(conn);
            for (int i = 0; i < 10; i++){
                instance.transact(conn);
                System.out.println("value :" + instance.getAdsResponse().getData().getDINT(0));                
            }
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }

    @Test
    public void testAdsWriteVariableByHandle() throws IOException {
        Connection conn = null;
        byte[] b    = {0x01,0x02,0x03,0x04};
        Data   data = new Data(new byte[4], Data.Endianness.LITTLEENDIAN);
        try{
            System.out.println("test AdsWriteVariableByHandle");
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.99.31.1.1"), AmsPortNr.PlcRuntimeSystem1);
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName handleCmd = new AdsGetSymbolHandleByName("MAIN.writeRegister");
            handleCmd.transact(conn);
            for (int i = 0; i < 10; i++){
                data.setDWORD(0, i);
                AdsWriteVariableByHandle instance = new AdsWriteVariableByHandle(handleCmd.getHandle(),4 , data);
                instance.transact(conn);
                Thread.sleep(10);
            }
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }

    @Test
    public void testAdsReadMultipeVariables() throws IOException {
        Connection conn = null;
        byte[] b = {0x01,0x02,0x03,0x04};
        try{
            System.out.println("test AdsReadMultipeVariables");
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.0.52.1.1"), AmsPortNr.PlcRuntimeSystem1);
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName handleCmd1 = new AdsGetSymbolHandleByName("MAIN.testDINT1");
            handleCmd1.transact(conn);
            Long handle1 = handleCmd1.getHandle();
            AdsGetSymbolHandleByName handleCmd2 = new AdsGetSymbolHandleByName("MAIN.testDINT2");
            handleCmd2.transact(conn);
            Long handle2 = handleCmd2.getHandle();
            AdsGetSymbolHandleByName handleCmd3 = new AdsGetSymbolHandleByName("MAIN.testDINT3");
            handleCmd3.transact(conn);
            Long handle3 = handleCmd3.getHandle();
//            Long handle1 = 101l;
//            Long handle2 = 102l;
//            Long handle3 = 103l;
            
            AdsReadVariableByHandle ar1 = new AdsReadVariableByHandle(handle1, 4);
            AdsReadVariableByHandle ar2 = new AdsReadVariableByHandle(handle2, 4);
            AdsReadVariableByHandle ar3 = new AdsReadVariableByHandle(handle3, 4);
//            AdsReadVariableByHandle ar1 = new AdsReadVariableByHandle(100, 4);
//            AdsReadVariableByHandle ar2 = new AdsReadVariableByHandle(101, 4);
            AdsReadMultiple instance = new AdsReadMultiple();
            instance.addAmsPacket(ar1);
            instance.addAmsPacket(ar2);
            instance.addAmsPacket(ar3);
            instance.transact(conn);
            
            System.out.println("testDINT1: error code " + ar1.getAdsResponse().getErrorCode());
            System.out.println("testDINT1: " + ar1.getAdsResponse().getData().getDINT(0));
            System.out.println("testDINT2: error code " + ar2.getAdsResponse().getErrorCode());
            System.out.println("testDINT2: " + ar2.getAdsResponse().getData().getDINT(0));
            System.out.println("testDINT3: error code " + ar3.getAdsResponse().getErrorCode());
            System.out.println("testDINT3: " + ar3.getAdsResponse().getData().getDINT(0));
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }    

    @Test
    public void testAdsWriteMultipeVariables() throws IOException {
        Connection conn = null;
        byte[] b = {0x01,0x02,0x03,0x04};
        byte[] b1 = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
        Data data1 = new Data(new byte[4], Data.Endianness.LITTLEENDIAN);
        Data data2 = new Data(new byte[4], Data.Endianness.LITTLEENDIAN);
        Data data3 = new Data(new byte[4], Data.Endianness.LITTLEENDIAN);
        Data data4 = new Data(new byte[4], Data.Endianness.LITTLEENDIAN);
        try{
            System.out.println("test AdsWriteMultipeVariables");
            System.out.println(InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.99.31.1.1"), AmsPortNr.PlcRuntimeSystem1);
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName handleCmd1 = new AdsGetSymbolHandleByName("MAIN.testDINT1");
            handleCmd1.transact(conn);
            Long handle1 = handleCmd1.getHandle();
            AdsGetSymbolHandleByName handleCmd2 = new AdsGetSymbolHandleByName("MAIN.testDINT2");
            handleCmd2.transact(conn);
            Long handle2 = handleCmd2.getHandle();
            AdsGetSymbolHandleByName handleCmd3 = new AdsGetSymbolHandleByName("MAIN.testDINT3");
            handleCmd3.transact(conn);
            Long handle3 = handleCmd3.getHandle();
            AdsGetSymbolHandleByName handleCmd4 = new AdsGetSymbolHandleByName("MAIN.testDINT4");
            handleCmd4.transact(conn);
            Long handle4 = handleCmd4.getHandle();
//            Long handle1 = 101l;
//            Long handle2 = 102l;
//            Long handle3 = 103l;
//            Long handle4 = 104l;
            data1.setDINT(0, 11);
            data2.setDINT(0, 12);
            data3.setDINT(0, 13);
            data4.setDINT(0, 14);
            AdsWriteVariableByHandle ar1 = new AdsWriteVariableByHandle(handle1, 4, data1);
            AdsWriteVariableByHandle ar2 = new AdsWriteVariableByHandle(handle2, 4, data2);
            AdsWriteVariableByHandle ar3 = new AdsWriteVariableByHandle(handle3, 4, data3);
            AdsWriteVariableByHandle ar4 = new AdsWriteVariableByHandle(handle4, 4, data4);
            AdsWriteMultiple instance = new AdsWriteMultiple();
            instance.addAmsPacket(ar1);
            instance.addAmsPacket(ar2);
            instance.addAmsPacket(ar3);
            instance.addAmsPacket(ar4);
            for (int i = 0; i < 1; i++){
//                data1.setDINT(0, 100000 + i);
//                data2.setDINT(0, 200000 + i);
//                data3.setDINT(0, 300000 + i);
//                data4.setDINT(0, 400000 + i);
                instance.transact(conn);
                Thread.sleep(10);
            }
//            data1.setDINT(0, 103);
//            data2.setDINT(0, 104);
//            instance.transact(conn);
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }   

    @Test
    public void testAdsGetMultipleHandles() throws IOException {
        Connection conn = null;
        byte[] b = {0x01,0x02,0x03,0x04};
        byte[] b1 = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
        Data cmd = new Data(new byte[1], Data.Endianness.LITTLEENDIAN);
        Data ack = new Data(new byte[1], Data.Endianness.LITTLEENDIAN);
        Data act = new Data(new byte[1], Data.Endianness.LITTLEENDIAN);
        Data res = new Data(new byte[2], Data.Endianness.LITTLEENDIAN);
        try{
            System.out.println("test AdsGetMultipleHandles");
//            conn = new Connection(InetAddress.getLocalHost().getHostAddress());
//            conn = new Connection(new AmsNetId("192.168.99.31.1.1"), AmsPortNr.PlcRuntimeSystem1);
            conn = new Connection(new AmsNetId("192.168.0.68.1.1"), AmsPortNr.PlcRuntimeSystem1);
            AdsGetSymbolHandleByName handleCmd1 = new AdsGetSymbolHandleByName("MAIN.testDINT1");
            AdsGetSymbolHandleByName handleCmd2 = new AdsGetSymbolHandleByName("MAIN.testDINT2");
            AdsGetSymbolHandleByName handleCmd3 = new AdsGetSymbolHandleByName("MAIN.testDINT3");
            AdsGetSymbolHandleByName handleCmd4 = new AdsGetSymbolHandleByName("MAIN.testDINT4");

            AdsReadWriteMultiple instance = new AdsReadWriteMultiple();
            instance.addAdsReadWrite(handleCmd1);
            instance.addAdsReadWrite(handleCmd2);
            instance.addAdsReadWrite(handleCmd3);
            instance.addAdsReadWrite(handleCmd4);
            instance.transact(conn);
            System.out.println("handle1: " + handleCmd1.getHandle());
            System.out.println("handle2: " + handleCmd2.getHandle());
            System.out.println("handle3: " + handleCmd3.getHandle());
            System.out.println("handle4: " + handleCmd4.getHandle());
        }
        catch(Exception exc){
            exc.printStackTrace();
            fail("exception thrown");
        }
        finally{
            if (conn != null) conn.close();
            System.out.println("connection closed");
        }
    }   

//    
//    @Test
//    public void testUri() throws IOException {
//        Connection conn = null;
//        try{
//            System.out.println("test URI");
//            URI uri  = new URI("ads://192.168.0.52:851/HelloWorld");
//            System.out.println(uri.getHost());
//            System.out.println(uri.getScheme());
//            System.out.println(uri.getPort());
//            System.out.println(uri.getQuery());
//            System.out.println(InetAddress.getLocalHost());
//            System.out.println(InetAddress.getLocalHost().getHostAddress());
//        }
//        catch(Exception exc){
//            exc.printStackTrace();
//            fail("exception thrown");
//        }
//        finally{
//            if (conn != null) conn.close();
//            System.out.println("connection closed");
//        }
//    }   
//    
}
