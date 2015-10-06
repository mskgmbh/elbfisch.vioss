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

package org.jpac.vioss.modbus;

import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.AddressException;

/**
 * Implements the address of an MODBUS data item inside a data block
 */
public class Address extends org.jpac.plc.Address {
    private static final int  MAXCHUNKSIZE = 256;//byte

    private DataBlock dataBlock;
    private int       chunkOfData;      //256 byte block which can be fetched in one modbus transaction
    private int       startingAddress;  
    private int       quantity;

    /**
     * @param datablock data block to be accessed
     * @param startingAddress the number of the bit/register to be accessed (0 up to 0xFFFF)
     * @param quantity the number of bits/registers to access
     */
    public Address(DataBlock dataBlock, int startingAddress, int quantity) throws IndexOutOfRangeException, AddressException {
        super(0, 0, NA);
        this.dataBlock = dataBlock;
        if (startingAddress < 0 || startingAddress > 0x0000FFFF){
            throw new IndexOutOfRangeException();            
        }
        if (isBitAddress()){
            if (quantity < 1 || quantity > 2000){
                throw new IndexOutOfRangeException();            
            }
            setByteIndex(startingAddress / 8);
            setBitIndex(startingAddress % 8);
            setSize((quantity + 7)/8);
            this.startingAddress = startingAddress; //bit address
        } else if (isRegisterAddress()){
            if (quantity < 1 || quantity > 125){
                throw new IndexOutOfRangeException();            
            }
            setByteIndex(startingAddress * 2);
            setBitIndex(0);
            setSize(quantity * 2);
            this.startingAddress = startingAddress; //register address 
        }
        chunkOfData = getByteIndex() / MAXCHUNKSIZE;
    }

    /**
     * 
     * @return true, if the addressed dataBlock is to be accessed as bits 
     */
    public boolean isBitAddress(){
        return dataBlock.isBitAccess();
    }

    /**
     * 
     * @return true, if the addressed dataBlock is to be accessed as registers
     */
    public boolean isRegisterAddress(){
        return dataBlock.isRegisterAccess();
    }
    
    /**
     * Getter method for returning the area to read from / write to
     * @return enum representing the area to read from / write to
     */
    public DataBlock getDataBlock(){
        return this.dataBlock;
    }

    /**
     * @param dataBlock the dataBlock to set
     */
    public void setDataBlock(DataBlock dataBlock) {
        this.dataBlock = dataBlock;
    }
    
    /**
     * @return the startingAddress
     */
    public int getStartingAddress() {
        return startingAddress;
    }

    /**
     * @param startingAddress the startingAddress to set
     */
    public void setStartingAddress(int startingAddress) {
        this.startingAddress = startingAddress;
    }

    /**
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @param quantity the quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * @return the chunkOfData
     */
    public int getChunkOfData() {
        return chunkOfData;
    }

    /**
     * @param chunkOfData the chunkOfData to set
     */
    public void setChunkOfData(int chunkOfData) {
        this.chunkOfData = chunkOfData;
    }
}