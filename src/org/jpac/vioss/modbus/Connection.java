/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Connection.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : Connections to modbus PLC
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Connection.java,v $
 *
 * This file is part of the jPac PLC communication library.
 * The jPac PLC communication library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The jPac PLC communication library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac PLC communication library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.jpac.vioss.modbus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import org.apache.log4j.Logger;
import org.jpac.plc.modbus.util.Modbus;

/**
 * represents a TCP/IP connection to a MODBUS plc.
 *
 */
public class Connection{
    static Logger Log = Logger.getLogger("jpac.vioss.modbus");
    
    static final int DEFAULTMODBUSPORT  = 502;

    private String            host;
    private int               port;
    private Socket            socket;
    private DataInputStream   in;
    private DataOutputStream  out;
    private boolean           connected;
    
    /**
     * an instance of Connection is created and the connection to given plc is initiated immediately
     * @param host ip address of the plc (e.g. 192.168.0.1)
     * @param port port over which to access the plc
     * @throws IOException
     */
    public Connection(String host, int port) throws IOException {
        try{
            initialize();
            connected = true;
        }
        catch(IOException exc){
            connected  = false;
            throw exc;
        }
    }
                            
    /**
     * an instance of Connection is created and the connection to given plc is initiated immediately
     * @param host ip address of the plc (e.g. 192.168.0.1)
     * @throws IOException
     */
    public Connection(String host) throws IOException {
        this(host, DEFAULTMODBUSPORT);
    }

    /**
     *  used to initialize the connection.
     * Method instantiates the socket to modbus system and installs the in and ouptput stream
     *
     * @throws IOException in case of io error in thge connection or streams
     */
    public synchronized void initialize() throws IOException {
        InetAddress addr = InetAddress.getByName(host);
        try {
            // create a tcp/ip socket for basic connectivity
            socket = new Socket(addr, Modbus.DEFAULT_PORT);
            try {
              socket.setSoTimeout(Modbus.DEFAULT_TIMEOUT);
            } catch (IOException ex) {
              /// nothing to do here
            }
            // prepare streams here
            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));            
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
    }
     
    public int getMaxTransferLength() {
        return 256;
    }

    /**
     * Getter-methode for calling the input stream from {@link org.jpac.plc.modbus.Connection}.
     * @return the {@link java.io.InputStream} hold in the connection
     * @throws IOException - n case of i/o error in input stream
     */
    public DataInputStream getInputStream() throws IOException {
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
    public DataOutputStream getOutputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return out;
    }    

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }
}
