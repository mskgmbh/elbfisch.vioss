/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : modbus server  ReadHoldingRegisters.java (versatile input output subsystem)
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
import org.jpac.vioss.modbus.DataBlock;
import org.jpac.vioss.modbus.FunctionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class ReadHoldingRegisters extends Command{
    private final Logger Log = LoggerFactory.getLogger("jpac.vios.modbus.server");
    
    protected final int BUFFERSIZE = 512;
    
    protected DataBlock dataBlock;
    protected int       address;
    protected int       size;
    protected int       sizeInBytes;
    protected byte[]    buffer;    
    
    public ReadHoldingRegisters(DataBlock datablock){
        super(FunctionCode.READHOLDINGREGISTERS);
        this.dataBlock = datablock;
    }
    
    //server
    @Override
    public void decode(ByteBuf byteBuf){
        super.decode(byteBuf);
        address     = byteBuf.readShort();
        size        = byteBuf.readShort();
        sizeInBytes = byteBuf.readByte();
    }
    
    //server
    @Override
    public Acknowledgement handleRequest(CommandHandler commandHandler) {
        ExceptionCode excCode = ExceptionCode.NONE;
        try{
            synchronized (dataBlock) {
                int addr = dataBlock.getAddress() - 2 * address;
                System.arraycopy(dataBlock.getData().getBytes(), addr, buffer, 0, sizeInBytes);
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
}
