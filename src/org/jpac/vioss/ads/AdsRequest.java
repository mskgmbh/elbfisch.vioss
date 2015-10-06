/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsRequest.java (versatile input output subsystem)
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


package org.jpac.vioss.ads;

import java.io.IOException;

/**
 *
 * @author berndschuster
 */
abstract public class AdsRequest extends AdsData{
        
    protected CommandId  commandId;
    protected StateFlags stateFlags;
    
    public AdsRequest(){
    }
    
    public AdsRequest(CommandId commandId){
        this.commandId   = commandId;
        this.stateFlags  = new StateFlags(false, true, false);
    }
            
    @Override
    public void read(Connection connection) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the commandId
     */
    public CommandId getCommandId() {
        return commandId;
    }

    /**
     * @param commandId the commandId to set
     */
    public void setCommandId(CommandId commandId) {
        this.commandId = commandId;
    }
    
     /**
     * @return the stateFlags
     */
    public StateFlags getStateFlags() {
        return stateFlags;
    }
    
    @Override
    public int size(){
        return 0;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + commandId + ", " + stateFlags + ")";
    }
    
    abstract public void writeMetaData(Connection connection) throws IOException;
    abstract public void writeData(Connection connection) throws IOException;
}
