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
import org.jpac.plc.ValueOutOfRangeException;

/**
 *
 * @author berndschuster
 */
public class IoSignedInteger extends org.jpac.vioss.IoSignedInteger {
    protected ProcessImageItem processImageItem;
    
    public IoSignedInteger(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
        setProcessImageItem(seizeProcessImageItem(uri));
        if (processImageItem == null){
            getIoHandler().discardSignal(this);//remove registration of this signal already done by super(..)
            throw new InconsistencyException("process image item '" + uri.getPath() + "' for signal " +this.getQualifiedIdentifier() + " not found");
        }
        if (processImageItem.getIoDirection() != IoDirection.INOUT && ioDirection != processImageItem.getIoDirection()){
            getIoHandler().discardSignal(this);//remove registration of this signal already done by super(..)
            throw new InconsistencyException("inconsistant io direction for signal " + this.getQualifiedIdentifier() + ". Must be " + processImageItem.getIoDirection());
        }
        if (processImageItem.getAddress().getBitIndex() != Address.NA){
            throw new InconsistencyException("signal " + this.getQualifiedIdentifier() + " must not be assigned to bit input/output");            
        }        
        setAddress(processImageItem.getAddress());
    }  
    
    protected ProcessImageItem seizeProcessImageItem(URI uri) throws InconsistencyException{
        StringTokenizer  tokenizer        = new StringTokenizer(uri.getPath(), "/");
        ProcessImageItem processImageItem = null;
        try{
            String token = tokenizer.nextToken();
            //uri : .../<identifier defined in JSON file of PiCtory>
            processImageItem = ((org.jpac.vioss.revpi.IOHandler)getIoHandler()).getProcessImage().getItem(token);
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
            switch(getAddress().getSize()){
                case 1:
                    set(processImageItem.getData().getBYTE(getAddress().getByteIndex()));
                    break;
                case 2:
                    set(processImageItem.getData().getINT(getAddress().getByteIndex()));
                    break;
                case 4:
                    set(processImageItem.getData().getDINT(getAddress().getByteIndex()));
                    break;
            }
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
        }
        finally{
            inCheck = false;
        }
    }

    @Override
    public void checkOut() throws SignalAccessException, AddressException{
        try{
            outCheck = true;
            //do not touch process image, if IoDirection is INOUT and this signal is invalid (avoid overwrite of configuration done by PiCtory)            
            //In this case this signal will get valid during next fetch of the process image (checkIn())
            if (isValid() || getIoDirection() == IoDirection.OUTPUT){
                switch(getAddress().getSize()){
                    case 1:
                        processImageItem.getData().setBYTE(getAddress().getByteIndex(), isValid() ? get() : 0);
                        break;
                    case 2:
                        processImageItem.getData().setINT(getAddress().getByteIndex(), isValid() ? get() : 0);
                        break;
                    case 4:
                        processImageItem.getData().setDINT(getAddress().getByteIndex(), isValid() ? get() : 0);
                        break;
                }
            }
        } catch(ValueOutOfRangeException exc){
            throw new SignalAccessException(getQualifiedIdentifier() + ": value out of range: " +  get());
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
