/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : modbus server WriteMultipleCoils.java (versatile input output subsystem)
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 *
 * This file is part of the jPac process automation controller.
 * jPac is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jPac is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpac.vioss.modbus.server;

import io.netty.buffer.ByteBuf;
import org.jpac.plc.Data;
import org.jpac.vioss.modbus.DataBlock;
import org.jpac.vioss.modbus.FunctionCode;

/**
 *
 * @author berndschuster
 */
public class WriteMultipleCoils extends Command{
    
    protected final int BUFFERSIZE = 512;
    
    protected DataBlock dataBlock;
    protected int       address;
    protected int       size;
    protected int       sizeInBytes;
    protected byte[]    buffer;
    
    public WriteMultipleCoils(DataBlock dataBlock){
        super(FunctionCode.WRITEMULTIPLECOILS);
        this.dataBlock = dataBlock;
        this.buffer    = new byte[BUFFERSIZE];
    }
    
    //server
    @Override
    public void decode(ByteBuf byteBuf){
        address     = byteBuf.readUnsignedShort();//bit address         // TODO: ULB: changed to unsigned: address     = byteBuf.readShort();
        size        = byteBuf.readUnsignedShort();//number of bits              // TODO: ULB: changed to unsigned: address     = byteBuf.readShort();
        sizeInBytes = byteBuf.readByte(); //number of bytes involved
        byteBuf.readBytes(buffer, 0, sizeInBytes);
        Log.debug("received FctCode: {}", this);
    }
    
    //server
    @Override
    public Acknowledgement handleRequest(CommandHandler commandHandler) {
        ExceptionCode excCode = ExceptionCode.NONE;
        try{
            synchronized (dataBlock) {
                Data data = dataBlock.getData();
                for (int i = address, sourceByteIndex = 0, bitCount = 0, b = buffer[sourceByteIndex]; i < address + size; i++) {
                    int byteIndex = i / 8;
                    int bitIndex  = i % 8;
                    data.setBIT(byteIndex, bitIndex, (b & 0x01) == 1);
                    b = b >> 1;
                    if ((bitCount < size -1) && ((++bitCount % 8) == 0)){ 
                       b = buffer[++sourceByteIndex];
                    }
                }
            }
        } catch(Exception exc){
            Log.error("Error: Failed to handle data:" + this);
            excCode = ExceptionCode.INVALIDADDRESS;
        }
       getAcknowledgement().setExceptionCode(excCode);
       getAcknowledgement().setAddress(address);
       getAcknowledgement().setSize(size);
       return getAcknowledgement();
    }
    
    @Override
    public String toString(){
        return getClass().getName() +"(" + address + ", " + size + ", " + sizeInBytes + ")";
    }
}
