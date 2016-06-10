/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : IoSignedInteger.java (versatile input output subsystem)
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

import java.net.URI;
import java.nio.ByteBuffer;
import org.jpac.AbstractModule;
import org.jpac.InconsistencyException;
import org.jpac.NumberOutOfRangeException;
import org.jpac.SignalAccessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignalInvalidException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import org.jpac.plc.IoDirection;

/**
 *
 * @author berndschuster
 */
public class IoDecimal extends org.jpac.vioss.IoDecimal implements IoSignal{
    private final static int         DECIMALSIZE       = 8;
    private final static Long        NULLHANDLE        = 0L;
    
    private String                   plcIdentifier;
    private AdsGetSymbolHandleByName adsGetSymbolHandleByName;
    private AdsReleaseHandle         adsReleaseHandle;
    private AdsReadVariableByHandle  adsReadVariableByHandle;
    private AdsWriteVariableByHandle adsWriteVariableByHandle;
    private boolean                  checkInFaultLogged;
    private boolean                  checkOutFaultLogged;
    private ByteBuffer               byteBuffer;
    private AdsErrorCode             adsErrorCode;
    
    public IoDecimal(AbstractModule containingModule, String identifier, URI uri, IoDirection ioDirection) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException{
        super(containingModule, identifier, uri, ioDirection);
        this.plcIdentifier = uri.getPath().substring(1);//path starts with a slash "/<plcIdentifier>"
        this.checkInFaultLogged  = false;
        this.checkOutFaultLogged = false;
        Log.error("!!!!!!!!!!!!!!   IoDecimal is not tested  yet");//TODO        
    }  
        
    @Override
    public void checkIn() throws SignalAccessException, AddressException, NumberOutOfRangeException{
        double doubleValue = 0.0;
        adsErrorCode = getAdsReadVariableByHandle().getAdsResponse().getErrorCode();
        if (adsErrorCode == AdsErrorCode.NoError){
            byteBuffer.put(getAdsReadVariableByHandle().getAdsResponse().getData().getBytes());   
            doubleValue = byteBuffer.getDouble();
            try{
                inCheck = true;
                set(doubleValue);
            }
            finally{
                inCheck = false;           
            }
            if (Log.isDebugEnabled() && isChanged()){
                try{Log.debug(this + " set to " + get());}catch(SignalInvalidException exc){/*cannot happen*/}
            }
            checkInFaultLogged = false;
       }
       else{
           if (!checkInFaultLogged){
               Log.error(this + " got invalid due to ads Error " + adsReadVariableByHandle.getAdsResponse().getErrorCode());
               checkInFaultLogged = true;
           }
           invalidate();
       }
    }

    @Override
    public void checkOut() throws SignalAccessException, AddressException, NumberOutOfRangeException{
        if (isValid()){
            try{byteBuffer.putDouble(get());}catch(SignalInvalidException exc){/*cannot happen*/}        
            getAdsWriteVariableByHandle().getData().setBytes(byteBuffer.array());
        }
        if (Log.isDebugEnabled() && isChanged()){
            try{Log.debug(this + " set to " + get());}catch(SignalInvalidException exc){/*cannot happen*/}
        }
        adsErrorCode = getAdsWriteVariableByHandle().getAdsResponse().getErrorCode();
        if (adsErrorCode != AdsErrorCode.NoError){
            if (!checkOutFaultLogged){
                checkOutFaultLogged = true;
                Log.error(this + " cannot be propagated to plc due to ads Error " + getAdsWriteVariableByHandle().getAdsResponse().getErrorCode());
            }
        }
        else{
            checkOutFaultLogged = false;
        }
    }
    
    @Override
    public AdsGetSymbolHandleByName getAdsGetSymbolHandleByName(){
        if (adsGetSymbolHandleByName == null){
            adsGetSymbolHandleByName = new AdsGetSymbolHandleByName(plcIdentifier);
        }
        return adsGetSymbolHandleByName;
    }

    @Override
    public AdsReadVariableByHandle getAdsReadVariableByHandle(){
        if (adsReadVariableByHandle == null){
            adsReadVariableByHandle = new AdsReadVariableByHandle(NULLHANDLE, DECIMALSIZE);                
        }
        return adsReadVariableByHandle;
    }

    @Override
    public AdsWriteVariableByHandle getAdsWriteVariableByHandle(){
        if (adsWriteVariableByHandle == null){
            adsWriteVariableByHandle = new AdsWriteVariableByHandle(NULLHANDLE, DECIMALSIZE, new Data(new byte[DECIMALSIZE], Data.Endianness.LITTLEENDIAN));
        }
        return adsWriteVariableByHandle;
    }

    @Override
    public AdsReleaseHandle getAdsReleaseHandle(){
        if (adsReleaseHandle == null){
            adsReleaseHandle = new AdsReleaseHandle(adsGetSymbolHandleByName.getHandle());
        }
        return adsReleaseHandle;
    }
    
    @Override
    public Object getErrorCode(){
        return adsErrorCode;
    }    
}
