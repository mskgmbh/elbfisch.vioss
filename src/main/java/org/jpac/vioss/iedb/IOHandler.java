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

package org.jpac.vioss.iedb;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.jpac.AsynchronousTask;
import org.jpac.CharStringValue;
import org.jpac.DecimalValue;
import org.jpac.InconsistencyException;
import org.jpac.IoDirection;
import org.jpac.JPac;
import org.jpac.LogicalValue;
import org.jpac.ProcessException;
import org.jpac.SignalAccessException;
import org.jpac.SignedIntegerValue;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.eclipse.paho.client.mqttv3.*;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.iedb.json.OpcUaConnectorConfig;
import org.jpac.vioss.iedb.json.DataPointDefinition;
import org.jpac.vioss.iedb.json.ValueList;
import org.jpac.vioss.iedb.json.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.jpac.Signal;

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler implements MqttCallback{
    static Logger Log = LoggerFactory.getLogger("jpac.vioss.iedb");
    private final static String  HANDLEDSCHEME             = "IE.DB";
    private final static int     CONNECTIONRETRYTIME       = 1000;//ms
    private final static String  PARAMETERCREDENTIALS      = "credentials";          
    private final static String  PARAMETERUSERNAME         = "username";
    private final static String  PARAMETERPASSWORD          = "password";
    private final static String  PARAMETERSSL              = "ssl";

    private final static String  PARAMETERSSLKEYKEYSTOREPATH    = "sslkeystorepath";
    private final static String  PARAMETERSSLKEYSTOREPASSWORD   = "sslkeystorepassword";
    private final static String  PARAMETERSSLKEYSTORETYPE       = "sslkeystoretype";
    private final static String  PARAMETERSSLTRUSTSTOREPATH     = "truststorepath";
    private final static String  PARAMETERSSLTRUSTSTOREPASSWORD = "truststorepassword";
    private final static String  PARAMETERSSLTRUSTSTORETYPE     = "trustkeystoretype";

    private final static String  METADATATAG                    = "/m/";
    private final static String  STATUSTAG                      = "/s/";
    private final static String  DATATAG                        = "/d/";
    private final static String  DATAPOINTTAG                   = "/dp/";
    private final static String  UPDATEREQUESTTOPIC             = "ie/c/j/simatic/v1/updaterequest";
    private final static String  UPDATEREQUESTPATH              = "{\"Path\": \"opcuac1/PLC_S7_1500/default\"}";
    public enum State             {IDLE, CONNECTING, SUBSCRIBING, TRANSCEIVING, CLOSINGCONNECTION, STOPPED};  
    
    private State                 state;
    private Connection            connection;
    private ConnectionRunner      connectionRunner;
    private boolean               connected;
    private boolean               connecting;
    private boolean               subscribed;
    private boolean               useSsl;
    
    private String                endpointUrl;  
    private MqttConnectOptions    mqttConnectOptions;

    private String                metaDataTopic;
    private String                dataTopic;
    private String                pubTopic;
    private String                statusTopic;
    private String                updateRequestTopic;
    private boolean               metaDataReceived;
    private boolean               awaitingMetadata;
    private MqttMessage           mqttMetaDataMessage;
    private MqttMessage           mqttDataMessage;
    private MqttMessage           mqttStatusMessage;
    private int                   sequenceNumber;

    private HashMap<String, DataPointDefinition> dataPointDefinitionsMap      = new HashMap<>();
    private HashMap<String, IoSignal>            signalByTopicIdMap           = new HashMap<>();
    private ArrayList<IoSignal>                  listOfSignalsToBeTransmitted = new ArrayList<>();
    private ObjectMapper                         objectMapper                 = new ObjectMapper();
        
    public IOHandler(URI uri, SubnodeConfiguration parameterConfiguration) throws IllegalUriException {
        //example UIR: "ie.db://192.168.0.181:9883/ie/d/j/simatic/v1/opcuac1/dp/msk/automation/byteArray0"
        //                                        "ie/c/j/simatic/v1/updaterequest";
        super(uri, parameterConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            JPac.getInstance().unregisterCyclicTask(this);
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        //build metadata topic from uri path
        metaDataTopic = uri.getPath();
        //strip trailing characters after /dp
        int dpIndex = metaDataTopic.indexOf(DATAPOINTTAG);
        if (dpIndex != -1) {
            metaDataTopic = metaDataTopic.substring(1, dpIndex + DATAPOINTTAG.length() - 1);
        }
        else {
            throw new IllegalArgumentException("Metadata topic does not contain DATAPOINTTAG (/dp): " + metaDataTopic);
        }
        //replace DATA or STATUS tag with METADATA tag to get metadata topic
        //substitute 4th DATA or STATUSTAG with METADATATAG
        if (metaDataTopic.contains(DATATAG)) {
            metaDataTopic = metaDataTopic.replaceFirst(DATATAG, METADATATAG);
        }
        else if (metaDataTopic.contains(STATUSTAG)) {
            metaDataTopic = metaDataTopic.replaceFirst(STATUSTAG, METADATATAG);
        }
        else {
            throw new IllegalArgumentException("Metadata topic does not contain DATA or STATUS tag (/d/ or /s/): " + metaDataTopic);
        }
        // // Derive data and status topics from metadata topic
        // dataTopic   = metaDataTopic.replaceFirst(METADATATAG, DATATAG);
        // statusTopic = metaDataTopic.replaceFirst(METADATATAG, STATUSTAG);
       
        // Parse SSL parameter from URI path, default to false
        this.useSsl = uri.getQuery() != null && uri.getQuery().contains("ssl=true");
        
        // Use ssl:// or tcp:// based on the ssl parameter
        String protocol = useSsl ? "ssl://" : "tcp://";
        this.endpointUrl = protocol + uri.getHost() + ":" + uri.getPort();

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        // Configure SSL socket factory if SSL is enabled
        if (useSsl) {
            try {
                HierarchicalConfiguration sslConfiguration = getParameterConfiguration().configurationAt(PARAMETERSSL);
                String keyStorePath       = sslConfiguration.getString(PARAMETERSSLKEYKEYSTOREPATH);
                String keyStorePassword   = sslConfiguration.getString(PARAMETERSSLKEYSTOREPASSWORD);
                String keyStoreType       = sslConfiguration.getString(PARAMETERSSLKEYSTORETYPE); // PKCS12, PEM, JKS, etc.
                String trustStorePath     = sslConfiguration.getString(PARAMETERSSLTRUSTSTOREPATH);
                String trustStorePassword = sslConfiguration.getString(PARAMETERSSLTRUSTSTOREPASSWORD);
                String trustStoreType     = sslConfiguration.getString(PARAMETERSSLTRUSTSTORETYPE); // PKCS12, PEM, JKS, etc.

                // 1. Load client key store (PKCS12 or JKS)
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

                // 2. Initialize KeyManagerFactory with client key
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, keyStorePassword.toCharArray());

                // 3. Load trust store (for server cert validation)
                KeyStore trustStore = KeyStore.getInstance(trustStoreType);
                trustStore.load(new FileInputStream(trustStorePath), trustStorePassword.toCharArray());

                // 4. Initialize TrustManagerFactory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                // 5. Initialize SSLContext
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                SocketFactory sslSocketFactory = sslContext.getSocketFactory();
                mqttConnectOptions.setSocketFactory(sslSocketFactory);
                Log.info("SSL socket factory configured for MQTT connection");
            } catch (Exception e) {
                Log.error("Failed to configure SSL socket factory: " + e.getMessage(), e);
            }
        }
        else{
            HierarchicalConfiguration tcpConfiguration = getParameterConfiguration().configurationAt(PARAMETERCREDENTIALS);
            String username = tcpConfiguration.getString(PARAMETERUSERNAME);
            String password = tcpConfiguration.getString(PARAMETERPASSWORD);
            mqttConnectOptions.setUserName(username);  
            mqttConnectOptions.setPassword(password.toCharArray());            
        }
        mqttMetaDataMessage    = new MqttMessage();
        mqttDataMessage        = new MqttMessage();
        mqttStatusMessage      = new MqttMessage();        

        this.state             = State.IDLE;
        this.connectionRunner  = new ConnectionRunner();
        this.sequenceNumber    = 0;
    }
    
    // /**
    //  * Configures SSL socket factory for MQTT connection options
    //  * @param options the MqttConnectOptions to configure
    //  */
    // private void configureSslSocketFactory(MqttConnectOptions options) {
    //     try {
    //         // Create default SSL context which uses the system's default trust store
    //         SSLContext sslContext = SSLContext.getDefault();
    //         SocketFactory sslSocketFactory = sslContext.getSocketFactory();
    //         options.setSocketFactory(sslSocketFactory);
    //         Log.info("SSL socket factory configured for MQTT connection");
    //     } catch (Exception e) {
    //         Log.error("Failed to configure SSL socket factory: " + e.getMessage(), e);
    //     }
    // }

    @Override
    public void run(){
        try{
            switch(state){
                case IDLE:
                    invalidateInputSignals();//invalidate signals in case of connection loss
                    try{connection.close();}catch(Exception exc){/*ignore*/}//close connection if open before
                    connection       = null;           
                    connected        = false;
                    connecting       = false;
                    metaDataReceived = false;
                    awaitingMetadata = false;

                    state            = State.CONNECTING;
                    //connect right away
                case CONNECTING:
                    connecting();
                    if (connected){
                        state = State.SUBSCRIBING;
                        Log.debug("Subscribing to MQTT topics (" + metaDataTopic + ") ...");
                    }
                    break;
                case SUBSCRIBING:
                    try{
                        subscribing();
                        if (subscribed){
                            state = State.TRANSCEIVING;
                            Log.debug("transceiving ...");

                        };
                    }
                    catch(IOException exc){
                        Log.error("Error: server " + getEndpointUrl() + " failed while subscribing: " + exc + ".");
                        invalidateInputSignals();
                        state = State.IDLE;
                    }
                    catch(Exception exc){
                        Log.error("Error: ", exc);
                        invalidateInputSignals();
                        state = State.STOPPED;
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
                        Log.debug("Connection lost, returning to IDLE state.");
                    }
                    catch(Exception exc){
                        Log.error("Error: ", exc);
                        //unrecoverable error occured. Stop transmission.
                        //invalidate all signals
                        invalidateInputSignals();
                        state = State.STOPPED; 
                        Log.debug("Unrecoverable error occurred, stopping transmission.");
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

    protected void subscribing() throws ProcessException, IOException, MqttException {
        if (!awaitingMetadata){
            //subscribe to the metadata topic and wait until industrial edge data broker responds
            connection.getMqttClient().subscribe(metaDataTopic, 1);
            awaitingMetadata = true;
        }
        else{
            //subscription underway, waiting for metadata response
            if (metaDataReceived){
                ObjectMapper         mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                OpcUaConnectorConfig parsed = mapper.readValue(getLastMqttMetaDataMessage().getPayload(), OpcUaConnectorConfig.class);
                metaDataReceived = false;
                //System.out.println("\nDeserialized object: " + parsed);
                //print names of all data points definitions
                if (Log.isDebugEnabled()){
                    Log.debug("Data Point Names received from industrial edge data broker:");
                    parsed.getConnections().forEach(connection -> {
                        connection.getDataPoints().forEach(dataPoint -> {
                            dataPoint.getDataPointDefinitions().forEach(definition -> {
                                Log.debug("  " + definition.getName());
                            });
                        });
                    });
                    //clear the existing data point definitions map before repopulating it
                    dataPointDefinitionsMap.clear();
                    parsed.getConnections().forEach(connection -> connection.getDataPoints().forEach(dp -> {
                        dp.getDataPointDefinitions().forEach(def -> {
                            dataPointDefinitionsMap.put(def.getName(), def);
                        });
                    }));
                    //assign topic IDs to input signals based on the data point definitions
                    signalByTopicIdMap.clear();
                    getInputSignals().forEach(signal -> {
                        RemoteSignalInfo rsi = (RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo();
                        DataPointDefinition def = dataPointDefinitionsMap.get(rsi.getTopic());
                        if (def != null){
                            if (typeMatches((IoSignal)signal, def)) {
                                rsi.setTopicId(def.getId());
                                if (isReadable(def)){
                                    signalByTopicIdMap.put(rsi.getTopicId(), (IoSignal)signal);
                                    Log.debug("Signal '" + signal.getQualifiedIdentifier() + "' assigned to '" + ((RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo()).getTopic() + "' by id " + ((RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo()).getTopicId());
                                }
                                else{
                                    Log.error("Signal '" + signal.getQualifiedIdentifier() + "' is not readable according to its data point definition.");
                                }
                            }
                            else{
                                Log.error("Type mismatch for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                                throw new InconsistencyException("Type mismatch for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                            }
                        }
                        else{
                            Log.error("No data point definition found for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                            throw new InconsistencyException("No data point definition found for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                        }
                    });
                    //assign output signals, too
                    getOutputSignals().forEach(signal -> {
                        RemoteSignalInfo rsi = (RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo();
                        DataPointDefinition def = dataPointDefinitionsMap.get(rsi.getTopic());
                        if (def != null){
                            if (typeMatches((IoSignal)signal, def)) {
                                rsi.setTopicId(def.getId());
                                if (isWritable(def)){
                                    Log.debug("Signal '" + signal.getQualifiedIdentifier() + "' assigned to '" + ((RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo()).getTopic() + "' by id " + ((RemoteSignalInfo)((IoSignal)signal).getRemoteSignalInfo()).getTopicId());
                                }
                                else{
                                    Log.error("Signal '" + signal.getQualifiedIdentifier() + "' is not writable according to its data point definition.");
                                }
                            }
                            else{
                                Log.error("Type mismatch for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                                throw new InconsistencyException("Type mismatch for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                            }
                        }
                        else{
                            Log.error("No data point definition found for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                            throw new InconsistencyException("No data point definition found for IO signal " + signal.getQualifiedIdentifier() + " topic " + rsi.getTopic());
                        }
                    });
                    pubTopic = parsed.getConnections().get(0).getDataPoints().get(0).getPubTopic();//assume that there is one connection and one list of data point definitions
                    dataTopic = parsed.getConnections().get(0).getDataPoints().get(0).getTopic();//assume that there is one connection and one list of data point definitions
                    statusTopic = parsed.getStatusTopic();//assume that there is one connection and one list of data point definitions
                    Log.debug("Pub topic: " + pubTopic + ", Data topic: " + dataTopic + ", Status topic: " + statusTopic);
                    updateRequestTopic = UPDATEREQUESTTOPIC;//preliminary ?
                    Log.debug("Update request topic: " + updateRequestTopic);
                };
                awaitingMetadata = false;
                //subscribe to the status topic provided by the industrial edge data broker
                connection.getMqttClient().subscribe(statusTopic, 1);
                //subscribe to group data topic provided by the industrial edge data broker
                connection.getMqttClient().subscribe(dataTopic, 1);
                //request initial push of data values from the industrial edge data broker
                connection.getMqttClient().publish(updateRequestTopic, UPDATEREQUESTPATH.getBytes(), 1, false);
                subscribed = true;
            }
        }
    }
    
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
            listOfSignalsToBeTransmitted.clear();
            for(Signal ios: getOutputSignals()){
                IoSignal ioSignal = (IoSignal)ios; 
                if (ioSignal.isToBePutOut()){
                    ioSignal.resetToBePutOut();
                    //transfer changed signal value to the remoteSignalInfo value
                    ioSignal.checkOut();
                    //and prepare value to be transferred to the remote side
                    checkOutSignal(ios);
                    listOfSignalsToBeTransmitted.add(ioSignal);
                }
            }
            if (!listOfSignalsToBeTransmitted.isEmpty()){
                //build a bulk message to be transmitted to the industrial edge data broker
                ValueList valueList = new ValueList();
                valueList.setSequence(sequenceNumber++ % Short.MAX_VALUE);
                ArrayList<Value> listOfJsonValues = new ArrayList<>();
                for(IoSignal ioSignal: listOfSignalsToBeTransmitted){
                    listOfJsonValues.add(((RemoteSignalInfo)(ioSignal.getRemoteSignalInfo())).computeJsonValue());
                }
                //transmit value list to the industrial edge data broker
                valueList.setValues(listOfJsonValues);
                if (Log.isDebugEnabled()){
                    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                    String newValueJson = mapper.writeValueAsString(valueList);                 
                    Log.debug("publishing " + pubTopic + ": " + newValueJson);
                }
                connection.getMqttClient().publish(pubTopic, objectMapper.writeValueAsBytes(valueList), 1, false);
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
    
    protected MqttMessage getLastMqttMetaDataMessage(){
        synchronized (mqttMetaDataMessage) {
            return mqttMetaDataMessage;
        }
    }

    protected MqttMessage getLastMqttDataMessage(){
        synchronized (mqttDataMessage) {
            return mqttDataMessage;
        }
    }

    protected MqttMessage getLastMqttStatusMessage(){
        synchronized (mqttStatusMessage) {
            return mqttStatusMessage;
        }
    }

    protected void setLastMqttMetaDataMessage(MqttMessage message){
        synchronized (mqttMetaDataMessage) {
            mqttMetaDataMessage = message;
        }
    }

    protected void setLastMqttDataMessage(MqttMessage message){
        synchronized (mqttDataMessage) {
            mqttDataMessage = message;
        }
    }

    protected void setLastMqttStatusMessage(MqttMessage message){
        synchronized (mqttStatusMessage) {
            mqttStatusMessage = message;
        }
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

    @Override
    public void connectionLost(Throwable cause) {
        //initiate reconnect
        state = State.IDLE;
        Log.error("lost connection to server " + getEndpointUrl() + " cause: " + cause.getMessage());   
        Log.error("Error:", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (topic.equals(metaDataTopic)){
            Log.debug("Received MQTT metadata message: " + message);
            setLastMqttMetaDataMessage(message);
            metaDataReceived = true;
        } else if (topic.equals(dataTopic)){
            //Log.debug("Received MQTT data message: " + message);
            setLastMqttDataMessage(message);
            handleDataTopic(message);
        } else if (topic.equals(statusTopic)){
            Log.debug("Received MQTT status message: " + message);
            setLastMqttStatusMessage(message);
        } else {
            Log.debug("Received MQTT message on unknown topic: " + topic);
        }
        //do nothing, we are only interested in the message
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //do nothing
    }        
    
    public void handleDataTopic(MqttMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ValueList    parsed = mapper.readValue(message.getPayload(), ValueList.class);        
		Log.debug("handleDataTopic(): data message arrived ");
        parsed.getValues().forEach(value -> {
            Log.debug("Value ID: " + value.getId() + ", Quality Code: " + value.getQualityCode() + ", Timestamp: " + value.getTimestamp() + ", Value: " + value);
            IoSignal ioSignal = signalByTopicIdMap.get(value.getId());
            if (ioSignal != null) {
                //topic registered for input, takeover the value, all others are ignored
                assignTopicValue(ioSignal, value);
            }
        });
    }

    private boolean typeMatches(IoSignal signal, DataPointDefinition dpd){
        switch(signal.getRemoteSignalInfo().getType()){
            case Logical:
                return dpd.getDataType().equals("Bool") && dpd.getArrayDimensions() == null;
            case SignedInteger:
                return ((dpd.getDataType().equals("Int") || dpd.getDataType().equals("Byte"))) && dpd.getArrayDimensions() == null;
            case Decimal:
                return dpd.getDataType().equals("Float") && dpd.getArrayDimensions() == null;
            case CharString:
                return dpd.getDataType().equals("String") && dpd.getArrayDimensions() == null;
            default:
                break;
        } 
        //non basic types (arrays)
        if (signal instanceof IoByteArray && dpd.getDataType().equals("Byte") && dpd.getArrayDimensions() != null && dpd.getArrayDimensions().get(0) == 0 && dpd.getValueRank() != null && dpd.getValueRank() == 1) {//[0] is the default
            return true;
        } else if (signal instanceof IoShortArray && dpd.getDataType().equals("Int") && dpd.getArrayDimensions() != null && dpd.getArrayDimensions().get(0) == 0 && dpd.getValueRank() != null && dpd.getValueRank() == 1) {//[0] is the default
            return true;
        } else if (signal instanceof IoIntArray && dpd.getDataType().equals("Int") && dpd.getArrayDimensions() != null && dpd.getArrayDimensions().get(0) == 0 && dpd.getValueRank() != null && dpd.getValueRank() == 1) {//[0] is the default
            return true;
        } else if (signal instanceof IoFloatArray && dpd.getDataType().equals("Float") && dpd.getArrayDimensions() != null && dpd.getArrayDimensions().get(0) == 0 && dpd.getValueRank() != null && dpd.getValueRank() == 1) {
            return true;
        } else if (signal instanceof IoDoubleArray && dpd.getDataType().equals("Double") && dpd.getArrayDimensions() != null && dpd.getArrayDimensions().get(0) == 0 && dpd.getValueRank() != null && dpd.getValueRank() == 1) {
            return true;
        }
        return false;
    }

    private boolean isWritable(DataPointDefinition dpd){
        // Implement the logic to check if the signal is writable based on the data point definition
        return dpd.getAccessMode().contains("w");
    }

    private boolean isReadable(DataPointDefinition dpd){
        // Implement the logic to check if the signal is readable based on the data point definition
        return dpd.getAccessMode().contains("r");
    }

    private void assignTopicValue(IoSignal signal, Value value){
		synchronized(this){
			RemoteSignalInfo rsi = (RemoteSignalInfo)signal.getRemoteSignalInfo();
			switch(rsi.getType()){
				case Logical:
					((LogicalValue)rsi.getMonitoredItemValue()).set(((int)(value.getValue().get(0))) != 0);
                    rsi.setMonitoredValueUpdated(true);
					break;
				case SignedInteger:
					((SignedIntegerValue)rsi.getMonitoredItemValue()).set((int)(value.getValue().get(0)));
                    rsi.setMonitoredValueUpdated(true);//NOT TESTED
					break;
				case Decimal:
					((DecimalValue)rsi.getMonitoredItemValue()).set((double)(value.getValue().get(0)));
                    rsi.setMonitoredValueUpdated(true);//NOT TESTED
					break;
				case CharString:
					((CharStringValue)rsi.getMonitoredItemValue()).set(value.getValue().get(0).toString());
					rsi.setMonitoredValueUpdated(true);//NOT TESTED
					break;
				case Unknown:
					if (signal instanceof IoByteArray) {//NOT TESTED
						// Convert value list to byte array
                        byte[] byteArray = new byte[value.getValue().size()];
                        for (int i = 0; i < value.getValue().size(); i++) {
                            byteArray[i] = ((Number)value.getValue().get(i)).byteValue();
                        }
						((ByteArrayValue)rsi.getMonitoredItemValue()).set(byteArray);
						rsi.setMonitoredValueUpdated(true);
					} else if (signal instanceof IoShortArray) {//NOT TESTED
						// Convert value list to short array
                        short[] shorts = new short[value.getValue().size()];
                        for (int i = 0; i < value.getValue().size(); i++) {
                            shorts[i] = ((Number)value.getValue().get(i)).shortValue();
                        }
						((ShortArrayValue)rsi.getMonitoredItemValue()).set(shorts);
						rsi.setMonitoredValueUpdated(true);
					} else if (signal instanceof IoIntArray) {
						// Convert value list to int array
                        int[] ints = new int[value.getValue().size()];
                        for (int i = 0; i < value.getValue().size(); i++) {
                            ints[i] = ((Number)value.getValue().get(i)).intValue();
                        }
						((IntArrayValue)rsi.getMonitoredItemValue()).set(ints);
						rsi.setMonitoredValueUpdated(true);
					} else if (signal instanceof IoFloatArray) {//NOT TESTED
						// Convert value list to float array
                        float[] floats = new float[value.getValue().size()];
                        for (int i = 0; i < value.getValue().size(); i++) {
                            floats[i] = ((Number)value.getValue().get(i)).floatValue();
                        }
						((FloatArrayValue)rsi.getMonitoredItemValue()).set(floats);
						rsi.setMonitoredValueUpdated(true);
					} else if (signal instanceof IoDoubleArray) {//NOT TESTED
						// Convert value list to double array
                        double[] doubles = new double[value.getValue().size()];
                        for (int i = 0; i < doubles.length; i++) {
                            doubles[i] = ((Number)value.getValue().get(i)).doubleValue();
                        }
						((DoubleArrayValue)rsi.getMonitoredItemValue()).set(doubles);
						rsi.setMonitoredValueUpdated(true);
					}
					break;
				default:
			}
		}        
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
                
        public Connection getConnection(){
            return this.connection;
        }

        public void resetConnection(){
            this.connection = null;
        }
    }       
}
