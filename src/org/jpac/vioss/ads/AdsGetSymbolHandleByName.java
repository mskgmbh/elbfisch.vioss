/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsGetSymbolHandleByName.java (versatile input output subsystem)
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
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import org.jpac.plc.PlcString;

/**
 *
 * @author berndschuster
 */
public class AdsGetSymbolHandleByName extends AdsReadWrite{
    private static final int INTLENGTH = 4;
    
    private Long             handle;
    private String           variableName;
    
    public AdsGetSymbolHandleByName(String variableName){
        super(IndexGroup.ADSIGRP_SYM_HNDBYNAME, 0);
        this.variableName = variableName;
        ((AdsReadWriteRequest)getAdsRequest()).setWriteLength(variableName.length());
        Data writeData = new Data(new byte[variableName.length()], Data.Endianness.LITTLEENDIAN);
        try{writeData.setSTRING(0, new PlcString(variableName, variableName.length()));}catch(Exception exc){/*cannot happen*/}
        ((AdsReadWriteRequest)getAdsRequest()).setWriteData(writeData);
        ((AdsReadWriteRequest)getAdsRequest()).setReadLength(INTLENGTH);//handle is an int value
    }
    
    @Override
    public void transact(Connection connection) throws IOException, WrongUseException{
       try{
           super.transact(connection);
           handle = ((AdsReadWriteResponse)getAdsResponse()).getData().getDWORD(0);
       }
       catch(AddressException exc){/*cannot happen*/}
       catch(IOException exc){
           throw new IOException("error while retrieving handle for symbol '" + variableName + "'", exc);
       }
    }
    
    public Long getHandle(){
        return handle;
    }
}
