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
import org.jpac.Address;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.NumberOutOfRangeException;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import static org.jpac.vioss.IOHandler.Log;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    private final static String  HANDLEDSCHEME       = "ADS";
    private final static int     TRANSMISSIONTIMEOUT = 1000;//ms
    private final static int     CONNECTIONRETRYTIME = 1000;//ms         

    public enum State            {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                state;
    private Connection           connection;
    private ConnectionRunner     connectionRunner;
    private boolean              connected;
    private boolean              connecting;
    private AdsReadWriteMultiple retrieveAdsVariableHandlesByName;
    private AdsReadMultiple      readVariablesByHandle;
    private AdsWriteMultiple     writeVariablesByHandle;
    private AdsWriteMultiple     releaseHandles;
    private AdsReadState         adsReadState;
        
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
                        transceiving();
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
        if (!readVariablesByHandle.getAmsPackets().isEmpty()){
            readVariablesByHandle.transact(connection);
            //propagate input signals
            for(org.jpac.plc.IoSignal ios: getInputSignals()){
                ios.checkIn();
            }
        }
        //prepare output signals for propagation to plc
        writeVariablesByHandle.clearAmsPackets();
        for(org.jpac.plc.IoSignal ios: getOutputSignals()){
            if (ios.isToBePutOut()){
                ios.resetToBePutOut();
                ios.checkOut();
                writeVariablesByHandle.addAmsPacket(((IoSignal)ios).getAdsWriteVariableByHandle());
            }
        }
        if (!writeVariablesByHandle.getAmsPackets().isEmpty()){
            writeVariablesByHandle.transact(connection);
        }
        return true;
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
        writeVariablesByHandle           = new AdsWriteMultiple();
        readVariablesByHandle            = new AdsReadMultiple();
        retrieveAdsVariableHandlesByName = new AdsReadWriteMultiple();
        releaseHandles                   = new AdsWriteMultiple();
        for (org.jpac.plc.IoSignal ios: getInputSignals()){
            retrieveAdsVariableHandlesByName.addAdsReadWrite(((IoSignal)ios).getAdsGetSymbolHandleByName());
            releaseHandles.addAmsPacket(((IoSignal)ios).getAdsReleaseHandle());
            readVariablesByHandle.addAmsPacket(((IoSignal)ios).getAdsReadVariableByHandle());
        }
        for (org.jpac.plc.IoSignal ios: getOutputSignals()){
            if (!retrieveAdsVariableHandlesByName.getAdsReadWrites().contains(ios)){//avoid int/out signals to be collected twice
                retrieveAdsVariableHandlesByName.addAdsReadWrite(((IoSignal)ios).getAdsGetSymbolHandleByName());
                releaseHandles.addAmsPacket(((IoSignal)ios).getAdsReleaseHandle());
            }
        }        
    }
    
    protected void assignHandlesToSignals(){
        for (org.jpac.vioss.IoSignal ios: getInputSignals()){
            ((IoSignal)ios).getAdsReadVariableByHandle().setHandle(((IoSignal)ios).getAdsGetSymbolHandleByName().getHandle());
        }
        for (org.jpac.vioss.IoSignal ios: getOutputSignals()){
            ((IoSignal)ios).getAdsWriteVariableByHandle().setHandle(((IoSignal)ios).getAdsGetSymbolHandleByName().getHandle());
        }
    }
        
    protected void logIoSignalsWithMissingHandle(){
        for (org.jpac.vioss.IoSignal ios: getInputSignals()){
            AdsErrorCode adsErrorCode = ((IoSignal)ios).getAdsGetSymbolHandleByName().getAdsResponse().getErrorCode();
            if (adsErrorCode != AdsErrorCode.NoError){
                Log.error("failed to retrieve handle for " + ios.getUri() + " ads error code: " + adsErrorCode);
            }
        }
        for (org.jpac.vioss.IoSignal ios: getOutputSignals()){
            AdsErrorCode adsErrorCode = ((IoSignal)ios).getAdsGetSymbolHandleByName().getAdsResponse().getErrorCode();
            if (adsErrorCode != AdsErrorCode.NoError){
                Log.error("failed to retrieve handle for " + ios.getUri() + " ads error code: " + adsErrorCode);
            }
        }
    }   
    
    protected void invalidateInputSignals() throws SignalAccessException{
        for (org.jpac.vioss.IoSignal ios: getInputSignals()){
            ios.invalidate();
        }        
    }
    
    @Override
    public boolean handles(Address address, URI uri) {
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
            AdsState  adsState = AdsState.Undefined;
            
            connected = false;
            Log.info("establishing connection ...");
            do{
                try{
                    connection = new Connection(getUri().getHost());
                    //wait, until plc is running
                    do{
                        adsReadState.transact(connection);
                        adsState = adsReadState.getAdsState();
                        if (adsState != AdsState.Run){
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
                    Log.info("... connection established");            
                }
                catch(Exception exc){
                    logIoSignalsWithMissingHandle();        
                    //close connection
                    try{connection.close();}catch(Exception ex){};
                    connection = null;
                    connected  = false;
                    Log.error("Error:", exc);
                }
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
