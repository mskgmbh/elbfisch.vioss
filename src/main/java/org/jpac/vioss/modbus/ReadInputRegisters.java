/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReadRegisters.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : -
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
import org.jpac.plc.ValueOutOfRangeException;

/**
 *
 * @author berndschuster
 */
public class ReadInputRegisters implements Request{
    private final static int PROTOCOLIDENTIFIER = 0x0000;
    private final static int UNITIDENTIFIER     = 0x01;
    private final static int LENGTHFIELD        = 0x0006;
    
    private DataBlock dataBlock;
    private Data      data;

    private int     transactionIdentifier;
    
    public ReadInputRegisters(DataBlock dataBlock){

        this.dataBlock = dataBlock;
        this.data      = new Data(new byte[2 * dataBlock.getSize()]);
    }

    @Override
    public void write(Connection conn) throws IOException {
        writeRequestHeader(conn);
        conn.getOutputStream().flush();
    }

    @Override
    public void read(Connection conn) throws IOException {
        readResponseHeader(conn);
        readData(conn);
    }
 
    public Data getData() {
    	return dataBlock.getData();
    }  

    protected void readData(Connection conn) throws IOException {
        int numberOfBytes = 2 * dataBlock.getSize();
        try{
            for (int i = 0; i < numberOfBytes; i++){
                data.setBYTE(i, (int)conn.getInputStream().readByte() & 0x000000FF);
            }
        }
        catch(AddressException | ValueOutOfRangeException exc){
            throw new IOException(exc);
        }        
    }

    protected void writeRequestHeader(Connection conn) throws IOException{
        conn.getOutputStream().writeShort(getNextTransactionIdentifier());                  //transaction id
        conn.getOutputStream().writeShort((short)PROTOCOLIDENTIFIER);                       //protocol identifier (always 0x0000)
        conn.getOutputStream().writeShort((short)LENGTHFIELD);                              //protocol identifier (always 0x0000)
        conn.getOutputStream().writeByte((byte)UNITIDENTIFIER);                             //unit identifier (not used)        
        conn.getOutputStream().writeByte((byte)FunctionCode.READINPUTREGISTERS.getValue()); //function code
        conn.getOutputStream().writeShort((short)dataBlock.getAddress());                   //address of the first register
        conn.getOutputStream().writeShort((short)dataBlock.getSize());                      //number of registers
    }
    
    protected void readResponseHeader(Connection conn) throws IOException{
        int receivedTransactionIdentifier = (int)conn.getInputStream().readShort();
        if (receivedTransactionIdentifier != getActualTransactionIdentifier()){
            throw new IOException("inconsistent transaction identifier received from modbus device over connection " + conn + " : " + receivedTransactionIdentifier);
        }
        int protocolIdentifier = conn.getInputStream().readShort();
        if (protocolIdentifier != PROTOCOLIDENTIFIER){
            throw new IOException("inconsistent protocol identifier received from modbus device over connection " + conn + " : " + protocolIdentifier);            
        }
        int lengthField = conn.getInputStream().readShort();
        int unitIdentifier = (int)conn.getInputStream().readByte();
        if (unitIdentifier != UNITIDENTIFIER){
            throw new IOException("inconsistent unit identifier received from modbus device over connection " + conn + " : " + unitIdentifier);            
        }
        int functionCode = (int)conn.getInputStream().readByte();
        int byteCount = (int)conn.getInputStream().readByte();
        if (functionCode != (byte)FunctionCode.READINPUTREGISTERS.getValue()){
            throw new IOException("exception received from modbus device over connection " + conn + " : function code = " + functionCode + ", exception code:" + byteCount);            
        }        
        if (byteCount != 2 * dataBlock.getSize()){
            throw new IOException("inconsistent byte count received from modbus device over connection " + conn + " : " + byteCount);                        
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
           
           Data rxData = new Data(new byte[8]);
           ReadInputRegisters rwreq = new ReadInputRegisters(new DataBlock(0,10,FunctionCode.READINPUTREGISTERS, FunctionCode.UNDEFINED, new Iec61131Address("%IW0")));
           long startTime;
           long stopTime;
           for (int i = 0; i < 10000; i++){
                startTime = System.nanoTime();
                rwreq.write(conn);
                rwreq.read(conn);
                //txData.setBIT(0, 0, i % 2 == 0);
                //txData.setBIT(1, 0, i % 2 == 1);
                stopTime = System.nanoTime();
                System.out.println(rxData.getBIT(1, 7) + " duration: " + (stopTime - startTime));
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
