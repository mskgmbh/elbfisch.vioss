/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : modbus server ReadCoilsAcknowledgement.java (versatile input output subsystem)
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
import org.jpac.vioss.modbus.FunctionCode;

/**
 *
 * @author berndschuster
 */
public class ReadCoilsAcknowledgement extends Acknowledgement{
    
    public ReadCoilsAcknowledgement(){
        super(FunctionCode.READCOILS);
    }
    

    @Override
    public void setBuffer(byte[] buffer){
        this.buffer = buffer;
    }    
     //server
    @Override
    public void encode(ByteBuf byteBuf){
        super.encode(byteBuf);
        if (this.exceptionCode == ExceptionCode.NONE){
            // byteBuf.writeShort(address); // TODO: ULB: address is not returned by protocol in ReadCoils-Response
            byteBuf.writeByte(sizeInBytes); // TODO: ULB: it is not the number of bits returned here but the number of bytes
            byteBuf.writeBytes(buffer, 0, sizeInBytes);
        }
        else{
           this.exceptionCode.encode(byteBuf);
        }
    }

    @Override
    public int getDataSizeInBytesForMBAP() {
        if (this.exceptionCode == ExceptionCode.NONE){
            return sizeInBytes + 2; // 1 Byte Function code + 1 Byte returned data length in bytes 
        }
        else {
            return 2; // 1 Byte Function/error code + 1 Byte exception code
        }
    }
}