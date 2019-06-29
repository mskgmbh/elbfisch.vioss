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

package org.jpac.vioss.raspi.gpio;

import java.net.URI;
import java.util.StringTokenizer;

import org.jpac.BasicSignalType;
import org.jpac.InconsistencyException;
import org.jpac.Signal;
import org.jpac.WrongUseException;
import org.jpac.vioss.IoSignal;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;

public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo {
	final static String PINPULLRESISTANCE       = "pinpullresistance";
	final static String PULLRESISTANCEOFF       = "off";
	final static String PULLRESISTANCEPULLDOWN  = "pulldown";
	final static String PULLRESISTANCEPULLUP    = "pullup";
	
	protected GpioPinDigital gpioPinDigital;

	public RemoteSignalInfo() {
		super();
		this.gpioPinDigital = null;
	}

	public RemoteSignalInfo(Signal ioSignal) {
		super(ioSignal.getQualifiedIdentifier(), BasicSignalType.fromSignal(ioSignal));
		IoSignal ios = (IoSignal)ioSignal;
        switch(ios.getIoDirection()){
            case INPUT:
                gpioPinDigital = GpioFactory.getInstance().provisionDigitalInputPin(seizePin(ios.getUri()), identifier, getPinPullResistance(ios));
                System.out.println("pinpullresistance: " + gpioPinDigital.getPullResistance());
                break;
            case OUTPUT:
                gpioPinDigital = GpioFactory.getInstance().provisionDigitalOutputPin(seizePin(ios.getUri()), identifier);
                break;
            default:
                throw new WrongUseException("signal '" + getIdentifier() + "' must be either input or output");
        }
	}

    protected Pin seizePin(URI uri) throws InconsistencyException{
        StringTokenizer tokenizer = new StringTokenizer(uri.getPath(), "/");
        Pin pin = null;
        try{//uri : .../<pin>
            int pinIndex       = Integer.parseInt(tokenizer.nextToken());
            if (pinIndex < 0 || pinIndex > 20){
                throw new InconsistencyException("illegal pin address specified in '" + uri.getPath() + "'");
            }
            pin = RaspiPin.getPinByAddress(pinIndex);
        }
        catch(Exception exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "' : " + exc);
        }
        return pin;
    }  
    
    protected PinPullResistance getPinPullResistance(IoSignal ios) {
    	PinPullResistance ppr;
    	if (ios.getParameters().get(PINPULLRESISTANCE) == null) {
    		ppr = null;
    	} else if (ios.getParameters().get(PINPULLRESISTANCE).equals(PULLRESISTANCEOFF)) {
    		ppr = PinPullResistance.PULL_DOWN;    		
    	} else if (ios.getParameters().get(PINPULLRESISTANCE).equals(PULLRESISTANCEPULLDOWN)) {
    		ppr = PinPullResistance.PULL_DOWN;    		
    	} else if (ios.getParameters().get(PINPULLRESISTANCE).equals(PULLRESISTANCEPULLUP)) {
    		ppr = PinPullResistance.PULL_UP;    		    		
    	} else {
    		throw new InconsistencyException("illegal value for pinpullresistance specified: '" + ios.getParameters().get(PINPULLRESISTANCE) + "'. Must be 'off', 'pulldown' or 'pullup");
    	}
    	return ppr;
    }
    
	public GpioPinDigital getGpioPinDigital() {
		return gpioPinDigital;
	}
	
	
}
