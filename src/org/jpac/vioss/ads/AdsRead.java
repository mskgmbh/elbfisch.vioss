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
    
    public AdsRead(IndexGroup indexGroup, int indexOffset, int length){
        setAdsRequest(new AdsReadRequest(indexGroup, indexOffset, length));
        setAdsResponse(new AdsReadResponse(length));
    }
    
    public class AdsReadRequest extends AdsRequest{
        private final static int INDEXOFFSETSIZE = 4;
        private final static int LENGTHSIZE      = 4;
        
        protected IndexGroup indexGroup;
        protected int        indexOffset;
        protected int        length;        
        

        public AdsReadRequest(IndexGroup indexGroup, int indexOffset, int length){
            super(CommandId.AdsRead);
            this.indexGroup  = indexGroup;
            this.indexOffset = indexOffset;
            this.length      = length;
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

        @Override
        public void write(Connection connection) throws IOException {
            writeMetaData(connection);
        }

        @Override
        public int size(){
            return IndexGroup.size() + INDEXOFFSETSIZE + LENGTHSIZE;
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
        private final static int LENGTHSIZE = 4;
        public AdsReadResponse(int length){
            super(length);
            this.length = length;
            this.data   = new Data(new byte[length], Data.Endianness.LITTLEENDIAN);
        }

        @Override
        protected void readMetaData(Connection connection) throws IOException{
            super.readMetaData(connection);
            int actualLength = connection.getInputStream().readInt();             
            if (actualLength != length){
                throw new IOException("data length (" + actualLength + ") does not match expected length (" + length + ")");
            }
        }
        
        @Override
        protected void readData(Connection connection) throws IOException {
            connection.getInputStream().read(data.getBytes(), 0, length);
        }

        @Override
        public int size(){
            return super.size() + LENGTHSIZE + length;
        }
    }    
}
