/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : CommandId.java (versatile input output subsystem)
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
public enum CommandId {
    Undefined                   (0x0000),
    AdsReadDeviceInfo           (0x0001),
    AdsRead                     (0x0002),
    AdsWrite                    (0x0003),
    AdsReadState                (0x0004),
    AdsWriteControl             (0x0005),
    AdsAddDeviceNotification    (0x0006),
    AdsDeleteDeviceNotification (0x0007),
    AdsDeviceNotification       (0x0008),
    AdsReadWrite                (0x0009);
    
    private int commandId;
    
    CommandId(int commandId){
        this.commandId = commandId;
    }

    public void write(Connection connection) throws IOException{
        connection.getOutputStream().writeShort(commandId);
    }
    
    public boolean equals(CommandId ci){
        return this.commandId == ci.commandId;
    }    
    
    public static int size(){
        return 2;
    }
    
    public static CommandId getValue(int commandId){
        boolean found = false;
        int     idx   = 0;
        CommandId[] commandIds = CommandId.values();
        for(int i = 0; i < commandIds.length && !found; i++){
            found = commandIds[i].commandId == commandId;
            if (found){
                idx = i;
            }
        }
        return commandIds[idx];
    }  
       

    @Override
   public String toString(){
       return super.toString() + "(" + commandId + ")";
   }
    
}
