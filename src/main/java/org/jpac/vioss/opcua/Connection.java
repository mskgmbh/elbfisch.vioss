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
package org.jpac.vioss.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.slf4j.LoggerFactory;
import org.jpac.JPac;
import org.slf4j.Logger;


/**
 * represents a TCP/IP connection to a MODBUS plc.
 *
 */
public class Connection{
    static Logger Log = LoggerFactory.getLogger("jpac.vioss.opcua");

    private final static String DEFAULTAPPLICATIONAMEPREFIX = "elbfisch instance";
    private final static String DEFAULTAPPLICATIONURI       = "urn:elbfisch:opcua";
    private final static int    DEFAULTREQUESTTIMEOUT       = 2000;//ms
    
    private final static String KEYSTORECLIENTALIAS         = "client-ai";//TODO must be changed to Elbfisch specific id's
    private final static String KEYSTOREPASSWORD            = "password";
    private final static String KEYSTOREINSTANCE            = "PKCS12";
    private final static String DEFAULTCERTSFILE            = "example-certs.pfx";
        
    protected String            endpointUrl;
    protected String            applicationName;
    protected String            applicationUri;
    protected SecurityPolicy    securityPolicy;
    protected X509Certificate   clientCertificate;
    protected KeyPair           clientKeyPair;
    protected IdentityProvider  identityProvider;
    protected int               requestTimeout;
    protected KeyStore          keyStore;
    protected OpcUaClient       client;
    protected OpcUaClientConfig clientConfig;
    
    protected boolean   connected;
                    
    /**
     * an instance of Connection is created and the connection to given the opc ua server is initiated immediately
     * @param endpointUrl url of the (server) endpoint to connect to (example: "opc.tcp://localhost:12685/elbfisch"
     * @throws java.lang.Exception
     */
    public Connection(String endpointUrl) throws Exception{
        this.endpointUrl       = endpointUrl;
        this.applicationName   = DEFAULTAPPLICATIONAMEPREFIX + JPac.getInstance().getInstanceIdentifier();
        this.applicationUri    = DEFAULTAPPLICATIONURI;
        this.securityPolicy    = SecurityPolicy.None;
        this.clientCertificate = getDefaultCertificate();
        this.clientKeyPair     = getDefaultKeyPair();
        this.identityProvider  = new AnonymousProvider();
        this.requestTimeout    = DEFAULTREQUESTTIMEOUT;
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
//        EndpointDescription[] endpoints = null;
        List<EndpointDescription> endpoints = null;

//        endpoints = UaTcpStackClient.getEndpoints(endpointUrl).get();
        endpoints = DiscoveryClient.getEndpoints(endpointUrl).get();
        if (endpoints == null){
            throw new IOException("endpoint " + endpointUrl + " cannot be reached");
        }
        
//        EndpointDescription endpointDescription = Arrays.stream(endpoints)
          EndpointDescription endpointDescription = endpoints.stream()
//                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                .findFirst().orElseThrow(() -> new IOException("endpoints for " + endpointUrl + " not reachable"));              
        clientConfig = OpcUaClientConfig.builder()
            .setEndpoint(endpointDescription)
            .setIdentityProvider(identityProvider)
            .setApplicationName(LocalizedText.english(applicationName))
            .setApplicationUri(applicationUri)
            .setCertificate(clientCertificate)
            .setKeyPair(clientKeyPair)
            .setRequestTimeout(uint(requestTimeout))
            .build(); 
//        client = new OpcUaClient(clientConfig);
        client = OpcUaClient.create(clientConfig);
        client.connect().get();
        connected = true;
    }
    
    /**
     * use to close an existing connection.
     * @throws java.lang.Exception
     */
    public synchronized void close() throws Exception{
        if (client != null){
            client.disconnect().get();
        }
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
     * @return the applicationUri
     */
    public String getApplicationUri() {
        return applicationUri;
    }

    /**
     * @param applicationUri the applicationUri to set
     * @return  this connection
     */
    public Connection setApplicationUri(String applicationUri) {
        this.applicationUri = applicationUri;
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
     * @return the identityProvider
     */
    public IdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    /**
     * @param identityProvider the identityProvider to set
     * @return this connection
     */
    public Connection setIdentityProvider(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
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
     * @return the securityPolicy
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }

    /**
     * @param securityPolicy the securityPolicy to set
     */
    public Connection setSecurityPolicy(SecurityPolicy securityPolicy) {
        this.securityPolicy = securityPolicy;
        return this;
    }
    
    public OpcUaClient getClient(){
        return this.client;
    }
    
    public UaVariableNode getVariableNode(NodeId nodeId) throws InterruptedException, ExecutionException{
        return (UaVariableNode)client.getAddressSpace().getVariableNode(nodeId).get();
    }
    
    public UaSubscription getSubscription(double publishingIntervall) throws Exception{
        return client.getSubscriptionManager().createSubscription(publishingIntervall).get();
    }

    /**
     * @return the connected
     */
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + endpointUrl + ")";
    }
}
