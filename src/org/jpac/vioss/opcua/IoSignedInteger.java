/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : IoSignedInteger.java (versatile input output subsystem)
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

package org.jpac.vioss.opcua;

import com.digitalpetri.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.ExtensionObject;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.StatusCode;
import com.digitalpetri.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import com.digitalpetri.opcua.stack.core.types.structured.ReadValueId;
import java.net.URI;
import org.jpac.AbstractModule;
import org.jpac.InconsistencyException;
import org.jpac.NumberOutOfRangeException;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.IoDirection;

/**
 *
 * @author berndschuster
 */
public class IoSignedInteger extends org.jpac.vioss.IoSignedInteger implements IoSignal{
    private IoSignalImpl ioSignalImpl;
    /**
     * 
     * @param containingModule  containing module
     * @param identifier        identifier of this signal
     * @param uri               uri, identifying the remote signal. Example "opc.tcp://localhost:12685/elbfisch/<namespaceindex>/<signal identifier>
     * @param ioDirection       input, output or both
     * @param samplingInterval  see OPC UA specification (MonitoringParameters)
     * @param extensionObject   see OPC UA specification (MonitoringParameters)
     * @param queueSize         see OPC UA specification (MonitoringParameters)
     * @param discardOldest     see OPC UA specification (MonitoringParameters)
     * @throws SignalAlreadyExistsException
     * @throws InconsistencyException
     * @throws WrongUseException 
     */    
    public IoSignedInteger(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection, double samplingInterval, ExtensionObject extensionObject, int queueSize, boolean discardOldest) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
        this.ioSignalImpl = new IoSignalImpl(this, uri, samplingInterval, extensionObject, queueSize, discardOldest);
        this.ioSignalImpl.setCheckInValueSetter(v -> {
                    try{
                        inCheck = true;
                        try{set((int)v.getValue());}catch(NumberOutOfRangeException exc){/*ignored*/};
                    }
                    catch(SignalAccessException exc){/*cannot happen*/}
                    finally{
                        inCheck = false;           
                    }
                });
        this.ioSignalImpl.setCheckOutValueGetter(() -> {return isValid() ? getValue() : null;});
    }
        
    public IoSignedInteger(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        this(containingModule, identifier, uri, ioDirection, 100.0, null, 10, false);
    }  

    @Override
    public void checkIn() throws SignalAccessException, AddressException{
        ioSignalImpl.checkIn();
    }
        
    @Override
    public void checkOut() throws SignalAccessException, AddressException{
        ioSignalImpl.checkOut();
    }
    
    public int getNameSpaceIndex(){
        return ioSignalImpl.nameSpaceIndex;
    }

    @Override
    public MonitoredItemCreateRequest getMonitoredItemCreateRequest(){
        return ioSignalImpl.getMonitoredItemCreateRequest();
    }

    @Override
    public UaMonitoredItem getMonitoredItem() {
        return ioSignalImpl.monitoredItem;
    }

    @Override
    public void setMonitoredItem(UaMonitoredItem monitoredItem) {
        ioSignalImpl.monitoredItem = monitoredItem;
        ioSignalImpl.monitoredItem.setValueConsumer(v -> {
                setOpcUaDataValue(v);
            });        
    }

    @Override
    public void setOpcUaDataValue(DataValue dataValue) {
        ioSignalImpl.setOpcUaDataValue(dataValue);
    }
    
    @Override
    public DataValue getWriteDataValue(){
        return ioSignalImpl.writeDataValue;
    }

    @Override
    public NodeId getNodeId() {
        return ioSignalImpl.nodeId;
    }
    
    @Override
    public ReadValueId getReadValueId(){
        return ioSignalImpl.readValueId;
    }

    @Override
    public ReadValueId getReadDataTypeValueId(){
        return ioSignalImpl.readDataTypeValueId;
    }

    @Override
    public void setRemotelyAvailable(boolean available) {
       ioSignalImpl.remotelyAvailable = available;
    }

    @Override
    public boolean isRemotelyAvailable() {
        return ioSignalImpl.remotelyAvailable;
    }

    @Override
    public void setStatusCode(StatusCode statusCode) {
        ioSignalImpl.statusCode = statusCode;
    }

    @Override
    public StatusCode getStatusCode() {
        return ioSignalImpl.statusCode;
    }
    
    @Override
    public Object getErrorCode(){
        return null;
    }        
}
