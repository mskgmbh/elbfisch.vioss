/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Modbus server MessageFactory.java (versatile input output subsystem)
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

package org.jpac.vioss.modbus.server;

import org.jpac.vioss.modbus.FunctionCode;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import org.jpac.InconsistencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class MessageFactory {
    private final Logger Log = LoggerFactory.getLogger("jpac.vios.modbus.server");    
    
    protected int                               fctCode;
    protected HashMap<FunctionCode, Message>    recycledMessages;
    protected DataBlocks                        datablocks;

    public MessageFactory(HashMap<FunctionCode, Message> recycledMessages, DataBlocks datablocks) {
    	this.recycledMessages   = recycledMessages;
        this.datablocks         = datablocks;
    }
    
    public Message getRecycledMessage(ByteBuf byteBuf) throws InvalidFunctionCodeException {
        Message message = null;
        fctCode         = byteBuf.readByte();
        FunctionCode commandId = FunctionCode.fromInt(fctCode);
        if (!recycledMessages.containsKey(commandId)){
            message = getCommand(commandId);
            recycledMessages.put(commandId, message);
        }
        message  = recycledMessages.get(commandId);
        if (message != null){
            message.decode(byteBuf);
        } else {
            throw new InvalidFunctionCodeException(fctCode);
        }
        return message;
    }

    protected Message getCommand(FunctionCode commandId) {
    	Message message;
        switch(commandId) {
            case READCOILS:
                message = new ReadCoils(datablocks.getOutputDatablock());
                break;
            case READHOLDINGREGISTERS:
                message = new ReadHoldingRegisters(datablocks.getOutputDatablock());
                break;
            case WRITEMULTIPLECOILS:
                message = new WriteMultipleCoils(datablocks.getInputDatablock());
                break;
            case WRITEMULTIPLEREGISTERS:
                message = new WriteMultipleRegisters(datablocks.getInputDatablock());                    
                break;
         default:
            message = null;
        }
        return message;
    }
    
    public int getFctCode(){
        return this.fctCode;
    }
}
