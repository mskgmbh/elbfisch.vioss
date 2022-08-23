/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IOHandler.java (versatile input output subsystem)
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

import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;

import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.SignedIntegerValue;
import org.jpac.vioss.IoSignal;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.jpac.IoDirection;
import org.jpac.NumberOutOfRangeException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.modbus.DataBlock;
import org.jpac.vioss.modbus.FunctionCode;
import org.jpac.vioss.modbus.InvalidAddressSpecifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    static Logger Log = LoggerFactory.getLogger("jpac.vioss.modbus.server");
	
    private final static String  HANDLEDSCHEME     = "MODBUS.SERVER";
    private final static String  DATABLOCKS	   = "datablocks";
    private final static String  INPUT    	   = "input";
    private final static String  OUTPUT    	   = "output";
    private final static String  ADDRESS           = "[@address]"; //[byte]
    private final static String  SIZE              = "[@size]";    //[byte]
    private final static String  ACCESS            = "[@access]";    //Input,Output   
    
    private final static String SERVICEENABLED     = "ServiceEnabled";
    private final static String BINDADDRESS        = "BindAddress";
    private final static String SERVICEPORT        = "ServicePort";
    private final static String DEFAULTACCESSLEVEL = "DefaultAccessLevel";


    public enum State {IDLE, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};
    
    
    private State state;
    
    private boolean    propServiceEnabled;
    private String     propBindAddress;
    private int        propServicePort;
    
    private Service    service;
    
    private DataBlocks datablocks;

    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException, WrongUseException, InvalidAddressSpecifierException{
        //URI: "modbus.server:"
        super(uri, parameterConfiguration);

        SubnodeConfiguration inputDatablockConfiguration = null;
        SubnodeConfiguration outputDatablockConfiguration = null;
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        try{
            propServiceEnabled     = getParameterConfiguration().getBoolean(SERVICEENABLED);
            propBindAddress        = getParameterConfiguration().getString(BINDADDRESS);
            propServicePort        = getParameterConfiguration().getInt(SERVICEPORT);

            //retrieve data block definitions for modbus device
            datablocks = new DataBlocks();
            inputDatablockConfiguration = getParameterConfiguration().configurationAt(DATABLOCKS).configurationAt(INPUT);
            if (inputDatablockConfiguration == null) {
                throw new WrongUseException("Error: no input datablock defined for modbus server: " + uri);
            }
            datablocks.setInputDatablock(new DataBlock(inputDatablockConfiguration.getInt(ADDRESS), inputDatablockConfiguration.getInt(SIZE), FunctionCode.UNDEFINED, FunctionCode.UNDEFINED, null));
            
            outputDatablockConfiguration = getParameterConfiguration().configurationAt(DATABLOCKS).configurationAt(OUTPUT);
            if (outputDatablockConfiguration == null) {
                throw new WrongUseException("Error: no output datablock defined for modbus server: " + uri);
            }
            datablocks.setOutputDatablock(new DataBlock(outputDatablockConfiguration.getInt(ADDRESS), outputDatablockConfiguration.getInt(SIZE), FunctionCode.UNDEFINED, FunctionCode.UNDEFINED, null));
            
        }catch(WrongUseException exc){
            Log.error("Failed to retrieve IOHandler configuration for " + getHandledScheme());
            return;
        }
        catch(Exception exc) {
                Log.error("Error: ",exc);
        }
        this.state = State.IDLE;
    }

    @Override
    public void run(){
        try{
            switch(state){
                case IDLE:
                    state      = State.TRANSCEIVING;
                    break;
                case TRANSCEIVING:
                    try{
                        if(!transceiving()){
                           throw new IOException("at least one signal could not properly be transferred.");
                        }
                    }
                    catch(IOException exc){
                        Log.error("Error: ", exc);
                        invalidateInputSignals();
                        state = State.IDLE;
                    }
                    break;
                case CLOSINGCONNECTION:
                    state = State.STOPPED;
                    break;
                case STOPPED:
                    //do nothing
                    break;                        
            }
        }
        catch(Exception exc){
            Log.error("Error:", exc);
            state = State.STOPPED;//stop processing
        }
        catch(Error err){
            Log.error("Error:", err);
            state = State.STOPPED;//stop processing
        }
        finally{

        }
    }          
    
    @Override
    public void prepare() {
        try{
            Log.info("starting up " + this);
            //assign RemoteSignalInfos to io signals
            for (Signal is: getInputSignals()){
            	IoSignal ioSig = (IoSignal)is;
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(is, datablocks.getInputDatablock()));
            }
            for (Signal os: getOutputSignals()){
            	IoSignal ioSig = (IoSignal)os;
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(os, datablocks.getOutputDatablock()));
            }                   
            //start modbus server
            service = null;
            if (propServiceEnabled){
                service = new Service(false, propBindAddress, propServicePort, datablocks);
                service.start();
            }
            setProcessingStarted(true);        
        }
        catch(InvalidAddressSpecifierException | InterruptedException | CertificateException | SSLException | Error exc){
            setProcessingStarted(false);       
            this.state = State.STOPPED;
            Log.error("Error: ", exc);
            Log.error("failed to start " + this);
        }
    }

    @Override
    public void stop() {
        try{
            Log.info("shutting down " + this);
            if (service != null){
                service.stop();
            }
            state = State.CLOSINGCONNECTION;
        }
        catch(Exception | Error exc){
            Log.error("Error: ", exc);
        }
        finally{
            state = State.STOPPED;
        }
    }
    
    /**
     * is called in every cycle while in state TRANSCEIVING
     */
    protected boolean transceiving() throws WrongUseException, SignalAccessException, NumberOutOfRangeException{
        boolean  allSignalsProperlyTransferred = true;
        try {
            if (service.atLeastOneClientConnected()){
                //propagate input signals
                synchronized (datablocks.getInputDatablock()) {                    
                    for(Signal ios: getInputSignals()){
                        //get signal value from process image ...
                        checkInSignal((IoSignal)ios);
                        //... and transfer it to the signal
                        ((IoSignal)ios).checkIn();
                    }
                }

            }
            else{
                for(Signal ios: getInputSignals()){
                    //invalidate input signals
                    ios.invalidate();
                }                
            }
            synchronized (datablocks.getOutputDatablock()) {                    
                //propagate output signals
                for(Signal ios: getOutputSignals()){
                    if (((IoSignal)ios).isToBePutOut()){
                        ((IoSignal)ios).resetToBePutOut();
                        //get signal value from the signal
                        ((IoSignal)ios).checkOut();
                        //and transfer it to the process (output)image
                        checkoutSignal((IoSignal)ios);
                    }
                }
            }            
        } catch(NumberOutOfRangeException | SignalAccessException exc) {
        	Log.error("Error: ", exc);
        	allSignalsProperlyTransferred = false;
        }
        return allSignalsProperlyTransferred;
    };    
    
    protected void checkInSignal(IoSignal ioSignal) {
    	boolean            boolVal;
    	int                intVal;
        try {
            RemoteSignalInfo rsi = (RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();

            switch(rsi.getIec61131Address().getType()) {
                case BIT:
                    boolVal   = rsi.getAssignedDataBlock().getData().getBIT(rsi.getDataByteIndex(), rsi.getDataBitIndex());
                    ((LogicalValue)rsi.getValue()).set(boolVal);
                    ((LogicalValue)rsi.getValue()).setValid(true);
                    break;
                case BYTE:
                    intVal = rsi.getAssignedDataBlock().getData().getBYTE(rsi.getDataByteIndex());
                    ((SignedIntegerValue)rsi.getValue()).set(intVal);
                    ((SignedIntegerValue)rsi.getValue()).setValid(true);
                    break;
                case WORD:
                    intVal = rsi.getAssignedDataBlock().getData().getWORD(rsi.getDataByteIndex());
                    ((SignedIntegerValue)rsi.getValue()).set(intVal);
                    ((SignedIntegerValue)rsi.getValue()).setValid(true);
                    break;
                case DWORD:
                    intVal = (int)rsi.getAssignedDataBlock().getData().getDWORD(rsi.getDataByteIndex());
                    ((SignedIntegerValue)rsi.getValue()).set(intVal);
                    ((SignedIntegerValue)rsi.getValue()).setValid(true);
                    break;
                default:
                    throw new WrongUseException("signal type " + rsi.getType() + " currently not implemented for MODBUS protocol");
            }            
        } catch (WrongUseException | AddressException exc) {
            Log.error("Error:", exc);
        }
    }
    
    protected void checkoutSignal(IoSignal ioSignal) {
    	LogicalValue       boolValue;
    	SignedIntegerValue intValue;
    	boolean            boolVal;
    	int                intVal;
    	try {
            RemoteSignalInfo   rsi = (RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
            
            switch(rsi.getIec61131Address().getType()) {
                    case BIT:
                        boolValue = (LogicalValue)rsi.getValue();
                        boolVal   = boolValue.isValid() ? boolValue.get() : false;
                        rsi.getAssignedDataBlock().getData().setBIT(rsi.getDataByteIndex(), rsi.getDataBitIndex(), boolVal);
                        break;
                    case BYTE:
                        intValue = (SignedIntegerValue)rsi.getValue();
                        intVal   = intValue.isValid() ? intValue.get() : 0;
                        rsi.getAssignedDataBlock().getData().setBYTE(rsi.getDataByteIndex(), intVal);
                        break;
                    case WORD:
                        intValue = (SignedIntegerValue)rsi.getValue();
                        intVal   = intValue.isValid() ? intValue.get() : 0;
                        rsi.getAssignedDataBlock().getData().setWORD(rsi.getDataByteIndex(), intVal);
                        break;		    		
                    case DWORD:
                        intValue = (SignedIntegerValue)rsi.getValue();
                        intVal   = intValue.isValid() ? intValue.get() : 0;
                        rsi.getAssignedDataBlock().getData().setDWORD(rsi.getDataByteIndex(), intVal);
                        break;
                    default:
                            throw new WrongUseException("signal type " + ((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getType() + " currently not implemented for ADS protocol");	   
            }
    	}
    	catch(AddressException | ValueOutOfRangeException exc) {
    		Log.error("Error: ", exc);
    	}
    }

    protected void invalidateInputSignals() throws SignalAccessException{
        for (Signal ios: getInputSignals()){
            ios.invalidate();
        }        
    }
    
    @Override
    public boolean handles(URI uri, IoDirection ioDirection) {
        boolean isHandledByThisInstance = false;
        isHandledByThisInstance = uri != null && this.getUri().getScheme().equals(uri.getScheme());
        return isHandledByThisInstance;
    }
    
    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }

    @Override
    public boolean isFinished() {
        return state == State.STOPPED;
    }
    
    @Override
    public String getTargetInstance(){
    	String ti = getUri().getScheme();
    	ti = ti + ":";
        return ti;
    }
}
