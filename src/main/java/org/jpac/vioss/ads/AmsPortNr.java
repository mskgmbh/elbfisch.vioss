/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AmsPortNr.java (versatile input output subsystem)
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
public enum AmsPortNr {
    Undefined          (0),      //added by jpac
    Local              (0x0FFFE),//addded by jpac
    Logger             (100),
    EventLogger        (110),
    IO                 (350),
    AdditionalTask1    (351),
    AdditionalTask2    (352),
    NC                 (500),
    PlcRuntimeSystem1  (851),
    PlcRuntimeSystem2  (852),
    PlcRuntimeSystem3  (853),
    PlcRuntimeSystem4  (854),
    CamshaftController (900),
    SystemService      (10000),
    Scope              (14000);
    private int portNr;
    
    AmsPortNr(int portNr){
        this.portNr = portNr;
    }
    
    public void write(Connection connection) throws IOException{
        connection.getOutputStream().writeShort(portNr);
    }
    
    public boolean equals(AmsPortNr pn){
        return this.portNr == pn.portNr;
    }    
    
    public static int size(){
        return 2;
    }

    public static AmsPortNr getValue(int portNr){
        boolean found = false;
        int     idx   = 0;
        AmsPortNr[] amsPortNrs = AmsPortNr.values();
        for(int i = 0; i < amsPortNrs.length && !found; i++){
            found = amsPortNrs[i].portNr == portNr;
            if (found){
                idx = i;
            }
        }
        return amsPortNrs[idx];
    }  
    
    @Override
   public String toString(){
       return super.toString() + "(" + portNr + ")";
   }    
}
