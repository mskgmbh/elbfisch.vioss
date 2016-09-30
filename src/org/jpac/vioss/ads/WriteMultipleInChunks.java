/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : WriteMultipleInChunks.java (versatile input output subsystem)
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
public class WriteMultipleInChunks {
    protected int                         maxNumberOfPacketsPerChunk;
    protected ArrayList<AmsPacket>        amsPackets;
    protected ArrayList<AdsWriteMultiple> writeMultipes;
    
    public WriteMultipleInChunks(int maxNumberOfReadWritesPerChunk){
        this.maxNumberOfPacketsPerChunk = maxNumberOfReadWritesPerChunk;
        this.amsPackets                 = new ArrayList<>();
    }
    
    public void addAmsPacket(AmsPacket amsPacket){
        this.amsPackets.add(amsPacket);
    }
    
    public ArrayList<AmsPacket> getAmsPackets(){
        return this.amsPackets;
    }
    
    public void clearAmsPackets(){
        this.amsPackets.clear();
    }
    
    public void transact(Connection connection) throws IOException{
        if (writeMultipes == null){
            //build chunks of requests according to the maximum number of requests per 
            int                  numberOfChunks  = (amsPackets.size() + maxNumberOfPacketsPerChunk - 1) / maxNumberOfPacketsPerChunk;
            int                  chunkSize       = amsPackets.size() / numberOfChunks;
            int                  actualChunkSize = 0; 
            int                  actualChunk     = 0;
            AdsWriteMultiple wm = new AdsWriteMultiple();
            writeMultipes = new ArrayList<>();
            writeMultipes.add(wm);
            for (AmsPacket adsPacket : amsPackets) {
                if (actualChunkSize >= chunkSize && actualChunk != numberOfChunks -1){
                    wm = new AdsWriteMultiple();
                    writeMultipes.add(wm);
                    actualChunkSize = 0;
                    actualChunk++;
                }
                wm.addAmsPacket(adsPacket);
                actualChunkSize++;
            }
        }
        for (AdsWriteMultiple wm: writeMultipes){
            wm.transact(connection);
        }
    }
}
