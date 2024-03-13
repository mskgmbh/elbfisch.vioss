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
import org.jpac.WrongUseException;
import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
public class WriteMultipleRegisters implements Request{
    private final static int PROTOCOLIDENTIFIER = 0x0000;
    private final static int UNITIDENTIFIER     = 0x01;
    private final static int LENGTHFIELD        = 0x000B;
    
    private DataBlock dataBlock;

    private int     transactionIdentifier;
    
    public WriteMultipleRegisters(DataBlock dataBlock){
        this.dataBlock = dataBlock;
        if ((dataBlock.getSize()%2) != 0) { // TODO: ULB: check inserted to verify that size is a multiple of 2 -> a whole multiple register size 
            throw new WrongUseException("dataBlock size must be multiple of 2");
        }
    }

    public void write(Connection conn) throws IOException {
        writeRequestHeader(conn);
        writeData(conn);
        conn.getOutputStream().flush();
    }

    protected void writeData(Connection conn) throws IOException {
        conn.getOutputStream().write(dataBlock.getData().getBytes()); // TODO: ULB: changed due to datablock change from word to byte base
    }

    public void read(Connection conn) throws IOException {
        readResponseHeader(conn);
    }
    
    public Data getData() {
    	return dataBlock.getData();
    }

    protected void writeRequestHeader(Connection conn) throws IOException{
        conn.getOutputStream().writeShort(getNextTransactionIdentifier());                       //transaction id
        conn.getOutputStream().writeShort((short)PROTOCOLIDENTIFIER);                            //protocol identifier (always 0x0000)
        conn.getOutputStream().writeShort((short)LENGTHFIELD);                                   //length field
        conn.getOutputStream().writeByte((byte)UNITIDENTIFIER);                                  //unit identifier (not used)        
        conn.getOutputStream().writeByte((byte)FunctionCode.WRITEMULTIPLEREGISTERS.getValue());  //function code
        conn.getOutputStream().writeShort(dataBlock.getAddress()/2);                             //address of the first register              // TODO: ULB: transform address from byte back to word for request; short kann keine Werte über 32768
        conn.getOutputStream().writeShort((short)(dataBlock.getSize()/2));                       //register count                             // TODO: ULB: changed due to datablock change from word to byte base
        conn.getOutputStream().writeByte((byte)(dataBlock.getSize()));                           //byte count of the registers to write       // TODO: ULB: changed due to datablock change from word to byte base
    }
    
    @SuppressWarnings("unused") 
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
        int functionCode        = (int)conn.getInputStream().readByte();
        int startingAddress     = (int)conn.getInputStream().readShort();
        int quantityOfRegisters = (int)conn.getInputStream().readShort();
        if (functionCode != (byte)FunctionCode.WRITEMULTIPLEREGISTERS.getValue()){
            throw new IOException("exception received from modbus device over connection " + conn + " : function code = " + functionCode + " Exception code " + (startingAddress & 0xFF));            
        }  
        if (quantityOfRegisters != dataBlock.getSize()/2){ // TODO: ULB: changed due to datablock change from word to byte base
            throw new IOException("inconsistent register count received from modbus device over connection " + conn + " : " + quantityOfRegisters);                        
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
    
     /*public static void main(String[] args){
       boolean handles = false;
       Connection conn = null;
       try{
           conn = new Connection("192.168.1.200", 502);
           
           DataBlock db = new DataBlock(0,2, FunctionCode.UNDEFINED, FunctionCode.WRITEMULTIPLEREGISTERS, new Iec61131Address("QW0"));   
           WriteMultipleRegisters wreq = new WriteMultipleRegisters(db);
           long startTime;
           long stopTime;
           for (int i = 0; i < 10000; i++){
                startTime = System.nanoTime();
                wreq.write(conn);
                wreq.read(conn);
                db.getData().setBYTE(1, i);
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
    }*/            
}