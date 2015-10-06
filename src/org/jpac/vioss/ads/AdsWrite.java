/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsWrite.java (versatile input output subsystem)
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
public class AdsWrite extends AmsPacket{
    AdsWriteRequest  adsReadRequest;
    AdsWriteResponse adsReadResponse;
    IndexGroup       indexGroup;
    long             indexOffset;
    int              length;
    
    public AdsWrite(IndexGroup indexGroup, long indexOffset, int length, Data data){
        this.indexGroup      = indexGroup;
        this.indexOffset     = indexOffset;
        this.length          = length;
        this.adsReadRequest  = new AdsWriteRequest(indexGroup, indexOffset, length, data);
        this.adsReadResponse = new AdsWriteResponse();  
    }
    
    @Override
    public AdsRequest getAdsRequest() {
        return adsReadRequest;
    }

    @Override
    public AdsResponse getAdsResponse() {
        return adsReadResponse;
    }
    
    public class AdsWriteRequest extends AdsRequest{
        protected IndexGroup indexGroup;
        protected long       indexOffset;
        protected int        length;    
        protected Data       data;
        

        public AdsWriteRequest(IndexGroup indexGroup, long indexOffset, int length, Data data){
            super(CommandId.AdsWrite);
            this.indexGroup  = indexGroup;
            this.indexOffset = indexOffset;
            this.length      = length;
            this.data        = data;
        }
        
        @Override
        public void writeMetaData(Connection connection) throws IOException {
            indexGroup.write(connection);
            connection.getOutputStream().writeInt((int)indexOffset);
            connection.getOutputStream().writeInt(length);
        }

        @Override
        public void writeData(Connection connection) throws IOException {
            connection.getOutputStream().write(data.getBytes(), 0, length);
        }

        @Override
        public void write(Connection connection) throws IOException {
            writeMetaData(connection);
            writeData(connection);
        }
        
        public void setData(Data data){
            this.data = data;
        }

        public Data getData(){
            return data;
        }

        @Override
        public int size(){
            return super.size() + IndexGroup.size() + 8 + (data != null ? data.getBytes().length : 0);
        }   
    } 
    
    public class AdsWriteResponse extends AdsResponse{

        public AdsWriteResponse(){
            super(0);
        }

        public AdsWriteResponse(int length){
            super(length);
        }

        @Override
        public void read(Connection connection) throws IOException {
            super.read(connection);
            readData(connection);
        }

        @Override
        public int size(){
            return super.size();
        }

        @Override
        public void readData(Connection connection) throws IOException {
            //nothing to read
        }
    }    
}
