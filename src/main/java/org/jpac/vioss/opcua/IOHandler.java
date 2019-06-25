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

package org.jpac.vioss.opcua;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.jpac.Address;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.JPac;
import org.jpac.Module;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.Logical;
import org.jpac.LogicalValue;
import org.jpac.SignedInteger;
import org.jpac.SignedIntegerValue;
import org.jpac.Decimal;
import org.jpac.DecimalValue;
import org.jpac.CharString;
import org.jpac.CharStringValue;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;
import org.jpac.Signal;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    private final static String             HANDLEDSCHEME             = "OPC.TCP";
    private final static double             DEFAULTPUBLISHINGINTERVAL = 10.0;//ms
    private final static TimestampsToReturn DEFAULTTIMESTAMPTORETURN  = TimestampsToReturn.Neither;
    private final static int                CONNECTIONRETRYTIME       = 1000;//ms         

    public enum State             {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                 state;
    private Connection            connection;
    private ConnectionRunner      connectionRunner;
    private boolean               connected;
    private boolean               connecting;


    private ArrayList<IoSignal>   writeIoSignals;
    private ArrayList<DataValue>  writeDataValues;
    private ArrayList<NodeId>     writeNodeIds;
    private List<StatusCode>      returnedStatusCodes;
    private ArrayList<NodeId>     checkServerStatusId;
 
    private HashMap<IoSignal, NodeId> nodeIds;
    
    private UaSubscription        subscription;  
    
    private String                endpointUrl;  
    private String                endpointUrlExtension;
        
    public IOHandler(URI uri) throws IllegalUriException {
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            JPac.getInstance().unregisterCyclicTask(this);
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        
        this.state                 = State.IDLE;
        this.writeIoSignals        = new ArrayList<>();
        this.writeDataValues       = new ArrayList<>();
        this.writeNodeIds          = new ArrayList<>();
        this.connectionRunner      = new ConnectionRunner(DEFAULTPUBLISHINGINTERVAL, DEFAULTTIMESTAMPTORETURN);
        this.endpointUrl           = buildEndpointUrl(uri);
        this.endpointUrlExtension  = buildEndpointUrlExtension(uri);
        this.checkServerStatusId   = new ArrayList<>();
        this.checkServerStatusId.add(Identifiers.Server_ServerStatus_State);
        this.nodeIds               = new HashMap<>();
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
                    if (connected){
                        state = State.TRANSCEIVING;
                    }
                    break;
                case TRANSCEIVING:
                    try{
                        transceiving();
                    }
                    catch(IOException | ExecutionException exc){
                        Log.error("Error: server " + getEndpointUrl() + " failed: " + exc + ".");
                        invalidateInputSignals();
                        state = State.IDLE;
                    }
                    catch(Exception exc){
                        Log.error("Error: ", exc);
                        //unrecoverable error occured. Stop transmission.
                        state = State.STOPPED; 
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

    @Override
    public void stop() {
        try{
            Log.info("shutting down " + this);
            state = State.CLOSINGCONNECTION;
            connectionRunner.terminate();
            if (connected){
                //release subscription handles
                closingConnection();
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
    protected void connecting() throws WrongUseException, InconsistencyException{
        if (!connected){
            if (!connecting){
                connectionRunner.start();
                connecting = true;
            }
            else{
                //connect to plc in progress
                connected  = connectionRunner.isFinished();
                if (connected){
                    connection   = connectionRunner.getConnection();
                    subscription = connectionRunner.getSubscription();
                    connecting   = false;
                }
            }
        }
        else{
            throw new InconsistencyException("might not be called in connected state");
        }
    };
    
    /**
     * is called in every cycle while in state TRANSCEIVING
     */
    protected void transceiving() throws ProcessException, IOException, AddressException, ExecutionException {
        try{
            DataValue serverState = checkServerState(connection);
            if (serverState.getStatusCode().isBad()){
                throw new IOException("server status code " + serverState.getStatusCode());
            }
            //propagate input signals received over the subscription
            for(Signal ios: getInputSignals()){
            	//transfer a copy of the monitoredItem to the remoteSignalInfo's value
            	checkInSignal(ios);
            	//and transfer it to the signal
                ((IoSignal)ios).checkIn();
            }
            //prepare output signals for propagation to plc
            writeIoSignals.clear();
            writeNodeIds.clear();
            writeDataValues.clear();
            for(Signal ios: getOutputSignals()){
                IoSignal ioSignal = (IoSignal)ios; 
                if (ioSignal.isToBePutOut() && ((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).isRemotelyAvailable()){
                    ioSignal.resetToBePutOut();
                    //transfer changed signal value to the remoteSignalInfo value
                    ioSignal.checkOut();
                    //and prepare value to be transferred to the remote side
                    checkOutSignal(ios);
                    writeIoSignals.add(ioSignal);
                    writeNodeIds.add(((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getNodeId());
                    writeDataValues.add(((RemoteSignalInfo)ioSignal.getRemoteSignalInfo()).getWriteDataValue());
                }
            }
            if (!writeNodeIds.isEmpty()){
                boolean writeDone   = false;
                do{
                    try{
                        returnedStatusCodes = connection.getClient().writeValues(writeNodeIds, writeDataValues).get();
                        writeDone   = true;
                    }
                    catch(InterruptedException exc){};
                    //ExecutionException  will be thrown upwards
                }
                while(!writeDone);
                //check, if signals have been properly transmitted
                boolean transmissionFailed = false;
                int i = 0;
                for(IoSignal ios: writeIoSignals){
                    ios.setErrorCode(returnedStatusCodes.get(i));
                    if (returnedStatusCodes.get(i).isBad()){
                        Log.error("Error: failed to write signal " + ((RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getNodeId().getIdentifier() + " to " + endpointUrl + ". Returned status code: " + returnedStatusCodes.get(i));
                        transmissionFailed = true;
                    }
                    i++;
                }
                if (transmissionFailed){
                    throw new IOException("failed to write signals to opc ua server " + endpointUrl);
                }
            }
        }
        finally{
            //make shure, that output signals are marked as being put out
            for(Signal ios: getOutputSignals()){
                ((IoSignal)ios).resetToBePutOut();
            }    
        }
    };    
    
    protected void closingConnection(){
        boolean done = false;
        try{
            if (subscription != null){
                //release subscription
                subscription.deleteMonitoredItems(subscription.getMonitoredItems());
                connection.getClient().getSubscriptionManager().deleteSubscription(subscription.getSubscriptionId());
            }
        }
        finally{
            if(connected && connection != null){
                try{connection.close();}catch(Exception exc){/*ignore*/}
            }
            connected = false;
        }
    };
    
    protected void checkInSignal(Signal ios) {
        IoSignal         ioSig = (IoSignal)ios;
        RemoteSignalInfo rsi   = (RemoteSignalInfo)ioSig.getRemoteSignalInfo();
        synchronized (rsi) {
        	if (rsi.getMonitoredItemValueUpdated()) {
		        if (rsi.getMonitoredItemValue().getStatusCode().isGood()){
		        	//make a physical copy of the value because its corresponding monitoredItem is written asynchronously by the OPC/UA API
		        	switch(rsi.getType()) {
		        		case Logical:
		        			((LogicalValue)rsi.getValue()).set((boolean)rsi.getMonitoredItemValue().getValue().getValue());
		        			break;
		        		case SignedInteger:
		        			((SignedIntegerValue)rsi.getValue()).set((int)rsi.getMonitoredItemValue().getValue().getValue());
		        			break;
		        		case Decimal:
		        			((DecimalValue)rsi.getValue()).set((double)rsi.getMonitoredItemValue().getValue().getValue());
		        			break;
		        		case CharString:
		        			((CharStringValue)rsi.getValue()).set(new String((String)rsi.getMonitoredItemValue().getValue().getValue()));
		        			break;
		        		default:
		        			throw new WrongUseException("type " + rsi.getType() + " currently not supported with OPC/UA protocol");
		        	}
		        	rsi.getValue().setValid(true);
		            rsi.setCheckInFaultLogged(false);
		       } else{
		           if (rsi.isCheckInFaultLogged()){
		               Log.error(((IoSignal)ioSig).getUri() + " got invalid due to server side error: " + rsi.getMonitoredItemValue().getStatusCode());
		               rsi.setCheckInFaultLogged(true);
		           }
		           rsi.getValue().setValid(false);
		       }		
	        }
        }
    }

    protected void checkOutSignal(Signal ios) {
        IoSignal         ioSig = (IoSignal)ios;
        RemoteSignalInfo rsi   = (RemoteSignalInfo)ioSig.getRemoteSignalInfo();
    	switch(ioSig.getRemoteSignalInfo().getType()) {
			case Logical:
		        rsi.setWriteDataValue(new DataValue(new Variant(ios.isValid() ? ((Logical)ios).get() : null), null, null));
				break;
			case SignedInteger:
		        rsi.setWriteDataValue(new DataValue(new Variant(ios.isValid() ? ((SignedInteger)ios).get() : null), null, null));
				break;
			case Decimal:
		        rsi.setWriteDataValue(new DataValue(new Variant(ios.isValid() ? ((Decimal)ios).get() : null), null, null));
				break;
			case CharString:
		        rsi.setWriteDataValue(new DataValue(new Variant(ios.isValid() ? ((CharString)ios).get() : null), null, null));
				break;
			default:
				throw new WrongUseException("type " + ioSig.getRemoteSignalInfo().getType() + " currently not supported with OPC/UA protocol");
    	}
    }

    protected DataValue checkServerState(Connection connection){
        DataValue status = null; 
        do{//retry in cases of InterruptedExceptions
           try{
               status = connection.getClient().readValues(0.0, TimestampsToReturn.Both, checkServerStatusId).get().get(0);               
           }
           catch(InterruptedException exc){}
           catch(ExecutionException   exc){
               status = new DataValue(StatusCode.BAD);
           };
        }
        while(status == null);
        return status;
    }

    protected void invalidateInputSignals(){
        for(Signal ios: getInputSignals()){
            try{ios.invalidate();}catch(SignalAccessException exc){/*cannot happen*/};
        }        
    }
    
    protected String buildEndpointUrl(URI uri){
        StringBuilder epPrefix = null;
        StringBuilder epSuffix = new StringBuilder();
        String[] pathTokens = uri.getPath().substring(1).split("/");
        int numberOfEndpointRelatedTokens = pathTokens.length - 2; //last two tokens are /<namespace index>/<node identifier>
        epPrefix = new StringBuilder("opc.tcp://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : ""));
        for (int i = 0; i < numberOfEndpointRelatedTokens; i++ ){
            epSuffix.append("/").append(pathTokens[i]);
        }
        return epPrefix.append(epSuffix).toString();
    }
                    
    protected String buildEndpointUrlExtension(URI uri){
        StringBuilder epSuffix = new StringBuilder();
        String[] pathTokens = uri.getPath().substring(1).split("/");
        int numberOfEndpointRelatedTokens = pathTokens.length - 2; //last two tokens are /<namespace index>/<node identifier>
        for (int i = 0; i < numberOfEndpointRelatedTokens; i++ ){
            epSuffix.append("/").append(pathTokens[i]);
        }
        return epSuffix.toString();
    }
    
    String encloseWithQuotes(String identifier) {	
    	return "\"" + identifier.replace (".", "\".\"") + "\"";
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
            isHandledByThisInstance &= this.endpointUrlExtension.equals(buildEndpointUrlExtension(uri));
        }
        catch(UnknownHostException exc){};
        return isHandledByThisInstance;
    }
    
    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }
    
    public String getEndpointUrl(){        
        return this.endpointUrl;
    }

    @Override
    public boolean isFinished() {
        return state == State.STOPPED;
    }
    
    @Override
    public String toString(){
        return getClass().getCanonicalName() + "(" + getEndpointUrl() + ")";
    }
    
    class ConnectionRunner extends AsynchronousTask{ 
        private Connection                            connection;
        private UaSubscription                        subscription;
        private List<UaMonitoredItem>                 monitoredItems;
        private double                                publishingTime;
        private TimestampsToReturn                    timestampsToReturn;

        public ConnectionRunner(double publishingTime, TimestampsToReturn timestampsToReturn){
            super("ConnectionRunner");
            this.publishingTime            = publishingTime;
            this.timestampsToReturn        = timestampsToReturn;
        }
        
        @Override
        public void doIt() throws ProcessException {
            boolean connected    = false;
            boolean errorOccured = false;
            
            Log.info("establishing connection to server " + getEndpointUrl() + " ...");
            do{
                try{
                    //check, if connection to server has already been established during this session
                    if (connection == null){
                        connection = new Connection(getEndpointUrl());
                        connection.connect();
                    }
                    //connection exists. Just wait, until the server is able to communicate
                    //do this both for new connections and reconnections
                    DataValue serverState = checkServerState(connection);
                    if (serverState.getStatusCode().isGood()){
                        //check, if all desired signals can be retrieved from the connected server
                        //tag signals accordingly (remotelyAvailable == true/false)
                        checkIfAllNodesAreAvailableOnServer(getInputSignals());                    
                        checkIfAllNodesAreAvailableOnServer(getOutputSignals());
                        ArrayList<MonitoredItemCreateRequest> monitoredItemsRequestList = new ArrayList<>();
                        //collect all input signals which are currently available on the remote server
                        for (Signal ios: getInputSignals()){
                            if (((RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).isRemotelyAvailable()){
                                monitoredItemsRequestList.add(getMonitoredItemCreateRequest(ios));
                            }
                        }
                        if (monitoredItemsRequestList.size() > 0){
                            //subscribe input signals
                            subscription   = connection.getClient().getSubscriptionManager().createSubscription(publishingTime).get();
                            monitoredItems = subscription.createMonitoredItems(timestampsToReturn, monitoredItemsRequestList).get();
                            assignMonitoredItemsToInputSignals();
                        }
                        connected  = true;                        
                    }
                }
                catch(Exception exc){
                    if  (connection != null){
                        try{connection.close();}catch(Exception ex){/*ignore*/};
                        connection = null;
                    }
                    if (Log.isDebugEnabled())Log.error("Error:", exc);
                }
                catch(Error exc){
                    if  (connection != null){
                        try{connection.close();}catch(Exception ex){/*ignore*/};
                        connection = null;
                    }
                    Log.error("Error:", exc);
                    errorOccured = true;
                }
                if (!connected){
                    try{Thread.sleep(CONNECTIONRETRYTIME);}catch(InterruptedException ex){/*cannot happen*/};                    
                }
            }
            while(!connected && !isTerminated() && !errorOccured);
            if (connected){
                Log.info("... connection to server " + getEndpointUrl() + " established");            
            }
        }
        
        private void assignMonitoredItemsToInputSignals(){
            for (UaMonitoredItem mi: monitoredItems){
            	Signal sig = getInputSignals().get(mi.getClientHandle().intValue());
                ((RemoteSignalInfo)((IoSignal)sig).getRemoteSignalInfo()).assignMonitoredItem(mi);
                //Log.info("Signal " + sig + "Status code: " + mi.getStatusCode());//TODO            
            }
        }
        
        private boolean checkIfAllNodesAreAvailableOnServer(List<Signal> ioSignals) throws Exception{
            boolean                 done                 = false;
            boolean                 allAvailable         = true;
            ArrayList<ReadValueId>  readDataTypeValueIds = new ArrayList<>();
            ReadResponse            readResponse         = null;
            
            if (ioSignals != null && ioSignals.size() > 0){
                for(Signal ios: ioSignals){
                    readDataTypeValueIds.add(((RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).getReadDataTypeValueId());
                }
                do{
                    try{
                        readResponse = getConnection().getClient().read(1000.0, TimestampsToReturn.Neither, readDataTypeValueIds).get();
                        done = true;
                    }
                    catch(InterruptedException exc){};
                }
                while(!done);
                if (readResponse != null){
                    int i = 0;
                    for(Signal ios: ioSignals){
                        StatusCode statusCode = readResponse.getResults()[i].getStatusCode();
                        ((RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo()).setRemotelyAvailable(statusCode.isGood());
                        if (statusCode.isBad()){
                            Log.error("failed to access '" + ((IoSignal)ios).getUri() + "'. Opc ua status code: " + statusCode);
                            allAvailable = false;
                        }
                        i++;
                    }
                }
            }
            return allAvailable;
        }
        
        public MonitoredItemCreateRequest getMonitoredItemCreateRequest(Signal signal) {
            MonitoredItemCreateRequest mcr = null;
            try{
            	//TODO samplingInterval, queueSize, discardOldest as driver parameters in configuration ????
                MonitoringParameters params = new MonitoringParameters(uint(getInputSignals().indexOf(signal)), (double)(JPac.getInstance().getCycleTime()/Module.ms), null, uint(1), true);       
                mcr                         = new MonitoredItemCreateRequest(((RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo()).getReadValueId(), MonitoringMode.Reporting, params);
            }
            catch(InconsistencyException exc){
                Log.error("Error: ",exc);
            }
            return mcr;
        }
                
        public Connection getConnection(){
            return this.connection;
        }

        public UaSubscription getSubscription(){
            return this.subscription;
        }
        
        public List<UaMonitoredItem> getMonitoredItems(){
            return this.monitoredItems;
        }        
    }       
}
