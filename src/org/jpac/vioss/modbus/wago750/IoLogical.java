/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : LogicalInput.java (versatile input output subsystem)
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

package org.jpac.vioss.modbus.wago750;

import java.net.URI;
import java.util.StringTokenizer;
import org.jpac.AbstractModule;
import org.jpac.InconsistencyException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.WrongUseException;
import org.jpac.plc.Address;
import org.jpac.plc.IoDirection;
import org.jpac.vioss.iec61131_3.AbsoluteAddress;

/**
 *
 * @author berndschuster
 */
public class IoLogical extends org.jpac.vioss.modbus.IoLogical{
        
    public IoLogical(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
    }  
    
    @Override
    protected Address seizeAddress(URI uri) throws InconsistencyException{
        StringTokenizer tokenizer = new StringTokenizer(uri.getPath(), "/");
        Address         adr       = null;
        try{//uri : .../<datablock>/<bitIndex> or .../<IEC61131-3 address>
            String token = tokenizer.nextToken().trim();
            int    bitIndex = 0;
            if (AbsoluteAddress.mightBeAnIEC61131Address(token)){
                AbsoluteAddress address = new AbsoluteAddress(token);
                bitIndex                = address.getBitAddress();
            }
            else{
                bitIndex  = Integer.parseInt(tokenizer.nextToken());
            }
            adr = new Address(bitIndex / 8, bitIndex % 8, 1);
        }
        catch(Exception exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "'");
        }
        return adr; 
    }    
}
