/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Modbus server Acknowledgement.java (versatile input output subsystem)
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

import org.jpac.vioss.modbus.FunctionCode;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class Acknowledgement implements Message{
    protected Logger  Log = LoggerFactory.getLogger("jpac.vios.modbus.server");
    
    protected FunctionCode  functionCode;
    protected ExceptionCode exceptionCode;
    protected int           address;
    protected int           size;
    protected int           sizeInBytes;
    protected byte[]        buffer;
    
    public Acknowledgement(FunctionCode messageId){
        this.functionCode  = messageId;
        this.exceptionCode = ExceptionCode.NONE;
    }   
    
    public FunctionCode getFunctionCode(){
        return functionCode;
    }

    @Override
    public void encode(ByteBuf targetByteBuf) {
        functionCode.encode(targetByteBuf, exceptionCode != ExceptionCode.NONE);
    }
    
    @Override
    public void decode(ByteBuf sourceByteBuf) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }    

    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(int address) {
        this.address = address;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the exceptionCode
     */
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }

    /**
     * @param exceptionCode the exceptionCode to set
     */
    public void setExceptionCode(ExceptionCode exceptionCode) {
        this.exceptionCode = exceptionCode;
    }
    
/**
     * @return the buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }    
    
     /**
     * @return the sizeInBytes
     */
    public int getSizeInBytes() {
        return sizeInBytes;
    }

    /**
     * @param sizeInBytes the sizeInBytes to set
     */
    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }  
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + exceptionCode + ")";
    }    


}
