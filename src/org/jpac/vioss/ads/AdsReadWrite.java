/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsReadWrite.java (versatile input output subsystem)
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
public class AdsReadWrite extends AmsPacket{
    private AdsReadWriteRequest  adsReadWriteRequest;
    private AdsReadWriteResponse adsReadWriteResponse;

    
    public AdsReadWrite(IndexGroup indexGroup, int indexOffset, int readLength, int writeLength, Data responseData, Data requestData){
        this.adsReadWriteRequest  = new AdsReadWriteRequest(indexGroup, indexOffset, readLength, writeLength, requestData);
        this.adsReadWriteResponse = new AdsReadWriteResponse();
    }
    
    public AdsReadWrite(IndexGroup indexGroup, int indexOffset){
        this(indexGroup, indexOffset, 0, 0, null, null);
    }
    
    
    @Override
    public AdsRequest getAdsRequest() {
        return adsReadWriteRequest;
    }

    @Override
    public AdsResponse getAdsResponse() {
        return adsReadWriteResponse;
    }

    public class AdsReadWriteRequest extends AdsRequest{
        protected static final int INDEXOFFSETSIZE = 4;
        protected static final int READLENGTHSIZE  = 4;
        protected static final int WRITELENGTHSIZE = 4;
        
        protected IndexGroup indexGroup;
        protected int        indexOffset;
        protected int        readLength;        
        protected int        writeLength;        

        protected Data       writeData;
        
        public AdsReadWriteRequest(IndexGroup indexGroup, int indexOffset, int readLength, int writeLength, Data writeData){
            super(CommandId.AdsReadWrite);
            this.indexGroup  = indexGroup;
            this.indexOffset = indexOffset;
            this.readLength  = readLength;
            this.writeLength = writeLength;
        }

        @Override
        public void writeMetaData(Connection connection) throws IOException {
            indexGroup.write(connection);
            connection.getOutputStream().writeInt(indexOffset);
            connection.getOutputStream().writeInt(readLength);
            connection.getOutputStream().writeInt(writeLength);
        }
        
        @Override
        public void writeData(Connection connection) throws IOException{
            connection.getOutputStream().write(writeData.getBytes(), 0, writeLength);
        }
        
           @Override
        public void write(Connection connection) throws IOException {
            writeMetaData(connection);
            writeData(connection);
        }

        /**
         * @return the indexGroup
         */
        public IndexGroup getIndexGroup() {
            return indexGroup;
        }

        /**
         * @param indexGroup the indexGroup to set
         */
        public void setIndexGroup(IndexGroup indexGroup) {
            this.indexGroup = indexGroup;
        }

        /**
         * @return the indexOffset
         */
        public int getIndexOffset() {
            return indexOffset;
        }

        /**
         * @param indexOffset the indexOffset to set
         */
        public void setIndexOffset(int indexOffset) {
            this.indexOffset = indexOffset;
        }

        /**
         * @return the readLength
         */
        public int getReadLength() {
            return readLength;
        }

        /**
         * @param readLength the readLength to set
         */
        public void setReadLength(int readLength) {
            this.readLength = readLength;
        }

        /**
         * @return the writeLength
         */
        public int getWriteLength() {
            return writeLength;
        }

        /**
         * @param writeLength the writeLength to set
         */
        public void setWriteLength(int writeLength) {
            this.writeLength = writeLength;
        }

        /**
         * @return the writeData
         */
        public Data getWriteData() {
            return writeData;
        }

        /**
         * @param writeData the writeData to set
         */
        public void setWriteData(Data writeData) {
            this.writeData = writeData;
        }

        @Override
        public int size(){
            return super.size() + IndexGroup.size() + INDEXOFFSETSIZE + READLENGTHSIZE + WRITELENGTHSIZE + getWriteLength();
        }   
    } 
    
    public class AdsReadWriteResponse extends AdsResponse{
        protected static final int  LENGTHSIZE = 4; 

        public AdsReadWriteResponse(){
            super(0);
        }

        public AdsReadWriteResponse(int length){
            super(length);
        }

        @Override
        public void read(Connection connection) throws IOException {
            super.readMetaData(connection);
            if (getErrorCode() == AdsErrorCode.NoError){                
                length = connection.getInputStream().readInt();
                readData(connection);
            }
        }

        @Override
        public int size(){
            return super.size() + LENGTHSIZE + (data != null ? data.getBytes().length : 0);
        }

        /**
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * @return the data
         */
        public Data getData() {
            return data;
        }
        
        @Override
        public void readMetaData(Connection connection) throws IOException{
            super.readMetaData(connection);
            if (getErrorCode() == AdsErrorCode.NoError){
                length = connection.getInputStream().readInt();
            }
        }

        @Override
        public void readData(Connection connection) throws IOException {
            if (data == null || length > data.getBytes().length){
                data = new Data(new byte[length], Data.Endianness.LITTLEENDIAN);
            }
            connection.getInputStream().read(data.getBytes(), 0, length);
        }
    }    
}
