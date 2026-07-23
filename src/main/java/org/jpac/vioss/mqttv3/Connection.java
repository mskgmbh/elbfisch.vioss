/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Connection.java (versatile input output subsystem)
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

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;


/**
 * represents a TCP/IP connection to a MODBUS plc.
 *
 */
public class Connection{
    static Logger Log = LoggerFactory.getLogger("jpac.vioss.mqttv3");

    private final static int    DEFAULTREQUESTTIMEOUT       = 2000;//ms
    
    private final static String KEYSTORECLIENTALIAS         = "client-ai";//TODO must be changed to Elbfisch specific id's
    private final static String KEYSTOREPASSWORD            = "password";
    private final static String KEYSTOREINSTANCE            = "PKCS12";
    private final static String DEFAULTCERTSFILE            = "example-certs.pfx";
        
    protected String             endpointUrl;
    protected String             applicationName;
    protected String             applicationUri;
    protected X509Certificate    clientCertificate;
    protected KeyPair            clientKeyPair;
    protected int                requestTimeout;
    protected KeyStore           keyStore;
    
    protected boolean            connected;
    protected MqttClient         mqttClient;
    protected MqttConnectOptions options;
                    
    /**
     * an instance of Connection is created and the connection to given MQTT broker is initiated immediately
     * @param endpointUrl url of the (server) endpoint to connect to (example: "mqtt.tcp://localhost:12685/...."
     * @throws java.lang.Exception
     */
    public Connection(String endpointUrl, MqttConnectOptions options) throws Exception{
        this.endpointUrl       = endpointUrl;
        this.clientCertificate = getDefaultCertificate();
        this.clientKeyPair     = getDefaultKeyPair();
        this.requestTimeout    = DEFAULTREQUESTTIMEOUT;
        this.mqttClient        = new MqttClient(endpointUrl, "Elbfisch MQTT client");
        this.options           = options;
        connected  = false;
    }

    private KeyStore getKeyStore() throws Exception{
        KeyStore ks;
        ks = KeyStore.getInstance(KEYSTOREINSTANCE);
        ks.load(getClass().getClassLoader().getResourceAsStream(DEFAULTCERTSFILE), KEYSTOREPASSWORD.toCharArray());        
        return ks;
    }
    
    private X509Certificate getDefaultCertificate() throws Exception{
        X509Certificate cert = null;
        keyStore = getKeyStore();
        Key clientPrivateKey = keyStore.getKey(KEYSTORECLIENTALIAS, KEYSTOREPASSWORD.toCharArray());
        if (clientPrivateKey instanceof PrivateKey) {
            cert = (X509Certificate) keyStore.getCertificate(KEYSTORECLIENTALIAS);
        }
        return cert;
    }
    
    private KeyPair getDefaultKeyPair() throws Exception{
        KeyPair keyPair = null;
        keyStore = getKeyStore();
        Key clientPrivateKey = keyStore.getKey(KEYSTORECLIENTALIAS, KEYSTOREPASSWORD.toCharArray());
        if (clientPrivateKey instanceof PrivateKey) {
            PublicKey clientPublicKey = getDefaultCertificate().getPublicKey();
            keyPair = new KeyPair(clientPublicKey, (PrivateKey) clientPrivateKey);
        }
        return keyPair;
    }

    /**
     * used to initialize the connection.
     * @throws java.io.IOException
     */
    public void connect() throws Exception{
        mqttClient.connect();
        connected = true;
    }
    
    /**
     * use to close an existing connection.
     * @throws java.lang.Exception
     */
    public synchronized void close() throws Exception{
        try{mqttClient.close();}catch(MqttException exc){/*ignore*/}
        connected = false;
    }
         
    /**
     * @return the applicationName
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName the applicationName to set
     * @return  this connection
     */
    public Connection setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * @return the clientCertificate
     */
    public Certificate getClientCertificate() {
        return clientCertificate;
    }

    /**
     * @param clientCertificate the clientCertificate to set
     * @return  this connection
     */
    public Connection setClientCertificate(X509Certificate clientCertificate) {
        this.clientCertificate = clientCertificate;
        return this;
    }

    /**
     * @return the clientKeyPair
     */
    public KeyPair getClientKeyPair() {
        return clientKeyPair;
    }

    /**
     * @param clientKeyPair the clientKeyPair to set
     * @return  this connection
     */
    public Connection setClientKeyPair(KeyPair clientKeyPair) {
        this.clientKeyPair = clientKeyPair;
        return this;
    }

    /**
     * @return the requestTimeout
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * @param requestTimeout the requestTimeout to set
     * @return  this connection
     */
    public Connection setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * @return the connected
     */
    public boolean isConnected() {
        return connected;
    }

    public MqttClient getMqttClient(){
        return this.mqttClient;
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + endpointUrl + ")";
    }
}
