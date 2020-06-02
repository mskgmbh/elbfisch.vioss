/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Revolution Pi 
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

package org.jpac.vioss.revpi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.StringTokenizer;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.jpac.Address;
import org.jpac.BasicSignalType;
import org.jpac.InconsistencyException;
import org.jpac.IoDirection;
import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.SignedIntegerValue;
import org.jpac.plc.AddressException;
import org.jpac.plc.ValueOutOfRangeException;

import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoLogical;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.IoSignedInteger;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{

    public enum RunnerState {CONNECTING, TRANSMITTING, ERROR};

    final static String HANDLEDSCHEME       = "REVPI";
    final static String DEVPICONTROL        = "/dev/piControl0";
    
    protected RandomAccessFile piControl;
    protected ProcessImage     processImage;
    protected boolean          runningOnRevPi;
    //TEST
                
    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException, IOException {
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.runningOnRevPi = new File(DEVPICONTROL).exists();
        this.processImage = new ProcessImage(DEVPICONTROL, this.runningOnRevPi);
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
        setProcessingStarted(true);
        Log.info("starting up " + this + (this.runningOnRevPi ? "" : " (simulated)"));
        
        try{//String identifier, BasicSignalType type, ProcessImageItem processImageItem
        	getInputSignals().forEach((s) -> ((IoSignal)s).setRemoteSignalInfo(new RemoteSignalInfo(s.getQualifiedIdentifier(), BasicSignalType.fromSignal(s), seizeProcessImageItem(s)))); 
        	getOutputSignals().forEach((s) -> ((IoSignal)s).setRemoteSignalInfo(new RemoteSignalInfo(s.getQualifiedIdentifier(), BasicSignalType.fromSignal(s), seizeProcessImageItem(s))));
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }
    }

    @Override
    public void stop(){
        Log.info("shutting down " + this + " ...");
        //TODO close file for RevPi's process image
        if (piControl != null){
            try{piControl.close();}catch(IOException exc){/*ignore*/};
        }
        setProcessingAborted(true);
    }
        
    @Override
    public void run(){
        try{
            if (!isProcessingAborted()){
                //invoke data interchange
                transceiveProcessImage();
            }
        }
        catch(Error exc){
            Log.error("Error: ", exc);
            Log.error("processing aborted for IOHandler " + this);
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }            
            //abort processing of this io handler
            setProcessingAborted(true);
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }            
            setProcessingAborted(true);
        }
    }
    
    @Override
    public boolean isFinished() {
        return isProcessingAborted();
    }
    
    protected void transceiveProcessImage() {
        try{
            //check for changes of output signals
            for (Signal s: getOutputSignals()) {
            	IoSignal ioSig = (IoSignal)s;
            	ioSig.checkOut();
            	transferValueToProcessImage(ioSig);
            }
            processImage.update();
            //update input signals
            for (Signal s: getInputSignals()) {
            	IoSignal ioSig = (IoSignal)s;
            	transferValueFromProcessImage(ioSig);
            	ioSig.checkIn();
            }
        }
        catch(Exception | Error exc){
            Log.error("Error: ", exc);
        }
    }
    
    protected ProcessImageItem seizeProcessImageItem(Signal ioSignal) throws InconsistencyException{
    	URI              uri              = ((IoSignal)ioSignal).getUri();
        StringTokenizer  tokenizer        = new StringTokenizer(uri.getPath(), "/");
        ProcessImageItem processImageItem = null;
        try{
            String token = tokenizer.nextToken();
            //uri : .../<identifier defined in JSON file of PiCtory>
            processImageItem = getProcessImage().getItem(token);
            if (processImageItem == null){
                discardSignal(ioSignal);//remove registration of this signal
                throw new InconsistencyException("process image item '" + uri.getPath() + "' for signal " + ioSignal.getQualifiedIdentifier() + " not found");
            }        
            if (processImageItem.getIoDirection() != IoDirection.INOUT && ((IoSignal)ioSignal).getIoDirection() != processImageItem.getIoDirection()){
                discardSignal(ioSignal);//remove registration of this signal
                throw new InconsistencyException("inconsistent io direction for signal " + ioSignal.getQualifiedIdentifier() + ". Must be " + processImageItem.getIoDirection());
            }
            if (ioSignal instanceof IoLogical && processImageItem.getAddress().getBitIndex() == Address.NA){
                discardSignal(ioSignal);//remove registration of this signal already done by super(..)
                throw new InconsistencyException("signal " + ioSignal.getQualifiedIdentifier() + " must be assigned to bit input/output");            
            }
            if (ioSignal instanceof IoSignedInteger && processImageItem.getAddress().getBitIndex() != Address.NA){
                throw new InconsistencyException("signal " + ioSignal.getQualifiedIdentifier() + " must not be assigned to either byte, int16 or int32 input/output");            
            }        
        }
        catch(AddressException exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "' : " + exc);
        }
        return processImageItem; 
    }
    
    protected void transferValueToProcessImage(IoSignal ioSig) throws AddressException, ValueOutOfRangeException{
    	RemoteSignalInfo rsi = (RemoteSignalInfo)ioSig.getRemoteSignalInfo();
    	ProcessImageItem pii = rsi.getProcessImageItem();
    	switch(ioSig.getRemoteSignalInfo().getType()) {
	    	case Logical:
	    			pii.getData().setBIT(pii.getAddress().getByteIndex(),pii.getAddress().getBitIndex(), rsi.getValue().isValid() ? ((LogicalValue)rsi.getValue()).get() : false);
	    			break;
	    	case SignedInteger:
		            switch(pii.getAddress().getSize()){
	                case 1:
	                	pii.getData().setBYTE(pii.getAddress().getByteIndex(), rsi.getValue().isValid() ? ((SignedIntegerValue)rsi.getValue()).get() : 0);
	                    break;
	                case 2:
	                	pii.getData().setINT(pii.getAddress().getByteIndex(), rsi.getValue().isValid() ? ((SignedIntegerValue)rsi.getValue()).get() : 0);
	                    break;
	                case 4:
	                	pii.getData().setDINT(pii.getAddress().getByteIndex(), rsi.getValue().isValid() ? ((SignedIntegerValue)rsi.getValue()).get() : 0);
	                    break;
		            }
	    			break;
	    		default:
    	}
    }

    protected void transferValueFromProcessImage(IoSignal ioSig) throws AddressException{
    	RemoteSignalInfo rsi = (RemoteSignalInfo)ioSig.getRemoteSignalInfo();
    	ProcessImageItem pii = rsi.getProcessImageItem();
    	switch(ioSig.getRemoteSignalInfo().getType()) {
	    	case Logical:
	    			LogicalValue logicalValue = (LogicalValue)rsi.getValue();
	    			logicalValue.set(pii.getData().getBIT(pii.getAddress().getByteIndex(),pii.getAddress().getBitIndex()));
	    			logicalValue.setValid(true);
	    			break;
	    	case SignedInteger:
	    			SignedIntegerValue signedIntegerValue = (SignedIntegerValue)rsi.getValue();
		            switch(pii.getAddress().getSize()){
	                case 1:
	                	signedIntegerValue.set(pii.getData().getBYTE(pii.getAddress().getByteIndex()));
	                    break;
	                case 2:
	                	signedIntegerValue.set(pii.getData().getINT(pii.getAddress().getByteIndex()));
	                	//System.out.println(ioSig + " : " + pii.getData().getINT(pii.getAddress().getByteIndex()));//TODO
	                    break;
	                case 4:
	                	signedIntegerValue.set(pii.getData().getDINT(pii.getAddress().getByteIndex()));
	                    break;
		            }
                	signedIntegerValue.setValid(true);
	    			break;
	    		default:
    	}
    }

    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }
    
    public ProcessImage getProcessImage(){
        return this.processImage;
    }        
}
