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
    private final static int INDEXOFFSETSIZE = 4;
    private final static int LENGTHSIZE      = 4;
    
    public AdsWrite(IndexGroup indexGroup, long indexOffset, int length, Data data){
        setAdsRequest(new AdsWriteRequest(indexGroup, indexOffset, length, data));
        setAdsResponse(new AdsWriteResponse());  
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
        
        public void setIndexGroup(IndexGroup indexGroup){
            this.indexGroup = indexGroup;
        }

        public void setIndexOffset(int indexOffset){
            this.indexOffset = indexOffset;
        }

        public void setLength(int length){
            this.length = length;
        }
        
        
        public Data getData(){
            return this.data;
        }
        
        @Override
        public int size(){
            return IndexGroup.size() + INDEXOFFSETSIZE + LENGTHSIZE + length;
        }   
    } 
    
    public class AdsWriteResponse extends AdsResponse{

        public AdsWriteResponse(){
            super(0);
        }

        public AdsWriteResponse(int length){
            super(length);
        }
    }    
}
