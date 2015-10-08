/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsReadWriteMultiple.java (versatile input output subsystem)
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
import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
public class AdsReadWriteMultiple extends AdsReadWrite{
    private static int LENGTHSIZE        = 4;
    private static int REQUESTHEADERSIZE = 16;
    private static int INT32SIZE         = 4;
    
    private ArrayList<AdsReadWrite>      adsReadWrites;
        
    public AdsReadWriteMultiple(){
        super(IndexGroup.ADSIGRP_SUMUP_READWRITE, 0);
        adsReadWrites = new ArrayList<AdsReadWrite>();
        setAdsRequest(new AdsReadWriteMultipleRequest(adsReadWrites));
        setAdsResponse(new AdsReadWriteMultipleResponse(adsReadWrites));
    }  
    
    public void addAdsReadWrite(AdsReadWrite adsReadWrite){
        adsReadWrites.add(adsReadWrite);
    }
        
    public void clearAdsReadWrites(){
        adsReadWrites.clear();
    }

    public ArrayList<AdsReadWrite> getAdsReadWrites(){
        return this.adsReadWrites;
    }

    public class AdsReadWriteMultipleRequest extends AdsReadWrite.AdsReadWriteRequest{
        private ArrayList<AdsReadWrite> adsReadWrites;
        
        public AdsReadWriteMultipleRequest(ArrayList<AdsReadWrite> adsReadWrites){
            super(IndexGroup.ADSIGRP_SUMUP_READWRITE, 0, 0, 0, null);
            this.adsReadWrites = adsReadWrites;
        }
        
        @Override
        public void writeMetaData(Connection connection) throws IOException {            
            int readLength = 0;
            int writeLength = 0;
            for(AdsReadWrite arw: adsReadWrites){
                readLength  += arw.getAdsResponse().size();
                writeLength += arw.getAdsRequest().size();
            }
            setReadLength(readLength);
            setWriteLength(writeLength);
            setIndexOffset(adsReadWrites.size());            
            super.writeMetaData(connection);
            for(AdsReadWrite arw: adsReadWrites){
                arw.getAdsRequest().writeMetaData(connection);
            }
        }

        @Override
        public void writeData(Connection connection) throws IOException {
            for(AdsReadWrite arw: adsReadWrites){
                arw.getAdsRequest().writeData(connection);
            }
        }
        
        @Override
        public int getWriteLength(){
            int allRequestsSize = 0;
            for(AdsReadWrite arw: adsReadWrites){
                allRequestsSize += arw.getAdsRequest().size();
            }
            return allRequestsSize;
        }   
    }
    
    public class AdsReadWriteMultipleResponse extends AdsReadWrite.AdsReadWriteResponse{
        private int                     numberOfFailedAccesses;
        private ArrayList<AdsReadWrite> adsReadWrites;
        
        public AdsReadWriteMultipleResponse(ArrayList<AdsReadWrite> adsReadWrites){
            super();
            this.adsReadWrites = adsReadWrites;
        }        

        @Override
        public void readMetaData(Connection connection) throws IOException{
            setLength(computeExpectedLength());
            super.readMetaData(connection);
            if (getErrorCode() != AdsErrorCode.NoError){
                //propagate general error to all requesting signals
                for(AdsReadWrite arw: adsReadWrites){
                    arw.getAdsResponse().setErrorCode(getErrorCode());
                }                                         
            }
        }
        
        @Override
        public void readData(Connection connection) throws IOException {
            //first read error codes
            for(AdsReadWrite arw: adsReadWrites){
                arw.getAdsResponse().setErrorCode(AdsErrorCode.getValue(connection.getInputStream().readInt()));
                if (arw.getAdsResponse().getErrorCode() != AdsErrorCode.NoError){
                    numberOfFailedAccesses++;
                }
                int actualLength = connection.getInputStream().readInt();
                if (actualLength !=  arw.getAdsResponse().getLength()){
                    throw new IOException("data length (" + actualLength + ") does not match expected length (" + arw.getAdsResponse().getLength() + ")");
                }
            }
            //then read data items
            for(AdsReadWrite arw: adsReadWrites){
                connection.getInputStream().read(arw.getAdsResponse().getData().getBytes(), 0, arw.getAdsResponse().getLength());
            }
        }
        
        public int getNumberOfFailedAccesses(){
            return this.numberOfFailedAccesses;
        }

        protected int computeExpectedLength(){
            int length = 0;
            for (AdsReadWrite arw: adsReadWrites){
                length += arw.getAdsResponse().size();
            }
            return length;
        }
        
    }   
}
