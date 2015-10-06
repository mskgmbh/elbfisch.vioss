/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsRead.java (versatile input output subsystem)
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
import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
public class AdsRead extends AmsPacket{
    AdsReadRequest  adsReadRequest;
    AdsReadResponse adsReadResponse;
    IndexGroup      indexGroup;
    int             indexOffset;
    int             length;
    
    public AdsRead(IndexGroup indexGroup, int indexOffset, int length){
        this.indexGroup      = indexGroup;
        this.indexOffset     = indexOffset;
        this.length          = length;
        this.adsReadRequest  = new AdsReadRequest(indexGroup, indexOffset, length);
        this.adsReadResponse = new AdsReadResponse(length);
    }
    
    @Override
    public AdsRequest getAdsRequest() {
        return adsReadRequest;
    }

    @Override
    public AdsResponse getAdsResponse() {
        return adsReadResponse;
    }

    public class AdsReadRequest extends AdsRequest{
        protected IndexGroup indexGroup;
        protected int        indexOffset;
        protected int        length;        
        

        public AdsReadRequest(IndexGroup indexGroup, int indexOffset, int length){
            super(CommandId.AdsRead);
            this.indexGroup  = indexGroup;
            this.indexOffset = indexOffset;
            this.length      = length;
        }

        @Override
        public void write(Connection connection) throws IOException {
            writeMetaData(connection);
        }

        @Override
        public int size(){
            return super.size() + IndexGroup.size() + 8;
        }   

        @Override
        public void writeMetaData(Connection connection) throws IOException {
            indexGroup.write(connection);
            connection.getOutputStream().writeInt(indexOffset);
            connection.getOutputStream().writeInt(length);            
        }

        @Override
        public void writeData(Connection connection) throws IOException {
            //nothing to write
        }
    } 
    
    public class AdsReadResponse extends AdsResponse{

        public AdsReadResponse(int length){
            super(length);
            this.length = length;
            this.data   = new Data(new byte[length], Data.Endianness.LITTLEENDIAN);
        }

        @Override
        public void read(Connection connection) throws IOException {
            super.read(connection);
            int actualLength = connection.getInputStream().readInt();             
            if (actualLength != length){
                throw new IOException("data length (" + actualLength + ") does not match expected length (" + length + ")");
            }
            readData(connection);
        }

        @Override
        public void readData(Connection connection) throws IOException {
            connection.getInputStream().read(data.getBytes(), 0, length);
        }

        public int getLength() {
            return length;
        }

        public Data getData() {
            return data;
        }
        
        @Override
        public int size(){
            return super.size() + 4 + data.getBytes().length;
        }
    }    
}
