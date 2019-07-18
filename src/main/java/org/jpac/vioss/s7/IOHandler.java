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

package org.jpac.vioss.s7;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.Module;
import org.jpac.SignedIntegerValue;
import org.jpac.Timer;
import org.jpac.vioss.IoSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.IoDirection;
import org.jpac.Logical;
import org.jpac.NumberOutOfRangeException;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.LobRxTx;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
	static Logger Log = LoggerFactory.getLogger("jpac.vioss.s7");
	
    private final static String  HANDLEDSCHEME     = "S7";
    
    private final static int     CONNECTIONRETRYTIME    = 1000;               //ms  
    private final static long    INPUTOUTPUTTIMEOUTTIME = 100 * Module.millis;//ms 


    public enum State            {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State             state;
    private Connection        connection;
    private ConnectionRunner  connectionRunner;
    private InputOutputRunner inputOutputRunner;
    
    private boolean           connected;
    private boolean           connecting;
    private String            host;
    private int               rack;
    private int               slot;
    private LobRxTx			  readCommand;
    private LobRxTx			  writeCommand;
    private int               readAddress;
    private int               readSize;
    private int               readDatablock;
    private int               writeAddress;
    private int               writeSize;
    private int               writeDatablock;

    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException, WrongUseException, InvalidAddressSpecifierException{
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.host = uri.getHost();

        StringTokenizer pathTokens = new StringTokenizer(uri.getPath(),"/");
        if (pathTokens.countTokens() != 4) {
        	throw new InvalidAddressSpecifierException("Error: uri must contain both a device identifier and an IEC 61131 address specifier: " + uri.getPath());
        }
        try {
	        rack = Integer.decode(pathTokens.nextToken());
	        slot = Integer.decode(pathTokens.nextToken());
        } catch(NumberFormatException exc) {
        	throw new InvalidAddressSpecifierException("Error: rack/slot/db specifiers must be decimal numbers (/<rack>/slot/<datablock>/<IEC61131 address>):" + uri.getPath());        	
        }
        this.connectionRunner = new ConnectionRunner(this + " connection runner");
        this.state            = State.IDLE;
    }
    
    @Override
    public void run(){
        try{
            switch(state){
                case IDLE:
                    connected  = false;
                    connecting = false;
                    state      = State.CONNECTING;
                    //connect right away
                case CONNECTING:
                    connecting();
                    if (!connecting){
                        if (connected){
                            state = State.TRANSCEIVING;
                        }
                        else{
                            state = State.STOPPED;
                            Log.error(this + " stopped.");
                        }
                    }
                    break;
                case TRANSCEIVING:
                    try{
                        if(!transceiving()){
                           throw new IOException("at least one ads signal could not properly be transferred.");
                        }
                    }
                    catch(IOException exc){
                        Log.error("Error: ", exc);
                        invalidateInputSignals();
                        try{connection.close();}catch(Exception ex){/*ignore*/};
                        state = State.IDLE;
                    }
                    break;
                case CLOSINGCONNECTION:
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
            if (state == State.STOPPED){
                //close connection, if open
                if (connected){
                    try{connection.close();}catch(Exception ex){/*ignore*/}
                    connected = false;
                }                
            }
        }
    }
    
    @Override
    public void prepare() {
    	int minReadIndex  = Integer.MAX_VALUE;
    	int maxReadIndex  = Integer.MIN_VALUE;
    	int minWriteIndex = Integer.MAX_VALUE;
    	int maxWriteIndex = Integer.MIN_VALUE;
    	int dataByteIndex, dataSize;
    	
        try{
            Log.info("starting up " + this);
            //assign RemoteSignalInfos and calculate data areas both for input and output.
            for (Signal is: getInputSignals()){
            	IoSignal ioSig = (IoSignal)is;
            	RemoteSignalInfo rsi = new RemoteSignalInfo(is);
            	dataByteIndex        = rsi.getIec61131Address().getDataByteIndex();
            	dataSize	         = rsi.getIec61131Address().getDataSize();
            	if (dataByteIndex < minReadIndex) minReadIndex =  dataByteIndex;
            	if (dataByteIndex + dataSize > maxReadIndex) maxReadIndex = dataByteIndex + dataSize;            	
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(is));
            }
            for (Signal os: getOutputSignals()){
            	IoSignal ioSig = (IoSignal)os;
            	RemoteSignalInfo rsi = new RemoteSignalInfo(os);
            	dataByteIndex = rsi.getIec61131Address().getDataByteIndex();
            	dataSize	  = rsi.getIec61131Address().getDataSize();
            	if (dataByteIndex < minWriteIndex) minWriteIndex = dataByteIndex;
            	if (dataByteIndex + dataSize > maxWriteIndex) maxWriteIndex = dataByteIndex + dataSize;            	
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(os));
            }            
            //seize read and write db from the first signal each input and output
            readDatablock  = getInputSignals().size()  != 0 ? ((RemoteSignalInfo)((IoSignal)getInputSignals().get(0)).getRemoteSignalInfo()).getDb()  : -1;
            writeDatablock = getOutputSignals().size() != 0 ? ((RemoteSignalInfo)((IoSignal)getOutputSignals().get(0)).getRemoteSignalInfo()).getDb() : -1;
            if ((readDatablock != -1 && writeDatablock != -1) && (readDatablock == writeDatablock)) {
               //both input and output directions are involved and the datablock is the same for both input and output direction
               //check if data ranges overlap
               if (((maxReadIndex  >= minWriteIndex) && (maxReadIndex  <= maxWriteIndex)) ||
            	   ((minReadIndex  >= minWriteIndex) && (minReadIndex  <= maxWriteIndex)) || 
            	   ((maxWriteIndex >= minReadIndex)  && (maxWriteIndex <= maxReadIndex))  ||
            	   ((minWriteIndex >= minReadIndex)  && (minWriteIndex <= maxReadIndex)))    {
            	   Log.warn(this +": WARNING: input and output data ranges overlap ! Output operations will perhaps unintentionally overwrite data items written by the plc");
               }
            }
            readAddress = minReadIndex;
            readSize    = maxReadIndex - minReadIndex + 1;
            writeAddress = minReadIndex;
            writeSize    = maxReadIndex - minReadIndex + 1;
            
            setProcessingStarted(true);        
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
        }
        catch(Error err){
            Log.error("Error: ", err);
        }
    }

    @Override
    public void stop() {
        try{
            Log.info("shutting down " + this);
            state = State.CLOSINGCONNECTION;
            inputOutputRunner.terminate();
            connectionRunner.terminate();
            if (connected){
                connection.close();
                connected = false;
            }
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
        }
        catch(Error err){
            Log.error("Error: ", err);
        }
        finally{
            if(connected){
                try{connection.close();}catch(Exception exc){/*ignore*/}
            }
            connected = false;            
            state = State.STOPPED;
        }
    }

    /**
     * is called in every cycle while in state CONNECTING
     */
    protected boolean connecting() throws WrongUseException, InconsistencyException{
        boolean done = false;
        if (!connected){
            if (!connecting){                
                connectionRunner.start();
                connecting = true;
            }
            else{
                //connect to plc in progress 
                if (connectionRunner.isFinished()){
                    if(connectionRunner.isConnectionEstablished()){
                        connection = connectionRunner.getConnection();
                        connected  = true;
                    }
                    else{
                        connection = null;
                        connected  = false;
                        }
                    connecting = false;                            
                }
            }
        }
        else{
            throw new InconsistencyException("might not be called in connected state");
        }
        return done;
    };
    
    protected boolean transceiving() throws IOException, WrongUseException, SignalAccessException, AddressException, NumberOutOfRangeException{
        boolean  allSignalsProperlyTransferred = true;
        try {
	        if (inputOutputRunner.errorOccured()) {
	        	throw new IOException("Failed to transceive process image");
	        }
	        if (inputOutputRunner.timedOut()) {
	        	throw new IOException("transceptiion of process image timed out");
	        }
	        //put out output signals
	        if (inputOutputRunner.isFinished() && !getOutputSignals().isEmpty()){
	            for(Signal ios: getOutputSignals()){
	                if (((IoSignal)ios).isToBePutOut()){
	                	((IoSignal)ios).resetToBePutOut();
	                	//transfer signal value to output image
	                	((IoSignal)ios).checkOut();
	                	//and prepare transmission to remote peer
	                	checkoutSignal((IoSignal)ios);
	                }
	            }
	        }
	        //read input signals
	        if (inputOutputRunner.isFinished() && !getInputSignals().isEmpty()){
	            //propagate input signals
	            for(Signal ios: getInputSignals()){
	            	//get signal value from process image ...
	            	checkInSignal((IoSignal)ios);
	            	//... and transfer it to the signal
	            	((IoSignal)ios).checkIn();
	            }
	        }
	        inputOutputRunner.start();
        } catch(Exception exc) {
        	Log.error("Error: ", exc);
        	allSignalsProperlyTransferred = false;
        }
        return allSignalsProperlyTransferred;
    };    
    
    protected boolean closingConnection() throws IOException, WrongUseException{
        boolean done = false;
        try{
        //and close connection to plc
        connection.close();
        connected = false;
        }
        finally{
            if(connected){
                try{connection.close();}catch(Exception exc){/*ignore*/}
            }
            connected = false;
        }
        return done;
    };

    protected void checkInSignal(IoSignal ioSignal) {
    	boolean          boolVal;
    	int              intVal;
    	RemoteSignalInfo rsi = (RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
    	try {
        	switch(rsi.getIec61131Address().type) {
		    	case BIT:
		    		boolVal = readCommand.getData().getBIT(rsi.getIec61131Address().getDataByteIndex(), rsi.getIec61131Address().getDataBitIndex());
		            ((LogicalValue)ioSignal.getRemoteSignalInfo().getValue()).set(boolVal);
		    		break;
		    	case BYTE:
		    		intVal = readCommand.getData().getBYTE(rsi.getIec61131Address().getDataByteIndex());
		            ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
		    		break;
		    	case WORD:
		    		intVal = readCommand.getData().getWORD(rsi.getIec61131Address().getDataByteIndex());
		    		((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
		    		break;		    		
		    	case DWORD:
		    		intVal = (int)readCommand.getData().getDWORD(rsi.getIec61131Address().getDataByteIndex());
		    		((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
		    		break;
		    	default:
		    		throw new WrongUseException("signal type " + ((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getType() + " currently not implemented for ADS protocol");	   
        	}
    	}
    	catch(AddressException exc) {/*cannot happen*/}
    }
    
    protected void checkoutSignal(IoSignal ioSignal) {
    	boolean          boolVal;
    	int              intVal;
    	RemoteSignalInfo rsi = (RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
    	try {
        	switch(rsi.getIec61131Address().type) {
		    	case BIT:
		    		boolVal = ((LogicalValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeCommand.getData().setBIT(rsi.getIec61131Address().getDataByteIndex(), rsi.getIec61131Address().getDataBitIndex(), boolVal);
		    		break;
		    	case BYTE:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeCommand.getData().setBYTE(rsi.getIec61131Address().getDataByteIndex(), intVal);
		    		break;
		    	case WORD:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeCommand.getData().setWORD(rsi.getIec61131Address().getDataByteIndex(), intVal);
		            break;		    		
		    	case DWORD:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeCommand.getData().setDWORD(rsi.getIec61131Address().getDataByteIndex(), intVal);
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
        int rack,slot,db;
        try{
            isHandledByThisInstance  = uri != null;
            isHandledByThisInstance &= this.getUri().getScheme().equals(uri.getScheme());
            InetAddress[] ia         = InetAddress.getAllByName(this.getUri().getHost());
            InetAddress[] ib         = InetAddress.getAllByName(uri.getHost());
            isHandledByThisInstance &= ia[0].equals(ib[0]);
            isHandledByThisInstance &= this.getUri().getPort() == uri.getPort();
            if (isHandledByThisInstance) {
	            try {
	                StringTokenizer pathTokens = new StringTokenizer(uri.getPath(),"/");
	                if (pathTokens.countTokens() != 4) {
	                	throw new InvalidAddressSpecifierException("Error: uri must contain both a device identifier and an IEC 61131 address specifier: " + uri.getPath());
	                }
	                try {
	        	        rack = Integer.decode(pathTokens.nextToken());
	        	        slot = Integer.decode(pathTokens.nextToken());
	        	        db   = Integer.decode(pathTokens.nextToken());
	                } catch(NumberFormatException exc) {
	                	throw new InvalidAddressSpecifierException("Error: rack/slot/db specifiers must be decimal numbers (/<rack>/slot/<datablock>/<IEC61131 address>):" + uri.getPath());        	
	                }
	                String iecAddressSpecifier = pathTokens.nextToken();
	            	isHandledByThisInstance   &= this.rack == rack;
	            	isHandledByThisInstance   &= this.slot == slot;
	            	isHandledByThisInstance   &= isSuitableForDatablock(db, ioDirection);
	            } catch(InvalidAddressSpecifierException exc) {
	            	isHandledByThisInstance = false;
	            }
            }
        }
        catch(UnknownHostException exc){};
        return isHandledByThisInstance;
    }
    
    protected boolean isSuitableForDatablock(int db, IoDirection ioDirection) {
    	boolean isSuitable = true;
    	try{
    	    boolean isSuitableAsInput  = getInputSignals().size()  == 0 || getInputSignals().stream().allMatch((s)-> db == seizeDatablock(s));
    	    boolean isSuitableAsOutput = getOutputSignals().size() == 0 || getOutputSignals().stream().allMatch((s)-> db == seizeDatablock(s));

    	    switch(ioDirection) {
		    	case INPUT:
		    		isSuitable = isSuitableAsInput;
		    		break;
		    	case OUTPUT:
		    		isSuitable = isSuitableAsOutput;
		    		break;
		    	case INOUT:
		    		isSuitable = isSuitableAsInput && isSuitableAsOutput;
		    		break;
	    	}
    	} catch(Exception exc) {
    		Log.error("Error: ", exc);
    		isSuitable = false;
    	}
    	return isSuitable;
    }
    
    protected int seizeDatablock(Signal signal) {
    	int db = 0;
        StringTokenizer pathTokens = new StringTokenizer(((IoSignal)signal).getUri().getPath(),"/");
	    Integer.decode(pathTokens.nextToken());
	    Integer.decode(pathTokens.nextToken());
    	return Integer.decode(pathTokens.nextToken());
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
    	ti = ti + "://" + host + "/" + rack + "/" + slot;
        return ti;
    }
    
    class ConnectionRunner extends AsynchronousTask{ 
        private Connection           connection;
        private boolean              connected;
        public ConnectionRunner(String identifier){
            super(identifier);
        }
        
        @Override
        public void doIt() throws ProcessException {
            boolean   exceptionOccured = false;
            
            connected = false;
            Log.info("establishing connection for " + getInputSignals().size() + " input and " +  getOutputSignals().size() + " output signals ...");
            do{
                try{
                	//establish connection
                    connection   = new Connection(host, rack, slot);
                    connected    = true;
                    readCommand  = new LobRxTx(connection, new Address(readDatablock , readAddress , readSize , Address.NA), 0, new Data(new byte[readSize]));
                    writeCommand = new LobRxTx(connection, new Address(writeDatablock, writeAddress, writeSize, Address.NA), 0, new Data(new byte[writeSize]));
                } catch(Exception exc){
                    Log.debug("Error:", exc);
                    try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};
                }
                if (isTerminated() )try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};
            }
            while(!connected && !isTerminated() && !exceptionOccured);
            if (connected){
                Log.info("... connection established as " + connection);            
            }
        }
        
        public Connection getConnection(){
            return this.connection;
        }
        
        public boolean isConnectionEstablished(){
            return connected;
        }
    } 
    class InputOutputRunner extends AsynchronousTask{ 
        private boolean errorOccured;
        private Timer   watchDog;

        public InputOutputRunner(String identifier){
            super(identifier);
            watchDog     = new Timer("InputOutputRunner.WatchDog");
            errorOccured = false;
        }
        
        @Override
        public void start(){
        	watchDog.start(INPUTOUTPUTTIMEOUTTIME);
        	super.start();
        }
        
        @Override
        public void doIt() throws ProcessException {
            try {
                Log.debug("invoking exchange of process image ...");
                writeCommand.getWriteRequest().write(connection);//request
                writeCommand.getWriteRequest().read(connection); //acknowledgement
                readCommand.getWriteRequest().write(connection); //request
	            readCommand.getReadRequest().read(connection);   //acknowledgement
	            errorOccured = false;
	            Log.debug("exchange of process image succeeded");
            } catch(Exception exc) {
            	Log.error("Error:", exc);
                errorOccured = true;
            }
        }
                
        public boolean errorOccured(){
            return isFinished() && errorOccured;
        }
        
        public boolean timedOut() {
        	return !isFinished() && !watchDog.isRunning();
        }
        
        public void resetError() {
        	errorOccured = false;
        }
    }    
    
}
