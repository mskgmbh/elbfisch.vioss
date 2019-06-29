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

package org.jpac.vioss.ads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.SignedIntegerValue;
import org.jpac.vioss.IoSignal;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.NumberOutOfRangeException;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.SignalInvalidException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    private final static String  HANDLEDSCHEME                = "ADS";
    private final static int     CONNECTIONRETRYTIME          = 1000;//ms  
    private final static int     NUMBEROFREADWRITESPERREQUEST = 100;
    private final static int     NUMBEROFREADSPERREQUEST      = 200;
    private final static int     NUMBEROFWRITESPERREQUEST     = 200;

    public enum State            {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                     state;
    private Connection                connection;
    private ConnectionRunner          connectionRunner;
    private boolean                   connected;
    private boolean                   connecting;
    private ReadWriteMultipleInChunks retrieveAdsVariableHandlesByName;
    private ReadMultipleInChunks      readVariablesByHandle;
    private WriteMultipleInChunks     writeVariablesByHandle;
    private WriteMultipleInChunks     releaseHandles;
    private AdsReadState              adsReadState;
    
    protected HashMap<IoSignal,AdsReadVariableByHandle>  listOfAdsReadVariableByHandle;
    protected HashMap<IoSignal,AdsWriteVariableByHandle> listOfAdsWriteVariableByHandle;
    protected HashMap<Integer,AdsGetSymbolHandleByName>  listOfAdsGetSymbolHandleByName;
        
    public IOHandler(URI uri) throws IllegalUriException {
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.connectionRunner = new ConnectionRunner(this + " connection runner");
        this.state            = State.IDLE;
        this.adsReadState     = new AdsReadState();
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
                        try{releaseHandles.transact(connection);}catch(Exception ex){/*ignore*/};
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
                //release ads handles
                releaseHandles.transact(connection);
                //and close connection to plc
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
        AdsState adsState;
        boolean  allSignalsProperlyTransferred = true;
        
        adsReadState.transact(connection);
        adsState = adsReadState.getAdsState();
        if (adsState == AdsState.Run || adsState == AdsState.Stop){
            //read input signals
            if (!readVariablesByHandle.getAmsPackets().isEmpty()){
                readVariablesByHandle.transact(connection);
                //propagate input signals
                for(Signal ios: getInputSignals()){
                	//get signal value from process image ...
                	checkInSignal((IoSignal)ios);
                	//... and transfer it to the signal
                	((IoSignal)ios).checkIn();
                	AdsErrorCode adsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getErrorCode();
                	if (adsErrorCode != AdsErrorCode.NoError){
                        allSignalsProperlyTransferred = false;
                    }
                }
            }
            //put out output signals
            if (!writeVariablesByHandle.getAmsPackets().isEmpty()){
                for(Signal ios: getOutputSignals()){
                    if (((IoSignal)ios).isToBePutOut()){
                    	((IoSignal)ios).resetToBePutOut();
                    	//transfer signal value to output image
                    	((IoSignal)ios).checkOut();
                    	//and prepare transmission to remote peer
                    	checkoutSignal((IoSignal)ios);
                    }
                }
                writeVariablesByHandle.transact(connection);
                for(Signal ios: getOutputSignals()){
                    AdsErrorCode adsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsWriteVariableByHandle().getAdsResponse().getErrorCode();
                    if (adsErrorCode != AdsErrorCode.NoError){
                        allSignalsProperlyTransferred = false;
                    }
                }
            }
        }
        else{
            throw new IOException("ADS state changed to " + adsState);
        }
        return allSignalsProperlyTransferred;
    };    
    
    protected boolean closingConnection() throws IOException, WrongUseException{
        boolean done = false;
        try{
        //release ads handles
        releaseHandles.transact(connection);
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
    
    protected void prepareSignalsForTransfer(){
        //collect signals to be transceived ...
        writeVariablesByHandle           = new WriteMultipleInChunks(NUMBEROFWRITESPERREQUEST);
        readVariablesByHandle            = new ReadMultipleInChunks(NUMBEROFREADSPERREQUEST);
        retrieveAdsVariableHandlesByName = new ReadWriteMultipleInChunks(NUMBEROFREADWRITESPERREQUEST);
        releaseHandles                   = new WriteMultipleInChunks(NUMBEROFWRITESPERREQUEST);
        for (Signal ios: getInputSignals()){
            retrieveAdsVariableHandlesByName.addAdsReadWrite(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName());
            readVariablesByHandle.addAmsPacket(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsReadVariableByHandle());
            releaseHandles.addAmsPacket(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsReleaseHandle());
        }
        for (Signal ios: getOutputSignals()){
            if (!getInputSignals().contains(ios)){//avoid in/out signals to be collected twice
                retrieveAdsVariableHandlesByName.addAdsReadWrite(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName());
                releaseHandles.addAmsPacket(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsReleaseHandle());
            }
            writeVariablesByHandle.addAmsPacket(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsWriteVariableByHandle());
        }        
    }
    
    protected void assignHandlesToSignals(){
        for (Signal ios: getInputSignals()){
        	((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsReadVariableByHandle().setHandle(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName().getHandle());
        }
        for (Signal ios: getOutputSignals()){
        	((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsWriteVariableByHandle().setHandle(((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName().getHandle());
        }
    }
        
    protected void logIoSignalsWithMissingHandle(){
        for (Signal ios: getInputSignals()){
            AdsErrorCode adsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName().getAdsResponse().getErrorCode();
            if (adsErrorCode != AdsErrorCode.NoError){
                Log.error("failed to retrieve handle for " + ((IoSignal)ios).getUri() + " ads error code: " + adsErrorCode);
            }
        }
        for (Signal ios: getOutputSignals()){
            AdsErrorCode adsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getAdsGetSymbolHandleByName().getAdsResponse().getErrorCode();
            if (adsErrorCode != AdsErrorCode.NoError){
                Log.error("failed to retrieve handle for " + ((IoSignal)ios).getUri() + " ads error code: " + adsErrorCode);
            }
        }
    }   
    
    
    protected void checkInSignal(IoSignal ioSignal) {
    	org.jpac.vioss.ads.RemoteSignalInfo rsi  = (org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo();
    	AdsReadVariableByHandle             rvbh = rsi.getAdsReadVariableByHandle();
        AdsErrorCode                adsErrorCode = rvbh.getAdsResponse().getErrorCode();
        AdsErrorCode            lastAdsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo()).getErrorCode();
        if (adsErrorCode == AdsErrorCode.NoError){
        	try {
	        	switch(((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getType()) {
			    	case Logical:
			            boolean boolVal = rvbh.getAdsResponse().getData().getBYTE(0) != 0;
			            ((LogicalValue)ioSignal.getRemoteSignalInfo().getValue()).set(boolVal);
			    		break;
			    	case SignedInteger:
			    		int intVal = rvbh.getAdsResponse().getData().getDINT(0); 
			            ((SignedIntegerValue)ioSignal.getRemoteSignalInfo().getValue()).set(intVal);
			    		break;
			    	case Decimal:
			    		throw new WrongUseException("Decimal currently not implemented for ADS protocol");
			    	case CharString:
			    		throw new WrongUseException("CharString currently not implemented for ADS protocol");
			    	default:
			    		throw new WrongUseException("signal type " + ((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getType() + " currently not implemented for ADS protocol");	   
	        	}
        	}
        	catch(AddressException exc) {
        		/*cannot happen*/
        	}
        	ioSignal.getRemoteSignalInfo().getValue().setValid(true);
            if (lastAdsErrorCode != null && lastAdsErrorCode != AdsErrorCode.NoError) {
            	//signal error state changed to NoError
                Log.info(ioSignal + " restored");
            }
       } else {
           if (lastAdsErrorCode != null && lastAdsErrorCode != adsErrorCode){
        	   //signal error state changed to Error
               Log.error(ioSignal + " got invalid due to ads Error " + adsErrorCode);
           }
           ((Signal)ioSignal).invalidate();
       }
       ((org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo()).setErrorCode(adsErrorCode);
       //Log.error("Ads Error code : " + adsErrorCode);
    }
    
    protected void checkoutSignal(IoSignal ioSignal) {
    	AdsWriteVariableByHandle wvbh         = ((org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo()).getAdsWriteVariableByHandle();
    	AdsErrorCode             adsErrorCode = wvbh.getAdsResponse().getErrorCode();
        AdsErrorCode         lastAdsErrorCode = ((org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo()).getErrorCode();
    	if (lastAdsErrorCode != null && lastAdsErrorCode != adsErrorCode) {
    		if (ioSignal.getErrorCode() != AdsErrorCode.NoError) {
                Log.error((Signal)ioSignal + " cannot be propagated to plc due to ads Error " + adsErrorCode);    			
    		} else {
                Log.info((IoSignal)ioSignal + " restored");    			
    		}
    	}
    	((org.jpac.vioss.ads.RemoteSignalInfo)(ioSignal).getRemoteSignalInfo()).setErrorCode(adsErrorCode);
    	try {
    		RemoteSignalInfo rsi = (RemoteSignalInfo)ioSignal.getRemoteSignalInfo(); 
        	switch(rsi.getType()) {
		    	case Logical:
		            wvbh.getData().setBYTE(0, rsi.getValue().isValid() && ((LogicalValue)rsi.getValue()).is(true) ? 1 : 0); 
		    		break;
		    	case SignedInteger:
		            wvbh.getData().setDINT(0, rsi.getValue().isValid() ? ((SignedIntegerValue)rsi.getValue()).get() : 0); 
		    		break;
		    	case Decimal:
		    		throw new WrongUseException("Decimal currently not implemented for ADS protocol");
		    	case CharString:
		    		throw new WrongUseException("CharString currently not implemented for ADS protocol");
		    	default:
		    		throw new WrongUseException("signal type " + ((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getType() + " currently not implemented for ADS protocol");	   
        	}
    	}
    	catch(AddressException | ValueOutOfRangeException exc) {
    		/*cannot happen*/
    	}
        if (Log.isDebugEnabled() && ((Signal)ioSignal).isChanged()){
            try{Log.debug(ioSignal + " transferred ");}catch(SignalInvalidException exc){/*cannot happen*/}
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
    
    class ConnectionRunner extends AsynchronousTask{ 
        private Connection           connection;
        private boolean              connected;
        public ConnectionRunner(String identifier){
            super(identifier);
        }
        
        @Override
        public void doIt() throws ProcessException {
            AdsState  adsState         = AdsState.Undefined;
            boolean   exceptionOccured = false;
            
            connected = false;
            Log.info("establishing connection for " + getInputSignals().size() + " input and " + getOutputSignals().size() + " output signals ...");
            do{
                do{
                    try{
                        connection = new Connection(getUri().getHost());
                        //wait, until plc is running
                        do{
                            adsReadState.transact(connection);
                            adsState = adsReadState.getAdsState();
                            if (adsState != AdsState.Run){
                                if (Log.isDebugEnabled())Log.debug("current ADS state: " + adsState);
                                try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};
                            }
                        }
                        while(adsState != AdsState.Run);
                        connected  = true;
                    }
                    catch(Exception exc){
                        if (Log.isDebugEnabled())Log.error("Error:", exc);
                        try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};
                    }
                }
                while(!connected && !isTerminated());
                if (connected && !isTerminated()){
                    //try to retrieve variable handles
                    try{
                        prepareSignalsForTransfer();
                        //retrieve ads handles
                        retrieveAdsVariableHandlesByName.transact(connection);
                        assignHandlesToSignals();
                    }
                    catch(Exception exc){
                        logIoSignalsWithMissingHandle();        
                        //close connection
                        try{connection.close();}catch(Exception ex){};
                        connection       = null;
                        connected        = false;
                        exceptionOccured = true;
                        Log.error("Error:", exc);
                    }
                }
                try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};
            }
            while(!connected && !isTerminated() && !exceptionOccured);
            if (connected){
                Log.info("... connection established as " + connection.getLocalAmsNetId());            
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
