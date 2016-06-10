/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : IoLogical.java (versatile input output subsystem)
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

import java.net.URI;
import java.util.StringTokenizer;
import org.jpac.AbstractModule;
import org.jpac.InconsistencyException;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignalInvalidException;
import org.jpac.WrongUseException;
import org.jpac.plc.Address;
import org.jpac.plc.AddressException;
import org.jpac.plc.IoDirection;

/**
 *
 * @author berndschuster
 */
public class IoLogical extends org.jpac.vioss.IoLogical{
        
    public IoLogical(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
    }  
    
    protected Address seizeAddress(URI uri) throws InconsistencyException{
        StringTokenizer tokenizer = new StringTokenizer(uri.getPath(), "/");
        Address         adr       = null;
        try{//uri : .../<datablock>/<bitIndex>
            tokenizer.nextToken();//skip data block specifier
            int bitIndex  = Integer.parseInt(tokenizer.nextToken());
            adr = new Address(bitIndex / 8, bitIndex % 8, 1);
        }
        catch(Exception exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "'");
        }
        return adr; 
    }
    
    @Override
    public void checkIn() throws SignalAccessException, AddressException{
       Boolean bool = null;
       try{bool = ((org.jpac.vioss.modbus.IOHandler)getIOHandler()).getBOOL(getAddress());}catch(InconsistencyException exc){/*cannot happen*/}
       if (bool != null){
            try{
                 inCheck = true;
                 set(bool);
            }
            finally{
                 inCheck = false;           
            }
            if (Log.isDebugEnabled() && isChanged()){
                try{Log.debug(this + " set to " + is(true));}catch(SignalInvalidException exc){/*cannot happen*/}
            }
       }
       else{
           invalidate();
       }
    }
    
    @Override
    public Object getErrorCode(){
        return null;
    }
    
}
