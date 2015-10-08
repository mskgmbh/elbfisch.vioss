/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsReadMultipeVariables.java (versatile input output subsystem)
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
public class AdsReadMultiple extends AdsReadWrite{
    private static int LENGTHSIZE        = 4;
    private static int REQUESTHEADERSIZE = 16;
    private static int INT32SIZE         = 4;
    
    private ArrayList<AmsPacket>    amsPackets;    
    
    public AdsReadMultiple(){
        super(IndexGroup.ADSIGRP_SUMUP_READ, 0);
        amsPackets = new ArrayList<AmsPacket>();
        setAdsRequest(new AdsReadMultipleRequest(amsPackets));
        setAdsResponse(new AdsReadMultipleResponse(amsPackets));
    }  
    
    public void addAmsPacket(AmsPacket amsPacket){
        amsPackets.add(amsPacket);
    }
    
    public void clearAmsPackets(){
        amsPackets.clear();
    }

    public class AdsReadMultipleRequest extends AdsReadWrite.AdsReadWriteRequest{
        private ArrayList<AmsPacket> amsPackets;
        
        public AdsReadMultipleRequest(ArrayList<AmsPacket> amsPackets){
            super(IndexGroup.ADSIGRP_SUMUP_READ, 0, 0, 0, null);
            this.amsPackets = amsPackets;
        }
        
        @Override
        public void writeMetaData(Connection connection) throws IOException {            
            int readLength = 0;
            int writeLength = 0;
            for(AmsPacket ap: amsPackets){
                readLength  += ap.getAdsResponse().size() - LENGTHSIZE;//length is not transmitted by plc in this case
                writeLength += ap.getAdsRequest().size();
            }
            setReadLength(readLength);
            setWriteLength(writeLength);
            setIndexOffset(amsPackets.size());            
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
    
    public class AdsReadMultipleResponse extends AdsReadWrite.AdsReadWriteResponse{
        private int                  numberOfFailedAccesses;
        private ArrayList<AmsPacket> amsPackets;
        
        public AdsReadMultipleResponse(ArrayList<AmsPacket> amsPackets){
            super();//length field is initialized to "0" at this point. 
            this.amsPackets = amsPackets;
        }        

        @Override
        public void readMetaData(Connection connection) throws IOException{
            //compute expected data length
            super.setLength(computeExpectedLength());
            super.readMetaData(connection);
            if (getErrorCode() != AdsErrorCode.NoError){
                //propagate general error to all requesting signals
                for(AmsPacket aw: amsPackets){
                    aw.getAdsResponse().setErrorCode(getErrorCode());
                }                                         
            }
        }
        
        @Override
        public void readData(Connection connection) throws IOException {
            //first read error codes
            numberOfFailedAccesses = 0;
            for(AmsPacket aw: amsPackets){
                aw.getAdsResponse().setErrorCode(AdsErrorCode.getValue(connection.getInputStream().readInt()));
                if (aw.getAdsResponse().getErrorCode() != AdsErrorCode.NoError){
                    numberOfFailedAccesses++;
                }
            }
            //then read data items
            for(AmsPacket aw: amsPackets){
                connection.getInputStream().read(aw.getAdsResponse().getData().getBytes(), 0, aw.getAdsResponse().getLength());
            }
        }
        
        public int getNumberOfFailedAccesses(){
            return this.numberOfFailedAccesses;
        }
        
        protected int computeExpectedLength(){
            int length = 0;
            for (AmsPacket ap: amsPackets){
                length += ap.getAdsResponse().size() - LENGTHSIZE;//length field is not transmitted
            }
            return length;
        }
    }   
}
