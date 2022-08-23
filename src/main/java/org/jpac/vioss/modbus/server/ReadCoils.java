/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : modbus server ReadCoils.java (versatile input output subsystem)
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
import java.util.Arrays;
import org.jpac.plc.Data;
import org.jpac.vioss.modbus.DataBlock;
import org.jpac.vioss.modbus.FunctionCode;

/**
 *
 * @author berndschuster
 */
public class ReadCoils extends Command{
    
    protected final int    BUFFERSIZE = 512;
    protected final byte[] MASK = {(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};
    
    protected DataBlock dataBlock;
    protected int       address;
    protected int       size;
    protected int       sizeInBytes;
    protected byte[]    buffer;
    
    public ReadCoils(DataBlock dataBlock){
        super(FunctionCode.READCOILS);
        this.dataBlock = dataBlock;
        this.buffer    = new byte[BUFFERSIZE];
    }
    
    //server
    @Override
    public void decode(ByteBuf byteBuf){
        address     = byteBuf.readShort();//bit address
        size        = byteBuf.readShort();//number of bits
        sizeInBytes = byteBuf.readByte(); //number of bytes involved
        Log.debug("received FctCode: {}", this);
    }
    
    //server
    @Override
    public Acknowledgement handleRequest(CommandHandler commandHandler) {
        ExceptionCode excCode = ExceptionCode.NONE;
        try{
            Data data = dataBlock.getData();
            Arrays.fill(buffer, (byte)0);
            synchronized (dataBlock) {
                for (int i = address, targetByteIndex = -1, targetBitIndex = -1; i < address + size; i++) {
                    int sourceByteIndex = i / 8;
                    int sourceBitIndex  = i % 8;
                    targetBitIndex      = ++targetBitIndex % 8;
                    if (targetBitIndex == 0) targetByteIndex++;
                    buffer[targetByteIndex] = data.getBIT(sourceByteIndex, sourceBitIndex) ? (byte)(buffer[targetByteIndex] | (byte)MASK[targetBitIndex]) : (byte)(buffer[targetByteIndex] & (MASK[targetBitIndex] ^ (byte)0xFF));
                }
            }
        } catch(Exception exc){
            Log.error("Error: Failed to handle data:" + this);
            excCode = ExceptionCode.INVALIDADDRESS;
        }
       getAcknowledgement().setExceptionCode(excCode);
       getAcknowledgement().setAddress(address);
       getAcknowledgement().setSize(size);
       getAcknowledgement().setSizeInBytes(sizeInBytes);
       getAcknowledgement().setBuffer(buffer);
       return getAcknowledgement();
    }
    
    @Override
    public String toString(){
        return getClass().getName() +"(" + address + ", " + size + ", " + sizeInBytes + ")";
    }
}
