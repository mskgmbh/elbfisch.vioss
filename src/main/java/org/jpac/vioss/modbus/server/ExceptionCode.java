/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ExceptionCode.java
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

package org.jpac.vioss.modbus.server;

import io.netty.buffer.ByteBuf;

/**
 * modbus function codes implemented in this package
 * @author berndschuster
 */
public enum ExceptionCode {
    UNDEFINED             (0x00),
    INVALIDFUNCTIONCODE   (0x01),
    INVALIDSIZE           (0x02),
    INVALIDADDRESS        (0x03),
    FUNCTIONFAILED        (0x04),
    NONE                  (0xFF);
    
    int ec; 
    
    ExceptionCode(int ec){
        this.ec = ec;
    }
    
    public int getValue(){
        return this.ec;
    }
    
    public void encode(ByteBuf byteBuf){
        byteBuf.writeByte(this.ec);
    }
    
    public int decode(ByteBuf byteBuf){
        this.ec = byteBuf.readByte();
        return ec;
    }    
}
