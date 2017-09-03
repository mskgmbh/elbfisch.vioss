/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : ReadMultipleInChunks.java (versatile input output subsystem)
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
public class ReadMultipleInChunks {
    protected int                        maxNumberOfPacketsPerChunk;
    protected ArrayList<AmsPacket>       amsPackets;
    protected ArrayList<AdsReadMultiple> readMultipes;
    
    public ReadMultipleInChunks(int maxNumberOfReadPerChunk){
        this.maxNumberOfPacketsPerChunk = maxNumberOfReadPerChunk;
        this.amsPackets                 = new ArrayList<>();
    }
    
    public void addAmsPacket(AmsPacket amsPacket){
        this.amsPackets.add(amsPacket);
    }
    
    public ArrayList<AmsPacket> getAmsPackets(){
        return this.amsPackets;
    }
    
    public void transact(Connection connection) throws IOException{
        if (readMultipes == null){
            //build chunks of requests according to the maximum number of requests per 
            int                  numberOfChunks  = (amsPackets.size() + maxNumberOfPacketsPerChunk - 1) / maxNumberOfPacketsPerChunk;
            int                  chunkSize       = amsPackets.size() / numberOfChunks;
            int                  actualChunkSize = 0; 
            int                  actualChunk     = 0;
            AdsReadMultiple rm = new AdsReadMultiple();
            readMultipes = new ArrayList<>();
            readMultipes.add(rm);
            for (AmsPacket adsPacket : amsPackets) {
                if (actualChunkSize >= chunkSize && actualChunk != numberOfChunks -1){
                    rm = new AdsReadMultiple();
                    readMultipes.add(rm);
                    actualChunkSize = 0;
                    actualChunk++;
                }
                rm.addAmsPacket(adsPacket);
                actualChunkSize++;
            }
        }
        for (AdsReadMultiple rm: readMultipes){
            rm.transact(connection);
        }
    }
}
