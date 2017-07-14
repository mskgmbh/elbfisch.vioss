/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AmsPacket.java (versatile input output subsystem)
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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jpac.WrongUseException;

/**
 *
 * @author berndschuster
 */
abstract public class AmsPacket {
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.ads");
    
    protected AmsTcpHeader requestAmsTcpHeader;
    protected AmsTcpHeader responseAmsTcpHeader;
    protected AmsHeader    requestAmsHeader;
    protected AmsHeader    responseAmsHeader;
    protected AdsRequest   adsRequest;
    protected AdsResponse  adsResponse;
   
    public AmsPacket(){
    }

    /**
     * @return the requestAmsTcpHeader
     */
    public AmsTcpHeader getRequestAmsTcpHeader() {
        if (requestAmsTcpHeader == null){
            requestAmsTcpHeader = new AmsTcpHeader();
        }
        return requestAmsTcpHeader;
    }

    /**
     * @return the requestAmsTcpHeader
     */
    public AmsTcpHeader getResponseAmsTcpHeader() {
        if (responseAmsTcpHeader == null){
            responseAmsTcpHeader = new AmsTcpHeader();
        }
        return responseAmsTcpHeader;
    }

    /**
     * @return the requestAmsHeader
     */
    public AmsHeader getRequestAmsHeader() {
        if (requestAmsHeader == null){
            requestAmsHeader = new AmsHeader();
        }        
        return requestAmsHeader;
    }

    /**
     * @return the requestAmsHeader
     */
    public AmsHeader getResponseAmsHeader() {
        if (responseAmsHeader == null){
            responseAmsHeader = new AmsHeader();
        }        
        return responseAmsHeader;
    }

    public AdsRequest getAdsRequest(){
        return adsRequest;
    }
    
    public AdsResponse getAdsResponse(){
        return adsResponse;
    }

    protected void setAdsRequest(AdsRequest adsRequest){
        this.adsRequest = adsRequest;
    }
    
    protected void setAdsResponse(AdsResponse adsResponse){
        this.adsResponse = adsResponse;
    }

    public void write(Connection connection) throws IOException, WrongUseException{
       //initialize headers
       getRequestAmsTcpHeader().setLength(AmsHeader.size() + getAdsRequest().size());
       getRequestAmsHeader().setSourceAmsNetId(connection.getLocalAmsNetId());
       getRequestAmsHeader().setSourceAmsPortNr(connection.getLocalAmsPortNr());
       getRequestAmsHeader().setTargetAmsNetId(connection.getTargetAmsNetId());
       getRequestAmsHeader().setTargetAmsPortNr(connection.getTargetAmsPortNr());
       getRequestAmsHeader().setCommandId(getAdsRequest().getCommandId());
       getRequestAmsHeader().setStateFlags(getAdsRequest().getStateFlags());
       getRequestAmsHeader().setDataLength(getAdsRequest().size());
       getRequestAmsHeader().setErrorCode(AdsErrorCode.NoError);
       //invoke id is set by the request header itself
       getRequestAmsHeader().setInitialized(true);
       //write packet to remote host
       if (Log.isDebugEnabled())Log.debug("writing " + getClass().getSimpleName() + " ...");
       getRequestAmsTcpHeader().write(connection);
       getRequestAmsHeader().write(connection);
       getAdsRequest().write(connection);
       connection.getOutputStream().flush();
       if (Log.isDebugEnabled())Log.debug("written " + getClass().getSimpleName());
    }

    public void read(Connection connection) throws IOException, WrongUseException{
       //read packet
       if (Log.isDebugEnabled())Log.debug("reading " + getClass().getSimpleName() + " ...");
       getResponseAmsTcpHeader().read(connection);
       getResponseAmsHeader().read(connection);
       if (getResponseAmsHeader().getInvokeId() != getRequestAmsHeader().getInvokeId()){
           Log.error("Error: unexpected invoke id in response AMS header :" + getResponseAmsHeader());
           Log.error("  stateFlags: " + getResponseAmsHeader().getStateFlags());
           Log.error("  dataLength: " + getResponseAmsHeader().getDataLength());
           Log.error("  errorCode : " + getResponseAmsHeader().getErrorCode());
           //discard rest of response
           connection.getInputStream().skip(connection.getInputStream().available());
           throw new IOException("returned response does not match request (mismatched invokeId: expected: " + getRequestAmsHeader().getInvokeId() + " received: " + getResponseAmsHeader().getInvokeId() + ")");
       }
       if (getResponseAmsHeader().getErrorCode() != AdsErrorCode.NoError){
           connection.getInputStream().skip(connection.getInputStream().available());
           throw new IOException("Ads Error " + getResponseAmsHeader().getErrorCode());           
       }
       
       getAdsResponse().read(connection);
       if (getAdsResponse().getErrorCode() != AdsErrorCode.NoError){
           connection.getInputStream().skip(connection.getInputStream().available());
           throw new IOException("Ads Response Error: " + getAdsResponse().getErrorCode());           
       }
       if (connection.getInputStream().available() > 0){
           Log.error("inconsistent response received. Some trailing bytes left: " + connection.getInputStream().available());         
           //Occasionally some bytes are left inside the stream even though the expected data has been properly read.
           //Drop them to clear stream for next request
           connection.getInputStream().skip(connection.getInputStream().available());
           //throw new IOException("inconsistent response received. Some trailing bytes left: " + connection.getInputStream().available());         
       }
       if (Log.isDebugEnabled())Log.debug("read " + getClass().getSimpleName());
    }
    
    public void transact(Connection connection) throws IOException, WrongUseException{
        write(connection);
        read(connection);
    }    
}
