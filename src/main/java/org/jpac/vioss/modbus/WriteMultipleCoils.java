/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : WriteRegisters.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 *
 * This file is part of the jPac PLC communication library.
 * The jPac PLC communication library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The jPac PLC communication library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac PLC communication library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.jpac.vioss.modbus;

import java.io.IOException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
public class WriteMultipleCoils implements Request{
    private final static int PROTOCOLIDENTIFIER = 0x0000;
    private final static int UNITIDENTIFIER     = 0x01;
    private final static int LENGTHFIELD        = 0x000B;
    
    private DataBlock dataBlock;

    private int     transactionIdentifier;
    
    public WriteMultipleCoils(DataBlock dataBlock){

        this.dataBlock = dataBlock;
    }

    public void write(Connection conn) throws IOException {
        writeRequestHeader(conn);
        writeData(conn);
        conn.getOutputStream().flush();
    }

    protected void writeData(Connection conn) throws IOException {
        try{
            for (int i = 0; i < dataBlock.getSize(); i++){
                conn.getOutputStream().writeShort(dataBlock.getData().getWORD(2 * i));
            }
        }
        catch(AddressException exc){
            throw new IOException(exc);
        }
    }

    public void read(Connection conn) throws IOException {
        readResponseHeader(conn);
    }
    
    public Data getData() {
    	return dataBlock.getData();
    }    

    protected void writeRequestHeader(Connection conn) throws IOException{
        conn.getOutputStream().writeShort(getNextTransactionIdentifier());                   //transaction id
        conn.getOutputStream().writeShort((short)PROTOCOLIDENTIFIER);                        //protocol identifier (always 0x0000)
        conn.getOutputStream().writeShort((short)LENGTHFIELD);                               //length field
        conn.getOutputStream().writeByte((byte)UNITIDENTIFIER);                              //unit identifier (not used)        
        conn.getOutputStream().writeByte((byte)FunctionCode.WRITEMULTIPLECOILS.getValue());  //function code
        conn.getOutputStream().writeShort((short)dataBlock.getAddress());                    //address of the first coil
        conn.getOutputStream().writeShort((short)(16 * dataBlock.getSize()));                //number of coils
        conn.getOutputStream().writeByte((byte)(2 * dataBlock.getSize()));                   //byte count of the registers to write 
    }
    
    protected void readResponseHeader(Connection conn) throws IOException{
        int receivedTransactionIdentifier = (int)conn.getInputStream().readShort();
        if (receivedTransactionIdentifier != getActualTransactionIdentifier()){
            throw new IOException("inconsistent transaction identifier received over modbus connection " + conn + " : " + receivedTransactionIdentifier);
        }
        int protocolIdentifier = conn.getInputStream().readShort();
        if (protocolIdentifier != PROTOCOLIDENTIFIER){
            throw new IOException("inconsistent protocol identifier received over modbus connection " + conn + " : " + protocolIdentifier);            
        }
        int lengthField = conn.getInputStream().readShort();
        int unitIdentifier = (int)conn.getInputStream().readByte();
        if (unitIdentifier != UNITIDENTIFIER){
            throw new IOException("inconsistent unit identifier received over modbus connection " + conn + " : " + unitIdentifier);            
        }
        int functionCode    = (int)conn.getInputStream().readByte();
        if (functionCode != FunctionCode.WRITEMULTIPLECOILS.getValue()){
        	int exceptionCode = (int)conn.getInputStream().readByte();
            throw new IOException("exception received from modbus device over connection " + conn + " : function code = " + Integer.toHexString(functionCode) + " exception code = " + exceptionCode);            
        }  
        int startingAddress = (int)conn.getInputStream().readShort();
        int bitcount        = (int)conn.getInputStream().readShort();
        if (bitcount != 16 * dataBlock.getSize()){
            throw new IOException("inconsistent byte count received from modbus device over connection " + conn + " : " + bitcount);                        
        }                
    }
    
    private short getNextTransactionIdentifier(){
        transactionIdentifier++;
        if (transactionIdentifier > 0xFFFF){
            transactionIdentifier = 0;
        }
        return (short)transactionIdentifier;
    }
    
    private short getActualTransactionIdentifier(){
        return (short)transactionIdentifier;
    } 
    
     public static void main(String[] args){
       boolean handles = false;
       Connection conn = null;
       try{
           conn = new Connection("192.168.1.200", 502);
           
           DataBlock db = new DataBlock(0,2, FunctionCode.UNDEFINED, FunctionCode.WRITEMULTIPLECOILS, new Iec61131Address("QW0"));   
           WriteMultipleCoils wreq = new WriteMultipleCoils(db);
           long startTime;
           long stopTime;
           for (int i = 0; i < 10000; i++){
                startTime = System.nanoTime();
                wreq.write(conn);
                wreq.read(conn);
                db.getData().setBYTE(0, i & 0xFF);
                //db.getData().setWORD(1, i);
                //txData.setBIT(0, 0, i % 2 == 0);
                //txData.setBIT(1, 0, i % 2 == 1);
                stopTime = System.nanoTime();
                System.out.println(" duration: " + (stopTime - startTime));
                Thread.sleep(500);
           }
           System.out.println(conn);
           conn.close();
       }
       catch(Error | Exception exc){
           exc.printStackTrace();
           try{conn.close();}catch(IOException ex){};
       }
    }            
}
