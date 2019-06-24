/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : ProcessImageItem.java
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

package org.jpac.vioss.revpi;

import com.fasterxml.jackson.databind.JsonNode;

import org.jpac.IoDirection;
import org.jpac.plc.Address;
import org.jpac.plc.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class ProcessImageItem {
    protected static Logger Log = LoggerFactory.getLogger("jpac.vioss.revpi");
    
    protected final String IDENTIFIER = "/0";
    protected final String SIZEINBITS = "/2";
    protected final String BYTEOFFSET = "/3";
    protected final String BITOFFSET  = "/7";
    
    protected String      identifier;
    protected Device      device;
    protected Address     address;
    protected IoDirection ioDirection;
    protected Data        data;
    
    public ProcessImageItem(Device device, int imageOffset, JsonNode itemNode, IoDirection ioDirection, Data data){
        this.device      = device;
        this.ioDirection = ioDirection;
        this.identifier  = itemNode.at(IDENTIFIER).asText();
        this.data        = data;
        
        int  byteOffset   = Integer.parseInt(itemNode.at(BYTEOFFSET).textValue()) - imageOffset;
        int  bitOffset    = !itemNode.at(BITOFFSET).textValue().equals("") ? Integer.parseInt(itemNode.at(BITOFFSET).textValue()) : 0;
        if (bitOffset > 15){
            throw new IndexOutOfBoundsException(identifier + ": bit index must be in the range of 0..15");
        }
        if (bitOffset > 7){
            byteOffset++;
            bitOffset -= 8;
        }
        int  size       = Integer.parseInt(itemNode.at(SIZEINBITS).textValue())/8;
        this.address    = new Address(byteOffset, size > 0 ? Address.NA : bitOffset, size == 0 ? Address.NA : size);
    }

    public ProcessImageItem(String identifier, Device device, Data data, Address address){
        this.identifier = identifier;
        this.device     = device;
        this.data       = data;
        this.address    = address;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Device getDevice() {
        return device;
    }
    
    public Data getData(){
        return this.data;
    }

    public Address getAddress() {
        return address;
    }
    
    public IoDirection getIoDirection(){
        return this.ioDirection;
    }
        
    @Override
    public String toString(){
        return this.getClass().getCanonicalName() + "(" + device.getIdentifier() + ", " + identifier + ", " + address + ")";
    }

}
