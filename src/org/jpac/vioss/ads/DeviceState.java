/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : DeviceState.java (versatile input output subsystem)
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
public class DeviceState {
    //error masks
    static final int UNDEFINED                     = 0x0000;
    static final int LINKERRORDETECTED             = 0x0001;
    static final int IOLOCKEDAFTERLINKERROR        = 0x0002;
    static final int LINKERRORREDUNDANCYADAPTER    = 0x0004;
    static final int MISSINGONEFRAME               = 0x0008;
    static final int OUTOFSENDRESOURCES            = 0x0010;
    static final int WATCHDOGTRIGGERED             = 0x0020;
    static final int ETHERNETDRIVERNOTFOUND        = 0x0040;
    static final int IORESETACTIVE                 = 0x0080;
    static final int ATLEASTONEDEVICEININITSTATE   = 0x0100;
    static final int ATLEASTONEDEVICEINPREOPSTATE  = 0x0100;
    static final int ATLEASTONEDEVICEINSAFEOPSTATE = 0x0100;
    static final int ATLEASTONEDEVICEINERRORSTATE  = 0x0100;
    static final int DCNOTINSYNC                   = 0x1000;
    
    int deviceState;
    
    public DeviceState(){
        deviceState = UNDEFINED;
    }

    public DeviceState(int deviceState) throws InvalidAddressException{
       deviceState = deviceState;
    }
    
    public void write(Connection connection) throws WrongUseException, IOException{        
        connection.getOutputStream().writeShort(deviceState);
    }
    
    public void read(Connection connection) throws IOException{
        deviceState = connection.getInputStream().readShort();
    }
    
    public int getState(){
        return deviceState;
    }
    
    public static int size(){
        return 2;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + deviceState + ")";
    }
}
