/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Mbap.java (versatile input output subsystem)
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

/**
 *
 * @author berndschuster
 */
public class Mbap implements Message {
    protected byte[] mbap = new byte[7];
    
    @Override
    public void encode(ByteBuf targetByteBuf) {
       for (int i = 0; i < 7; i++) {
            targetByteBuf.writeByte(mbap[i]);
        }
    }

    @Override
    public void decode(ByteBuf sourceByteBuf) {
        for (int i = 0; i < 7; i++) {
            mbap[i] = sourceByteBuf.readByte();
        }
    }
    
    @Override
    public String toString(){
        return this.getClass().getName() + "(" + mbap[0] + ", " + mbap[1] + ", " + mbap[2] + ", " + mbap[3] + ", " + mbap[4] + ", " + mbap[5] + ", "+ mbap[6] + ")";
    }
    
}
