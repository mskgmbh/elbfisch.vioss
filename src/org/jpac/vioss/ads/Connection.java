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

package org.jpac.vioss.ads;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.jpac.vioss.InvalidAddressException;
import org.jpac.vioss.LittleEndianDataInputStream;
import org.jpac.vioss.LittleEndianDataOutputStream;
/**
 * represents a TCP/IP connection to a MODBUS plc.
 *
 */
public class Connection{
    static Logger Log = Logger.getLogger("jpac.vioss.ads");
    
    static  final int                    ADSPORT           = 0xBF02; //48898
    static  final int                    SOCKETTIMEOUT     = 5000;   //ms
    static  final String                 AMSNETIDEXTENSION = ".1.1";
    static  final AmsPortNr              DEFAULTAMSPORTNR  = AmsPortNr.PlcRuntimeSystem1;
    
    private Socket                       socket;
    private LittleEndianDataInputStream  in;
    private LittleEndianDataOutputStream out;

    protected String                     localHost;
    protected String                     plcHost;
    protected AmsNetId                   targetAmsNetId;
    protected AmsNetId                   localAmsNetId;
    protected AmsPortNr                  targetAmsPortNr;
    protected AmsPortNr                  localAmsPortNr;
    
    protected boolean                    connected;
    protected int                        uniqueInvokeId;
        
    /**
     * an instance of Connection is created and the connection to given the plc is initiated immediately
     * REMARKS: target ip will be build on basis of the given given AMS net id (1st 4 bytes)
     *          connection to the remote ads router is done on port 0xBF02
     * @param amsNetId  AMS net id of the plc (e.g. 192.168.0.52.1.1)
     * @param amsPortNr AMS port number of the plc
     * @throws IOException
     * 
     */
    public Connection(AmsNetId amsNetId, AmsPortNr amsPortNr) throws IOException, InvalidAddressException {
        this.plcHost       = amsNetId.getIPv4();
        targetAmsNetId     = amsNetId;
        targetAmsPortNr    = amsPortNr;
        localAmsNetId      = new AmsNetId(InetAddress.getLocalHost().getHostAddress() + AMSNETIDEXTENSION);
        localAmsPortNr     = AmsPortNr.Local;
        uniqueInvokeId     = 0;
        initialize();
    }
            
    /**
     * an instance of Connection is created and the connection to given the plc is initiated immediately
     * REMARKS: AMS net id will be build on basis of the given given plcHost net id by appending ".1.1"
     *     AMS port number is assumed to be 851
     *     connection to the remote ads router is done on port 0xBF02
     * @param host: ip address of the plc (e.g. 192.168.0.52)
     * @throws IOException
     * @throws org.jpac.vioss.InvalidAddressException
     * 
     */
    public Connection(String plcHost) throws IOException, InvalidAddressException {
        //find the local inet address which best fit to the plcHost address
        //presumed that both recide in the same subnet
        long bestMatch            = Long.MAX_VALUE;
        String bestMatchedLocalIp = "";
        Enumeration nis = NetworkInterface.getNetworkInterfaces();
        while(nis.hasMoreElements()){
            NetworkInterface ni =(NetworkInterface) nis.nextElement();
            Enumeration ias = ni.getInetAddresses();
            while(ias.hasMoreElements()) {
                InetAddress ia    = (InetAddress) ias.nextElement();
                long        match = matchOctets(ia.getHostAddress(), plcHost);
                if (match < bestMatch){
                    bestMatch = match;
                    bestMatchedLocalIp = ia.getHostAddress();
                }
            }
        }
        this.localHost  = bestMatchedLocalIp;
        this.plcHost    = plcHost;
        targetAmsNetId  = new AmsNetId(InetAddress.getByName(plcHost).getHostAddress() + AMSNETIDEXTENSION);;
        targetAmsPortNr = DEFAULTAMSPORTNR;
        localAmsNetId   = new AmsNetId(InetAddress.getByName(localHost).getHostAddress() + AMSNETIDEXTENSION);
        localAmsPortNr  = AmsPortNr.Local;
        initialize();
    }
    
