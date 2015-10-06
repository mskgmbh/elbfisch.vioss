/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReadBits.java
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
public class ReadBits implements Request{
    private final static int PROTOCOLIDENTIFIER = 0x0000;
    private final static int UNITIDENTIFIER     = 0x01;
    private final static int LENGTHFIELD        = 0x0006;
    
    private Address address;
    private Data    data   ;

    private int     transactionIdentifier;
    
    public ReadBits(Address address){
        if (!address.getDataBlock().isBitAccess()){
            throw new IllegalArgumentException("bit access not allowed for datablock " + address.getDataBlock());
        }
        this.address = address;
        this.data    = new Data(new byte[address.getSize()]);
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

    protected void readData(Connection conn) throws IOException {
        int numberOfBytes = address.getSize();
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
        conn.getOutputStream().writeShort(getNextTransactionIdentifier());                                   //transaction id
        conn.getOutputStream().writeShort((short)PROTOCOLIDENTIFIER);                                        //protocol identifier (always 0x0000)
        conn.getOutputStream().writeShort((short)LENGTHFIELD);                                               //length field
        conn.getOutputStream().writeByte((byte)UNITIDENTIFIER);                                              //unit identifier (not used)        
        conn.getOutputStream().writeByte((byte)address.getDataBlock().getReadFunctionCode().getValue());     //function code
        conn.getOutputStream().writeShort((short)address.getStartingAddress());                              //address of the first bit
        conn.getOutputStream().writeShort((short)address.getQuantity());                                     //bit count for read access 1-2000
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
        if (functionCode != (byte)address.getDataBlock().getReadFunctionCode().getValue()){
            throw new IOException("exception received from modbus device over connection " + conn + " : function code = " + functionCode + ", exception code:" + byteCount);            
        }        
        if (byteCount != address.getSize()){
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
           ReadBits rwreq = new ReadBits(new Address(new DataBlock("PHYSICALCOILS/1"),0,8));
           long startTime;
           long stopTime;
           for (int i = 0; i < 10000; i++){
                startTime = System.nanoTime();
//TODO !!!!                rwreq.write(conn);
//TODO !!!!                rwreq.read(conn);
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
