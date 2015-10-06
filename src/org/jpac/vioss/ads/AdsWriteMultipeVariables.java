/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsWriteMultipeVariables.java (versatile input output subsystem)
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
import java.util.ArrayList;

/**
 *
 * @author berndschuster
 */
public class AdsWriteMultipeVariables extends AdsReadWrite{
    private static int LENGTHSIZE        = 4;
    private static int REQUESTHEADERSIZE = 16;
    private static int INT32SIZE         = 4;
    
    private ArrayList<AmsPacket>              amsPackets;
    
    private AdsWriteMultipleVariablesRequest  adsWriteMultipleVariablesRequest;
    private AdsWriteMultipleVariablesResponse adsWriteMultipleVariablesResponse;    
    
    public AdsWriteMultipeVariables(){
        super(IndexGroup.ADSIGRP_SUMUP_WRITE, 0);
        amsPackets = new ArrayList<AmsPacket>();
        this.adsWriteMultipleVariablesRequest  = new AdsWriteMultipleVariablesRequest(amsPackets);
        this.adsWriteMultipleVariablesResponse = new AdsWriteMultipleVariablesResponse(amsPackets);
    }  
    
    public void addAmsPacket(AmsPacket amsPacket){
        amsPackets.add(amsPacket);
    }
    
    @Override
    public AdsRequest getAdsRequest(){
        return this.adsWriteMultipleVariablesRequest;
    }
    
    @Override
    public AdsResponse getAdsResponse(){
        return this.adsWriteMultipleVariablesResponse;
    }

    public class AdsWriteMultipleVariablesRequest extends AdsReadWriteRequest{
        private ArrayList<AmsPacket> amsPackets;
        
        public AdsWriteMultipleVariablesRequest(ArrayList<AmsPacket> amsPackets){
            super(IndexGroup.ADSIGRP_SUMUP_WRITE, 0, 0, 0, null);
            this.amsPackets = amsPackets;
        }
        
        public void addAmsPacket(AmsPacket amsPacket){
            amsPackets.add(amsPacket);
        }

        @Override
        public void writeMetaData(Connection connection) throws IOException {            
            int readLength = 0;
            int writeLength = 0;
            setIndexOffset(amsPackets.size());
            for(AmsPacket ap: amsPackets){
                readLength  += ap.getAdsResponse().size();
                writeLength += ap.getAdsRequest().size();
            }
            setReadLength(readLength);
            setWriteLength(writeLength);
            super.writeMetaData(connection);
            for(AmsPacket aw: amsPackets){
                aw.getAdsRequest().writeMetaData(connection);
            }
        }

        @Override
        public void writeData(Connection connection) throws IOException {
            for(AmsPacket ap: amsPackets){
                ap.getAdsRequest().writeData(connection);
            }
        }
        
        @Override
        public int getWriteLength(){
            int allRequestsSize = 0;
            for(AmsPacket aw: amsPackets){
                allRequestsSize += aw.getAdsRequest().size();
            }
            return allRequestsSize;
        }   
    }
    
    public class AdsWriteMultipleVariablesResponse extends AdsReadWriteResponse{
        private int                  numberOfFailedAccesses;
        private ArrayList<AmsPacket> amsPackets;
        
        public AdsWriteMultipleVariablesResponse(ArrayList<AmsPacket> amsPackets){
            super();
            this.amsPackets = amsPackets;
        }        

        @Override
        public void readMetaData(Connection connection) throws IOException{
            super.readMetaData(connection);
            if (length != amsPackets.size() * AdsErrorCode.size()){
                throw new IOException("length of data block (" + length + ") does not match expected length (" + amsPackets.size() * AdsErrorCode.size() + ")");
            }
            if (getErrorCode() != AdsErrorCode.NoError){
                //propagate general error to all requesting signals
                for(AmsPacket aw: amsPackets){
                    aw.getAdsResponse().setErrorCode(getErrorCode());
                }                                         
            }
        }
        
        @Override
        public void readData(Connection connection) throws IOException {
            for(AmsPacket aw: amsPackets){
                aw.getAdsResponse().setErrorCode(AdsErrorCode.getValue(connection.getInputStream().readInt()));
                if (aw.getAdsResponse().getErrorCode() != AdsErrorCode.NoError){
                    numberOfFailedAccesses++;
                }
            }                         
        }
        
        public int getNumberOfFailedAccesses(){
            return this.numberOfFailedAccesses;
        }
    }   
}
