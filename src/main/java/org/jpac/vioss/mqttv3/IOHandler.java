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

package org.jpac.vioss.mqttv3;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.IoDirection;
import org.jpac.JPac;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.eclipse.paho.client.mqttv3.*;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jpac.Signal;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler implements MqttCallback{
    static Logger Log = LoggerFactory.getLogger("jpac.vioss.mqttv3");
    private final static String             HANDLEDSCHEME             = "MQTTV3.TCP";
    private final static int                CONNECTIONRETRYTIME       = 1000;//ms
    private final static String             PARAMETERCREDENTIALS      = "credentials";
    private final static String             PARAMETERUSERNAME         = "username";
    private final static String            PARAMETERPASSWORD          = "password";

    public enum State             {IDLE, CONNECTING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                 state;
    private Connection            connection;
    private ConnectionRunner      connectionRunner;
    private boolean               connected;
    private boolean               connecting;
    
    private String                endpointUrl;  
    private MqttConnectOptions    mqttConnectOptions;
        
    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException {
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            JPac.getInstance().unregisterCyclicTask(this);
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.endpointUrl = "tcp://" + uri.getHost() + ":" + uri.getPort();

        HierarchicalConfiguration deviceConfiguration = getParameterConfiguration().configurationAt(PARAMETERCREDENTIALS);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(deviceConfiguration.getString(PARAMETERUSERNAME));
        mqttConnectOptions.setPassword(deviceConfiguration.getString(PARAMETERPASSWORD).toCharArray());

        this.state                 = State.IDLE;
    
        this.connectionRunner      = new ConnectionRunner();
    }

    @Override
    public void run(){
        try{
            switch(state){
                case IDLE:
                    invalidateInputSignals();//invalidate signals in case of connection loss
                    try{connection.close();}catch(Exception exc){/*ignore*/}//close connection if open before
                    connection = null;           
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
                        //invalidate all signals
                        invalidateInputSignals();
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
            invalidateInputSignals();
            state = State.STOPPED;//stop processing
        }
        catch(Error err){
            Log.error("Error:", err);
            invalidateInputSignals();
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
                connectionRunner.resetConnection();//reset an propably open connection
                connectionRunner.start();
                connecting = true;
            }
            else{
                //connect to plc in progress
                connected  = connectionRunner.isFinished();
                if (connected){
                    connection   = connectionRunner.getConnection();
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
    protected void transceiving() throws ProcessException, IOException, AddressException, ExecutionException, MqttException {
        try{
            //propagate input signals received over the subscription
            for(Signal ios: getInputSignals()){
            	//transfer a copy of the monitoredItem to the remoteSignalInfo's value
            	checkInSignal(ios);
            	//and transfer it to the signal
                ((IoSignal)ios).checkIn();
            }
            for(Signal ios: getOutputSignals()){
                IoSignal ioSignal = (IoSignal)ios; 
                if (ioSignal.isToBePutOut()){
                    ioSignal.resetToBePutOut();
                    //transfer changed signal value to the remoteSignalInfo value
                    ioSignal.checkOut();
                    //and prepare value to be transferred to the remote side
                    checkOutSignal(ios);
                    writeRemoteItem((RemoteSignalInfo)(ioSignal.getRemoteSignalInfo())); // set initial value to remote item                    
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
        try{
            // if (subscription != null){
            //     //release subscription
            //     subscription.deleteMonitoredItems(subscription.getMonitoredItems());
            //     connection.getClient().getSubscriptionManager().deleteSubscription(subscription.getSubscriptionId());
            // }
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
        if (rsi.isMonitoredValueUpdated()) {
            synchronized (rsi) {
                rsi.setValue(rsi.getMonitoredItemValue());
            }
            rsi.setMonitoredValueUpdated(false);//mark monitored item as processed
            rsi.getValue().setValid(true);
            rsi.setCheckInFaultLogged(false);
        }
    };

    protected void checkOutSignal(Signal ios) {
        RemoteSignalInfo rsi = (RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo();
        synchronized(rsi) {
            rsi.getRemoteItemValue().setValue(rsi.getValue().getValue());
        }
    }

    protected void invalidateInputSignals(){
        for(Signal ios: getInputSignals()){
            try{ios.invalidate();}catch(SignalAccessException exc){/*cannot happen*/};
        }        
    }
    
    String encloseWithQuotes(String identifier) {	
    	return "\"" + identifier.replace (".", "\".\"") + "\"";
    }    
    
    @Override
    public boolean handles(URI uri, IoDirection ioDirection) {
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
    
    public String getEndpointUrl(){        
        return this.endpointUrl;
    }

    @Override
    public boolean isFinished() {
        return state == State.STOPPED;
    }

    private void writeRemoteItem(RemoteSignalInfo rsi) throws MqttException{
        var msg = new MqttMessage(rsi.genPayload());
        msg.setQos(1);
        connection.getMqttClient().publish(rsi.getTopic(), msg);
    }

    @Override
    public void connectionLost(Throwable cause) {
        //initiate reconnect
        state = State.IDLE;
        Log.error("lost connection to server " + getEndpointUrl() + " cause: " + cause.getMessage());   
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //do nothing
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //do nothing
    }        
    
    @Override
    public String toString(){
        return getClass().getCanonicalName() + "(" + getEndpointUrl() + ")";
    }
    
    class ConnectionRunner extends AsynchronousTask{ 
        private Connection connection;

        public ConnectionRunner(){
            super("ConnectionRunner");
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
                        connection = new Connection(getEndpointUrl(), mqttConnectOptions);
                        connection.connect();
                        connection.getMqttClient().setCallback(IOHandler.this);//let the ioHandler handle connection losses etc.
                        subscribeInputSignals();
                        connected = true;
                    }
                }
                catch(Exception exc){
                    if  (connection != null){
                        try{connection.close();}catch(Exception ex){/*ignore*/};
                        connection = null;
                    }
                    //if (Log.isDebugEnabled())Log.error("Error:", exc);
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

        private void subscribeInputSignals(){
            for(Signal ios: getInputSignals()){
                RemoteSignalInfo rsi = (RemoteSignalInfo)((IoSignal)ios).getRemoteSignalInfo();
                try{
                    connection.getMqttClient().subscribe(rsi.getTopic(), 1, (IMqttMessageListener)rsi);
                    Log.debug("Subscribed topic:'" + rsi.getTopic() + "'");
                }
                catch(MqttException exc){
                    Log.error("Error subscribing to topic " + rsi.getTopic() + " on MQTT broker " + getEndpointUrl(), exc);
                }
            }
        }
                
        public Connection getConnection(){
            return this.connection;
        }

        public void resetConnection(){
            this.connection = null;
        }
    }       
}
