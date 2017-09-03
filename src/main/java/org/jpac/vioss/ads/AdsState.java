/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsState.java (versatile input output subsystem)
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
public enum AdsState {
     Invalid      (0),
     Idle         (1),
     Reset        (2),
     Init         (3),
     Start        (4),
     Run          (5),
     Stop         (6),
     SaveCfg      (7),
     LoadCfg      (8),
     PowerFailure (9),
     PowerGood    (10),
     Error        (11),
     Shutdown     (12),
     Suspend      (13),
     Resume       (14),
     Config       (15),   
     Undefined    (-1);
     
    private int adsState;
    
    AdsState(int adsState){
        this.adsState = adsState;
    }
    
    public void write(Connection connection) throws IOException{
        connection.getOutputStream().writeShort(adsState);
    }
    
    public boolean equals(AdsState as){
        return this.adsState == as.adsState;
    }    
    
    public static int size(){
        return 2;
    }
    
    public static AdsState getValue(int adsState){
        boolean found = false;
        int     idx   = 0;
        AdsState[] adsStates = AdsState.values();
        for(int i = 0; i < adsStates.length && !found; i++){
            found = adsStates[i].adsState == adsState;
            if (found){
                idx = i;
            }
        }
        return adsStates[idx];
    }        

    @Override
   public String toString(){
       return super.toString() + "(" + adsState + ")";
   }
    
}
