package org.jpac.vioss.raspi.i2c;

import java.util.HashMap;
import com.pi4j.io.i2c.I2CBus;

class Bus {
	protected I2CBus i2cBus;
	protected HashMap<Integer, Device> devices;
	
	public Bus(I2CBus i2cBus) {
		this.i2cBus  = i2cBus;
		this.devices = new HashMap<>();
	}

	public I2CBus getI2CBus() {
		return this.i2cBus;
	}
	
	public HashMap<Integer, Device> getDevices() {
		return this.devices;
	}
}
