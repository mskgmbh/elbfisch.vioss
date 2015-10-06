/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AmsTcpHeader.java (versatile input output subsystem)
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
import org.apache.log4j.Logger;

/**
 *
 * @author berndschuster
 */
public class AmsTcpHeader {
    static public Logger Log = Logger.getLogger("jpac.vioss.ads");
    
    private int     reserved;
    private Integer length;

    public AmsTcpHeader(){
    }
    
    public AmsTcpHeader(int length){
        this.reserved    = 0;
        this.length = length;  
    }
    
    public void write(Connection connection) throws IOException{
        if (length == null){
            throw new IOException("length of the AMS request not set");
        }
        if (Log.isDebugEnabled())Log.debug("  " + this);
        connection.getOutputStream().writeShort(reserved);
        connection.getOutputStream().writeInt(length);
    }

    public void read(Connection connection) throws IOException{
        //TODO check if the socket timeout works properly !!!!!
        reserved = connection.getInputStream().readShort();
        if (reserved != 0){
            throw new IOException("protocol error: reserved field should be '0':" + reserved);
        }
        length   = connection.getInputStream().readInt();
        if (length < AmsHeader.size()){
            //must at least reflect the size of the ams header
            throw new IOException("inconsistant length:" + length);
        }
        if (Log.isDebugEnabled())Log.debug("  " + this);
    }
    
    public void setLength(int length){
        this.length = length;
    }
    
    public int getLength(){
        return this.length;
    }
    
    public static int size(){
        return 6;
    }
    
    @Override
    public String toString(){
      return getClass().getSimpleName() + "(" + length + ")";
    }
}
