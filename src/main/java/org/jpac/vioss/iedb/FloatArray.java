/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : FloatArray.java (versatile input output subsystem)
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

package org.jpac.vioss.iedb;

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

public class FloatArray extends Generic<float[]> {
    private FloatArrayValue wrapperValue;
/**
     * constructs a FloatArray signal
     * @param containingModule: module, this signal is contained in
     * @param identifier: identifier of the signal
     * @param ioDirection: defines the signal as being either an INPUT or OUTPUT signal. (Relevant in distributed applications)
     * @throws org.jpac.SignalAlreadyExistsException
     */
    public FloatArray(AbstractModule containingModule, String identifier, IoDirection ioDirection) throws SignalAlreadyExistsException{
        super(containingModule, identifier, ioDirection);
        this.wrapperValue       = new FloatArrayValue();
    }

/**
     * used to set the float array to the given value
     * @param value: value, the float array is set to
     * @throws org.jpac.SignalAccessException when a given module is not allowed to set the signal
     */
    public void set(float[] value) throws SignalAccessException{
        synchronized(this){
            wrapperValue.set(value);
            wrapperValue.setValid(true);            
            setValue(wrapperValue);
        }
    }
    
    /**
     * used to set the FloatArray from any thread, which is not a module and not the jPac thread
     * The value is changed synchronized to the jPac cycle
     * @param value: value, the FloatArray is set to
     */
    public void setDeferred(float[] value){
        FloatArrayValue localWrapperValue = new FloatArrayValue();
        localWrapperValue.set(value);
        localWrapperValue.setValid(true);
        setValueDeferred(localWrapperValue);
    }


    /**
     * returns the value of the FloatArray. If the calling module is the containing module the value of this signal is returned.
     * If the calling module is a foreign module the propagated signal is returned.
     * @return see above
     * @throws org.jpac.SignalInvalidException if the valid is not valid in the case of accessing it.
     */
    public float[] get() throws SignalInvalidException{
        return ((FloatArray)getValidatedValue()).get();
    }
    
    /**
     * used to set the intrinsic function of this signal.
     * @param intrinsicFunction 
     */
    public void setIntrinsicFunction(Supplier<float[]> intrinsicFunction){
        setIntrinsicFct(intrinsicFunction);
    }

    // /**
    //  * returns a process event (CharStringChanges), which is fired, if the char string changes
    //  */    
    // public CharStringChanges changes(){
    //     return new CharStringChanges(this);
    // }

    /**
     * used to connect this float array to another float array. One float array can be connected
     * to multiple float arrays.
     * The connection is unidirectional: Changes of the connecting signal (sourceSignal) will be
     * propagated to the signals it is connected to (targetSignal): sourceSignal.connect(targetSignal).
     * @param targetSignal
     * @throws org.jpac.SignalAlreadyConnectedException
     */
    public void connect(FloatArray targetSignal) throws SignalAlreadyConnectedException{
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
        return signal instanceof FloatArray;
    }

    @Override
    protected void propagateSignalInternally() {
        //physically copy the value to the propagated value
        ((FloatArrayValue)getPropagatedValue()).copy((FloatArrayValue)getValue());
    }

    @Override
    protected void updateValue(Signal o) throws SignalAccessException {
        try{
            if (o instanceof FloatArray){
               set(((FloatArray)o).get());
            }
        }
        catch(Exception exc){
            //Log.error("Error: ", exc);
            throw new SignalAccessException(exc.getMessage());
        }
    }


    @Override
    protected Value getTypedValue() {
    	return new FloatArrayValue();
    }    
}
