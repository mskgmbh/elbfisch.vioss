/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Raspberry Pi
 * MODULE    : IoLogical.java (versatile input output subsystem)
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

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import java.net.URI;
import java.util.StringTokenizer;
import org.jpac.AbstractModule;
import org.jpac.plc.Address;
import org.jpac.InconsistencyException;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignalInvalidException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.IoDirection;

/**
 *
 * @author berndschuster
 */
public class IoLogical extends org.jpac.vioss.IoLogical {
    
    private Pin pin;

    public IoLogical(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
        setAddress(seizeAddress(uri));
        switch(ioDirection){
            case INPUT:
                pin  = (Pin)GpioFactory.getInstance().provisionDigitalInputPin(getRaspiPin((org.jpac.plc.Address)getAddress()), identifier);
                break;
            case OUTPUT:
                pin  = (Pin)GpioFactory.getInstance().provisionDigitalOutputPin(getRaspiPin((org.jpac.plc.Address)getAddress()), identifier);
                break;
            default:
                throw new WrongUseException("signal '" + getIdentifier() + "' must be either input or output");
        }
        //pin.setPullResistance(PinPullResistance.OFF);
        getIOHandler().registerInputSignal(this);        
    }  
    
    protected Address seizeAddress(URI uri) throws InconsistencyException{
        StringTokenizer tokenizer = new StringTokenizer(uri.getPath(), "/");
        Address         adr       = null;
        try{//uri : .../<pin>
            int pin       = Integer.parseInt(tokenizer.nextToken());
            if (pin < 0 || pin > 20){
                throw new InconsistencyException("illegal pin address specified in '" + uri.getPath() + "'");
            }
            int byteIndex = pin / 8;
            int bitIndex  = pin % 8;
            adr = new Address(byteIndex, bitIndex, 1);
        }
        catch(Exception exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "' : " + exc);
        }
        return adr; 
    }
    
    private Pin getRaspiPin(Address address){
        Pin pin = null;
        int pinNumber = address.getByteIndex() * 8 + address.getBitIndex();
        switch(pinNumber){
            case 0:
                pin = RaspiPin.GPIO_00;
                break;
            case 1:
                pin = RaspiPin.GPIO_01;
                break;
            case 2:
                pin = RaspiPin.GPIO_02;
                break;
            case 3:
                pin = RaspiPin.GPIO_03;
                break;
            case 4:
                pin = RaspiPin.GPIO_04;
                break;
            case 5:
                pin = RaspiPin.GPIO_05;
                break;
            case 6:
                pin = RaspiPin.GPIO_06;
                break;
            case 7:
                pin = RaspiPin.GPIO_07;
                break;
            case 8:
                pin = RaspiPin.GPIO_08;
                break;
            case 9:
                pin = RaspiPin.GPIO_09;
                break;
            case 10:
                pin = RaspiPin.GPIO_10;
                break;
            case 11:
                pin = RaspiPin.GPIO_11;
                break;
            case 12:
                pin = RaspiPin.GPIO_12;
                break;
            case 13:
                pin = RaspiPin.GPIO_13;
                break;
            case 14:
                pin = RaspiPin.GPIO_14;
                break;
            case 15:
                pin = RaspiPin.GPIO_15;
                break;
            case 16:
                pin = RaspiPin.GPIO_16;
                break;
            case 17:
                pin = RaspiPin.GPIO_17;
                break;
            case 18:
                pin = RaspiPin.GPIO_18;
                break;
            case 19:
                pin = RaspiPin.GPIO_19;
                break;
            case 20:
                pin = RaspiPin.GPIO_20;
                break;
        }
        return pin;
    }

    public Pin getPin(){
        return this.pin;
    }
    
    @Override
    public void checkIn() throws SignalAccessException, AddressException {
        try{
            inCheck = true;
            set(((GpioPinDigitalInput)pin).getState().isHigh());
        }
        finally{
            inCheck = false;
        }
    }

    @Override
    public void checkOut() throws SignalAccessException, AddressException{
        try{
            outCheck = true;
            try{((GpioPinDigitalOutput)pin).setState(is(true));}catch(SignalInvalidException exc){/*cannot happen*/}
        }
        finally{
            outCheck = false;
        }
    }
}
