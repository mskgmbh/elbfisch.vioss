/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Address.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : represents the address of a modbus data item
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
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

package org.jpac.vioss.ads;

import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.AddressException;

/**
 * Implements the address of an MODBUS data item inside a data block
 */
public class Address extends org.jpac.plc.Address {

    private IndexGroup indexGroup;

    /**
     * @param datablock data block to be accessed
     * @param indexOffset the number of the bit/register to be accessed (0 up to 0xFFFF)
     * @param length the number bytes to access
     */
    public Address(IndexGroup indexGroup, int indexOffset, int length) throws IndexOutOfRangeException, AddressException {
        super(0, 0, NA);
        this.indexGroup = indexGroup;
        if (indexOffset < 0 || indexOffset > 0x0000FFFF){
            throw new IndexOutOfRangeException();            
        }
        setByteIndex(indexOffset);
        setBitIndex(Address.NA);
        setSize(length);
    }
    
    /**
     * Getter method for returning the area to read from / write to
     * @return enum representing the area to read from / write to
     */
    public IndexGroup getIndexGroup(){
        return this.indexGroup;
    }
    
    public void setIndexGroup(IndexGroup indexGroup){
        this.indexGroup = indexGroup;
    }
}