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

package org.jpac.vioss.raspi.i2c;

import java.io.IOException;
import java.util.StringTokenizer;

import org.jpac.BasicSignalType;
import org.jpac.InconsistencyException;
import org.jpac.ProcessException;
import org.jpac.Signal;
import org.jpac.WrongUseException;
import org.jpac.plc.Address;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.vioss.IoSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo {
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.raspi");

	final protected String    ENDIANESS     = "endianess";
	final protected String    BIGENDIAN     = "bigendian";
	final protected String    LITTLEENDIAN  = "littleendian";
	final protected String    NOTAPPLICABLE = "na";
	
	protected I2CBus    	  i2cBus;
	protected I2CDevice 	  i2cDevice;
	protected Device    	  device;
	protected Address   	  address;
	protected Data            readData;
	protected Data            writeData;
	protected Data.Endianness endianness;
	protected Signal    	  ioSignal;

	public RemoteSignalInfo() {
		super();
	}

	public RemoteSignalInfo(Signal ioSignal) {		
		super(ioSignal.getQualifiedIdentifier(), BasicSignalType.fromSignal(ioSignal));
		this.ioSignal   = ioSignal;
		this.endianness = null;
		this.readData   = null; //will be instantiated write before first access
		this.writeData  = null; //will be instantiated write before first access
		
		IoSignal ios   = (IoSignal)ioSignal;
		
		int     i2cBusIndex, i2cDeviceAddress, dataSize, dataByteAddress, dataBitAddress = 0;
		
        StringTokenizer tokenizer = new StringTokenizer(ios.getPath(), "/");
        try{//URI: "pi.i2c:/<bus>/<device>/<datasize>/<byte address>[/<bit address>][?endianess=bigendian | littleendian]"
            i2cBusIndex = parseIntAsDecOrHex(tokenizer.nextToken());
            if (i2cBusIndex < 0 || i2cBusIndex > 17){
                throw new WrongUseException("illegal i2c bus specified in '" + ios.getPath() + "'. Must be 0.. 17 (dec)");
            }
            i2cDeviceAddress  = parseIntAsDecOrHex(tokenizer.nextToken()) & 0xFF;//take 1 unsigned byte as the device address
            String dataSizeToken = tokenizer.nextToken();
            if (dataSizeToken.trim().equals(NOTAPPLICABLE)) {
            	dataSize = Address.NA;
            } else {
                dataSize = parseIntAsDecOrHex(dataSizeToken) & 0xFF;//take 1 unsigned byte as data size            	
                if (dataSize < 1 || dataSize > 4){
                    throw new WrongUseException("illegal data size specified in '" + ios.getPath() + "'. Must be 1..4 [byte]");
                }
            }
            dataByteAddress = parseIntAsDecOrHex(tokenizer.nextToken()) & 0xFF;//take 1 unsigned byte as the byte address            	
            if (type.equals(BasicSignalType.Logical)) {
            	dataBitAddress = parseIntAsDecOrHex(tokenizer.nextToken()) & 0xFF;//take 1 unsigned byte as the bit address
                if (dataBitAddress > 8 || dataSize != Address.NA){
                    throw new WrongUseException("illegal address specified in '" + ios.getPath() + "'. data size must be 'na' and bit address must be 1..8 ");
                }
            }
            String endianessValue = ios.getParameters().get(ENDIANESS);
            if (endianessValue != null){
            	if (endianessValue.equals(BIGENDIAN) || endianessValue.equals(LITTLEENDIAN)) {
            		this.endianness = ios.getParameters().get(ENDIANESS).equals(BIGENDIAN) ? Data.Endianness.BIGENDIAN: Data.Endianness.LITTLEENDIAN;
            	} else {
            		throw new WrongUseException(ioSignal + ": value of parameter 'endianess' must be either " + BIGENDIAN + " or " + LITTLEENDIAN);
            	}
            } else {
            	this.endianness = Data.Endianness.BIGENDIAN;
            }
        	if (((IOHandler)ios.getIOHandler()).isRunningOnRaspi()) {
		        i2cBus    = I2CFactory.getInstance(i2cBusIndex);    
		        i2cDevice = i2cBus.getDevice(i2cDeviceAddress);
        	} else {
        		i2cBus    = null;
        		i2cDevice = null;
        	}
	        address   = new Address(dataByteAddress, type == BasicSignalType.Logical ? dataBitAddress : Address.NA, dataSize);
	        if (!((IOHandler)ios.getIOHandler()).getBusses().containsKey(i2cBusIndex)) {
	        	((IOHandler)ios.getIOHandler()).getBusses().put(i2cBusIndex, new Bus(i2cBus));
	        } 
	        if (!((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().containsKey(i2cDeviceAddress)) {
	        	((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().put(i2cDeviceAddress, new Device(i2cDevice, endianness));
	        }
	        device = ((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().get(i2cDeviceAddress);
	        
	        switch (ios.getIoDirection()) {
	        	case INPUT:
	        		((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().get(i2cDeviceAddress).addToReadAddressRange(address);
	        		break;
	        	case OUTPUT:
	        		((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().get(i2cDeviceAddress).addToWriteAddressRange(address);
	        		break;
	        	case INOUT:
	        		((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().get(i2cDeviceAddress).addToReadAddressRange(address);
	        		((IOHandler)ios.getIOHandler()).getBusses().get(i2cBusIndex).getDevices().get(i2cDeviceAddress).addToWriteAddressRange(address);
	        	default:
	        }
        } catch(UnsupportedBusNumberException | IOException | WrongUseException exc) {
            throw new InconsistencyException("illegal address specification in '" + ios.getUri() + "' : " + exc);        	
        } 
	}
	
	public int parseIntAsDecOrHex(String intStr) throws NumberFormatException{
		Integer value = null;
		//test, if value is decimal
		try {value = Integer.parseInt(intStr, 10);}catch(NumberFormatException exc) {};
		if (value == null) {
			//if not, try if its hex
			value = Integer.parseInt(intStr, 16);			
		}
		return value;
	}
	
	public void pushValueToDevice() {
		try {
			switch(getType()) {
				case Logical:
					writeData.setBIT(address.getByteIndex(), address.getBitIndex(), (boolean)value.getValue());
					break;
				case SignedInteger:
					switch(address.getSize()) {
						case 1:
							writeData.setBYTE(address.getByteIndex() - device.getStartOfWriteAddressRange(), (int)getValue().getValue() & 0xFF );
							break;
						case 2:
							writeData.setINT(address.getByteIndex() - device.getStartOfWriteAddressRange(), (int)getValue().getValue());
							break;
						case 4:
							writeData.setDINT(address.getByteIndex() - device.getStartOfWriteAddressRange(), (int)getValue().getValue());
							break;
					}
					break;
				default:
					throw new WrongUseException("type not handled by " + ((IoSignal)ioSignal).getIOHandler());
			}
		} catch(AddressException | ValueOutOfRangeException exc) {
			throw new ProcessException(exc);
		}
	}

	public void pullValueFromDevice() {
		try {
			switch(getType()) {
				case Logical:
					getValue().setValue(readData.getBIT(address.getByteIndex(), address.getBitIndex()));
					break;
				case SignedInteger:
					switch(address.getSize()) {
						case 1:
							getValue().setValue(readData.getBYTE(address.getByteIndex() - device.getStartOfReadAddressRange())  & 0xFF);
							break;
						case 2:
							getValue().setValue(readData.getINT(address.getByteIndex() - device.getStartOfReadAddressRange()));
							break;
						case 4:
							getValue().setValue(readData.getDINT(address.getByteIndex() - device.getStartOfReadAddressRange()));
							break;
					}
					break;
				default:
					throw new WrongUseException("type not handled by " + ((IoSignal)ioSignal).getIOHandler());
			}
			//value is valid after successful read from device
			getValue().setValid(true);			
		} catch(AddressException exc) {
			throw new ProcessException(exc);
		}
	}

	public void assignDevicesReadBuffer() {
		readData = new Data(device.readDataBuffer, endianness);
	}
	
	public void assignDevicesWriteBuffer() {
		writeData = new Data(device.writeDataBuffer, endianness);
	}

	public I2CBus getI2cBus() {
		return i2cBus;
	}

	public I2CDevice getI2cDevice() {
		return i2cDevice;
	}

	public Address getAddress() {
		return address;
	}
}
