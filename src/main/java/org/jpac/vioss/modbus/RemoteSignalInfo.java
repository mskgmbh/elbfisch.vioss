/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : SignalInfo.java
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

package org.jpac.vioss.modbus;

import java.util.StringTokenizer;

import org.jpac.BasicSignalType;
import org.jpac.IoDirection;
import org.jpac.Signal;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.modbus.Iec61131Address.AccessMode;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo{
    
    private Signal          ioSignal;
    private Iec61131Address iec61131Address;

    public RemoteSignalInfo(Signal ioSignal){
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
    	this.ioSignal = ioSignal;
    	try {
            StringTokenizer pathTokens = new StringTokenizer(((IoSignal)ioSignal).getUri().getPath(),"/");
            if (pathTokens.countTokens() != 2) {
            	throw new InvalidAddressSpecifierException("Error: uri must contain both a device identifier and an IEC 61131 address specifier: " + ((IoSignal)ioSignal).getUri().getPath());
            }
            pathTokens.nextToken();//skip device identifier
            String iecAddressSpecifier = pathTokens.nextToken();

    		this.iec61131Address = new Iec61131Address(iecAddressSpecifier);
    		if ((this.iec61131Address.getAccessMode() == AccessMode.INPUT  && ((IoSignal)ioSignal).getIoDirection() != IoDirection.INPUT) ||
    			(this.iec61131Address.getAccessMode() == AccessMode.OUTPUT && ((IoSignal)ioSignal).getIoDirection() != IoDirection.OUTPUT)  ){
            	throw new InvalidAddressSpecifierException("Error: IoDirection of IoSignal must match IEC61131 address specifier: " + ((IoSignal)ioSignal).getUri().getPath());    			
    		}
    				
    	} catch(InvalidAddressSpecifierException exc) {
    		Log.error("Error:", exc);
    	}
    }
    
    public Iec61131Address getIec61131Address() {
    	return this.iec61131Address;
    }
}
