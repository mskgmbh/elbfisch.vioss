/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IoSignalImpl.java (versatile input output subsystem)
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
import com.digitalpetri.opcua.stack.core.AttributeId;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.ExtensionObject;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.QualifiedName;
import com.digitalpetri.opcua.stack.core.types.builtin.StatusCode;
import com.digitalpetri.opcua.stack.core.types.builtin.Variant;
import static com.digitalpetri.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import com.digitalpetri.opcua.stack.core.types.enumerated.MonitoringMode;
import com.digitalpetri.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import com.digitalpetri.opcua.stack.core.types.structured.MonitoringParameters;
import com.digitalpetri.opcua.stack.core.types.structured.ReadValueId;
import java.net.URI;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jpac.InconsistencyException;
import org.jpac.SignalAccessException;
import org.jpac.Value;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import static org.jpac.vioss.IoLogical.Log;

/**
 *
 * @author berndschuster
 */
public class IoSignalImpl extends org.jpac.vioss.IoSignalImpl{
    public String          plcIdentifier;
    public Integer         nameSpaceIndex;
    public boolean         checkInFaultLogged;
    public boolean         checkOutFaultLogged;
    public UaMonitoredItem monitoredItem;
    public DataValue       monitoredItemValue;
    public Boolean         monitoredItemValueUpdated;
    public double          samplingRate;
    public ExtensionObject extensionObject;
    public int             queueSize;
    public boolean         discardOldest;
    public DataValue       writeDataValue;
    public Variant         writeVariant;
    public StatusCode      writeStatusCode;
    public NodeId          nodeId;
    public ReadValueId     readValueId;
    public ReadValueId     readDataTypeValueId;
    public boolean         remotelyAvailable;
    public StatusCode      statusCode;
    
    private Consumer<Variant> checkInValueSetter;
    private Supplier<Value>   checkOutValueGetter;

    public IoSignalImpl(IoSignal containingSignal, URI uri, double samplingRate, ExtensionObject extensionObject, int queueSize, boolean discardOldest) throws InconsistencyException, WrongUseException {
        super(containingSignal, uri);
        this.samplingRate    = samplingRate;
        this.extensionObject = extensionObject;
        this.queueSize       = queueSize;
        this.discardOldest   = discardOldest;
        StringTokenizer path = new StringTokenizer(uri.getPath().substring(1),"/");
        try{this.nameSpaceIndex  = Integer.parseInt(path.nextToken());}catch(NumberFormatException exc){/*nothing to do*/};        
        if (this.nameSpaceIndex == null){
            //skip possible instance identifier
            try{this.nameSpaceIndex  = Integer.parseInt(path.nextToken());}catch(NumberFormatException exc){/*nothing to do*/};        
        }
        if (this.nameSpaceIndex == null){
            throw new InconsistencyException(("illegal namespace index in uri '" + uri + "'"));
        }
        if (!path.hasMoreTokens()){
            throw new InconsistencyException(("missing signal identifier '" + uri + "'"));            
        }
        this.plcIdentifier             = path.nextToken();
        this.nodeId                    = new NodeId(nameSpaceIndex, plcIdentifier);
        this.checkInFaultLogged        = false;
        this.checkOutFaultLogged       = false;
        this.monitoredItemValueUpdated = false;
        this.writeVariant              = new Variant(null);
        this.writeStatusCode           = StatusCode.BAD;
        this.writeDataValue            = new DataValue(writeVariant, writeStatusCode);
        this.readValueId               = new ReadValueId(nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
        this.readDataTypeValueId       = new ReadValueId(nodeId, AttributeId.DataType.uid(), null, QualifiedName.NULL_VALUE);
        this.remotelyAvailable         = false;
        this.statusCode                = null;        
    }
    
    public void init(){
        
    }
    
    public void checkIn() throws SignalAccessException, AddressException{
        DataValue actualMonitoredItem;
        boolean   updateOccured = false;
        synchronized(monitoredItemValueUpdated){
            //take over actual monitoredItem
            actualMonitoredItem       = monitoredItemValue;
            updateOccured             = monitoredItemValueUpdated;
            monitoredItemValueUpdated = false;
        }
        if (updateOccured){
            if (actualMonitoredItem.getStatusCode().isGood()){
                checkInValueSetter.accept(actualMonitoredItem.getValue());
                checkInFaultLogged = false;
           }
           else{
               if (!checkInFaultLogged){
                   Log.error(this + " got invalid due to server side error");
                   checkInFaultLogged = true;
               }
               containingSignal.invalidate();
           }
       }
    }
    
    public void checkOut() throws SignalAccessException, AddressException{
        Value   value = checkOutValueGetter.get();
//        writeDataValue  = new DataValue(new Variant(value != null ? value.getValue() : null), value != null ? StatusCode.GOOD : StatusCode.BAD);
//        writeDataValue  = new DataValue(new Variant(value.getValue()));
        writeDataValue  = new DataValue(new Variant(value.getValue()), null, null);
    }
   
    public MonitoredItemCreateRequest getMonitoredItemCreateRequest() {
        MonitoredItemCreateRequest mcr = null;
        try{
            MonitoringParameters params = new MonitoringParameters(uint(((IOHandler)getIOHandler()).getInputSignals().indexOf(getContainingSignal())), samplingRate, extensionObject, uint(queueSize), discardOldest);       
            mcr                         = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, params);
        }
        catch(InconsistencyException exc){
            Log.error("Error: ",exc);
        }
        return mcr;
    }
    
    public void setCheckInValueSetter(Consumer<Variant> checkInValueSetter){
        this.checkInValueSetter = checkInValueSetter;
    }
    
    public void setCheckOutValueGetter(Supplier<Value> checkOutValueGetter){
        this.checkOutValueGetter = checkOutValueGetter;
    }

    public void setOpcUaDataValue(DataValue dataValue) {
        synchronized(monitoredItemValueUpdated){
            this.monitoredItemValue        = dataValue;
            this.monitoredItemValueUpdated = true;
        }
    }
}
