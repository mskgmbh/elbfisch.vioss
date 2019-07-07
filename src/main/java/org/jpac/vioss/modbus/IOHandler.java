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

package org.jpac.vioss.modbus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.SignedIntegerValue;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.modbus.Iec61131Address.AccessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.NumberOutOfRangeException;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
	//static Logger Log = LoggerFactory.getLogger("jpac.vioss.modbus");
	
    private final static String  HANDLEDSCHEME     = "MODBUS";
    private final static String  DATABLOCK	       = "datablock";
    private final static String  ADDRESS           = "[@address]";
    private final static String  SIZE              = "[@size]";
    private final static String  IEC61131ADDRESS   = "[@iec61131Address]";
    private final static String  READFUNCTIONCODE  = "[@ReadFuncionCode]";
    private final static String  WRITEFUNCTIONCODE = "[@WriteFuncionCode]";
    private final static String  NA                = "NA";

    
    private final static int     CONNECTIONRETRYTIME  = 1000;//ms  

    public enum State            {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                      state;
    private Connection                 connection;
    private ConnectionRunner           connectionRunner;
    private boolean                    connected;
    private boolean                    connecting;
    private String                     host;
    private int                        port;
    
    private HashMap<Iec61131Address, DataBlock> datablocks;
    private DataBlock                           assignedReadDataBlock;
    private DataBlock                           assignedWriteDataBlock;
    private Request                             readRequest;
    private Request                             writeRequest;
    private String                              deviceIdentifier;

    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException, WrongUseException, InvalidAddressSpecifierException{
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.host = uri.getHost();
        this.port = uri.getPort() != -1 ? uri.getPort() : Modbus.DEFAULT_PORT;

        StringTokenizer pathTokens = new StringTokenizer(uri.getPath(),"/");
        if (pathTokens.countTokens() != 2) {
        	throw new InvalidAddressSpecifierException("Error: uri must contain both a device identifier and an IEC 61131 address specifier: " + uri.getPath());
        }
        deviceIdentifier           = pathTokens.nextToken();
        String iecAddressSpecifier = pathTokens.nextToken();
        
        HierarchicalConfiguration deviceConfiguration = getParameterConfiguration().configurationAt(deviceIdentifier);
        List<HierarchicalConfiguration> datablockConfigurations = deviceConfiguration.configurationsAt(DATABLOCK);
        if (datablockConfigurations == null || datablockConfigurations.isEmpty()) {
        	throw new WrongUseException("Error: no datablocks defined for modbus device. Remote instance: " + uri);
        }
        //retrieve data block definitions for modbus device
        datablocks = new HashMap<>();
        datablockConfigurations.forEach((hc) -> {
        	try {
	            int             address           = hc.getInt(ADDRESS);
	            int             size              = hc.getInt(SIZE);
	            String          readFC            = hc.getString(READFUNCTIONCODE).toUpperCase();
	            String          writeFC           = hc.getString(WRITEFUNCTIONCODE).toUpperCase();
	            FunctionCode    readFunctionCode  = readFC.equals(NA) ? FunctionCode.UNDEFINED : FunctionCode.fromInt(Integer.decode(readFC));
	            FunctionCode    writeFunctionCode = writeFC.equals(NA) ? FunctionCode.UNDEFINED : FunctionCode.fromInt(Integer.decode(writeFC));
	            Iec61131Address iec61131Address   = new Iec61131Address(hc.getString(IEC61131ADDRESS));
	            DataBlock       db = null;
	            try {
	            	db = new DataBlock(address, size, readFunctionCode, writeFunctionCode, iec61131Address);
		            datablocks.put(iec61131Address, db);
		            Log.debug("datablock defined: " + datablocks.get(iec61131Address));
	            } catch(WrongUseException exc) {
	            	Log.error("Error in configuration for modbus device '" + deviceIdentifier + "': " + exc.getMessage());
	            }
        	} catch(Exception exc) {
        		Log.error("Error: ",exc);
        	}
        });
        Iec61131Address iecAddress = new Iec61131Address(iecAddressSpecifier);
        DataBlock assignedDataBlock = retrieveDataBlock(iecAddress);
        if (assignedDataBlock == null) {
        	throw new InvalidAddressSpecifierException("Error: Modbus device does not contain any datablock assignable to " + uri.getPath());
        }
        
        switch (assignedDataBlock.getIec61131Address().getAccessMode()){
        	case INPUT : 
        		assignedReadDataBlock  = assignedDataBlock;
        		readRequest  = getReadRequestFromFunctionCode(assignedReadDataBlock);
        	    break;
        	case OUTPUT: 
        		assignedWriteDataBlock = assignedDataBlock;
        		writeRequest = getWriteRequestFromFunctionCode(assignedWriteDataBlock);
        		break;
        	case MEMORY: 
        		assignedReadDataBlock  = assignedDataBlock;
        		assignedWriteDataBlock = assignedDataBlock;
        		readRequest  = getReadRequestFromFunctionCode(assignedReadDataBlock);
        		writeRequest = getWriteRequestFromFunctionCode(assignedWriteDataBlock);
        		break;
        	default:
        }
        
        this.connectionRunner = new ConnectionRunner(this + " connection runner");
        this.state            = State.IDLE;
    }
    
    protected DataBlock retrieveDataBlock(Iec61131Address iecAddress) {
        //find data block defined in org.jpac.Configuration which is to be accessed by this io handler
    	DataBlock datablock = null;
    	try {
    		datablock = datablocks.values().stream().filter((db) -> db.contains(iecAddress)).findFirst().get();
    	} catch(NoSuchElementException exc) {
    		datablock = null;
    	}
        return datablock;
    }
    
    protected Request getReadRequest() {
    	return readRequest;
    }
    
    protected Request getWriteRequest() {
    	return writeRequest;
    }

    protected Request getReadRequestFromFunctionCode(DataBlock db) {
    	switch (db.getReadFunctionCode()) {
    		case READCOILS             : return new ReadCoils(db); 
    		case READDISCRETEINPUTS    : return new ReadDiscreteInputs(db);
    		case READHOLDINGREGISTERS  : return new ReadHoldingRegisters(db);
    		case READINPUTREGISTERS    : return new ReadInputRegisters(db);
    		default                    : return null;
    	}    			
    }

    protected Request getWriteRequestFromFunctionCode(DataBlock db) {
    	switch (db.getWriteFunctionCode()) {
    		case WRITEMULTIPLECOILS    : return new WriteMultipleCoils(db);
    		case WRITEMULTIPLEREGISTERS: return new WriteMultipleRegisters(db);
    		default                    : return null;
    	}    			
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
        try{
            Log.info("starting up " + this);
            for (Signal is: getInputSignals()){
            	IoSignal ioSig = (IoSignal)is;
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(is));
            }
            for (Signal os: getOutputSignals()){
            	IoSignal ioSig = (IoSignal)os;
            	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(os));
            }                   
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
    
    /**
     * is called in every cycle while in state TRANSCEIVING
     */
    protected boolean transceiving() throws IOException, WrongUseException, SignalAccessException, AddressException, NumberOutOfRangeException{
        boolean  allSignalsProperlyTransferred = true;
        try {
        //put out output signals
        if (assignedWriteDataBlock != null){
            for(Signal ios: getOutputSignals()){
                if (((IoSignal)ios).isToBePutOut()){
                	((IoSignal)ios).resetToBePutOut();
                	//transfer signal value to output image
                	((IoSignal)ios).checkOut();
                	//and prepare transmission to remote peer
                	checkoutSignal((IoSignal)ios);
                }
            }
            writeRequest.write(connection);//request
            writeRequest.read(connection); //acknowledgement
        }
        //read input signals
        if (assignedReadDataBlock != null){
            readRequest.write(connection);//request
            readRequest.read(connection); //acknowledgement
            //propagate input signals
            for(Signal ios: getInputSignals()){
            	//get signal value from process image ...
            	checkInSignal((IoSignal)ios);
            	//... and transfer it to the signal
            	((IoSignal)ios).checkIn();
            }
        }
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
    	RemoteSignalInfo rsi = (org.jpac.vioss.modbus.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
    	try {
        	switch(rsi.getIec61131Address().type) {
		    	case BIT:
		    		boolVal = readRequest.getData().getBIT(rsi.getIec61131Address().getDataByteIndex(), rsi.getIec61131Address().getDataBitIndex());
		            ((LogicalValue)ioSignal.getRemoteSignalInfo().getValue()).set(boolVal);
		    		break;
		    	case BYTE:
		    		intVal = readRequest.getData().getBYTE(rsi.getIec61131Address().getDataByteIndex());
		            ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
		    		break;
		    	case WORD:
		    		intVal = readRequest.getData().getWORD(rsi.getIec61131Address().getDataByteIndex());
		    		((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
		    		break;		    		
		    	case DWORD:
		    		intVal = (int)readRequest.getData().getDWORD(rsi.getIec61131Address().getDataByteIndex());
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
    	RemoteSignalInfo rsi = (org.jpac.vioss.modbus.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
    	try {
        	switch(rsi.getIec61131Address().type) {
		    	case BIT:
		    		boolVal = ((LogicalValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeRequest.getData().setBIT(rsi.getIec61131Address().getDataByteIndex(), rsi.getIec61131Address().getDataBitIndex(), boolVal);
		    		break;
		    	case BYTE:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeRequest.getData().setBYTE(rsi.getIec61131Address().getDataByteIndex(), intVal);
		    		break;
		    	case WORD:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeRequest.getData().setWORD(rsi.getIec61131Address().getDataByteIndex(), intVal);
		            break;		    		
		    	case DWORD:
		    		intVal  = ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).get();
		            writeRequest.getData().setDWORD(rsi.getIec61131Address().getDataByteIndex(), intVal);
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
    public boolean handles(URI uri) {
        boolean isHandledByThisInstance = false;
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
	                if (pathTokens.countTokens() != 2) {
	                	throw new InvalidAddressSpecifierException("Error: uri must contain both a device identifier and an IEC 61131 address specifier: " + uri.getPath());
	                }
	                String deviceIdentifier    = pathTokens.nextToken();
	                String iecAddressSpecifier = pathTokens.nextToken();
	            	isHandledByThisInstance   &= this.deviceIdentifier.equals(deviceIdentifier);
	            	
	            	Iec61131Address iecadr     = new Iec61131Address(iecAddressSpecifier);
	            	switch(iecadr.getAccessMode()) {
	            		case INPUT:
	                    	if (assignedReadDataBlock != null) {
	                    		//data block for input already defined by the constructor
	                    		//check, if it contains the given iec address
	                    		isHandledByThisInstance = assignedReadDataBlock.contains(iecadr);
	                    	} else {
	                    		//input data block not assigned for this io handler. Assign it now
	                    		assignedReadDataBlock = retrieveDataBlock(iecadr);
	                    		//and declare, that this uri will be handled by this io handler
	                    		isHandledByThisInstance = true;
	                    	}
	            			break;
	            		case OUTPUT:
	                    	if (assignedWriteDataBlock != null) {
	                    		//data block for output already defined by the constructor
	                    		//check, if it contains the given iec address
	                    		isHandledByThisInstance = assignedWriteDataBlock.contains(iecadr);
	                    	} else {
	                    		//output data block not assigned for this io handler. Assign it now
	                    		assignedWriteDataBlock = retrieveDataBlock(iecadr);
	                    		//and declare, that this uri will be handled by this io handler
	                    		isHandledByThisInstance = true;
	                    	}
	            			break;
	            		case MEMORY:
	            			isHandledByThisInstance = (assignedReadDataBlock.getIec61131Address().getAccessMode() == AccessMode.MEMORY) && 
	            									  assignedReadDataBlock.contains(iecadr);
	            			break;
	            		default:
	            			isHandledByThisInstance = false;
	            	}
	            } catch(InvalidAddressSpecifierException exc) {
	            	isHandledByThisInstance = false;
	            }
            }
        }
        catch(UnknownHostException exc){};
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
    	ti = ti + "://" + host + ":" + port + "/" + this.deviceIdentifier;
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
            Log.info("establishing connection for " + getInputSignals().size() + " input (" + assignedReadDataBlock.iec61131Address.getAddressSpecifier() + ") and " + 
                                                      getOutputSignals().size() + " output (" + assignedWriteDataBlock.iec61131Address.getAddressSpecifier() + ") signals ...");
            do{
                try{
                	//establish connection
                    connection = new Connection(getUri().getHost(), getUri().getPort());
                    connected  = true;
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
}
