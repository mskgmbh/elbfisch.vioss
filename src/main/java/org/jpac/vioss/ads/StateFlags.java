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
import org.jpac.WrongUseException;

/**
 *
 * @author berndschuster
 */
public class StateFlags {
    private static final int RESPONSEMASK = 0x0001;
    private static final int ADSCMDMASK   = 0x0004;
    private static final int UDPPROTMASK  = 0x0040;
    
    private static final int INVALID     = -1;
    
    private boolean isResponse;
    private boolean isAdsCommand;
    private boolean useUdpProtocol;
    
    int stateFlags;
    
    public StateFlags(){
        stateFlags = INVALID;
    }

    public StateFlags(boolean isResponse, boolean isAdsCommand, boolean useUdpProtocol){
        this.isResponse      = isResponse;
        this.isAdsCommand   = isAdsCommand;
        this.useUdpProtocol = useUdpProtocol;
        stateFlags = 0;
        stateFlags = isResponse     ? RESPONSEMASK : 0;
        stateFlags = isAdsCommand   ? stateFlags | ADSCMDMASK  : stateFlags;
        stateFlags = useUdpProtocol ? stateFlags | UDPPROTMASK : stateFlags;
    }
    
    public void write(Connection connection) throws WrongUseException, IOException{
        if (stateFlags == INVALID){
            throw new WrongUseException("state flags not set");
        }
        connection.getOutputStream().writeShort(stateFlags);
    }
    
    public void read(Connection connection) throws IOException{
        stateFlags     = connection.getInputStream().readShort();
        isResponse     = (stateFlags & RESPONSEMASK) != 0;
        isAdsCommand   = (stateFlags & ADSCMDMASK) != 0;
        useUdpProtocol = (stateFlags & UDPPROTMASK) != 0;
    }
    
    public static int size(){
        return 2;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(response = " + isResponse + " AdsCommand = " + isAdsCommand + " useUdpProtocol = " + useUdpProtocol +  ")";
    }
}
