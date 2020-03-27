package org.jpac.vioss.raspi.i2c;

import java.io.IOException;

import org.jpac.plc.Address;
import org.jpac.plc.Data;

import com.pi4j.io.i2c.I2CDevice;

class Device {
	protected I2CDevice       i2cDevice;
	protected int             startOfReadAddressRange;
	protected int             endOfReadAddressRange;
	protected int             startOfWriteAddressRange;
	protected int             endOfWriteAddressRange;
	protected byte[]          readDataBuffer;
	protected byte[]          writeDataBuffer;
	protected byte[]          shadowWriteDataBuffer;
	protected Data.Endianness endianness;
	
	public Device(I2CDevice i2cDevice, Data.Endianness endianness) {
		this.i2cDevice                = i2cDevice;
		this.endianness               = endianness;
		this.startOfReadAddressRange  = Integer.MAX_VALUE;
		this.endOfReadAddressRange    = Integer.MIN_VALUE;
		this.startOfWriteAddressRange = Integer.MAX_VALUE;
		this.endOfWriteAddressRange   = Integer.MIN_VALUE;
	}
	
	public void addToReadAddressRange(Address address) {
		if (address.getByteIndex() < startOfReadAddressRange)
			startOfReadAddressRange = address.getByteIndex();
		if (address.getByteIndex() + address.getSize() > endOfReadAddressRange)
			endOfReadAddressRange = address.getByteIndex() + address.getSize() - 1;
	}

	public void addToWriteAddressRange(Address address) {
		if (address.getByteIndex() < startOfWriteAddressRange)
			startOfWriteAddressRange = address.getByteIndex();
		if (address.getByteIndex() + address.getSize() > endOfWriteAddressRange)
			endOfWriteAddressRange = address.getByteIndex() + address.getSize() - 1;
	}
	
	public void readWrite() throws IOException{
		if (i2cDevice != null) {
			//TODO block read/write operations did not work for a specific i2c device. This must be optimized
			for (int i = 0; i <  writeDataBuffer.length; i++) {
				if (writeDataBuffer[i] != shadowWriteDataBuffer[i]) {
					//write byte only, if changed to avoid side effects
//TODO					i2cDevice.write(i + startOfWriteAddressRange, writeDataBuffer[i]);
//					i2cDevice.write(writeDataBuffer[i]);
					shadowWriteDataBuffer[i] = writeDataBuffer[i];
				}
			}
//TODO			i2cDevice.read(startOfReadAddressRange, readDataBuffer, 0, readDataBuffer.length);
			readDataBuffer[0] = (byte)i2cDevice.read();
		} else {
			//application is not run on an raspberry pi
			//copy write data to read data inside overlapping address range for simulation purposes 
			int startAddressRange = startOfWriteAddressRange > startOfReadAddressRange ? startOfWriteAddressRange : startOfReadAddressRange;
			int endAddressRange   = endOfWriteAddressRange   <   endOfReadAddressRange ? endOfWriteAddressRange   : endOfReadAddressRange;
			for (int i = startAddressRange; i <= endAddressRange; i++) {
				getReadDataBuffer()[i - startOfReadAddressRange] = getWriteDataBuffer()[i - startOfWriteAddressRange];
			}
		}
	}

	/**
	 * called by IOHandler.prepare() after all associated signals have registered and address ranges settled
	 */
	public void allocateDataBuffers() {
		readDataBuffer        = new byte[endOfReadAddressRange  - startOfReadAddressRange + 1];
		writeDataBuffer       = new byte[endOfWriteAddressRange - startOfWriteAddressRange + 1];
		shadowWriteDataBuffer = new byte[endOfWriteAddressRange - startOfWriteAddressRange + 1];
	}
	
	public byte[] getReadDataBuffer() {
		return readDataBuffer;
	}

	public byte[] getWriteDataBuffer() {
		return writeDataBuffer;
	}

	public int getStartOfReadAddressRange() {
		return startOfReadAddressRange;
	}

	public int getEndOfReadAddressRange() {
		return endOfReadAddressRange;
	}

	public int getStartOfWriteAddressRange() {
		return startOfWriteAddressRange;
	}

	public int getEndOfWriteAddressRange() {
		return endOfWriteAddressRange;
	}
	
	public Data.Endianness getEndianness(){
		return this.endianness;
	}
}
