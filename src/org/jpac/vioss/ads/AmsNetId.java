/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AMSNetId.java (versatile input output subsystem)
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
import java.util.StringTokenizer;
import org.jpac.WrongUseException;
import org.jpac.vioss.InvalidAddressException;

/**
 *
 * @author berndschuster
 */
public class AmsNetId {
    private byte[] netIdBytes;
    private String netId;
    
    private boolean initialized;
    
    public AmsNetId(){
        initialized = false;
    }

    public AmsNetId(String netId) throws InvalidAddressException{
        this.netId = netId;
        netIdToBytes();
        initialized = true;
    }
    
    public void write(Connection connection) throws WrongUseException, IOException{
        if (!initialized){
            throw new WrongUseException("net id not specified");
        }
        byte[] nb = getNetIdBytes();
        for (int i = 0; i < nb.length; i++){
            connection.getOutputStream().write(nb[i]);
        }
    }
    
    public void read(Connection connection) throws IOException{
        byte[] nb = getNetIdBytes();
        for (int i = 0; i < nb.length; i++){
            nb[i] = (byte)connection.getInputStream().read();
        }
        netId = bytesToNetId();
    }

    protected void netIdToBytes() throws InvalidAddressException{
        StringTokenizer st             = new StringTokenizer(netId, ".");
        int             numberOfTokens = st.countTokens();
        byte[]          nb = getNetIdBytes();
        
        if (numberOfTokens != 6){
            throw new InvalidAddressException("invalid net id: " + netId);            
        }
        for(int i = 0; i < numberOfTokens; i++ ){
            try{
                int number = Integer.parseUnsignedInt(st.nextToken());
                if (number > 0x00FF){
                    throw new InvalidAddressException("invalid net id: " + netId);
                }
                nb[i] = (byte)number;
            }
            catch(NumberFormatException exc){
                throw new InvalidAddressException("invalid net id: " + netId);                
            }
        }
    }
    
    protected String bytesToNetId(){
        byte[] nb = getNetIdBytes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nb.length; i++){
            sb.append(0xff & nb[i]);
            if (i < 5){
                sb.append('.');
            }
        }
        return sb.toString();
    }
    
    protected byte[] getNetIdBytes(){
        if (netIdBytes == null){
            netIdBytes = new byte[6];
        }
        return netIdBytes;
    }
    
    public String getNetId(){
        return netId;
    }
    
    public String getIPv4(){
        return (0xff & netIdBytes[0]) + "." + (0xff & netIdBytes[1]) + "." + (0xff & netIdBytes[2]) + "." + (0xff & netIdBytes[3]);
    }
    
    public static int size(){
        return 6;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + netId + ")";
    }
}
