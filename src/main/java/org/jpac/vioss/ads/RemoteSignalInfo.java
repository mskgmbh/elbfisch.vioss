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

package org.jpac.vioss.ads;

import org.apache.commons.lang.NotImplementedException;
import org.jpac.BasicSignalType;
import org.jpac.Signal;
import org.jpac.plc.Data;
import org.jpac.vioss.IoSignal;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo{
	private final int    DEFAULTSIZE   = 4;
	private final String PARAMETERSIZE = "size";
	
    private Signal  ioSignal;
    
    private AdsGetSymbolHandleByName adsGetSymbolHandleByName;
    private AdsReadVariableByHandle  adsReadVariableByHandle;
    private AdsWriteVariableByHandle adsWriteVariableByHandle;
    private AdsReleaseHandle         adsReleaseHandle;
    private AdsErrorCode             adsErrorCode;
    
    private Integer				     size;
    
//    private Long                     handle;

    public RemoteSignalInfo(Signal ioSignal){
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
    	this.ioSignal  = ioSignal;
    	getSize();//compute size [byte] of the corresponding plc variable
    }    
    
    protected int getSize() {
    	if (size == null) {
	    	switch (getType()) {
	    	case Logical:
	    		size = 1;
	    		break;
	    	case SignedInteger:
	    		size = (((IoSignal)ioSignal).getParameters().get("size") == null) ? DEFAULTSIZE : Integer.parseInt(((IoSignal)ioSignal).getParameters().get(PARAMETERSIZE));
	    		if (size != 1 && size != 2 && size != 4) throw new IllegalArgumentException("size of a SignedInteger must be 1,2 or 4: " + ioSignal);
	    		break;	
	    	case CharString:
	    		size = (((IoSignal)ioSignal).getParameters().get("size") == null) ? DEFAULTSIZE : Integer.parseInt(((IoSignal)ioSignal).getParameters().get(PARAMETERSIZE));
	    		if (size < 1 ) throw new IllegalArgumentException("size of a CharString must be  > 1 : " + ioSignal);
	    		break;	
	    	default:
	    		throw new NotImplementedException(getType() + " not implemented for ADS");
	    	}
    	}
    	return size;
    }
    
//    public Long getHandle() {
//    	return handle;
//    }
//    
//    public void setHandle(Long handle) {
//    	this.handle = handle;
//    }

	
    public AdsGetSymbolHandleByName getAdsGetSymbolHandleByName(){
        if (adsGetSymbolHandleByName == null){
            adsGetSymbolHandleByName = new AdsGetSymbolHandleByName(((IoSignal)ioSignal).getPath());
        }
        return adsGetSymbolHandleByName;
    }
    
    public AdsReadVariableByHandle getAdsReadVariableByHandle(){
        if (adsReadVariableByHandle == null){
            if (adsGetSymbolHandleByName != null){
                Long handle = adsGetSymbolHandleByName.getHandle() == null ? 0 : adsGetSymbolHandleByName.getHandle();//handle is not retrieved from plc at this point
                adsReadVariableByHandle = new AdsReadVariableByHandle(handle, getSize());                
            }
            else{
                Log.error("missing symbol handle for " + ((IoSignal)ioSignal).getPath());
            }
        }
        return adsReadVariableByHandle;
    }

    public AdsWriteVariableByHandle getAdsWriteVariableByHandle(){
        if (adsWriteVariableByHandle == null){
            if (adsGetSymbolHandleByName != null){
                Long handle = adsGetSymbolHandleByName.getHandle() == null ? 0 : adsGetSymbolHandleByName.getHandle();//handle is not retrieved from plc, at this point;
                adsWriteVariableByHandle = new AdsWriteVariableByHandle(handle, getSize(), new Data(new byte[getSize()], Data.Endianness.LITTLEENDIAN));
            }
            else{
                Log.error("missing symbol handle for " + ((IoSignal)ioSignal).getPath());
            }
        }
        return adsWriteVariableByHandle;
    }

    public AdsReleaseHandle getAdsReleaseHandle(){
        if (adsReleaseHandle == null){
            adsReleaseHandle = new AdsReleaseHandle(adsGetSymbolHandleByName.getHandle());
        }
        return adsReleaseHandle;
    }
    
    public void setErrorCode(AdsErrorCode adsErrorCode){
        this.adsErrorCode = adsErrorCode;
    }	
    
    public AdsErrorCode getErrorCode(){
        return adsErrorCode;
    }	

	@Override
    public String toString(){
        return super.toString().replace(")", ",") + ioSignal.getHandle() + ")";
    }    
}
