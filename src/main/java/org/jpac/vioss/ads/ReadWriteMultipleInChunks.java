/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : ReadWriteMultipleInChunks.java (versatile input output subsystem)
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
public class ReadWriteMultipleInChunks {
    protected int                             maxNumberOfReadWritesPerChunk;
    protected ArrayList<AdsReadWrite>         adsReadWrites;
    protected ArrayList<AdsReadWriteMultiple> readWriteMultipes;
    
    public ReadWriteMultipleInChunks(int maxNumberOfReadWritesPerChunk){
        this.maxNumberOfReadWritesPerChunk = maxNumberOfReadWritesPerChunk;
        this.adsReadWrites                 = new ArrayList<>();
    }
    
    public void addAdsReadWrite(AdsReadWrite adsReadWrite){
        this.adsReadWrites.add(adsReadWrite);
    }
    
    public ArrayList<AdsReadWrite> getAdsReadWrites(){
        return this.adsReadWrites;
    }
    
    public void transact(Connection connection) throws IOException{
        if (readWriteMultipes == null){
            //build chunks of requests according to the maximum number of requests per 
            int                  numberOfChunks  = (adsReadWrites.size() + maxNumberOfReadWritesPerChunk - 1) / maxNumberOfReadWritesPerChunk;
            int                  chunkSize       = adsReadWrites.size() / numberOfChunks;
            int                  actualChunkSize = 0; 
            int                  actualChunk     = 0;
            AdsReadWriteMultiple rwm = new AdsReadWriteMultiple();
            readWriteMultipes = new ArrayList<>();
            readWriteMultipes.add(rwm);
            for (AdsReadWrite adsReadWrite : adsReadWrites) {
                if (actualChunkSize >= chunkSize && actualChunk != numberOfChunks -1){
                    rwm = new AdsReadWriteMultiple();
                    readWriteMultipes.add(rwm);
                    actualChunkSize = 0;
                    actualChunk++;
                }
                rwm.addAdsReadWrite(adsReadWrite);
                actualChunkSize++;
            }
        }
        for (AdsReadWriteMultiple rwm: readWriteMultipes){
            rwm.transact(connection);
        }
    }
}
