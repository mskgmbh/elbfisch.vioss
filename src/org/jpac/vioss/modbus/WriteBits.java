/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : WriteMultipleCoils.java
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

/**
 *
 * @author berndschuster
 */
public class WriteBits implements Request{
    private final static int PROTOCOLIDENTIFIER = 0x0000;
    private final static int UNITIDENTIFIER     = 0x01;
    private final static int LENGTHFIELD        = 0x0009;
    
    private Address writeAddress;
    private Data    writeData;

    private int     transactionIdentifier;
    
    public WriteBits(Address address, Data writeData){
        if (address.getDataBlock().getValue() != DataBlock.DataBlocks.INTERNALBITS && 
            address.getDataBlock().getValue() != DataBlock.DataBlocks.PHYSICALCOILS){
            throw new IllegalArgumentException("bit write access not allowed for datablock " + address.getDataBlock());
        }        
        this.writeAddress = address;
        this.writeData    = writeData;
    }

    public void write(Connection conn) throws IOException {
        writeRequestHeader(conn);
        writeData(conn);
        conn.getOutputStream().flush();
    }

    protected void writeData(Connection conn) throws IOException {
        int numberOfBytes = writeAddress.getSize();
        try{
            for (int i = 0; i < numberOfBytes; i++){
                conn.getOutputStream().writeByte(writeData.getBYTE(i));
            }
        }
        catch(AddressException exc){
            throw new IOException(exc);
        }
    }

    public void read(Connection conn) throws IOException {
        readResponseHeader(conn);
    }


    protected void writeRequestHeader(Connection conn) throws IOException{
        conn.getOutputStream().writeShort(getNextTransactionIdentifier());                   //transaction id
        conn.getOutputStream().writeShort((short)PROTOCOLIDENTIFIER);                        //protocol identifier (always 0x0000)
        conn.getOutputStream().writeShort((short)LENGTHFIELD);                               //protocol identifier (always 0x0000)
        conn.getOutputStream().writeByte((byte)UNITIDENTIFIER);                              //unit identifier (not used)        
        conn.getOutputStream().writeByte((byte)FunctionCode.WRITEMULTIPLECOILS.getValue());  //function code
        conn.getOutputStream().writeShort((short)writeAddress.getStartingAddress());         //address of the first bit
        conn.getOutputStream().writeShort((short)(writeAddress.getQuantity()));              //bit count for write operation 1-2000
        conn.getOutputStream().writeByte((byte)writeAddress.getSize());                      //number of bytes to write                                                //unit identifier (not used)        
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
        int functionCode = (int)conn.getInputStream().readByte();
        int byteCount = (int)conn.getInputStream().readByte();
        if (functionCode != FunctionCode.WRITEMULTIPLECOILS.getValue()){
            throw new IOException("exception received from modbus device over connection " + conn + " : function code = " + functionCode + ", exception code:" + byteCount);            
        } 
        
        int startingAddress = conn.getInputStream().readShort();
        int quantity        = (int)conn.getInputStream().readByte();
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
           
           Data txData = new Data(new byte[2]);
           Data rxData = new Data(new byte[8]);
           txData.setBYTE(0,(byte)0x00);
           txData.setBYTE(1,(byte)0x00);
//           txData.setBYTE(2,(byte)0x00);
//           txData.setBYTE(3,(byte)0x00);
           WriteBits req = new WriteBits(new Address(new DataBlock(DataBlock.DataBlocks.PHYSICALCOILS), 0, 2) , txData);
           long startTime;
           long stopTime;
           for (int i = 0; i < 10000; i++){
                startTime = System.nanoTime();
                req.write(conn);
                req.read(conn);
                //txData.setBIT(0, 0, i % 2 == 0);
                txData.setBIT(1, 0, i % 2 == 1);
                stopTime = System.nanoTime();
                System.out.println(rxData.getBYTE(1) + " duration: " + (stopTime - startTime));
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
