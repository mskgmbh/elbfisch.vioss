/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Raspberry Pi
 * MODULE    : IOHandler.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : -
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : -
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

package org.jpac.vioss.raspi.gpio;

import java.net.URI;
import org.jpac.ProcessException;
import org.jpac.Signal;
import org.jpac.SignalInvalidException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.jpac.IoDirection;
import org.jpac.LogicalValue;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{

    final static String HANDLEDSCHEME       = "PI.GPIO";
                
    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException {
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
    }
    
    @Override
    public boolean handles(URI uri, IoDirection ioDirection) {
        boolean isHandledByThisInstance = false;
        try{
            isHandledByThisInstance  = uri != null;
            isHandledByThisInstance &= this.getUri().getScheme().equals(uri.getScheme());
        }
        catch(Exception exc){};
        return isHandledByThisInstance;
    }
    
    @Override
    public void prepare(){
        for (Signal is: getInputSignals()){
        	IoSignal ioSig = (IoSignal)is;
        	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(is));
        }
        for (Signal os: getOutputSignals()){
        	IoSignal ioSig = (IoSignal)os;
        	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(os));
        }
        setProcessingStarted(true);
        Log.info("starting up " + this);
    }

    @Override
    public void stop(){
        Log.info("shutting down ..." + this);
        setProcessingAborted(true);
    }
        
    @Override
    public void run(){
        try{
            if (!isProcessingAborted()){
                inputOutputProcessImage();
            }
        }
        catch(Error exc){
            Log.error("Error: ", exc);
            Log.error("processing aborted for IOHandler " + this);
            //abort processing of this io handler
            setProcessingAborted(true);
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
            //abort processing of this io handler
            setProcessingAborted(true);
        }
    }

    @Override
    public boolean isFinished() {
        return isProcessingAborted();
    }
    
    protected void inputOutputProcessImage() {
        try{
            for (Signal os: getOutputSignals()){
            	IoSignal             ioSig = (IoSignal)os;
            	GpioPinDigitalOutput pin   = (GpioPinDigitalOutput)((RemoteSignalInfo)ioSig.getRemoteSignalInfo()).getGpioPinDigital();
                ioSig.checkOut();
                LogicalValue value = (LogicalValue)ioSig.getRemoteSignalInfo().getValue();
            	try{pin.setState(value.isValid() && (Boolean)value.getValue());}catch(SignalInvalidException exc){/*cannot happen*/}
            }
            for (Signal is: getInputSignals()){
            	IoSignal            ioSig = (IoSignal)is;
            	GpioPinDigitalInput pin   = (GpioPinDigitalInput)((RemoteSignalInfo)ioSig.getRemoteSignalInfo()).getGpioPinDigital();
                ((LogicalValue)ioSig.getRemoteSignalInfo().getValue()).set(pin.getState().isHigh());
                ioSig.getRemoteSignalInfo().getValue().setValid(true);
                ioSig.checkIn();                                    
            }
        }
        catch(Error exc){
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }
            throw new ProcessException(exc);                
        }
        catch(Exception exc){
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }
            throw new ProcessException(exc);
        }
    }

    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }
}
