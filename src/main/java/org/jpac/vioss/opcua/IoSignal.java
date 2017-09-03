/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IoSignal.java (versatile input output subsystem)
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

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

/**
 *
 * @author berndschuster
 */
public interface IoSignal extends org.jpac.vioss.IoSignal{
    public MonitoredItemCreateRequest getMonitoredItemCreateRequest();
    public UaMonitoredItem            getMonitoredItem();
    public void setMonitoredItem(UaMonitoredItem monitoredItem);
    /**
     * is used by the opc:ua vioss subscription to update input variables.
     * Should not be used by the application itself
     * @param dataValue 
     */
    public void setOpcUaDataValue(DataValue dataValue);
    public NodeId getNodeId();
    public DataValue getWriteDataValue();
    public ReadValueId getReadValueId();
    public ReadValueId getReadDataTypeValueId();    
    public void setRemotelyAvailable(boolean available);
    public boolean isRemotelyAvailable();
    public void setStatusCode(StatusCode statusCode);
    public StatusCode getStatusCode();
}
