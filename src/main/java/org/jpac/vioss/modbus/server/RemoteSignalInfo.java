/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : RemoteSignalInfo.java
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

import org.jpac.vioss.modbus.*;
import java.util.StringTokenizer;

import org.jpac.BasicSignalType;
import org.jpac.IoDirection;
import org.jpac.Signal;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.modbus.Iec61131Address.AccessMode;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo{
    
    private Signal          ioSignal;
    private DataBlock       assignedDataBlock;
    private Iec61131Address iec61131Address;
    private int             dataByteIndex;//byte index in org.jpac.plc.Data
    private int             dataBitIndex; //bit index in org.jpac.plc.Data    

    public RemoteSignalInfo(Signal ioSignal, DataBlock dataBlock) throws InvalidAddressSpecifierException{
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
        int iecByteIndex       = 0;
        int iecItemSize        = 0;
        this.ioSignal          = ioSignal;
        this.assignedDataBlock = dataBlock;
        StringTokenizer pathTokens = new StringTokenizer(((IoSignal)ioSignal).getUri().toString(),":");
        if (pathTokens.countTokens() != 2) {
            throw new InvalidAddressSpecifierException("Error: uri must contain both a scheme and an IEC 61131 address specifier: " + ((IoSignal)ioSignal).getUri());
        }
        pathTokens.nextToken();//skip scheme
        String iecAddressSpecifier = pathTokens.nextToken();

        this.iec61131Address = new Iec61131Address(iecAddressSpecifier);
        if ((this.iec61131Address.getAccessMode() == AccessMode.INPUT  && ((IoSignal)ioSignal).getIoDirection() != IoDirection.INPUT) ||
            (this.iec61131Address.getAccessMode() == AccessMode.OUTPUT && ((IoSignal)ioSignal).getIoDirection() != IoDirection.OUTPUT)  ){
            throw new InvalidAddressSpecifierException("Error: IoDirection of IoSignal must match IEC61131 address specifier: " + ((IoSignal)ioSignal).getUri());    			
        }
        //provide data byte and bit index for accessing org.jpac.plc.Data
        switch(iec61131Address.getType()) {
            case BIT:
                    iecByteIndex  = 2 * iec61131Address.getAddress();
                    iecItemSize   = 2;
                    dataByteIndex = iecByteIndex - assignedDataBlock.getAddress();//address is byte address. // TODO: ULB: original //address is word address.
                    dataBitIndex  = iec61131Address.getBitAddress();
                    if (dataBitIndex > 7) {
                        dataBitIndex -= 8;
                        dataByteIndex++;
                    }
                    break;
            case BYTE:
                    iecByteIndex  = iec61131Address.getAddress();
                    iecItemSize   = 1;
                    dataByteIndex = iecByteIndex - assignedDataBlock.getAddress(); 
                    dataBitIndex  = 0;
                    break;			
            case WORD:
                    iecByteIndex  = 2 * iec61131Address.getAddress();
                    iecItemSize   = 2;
                    dataByteIndex = iecByteIndex - assignedDataBlock.getAddress();
                    dataBitIndex  = 0;
                    break;
            case DWORD:
                    iecByteIndex  = 4 * iec61131Address.getAddress();
                    iecItemSize   = 4;
                    dataByteIndex = iecByteIndex - assignedDataBlock.getAddress();
                    dataBitIndex  = 0;
                    break;
            default:
                    //cannot happen
                    break;
        }
        if (iecByteIndex < assignedDataBlock.getAddress() || (iecByteIndex + iecItemSize) > (assignedDataBlock.getAddress() + assignedDataBlock.getSize())){ 
            throw new InvalidAddressSpecifierException("signal does not fit into assigned datablock:" + ioSignal); 
        }
        Log.debug(ioSignal + " assigned to " + assignedDataBlock + " dataByteIndex = " + dataByteIndex + " databitIndex = " + dataBitIndex + ")");
    }
    
    public Iec61131Address getIec61131Address() {
    	return this.iec61131Address;
    }
    
    public DataBlock getAssignedDataBlock() {
    	return this.assignedDataBlock;
    }

    public int getDataByteIndex() {
    	return this.dataByteIndex;
    }

    public int getDataBitIndex() {
    	return this.dataBitIndex;
    }
}
