/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IoByteArray.java (versatile input output subsystem)
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

package org.jpac.vioss.mqttv3;

import org.jpac.Signal;

import java.util.function.Supplier;

import org.jpac.AbstractModule;
import org.jpac.IoDirection;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyConnectedException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignalInvalidException;
import org.jpac.Generic;
import org.jpac.Value;

public class ByteArray extends Generic<byte[]> {
    private ByteArrayValue wrapperValue;
/**
     * constructs a ByteArray signal
     * @param containingModule: module, this signal is contained in
     * @param identifier: identifier of the signal
     * @param ioDirection: defines the signal as being either an INPUT or OUTPUT signal. (Relevant in distributed applications)
     * @throws org.jpac.SignalAlreadyExistsException
     */
    public ByteArray(AbstractModule containingModule, String identifier, IoDirection ioDirection) throws SignalAlreadyExistsException{
        super(containingModule, identifier, ioDirection);
        this.wrapperValue       = new ByteArrayValue();
    }

/**
     * used to set the byte array to the given value
     * @param value: value, the byte array is set to
     * @throws org.jpac.SignalAccessException when a given module is not allowed to set the signal
     */
    public void set(byte[] value) throws SignalAccessException{
        synchronized(this){
            wrapperValue.set(value);
            wrapperValue.setValid(true);            
            setValue(wrapperValue);
        }
    }
    
    /**
     * used to set the ByteArray from any thread, which is not a module and not the jPac thread
     * The value is changed synchronized to the jPac cycle
     * @param value: value, the ByteArray is set to
     */
    public void setDeferred(byte[] value){
        ByteArrayValue localWrapperValue = new ByteArrayValue();
        localWrapperValue.set(value);
        localWrapperValue.setValid(true);
        setValueDeferred(localWrapperValue);
    }
    

    /**
     * returns the value of the ByteArray. If the calling module is the containing module the value of this signal is returned.
     * If the calling module is a foreign module the propagated signal is returned.
     * @return see above
     * @throws org.jpac.SignalInvalidException if the valid is not valid in the case of accessing it.
     */
    public byte[] get() throws SignalInvalidException{
        return ((ByteArray)getValidatedValue()).get();
    }
    
    /**
     * used to set the intrinsic function of this signal.
     * @param intrinsicFunction 
     */
    public void setIntrinsicFunction(Supplier<byte[]> intrinsicFunction){
        setIntrinsicFct(intrinsicFunction);
    }

    // /**
    //  * returns a process event (CharStringChanges), which is fired, if the char string changes
    //  */    
    // public CharStringChanges changes(){
    //     return new CharStringChanges(this);
    // }

    /**
     * used to connect this char string to another char string. One char string can be connected
     * to multiple char strings.
     * The connection is unidirectional: Changes of the connecting signal (sourceSignal) will be
     * propagated to the signals it is connected to (targetSignal): sourceSignal.connect(targetSignal).
     * @param targetSignal
     * @throws org.jpac.SignalAlreadyConnectedException
     */
    public void connect(ByteArray targetSignal) throws SignalAlreadyConnectedException{
        super.connect(targetSignal);
    }

    @Override
    protected void deferredConnect(Signal targetSignal) throws SignalAlreadyConnectedException{
        super.deferredConnect(targetSignal);
    }

    @Override
    protected void deferredDisconnect(Signal targetSignal){
        super.deferredDisconnect(targetSignal);
    }
    

    @Override
    protected boolean isCompatibleSignal(Signal signal) {
        return signal instanceof ByteArray;
    }

    @Override
    protected void propagateSignalInternally() {
        //physically copy the value to the propagated value
        ((ByteArrayValue)getPropagatedValue()).copy((ByteArrayValue)getValue());
    }

    @Override
    protected void updateValue(Signal o) throws SignalAccessException {
        try{
            if (o instanceof ByteArray){
               set(((ByteArray)o).get());
            }
        }
        catch(Exception exc){
            //Log.error("Error: ", exc);
            throw new SignalAccessException(exc.getMessage());
        }
    }


    @Override
    protected Value getTypedValue() {
    	return new ByteArrayValue();
    	//return new <Generic<ValueImpl>()>;
    }    
}
