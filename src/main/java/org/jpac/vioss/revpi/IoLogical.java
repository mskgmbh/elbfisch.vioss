/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Revolution Pi
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

package org.jpac.vioss.revpi;

import java.net.URI;
import java.util.StringTokenizer;
import org.jpac.AbstractModule;
import org.jpac.Address;
import org.jpac.InconsistencyException;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.IoDirection;

/**
 *
 * @author berndschuster
 */
public class IoLogical extends org.jpac.vioss.IoLogical {
    protected ProcessImageItem processImageItem;
    
    public IoLogical(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
        setProcessImageItem(seizeProcessImageItem(uri));
        if (processImageItem.getIoDirection() != IoDirection.INOUT && ioDirection != processImageItem.getIoDirection()){
            getIOHandler().discardSignal(this);//remove registration of this signal already done by super(..)
            throw new InconsistencyException("inconsistant io direction for signal " + this.getQualifiedIdentifier() + ". Must be " + processImageItem.getIoDirection());
        }
        if (processImageItem.getAddress().getBitIndex() == Address.NA){
            getIOHandler().discardSignal(this);//remove registration of this signal already done by super(..)
            throw new InconsistencyException("signal " + this.getQualifiedIdentifier() + " must be assigned to bit input/output");            
        }
        setAddress(processImageItem.getAddress());
    }  
    
    protected ProcessImageItem seizeProcessImageItem(URI uri) throws InconsistencyException{
        StringTokenizer  tokenizer        = new StringTokenizer(uri.getPath(), "/");
        ProcessImageItem processImageItem = null;
        try{
            String token = tokenizer.nextToken();
            //uri : .../<identifier defined in JSON file of PiCtory>
            processImageItem = ((org.jpac.vioss.revpi.IOHandler)getIOHandler()).getProcessImage().getItem(token);
        }
        catch(Exception exc){
            throw new InconsistencyException("illegal address specification in '" + uri.getPath() + "' : " + exc);
        }
        return processImageItem; 
    }

    public boolean isIntegerNumber(String str)
    {
      return str.matches("\\d+");  //match an integer number
    }
    
    private  void setProcessImageItem(ProcessImageItem processImageItem){
        this.processImageItem = processImageItem;
    }
    
    @Override
    public void checkIn() throws SignalAccessException, AddressException {
        try{
            inCheck = true;
            set(processImageItem.getData().getBIT(getAddress().getByteIndex(),getAddress().getBitIndex()));
        }
        finally{
            inCheck = false;
        }
    }

    @Override
    public void checkOut() throws SignalAccessException, AddressException{
        try{
            outCheck = true;
            processImageItem.getData().setBIT(getAddress().getByteIndex(),getAddress().getBitIndex(), isValid() ? is(true) : false);
        }
        finally{
            outCheck = false;
        }
    }
    
    @Override
    public Object getErrorCode(){
        return null;
    }        
}
