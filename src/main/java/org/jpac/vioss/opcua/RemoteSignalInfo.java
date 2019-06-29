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

package org.jpac.vioss.opcua;

import java.util.StringTokenizer;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jpac.BasicSignalType;
import org.jpac.InconsistencyException;
import org.jpac.LogicalValue;
import org.jpac.Signal;
import org.jpac.SignedIntegerValue;
import org.jpac.DecimalValue;
import org.jpac.CharStringValue;
import org.jpac.WrongUseException;
import org.jpac.vioss.IoSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo{
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.IOHandler");
    
	protected static String   PARAMETER_USEQUOTES ="useQuotes";
	protected static String   PARAMETERVALUE_TRUE ="true";
	protected Integer         nameSpaceIndex;
    protected boolean         checkInFaultLogged;
    protected boolean         checkOutFaultLogged;
    protected UaMonitoredItem monitoredItem;
    protected DataValue       monitoredItemValue;
    protected Boolean         monitoredItemValueUpdated;
    protected DataValue       writeDataValue;
    protected Variant         writeVariant;
    protected StatusCode      writeStatusCode;
    protected NodeId          nodeId;
    protected ReadValueId     readValueId;
    protected ReadValueId     readDataTypeValueId;
    protected boolean         remotelyAvailable;
    protected StatusCode      statusCode;
    protected boolean		  useQuotes;
    protected Signal          ioSignal;

    public RemoteSignalInfo(Signal ioSignal){
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
    	this.ioSignal         = ioSignal;
        this.useQuotes        = ((IoSignal)ioSignal).getParameters().containsKey(PARAMETER_USEQUOTES) && ((IoSignal)ioSignal).getParameters().get(PARAMETER_USEQUOTES).equals("PARAMETERVALUE_TRUE");//use quotes for accessing S7 plc's
        StringTokenizer path  = new StringTokenizer(((IoSignal)ioSignal).getUri().getPath().substring(1),"/");
        
        try{this.nameSpaceIndex  = Integer.parseInt(path.nextToken());}catch(NumberFormatException exc){/*nothing to do*/};        
        if (this.nameSpaceIndex == null){
            //skip possible instance identifier
            try{this.nameSpaceIndex  = Integer.parseInt(path.nextToken());}catch(NumberFormatException exc){/*nothing to do*/};        
        }
        if (this.nameSpaceIndex == null){
            throw new InconsistencyException(("illegal namespace index in uri '" + ((IoSignal)ioSignal).getUri() + "'"));
        }
        if (!path.hasMoreTokens()){
            throw new InconsistencyException(("missing signal identifier '" + ((IoSignal)ioSignal).getUri() + "'"));            
        }
        this.identifier                = useQuotes ? encloseWithQuotes(path.nextToken()) : path.nextToken();
        this.nodeId                    = new NodeId(nameSpaceIndex, identifier);        	
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
    
    public void setOpcUaDataValue(DataValue dataValue) {
        //Log.info("setOpcUaDataValue {}: {}", ioSignal, getValue());            
    	synchronized(this){
            this.monitoredItemValue        = dataValue;
            this.monitoredItemValueUpdated = true;
        }
    }
    
    protected String encloseWithQuotes(String identifier) {	
    	return "\"" + identifier.replace (".", "\".\"") + "\"";
    }
    
            
	public Integer getNameSpaceIndex() {
		return nameSpaceIndex;
	}

	public void setNameSpaceIndex(Integer nameSpaceIndex) {
		this.nameSpaceIndex = nameSpaceIndex;
	}

	public boolean isCheckInFaultLogged() {
		return checkInFaultLogged;
	}

	public void setCheckInFaultLogged(boolean checkInFaultLogged) {
		this.checkInFaultLogged = checkInFaultLogged;
	}

	public boolean isCheckOutFaultLogged() {
		return checkOutFaultLogged;
	}

	public void setCheckOutFaultLogged(boolean checkOutFaultLogged) {
		this.checkOutFaultLogged = checkOutFaultLogged;
	}

	public UaMonitoredItem getMonitoredItem() {
		return monitoredItem;
	}

	public void assignMonitoredItem(UaMonitoredItem monitoredItem) {
		this.monitoredItem = monitoredItem;
        this.monitoredItem.setValueConsumer(v -> {
            setOpcUaDataValue(v);
        });        
	}

	public DataValue getMonitoredItemValue() {
		return monitoredItemValue;
	}

	public void setMonitoredItemValue(DataValue monitoredItemValue) {
		this.monitoredItemValue = monitoredItemValue;
	}

	public Boolean getMonitoredItemValueUpdated() {
		return monitoredItemValueUpdated;
	}

	public void setMonitoredItemValueUpdated(Boolean monitoredItemValueUpdated) {
		this.monitoredItemValueUpdated = monitoredItemValueUpdated;
	}

	public DataValue getWriteDataValue() {
		return writeDataValue;
	}

	public void setWriteDataValue(DataValue writeDataValue) {
		this.writeDataValue = writeDataValue;
	}

	public Variant getWriteVariant() {
		return writeVariant;
	}

	public void setWriteVariant(Variant writeVariant) {
		this.writeVariant = writeVariant;
	}

	public StatusCode getWriteStatusCode() {
		return writeStatusCode;
	}

	public void setWriteStatusCode(StatusCode writeStatusCode) {
		this.writeStatusCode = writeStatusCode;
	}

	public NodeId getNodeId() {
		return nodeId;
	}

	public void setNodeId(NodeId nodeId) {
		this.nodeId = nodeId;
	}

	public ReadValueId getReadValueId() {
		return readValueId;
	}

	public void setReadValueId(ReadValueId readValueId) {
		this.readValueId = readValueId;
	}

	public ReadValueId getReadDataTypeValueId() {
		return readDataTypeValueId;
	}

	public void setReadDataTypeValueId(ReadValueId readDataTypeValueId) {
		this.readDataTypeValueId = readDataTypeValueId;
	}

	public boolean isRemotelyAvailable() {
		return remotelyAvailable;
	}

	public void setRemotelyAvailable(boolean remotelyAvailable) {
		this.remotelyAvailable = remotelyAvailable;
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}

	@Override
    public String toString(){
        return super.toString();
    }    
}
