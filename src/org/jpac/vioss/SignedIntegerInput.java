/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : SignedIntegerInput.java (versatile input output subsystem)
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

package org.jpac.vioss;

import java.net.URI;
import org.apache.log4j.Logger;
import org.jpac.AbstractModule;
import org.jpac.Address;
import org.jpac.InconsistencyException;
import org.jpac.Logical;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignedInteger;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;

/**
 *
 * @author berndschuster
 */
abstract public class SignedIntegerInput extends SignedInteger implements InputSignal{
    static public Logger Log = Logger.getLogger("jpac.Signal");

    private URI       uri;
    private IOHandler ioHandler;
    private Address   address;
    
    /**
     * constructs a logical input signal
     * @param containingModule: module, this signal is contained in
     * @param identifier: identifier of the signal
     * @param uri: unified resource identifier of the input signal
     * @param minValue: minimum value
     * @param maxValue: maximum value
     * @throws SignalAlreadyExistsException: a signal with this identifier is already registered
     * @throws InconsistencyException: an IOHandler for the given URI cannot be instantiated
     */
    public SignedIntegerInput(AbstractModule containingModule, String identifier, URI uri, int minValue, int maxValue) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException {
        super(containingModule, identifier, minValue, maxValue);
        this.uri       = uri;
        this.ioHandler = null;
        this.address   = null;
    }    

    /**
     * constructs a logical input signal
     * @param containingModule: module, this signal is contained in
     * @param identifier: identifier of the signal
     * @param uri: unified resource identifier of the input signal
     * @throws SignalAlreadyExistsException: a signal with this identifier is already registered
     * @throws InconsistencyException: an IOHandler for the given URI cannot be instantiated
     */
    public SignedIntegerInput(AbstractModule containingModule, String identifier, URI uri) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException {
        this(containingModule, identifier, uri, 0, 0);
        this.rangeChecked = false;
    }  
    
    /**
     * returns the IOHandler, this signal is assigned to
     * @return 
     */
    public IOHandler getIOHandler() throws InconsistencyException{
        if (ioHandler == null){
            try {
                ioHandler = IOHandlerFactory.getHandlerFor(getAddress(), getUri());
            } catch (ClassNotFoundException ex) {
                throw new InconsistencyException("no IOHandler found for " + uri);
            }            
        }
        return ioHandler;
    }

    public URI getUri(){
        return this.uri;
    }
    
    @Override
    public void setAddress(Address address){
        this.address = address;
    }
    
    @Override
    public Address getAddress(){
        return this.address;
    }

    /**
     * used to fetch the state of the input signal from the given URI.
     * @throws org.jpac.SignalAccessException if called outside the containing module or jpac 
     * @throws org.jpac.plc.AddressException  if address is invalid
     */
    @Override
    abstract public void fetch() throws SignalAccessException, AddressException;       
}