    private long matchOctets(String localHost, String plcHost) throws InvalidAddressException{
        long matchValue = 0;
        
        try{
            int[] octetsLocalHost = getOctets(localHost);
            int[] octetsPlcHost   = getOctets(plcHost);
            if (octetsLocalHost.length == octetsPlcHost.length){
                for (int i = 0; i < octetsPlcHost.length; i++){
                    matchValue = 256 * matchValue + Math.abs(octetsPlcHost[i] - octetsLocalHost[i]);
                }
            }
            else{
                matchValue = Long.MAX_VALUE;            
            }
        }
        catch(InvalidAddressException exc){
            matchValue = Long.MAX_VALUE;
        }
        return matchValue;
    }
    
    protected int[] getOctets(String inetAddress) throws InvalidAddressException{
        StringTokenizer st  = new StringTokenizer(inetAddress, ".");
        int             numberOfTokens = st.countTokens(); 
        int[]           octets = new int[4];
        if (numberOfTokens != 4){
            throw new InvalidAddressException("invalid net id: '" + inetAddress + "'");            
        }
        for(int i = 0; i < numberOfTokens; i++ ){
            try{
                int number = Integer.parseUnsignedInt(st.nextToken());
                if (number > 0x00FF){
                    throw new InvalidAddressException("invalid net id: '" + inetAddress + "'"); 
                }
                octets[i] = (byte)number;
            }
            catch(NumberFormatException exc){
                throw new InvalidAddressException("invalid net id: '" + inetAddress + "'");                 
            }
        }
        return octets;
    }    

    /**
     *  used to initialize the connection.
     * Method instantiates the socket to modbus system and installs the in and ouptput stream
     *
     * @throws IOException in case of io error in thge connection or streams
     */
    public synchronized void initialize() throws IOException {
        InetAddress addr = InetAddress.getByName(plcHost);
        try {
            // create a tcp/ip socket for basic connectivity
            socket = new Socket(addr, ADSPORT);
            socket.setSoTimeout(SOCKETTIMEOUT);

            // prepare streams here
             in  = new LittleEndianDataInputStream(new BufferedInputStream(socket.getInputStream()));
             out = new LittleEndianDataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            connected = true;
            if (Log.isDebugEnabled()) Log.debug("Connected to " + addr.toString() + ":" + socket.getPort());
        } 
        catch (Exception ex) {
            throw new IOException("Error: " , ex);
        }
    }

    public synchronized void setSoTimeout(int timeout) throws IOException {
        if(socket == null) {
            throw new IOException("Invalid socket found while setting socket timeout.");
        }
        socket.setSoTimeout(timeout);
    }
    
    /**
     * use to close an existing connection.
     */
    public synchronized void close() throws IOException{
        connected = false;
        socket.close();
        if (Log.isDebugEnabled()) Log.info("connection to ADS device closed");
    }
     
    /**
     * Getter-methode for calling the input stream from {@link org.jpac.plc.modbus.Connection}.
     * @return the {@link java.io.InputStream} hold in the connection
     * @throws IOException - n case of i/o error in input stream
     */
    public LittleEndianDataInputStream getInputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return in;
    }

    /**
     * Getter-methode for calling the output stream from {@link org.jpac.plc.modbus.Connection}.
     * @return the {@link java.io.OutputStream} hold in the connection
     * @throws IOException - n case of i/o error in output stream
     */
    public LittleEndianDataOutputStream getOutputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return out;
    }

    /**
     * @return the targetAmsNetId
     */
    public AmsNetId getTargetAmsNetId() {
        return targetAmsNetId;
    }

    /**
     * @return the localAmsNetId
     */
    public AmsNetId getLocalAmsNetId() {
        return localAmsNetId;
    }

    /**
     * @return the targetAmsPortNr
     */
    public AmsPortNr getTargetAmsPortNr() {
        return targetAmsPortNr;
    }

    /**
     * @return the localAmsPortNr
     */
    public AmsPortNr getLocalAmsPortNr() {
        return localAmsPortNr;
    }
    
    public int getUniqueInvokeId(){
        return ++uniqueInvokeId > Short.MAX_VALUE ? 0 : uniqueInvokeId;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + plcHost + ")";
    }
}
