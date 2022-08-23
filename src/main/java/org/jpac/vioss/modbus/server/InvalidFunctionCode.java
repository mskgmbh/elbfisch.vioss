/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : modbus server InvalidFunctionCode.java (versatile input output subsystem)
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

import io.netty.buffer.ByteBuf;
import org.jpac.vioss.modbus.FunctionCode;

/**
 *
 * @author berndschuster
 */
public class InvalidFunctionCode extends Command{
    protected int commandId;
    
    public InvalidFunctionCode(FunctionCode fctCode){
        super(fctCode);
        Log.debug("received invalid FunctionCode");
    }
    
    //server
    @Override
    public void decode(ByteBuf byteBuf){

    }
    
    //server
    @Override
    public Acknowledgement handleRequest(CommandHandler commandHandler) {
       ExceptionCode excCode = ExceptionCode.INVALIDFUNCTIONCODE;
       getAcknowledgement().setExceptionCode(excCode);
       return getAcknowledgement();
    }

    /**
     * @return the commandId
     */
    public int getCommandId() {
        return commandId;
    }

    /**
     * @param commandId the commandId to set
     */
    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }
    
    @Override
    public String toString(){
        return getClass().getName() +"()";
    }
}
