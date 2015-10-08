/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IndexGroup.java (versatile input output subsystem)
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

/**
 *
 * @author berndschuster
 */
public enum IndexGroup {
    UNDEFINED                   (0x00000),    
    ADSIGRP_SYMTAB              (0x0F000), 	 
    ADSIGRP_SYMNAME             (0x0F001), 	 
    ADSIGRP_SYMVAL              (0x0F002), 	 
    ADSIGRP_SYM_HNDBYNAME 	(0x0F003),
    ADSIGRP_SYM_VALBYNAME 	(0x0F004), 	 
    ADSIGRP_SYM_VALBYHND 	(0x0F005), 	 
    ADSIGRP_SYM_RELEASEHND 	(0x0F006), 	 
    ADSIGRP_SYM_INFOBYNAME 	(0x0F007), 	 
    ADSIGRP_SYM_VERSION 	(0x0F008), 	 
    ADSIGRP_SYM_INFOBYNAMEEX 	(0x0F009), 	 
    ADSIGRP_SYM_DOWNLOAD 	(0x0F00A), 	 
    ADSIGRP_SYM_UPLOAD          (0x0F00B), 	 
    ADSIGRP_SYM_UPLOADINFO 	(0x0F00C), 	 
    ADSIGRP_SYMNOTE             (0x0F010), 	 
    ADSIGRP_IOIMAGE_RWIB 	(0x0F020), 	 
    ADSIGRP_IOIMAGE_RWIX 	(0x0F021), 	 
    ADSIGRP_IOIMAGE_RISIZE 	(0x0F025), 	 
    ADSIGRP_IOIMAGE_RWOB 	(0x0F030), 	 
    ADSIGRP_IOIMAGE_RWOX 	(0x0F031), 	 
    ADSIGRP_IOIMAGE_RWOSIZE 	(0x0F035), 	 
    ADSIGRP_IOIMAGE_CLEARI 	(0x0F040), 	 
    ADSIGRP_IOIMAGE_CLEARO 	(0x0F050), 	 
    ADSIGRP_IOIMAGE_RWIOB 	(0x0F060),
    ADSIGRP_SUMUP_READ          (0x0F080),
    ADSIGRP_SUMUP_WRITE         (0x0F081),
    ADSIGRP_SUMUP_READWRITE     (0x0F082),
    ADSIGRP_DEVICE_DATA 	(0x0F100), 	 
    ADSIOFFS_DEVDATA_ADSSTATE 	(0x00000), 	 
    ADSIOFFS_DEVDATA_DEVSTATE 	(0x00002);   
    
    private int indexGroup;
    
    IndexGroup(int indexgroup){
        this.indexGroup = indexgroup;
    }
    
    public void read(Connection connection) throws IOException{
        indexGroup = connection.getInputStream().readInt();
    }
    public void write(Connection connection) throws IOException{
        connection.getOutputStream().writeInt(indexGroup);
    }
    
    public boolean equals(IndexGroup ig){
        return this.indexGroup == ig.indexGroup;
    }
    
    static public int size(){
        return 4;
    }
    
    public static IndexGroup getValue(int indexGroup){
        boolean found = false;
        int     idx   = 0;
        IndexGroup[] indexGroups = IndexGroup.values();
        for(int i = 0; i < indexGroups.length && !found; i++){
            found = indexGroups[i].indexGroup == indexGroup;
            if (found){
                idx = i;
            }
        }
        return indexGroups[idx];
    }  
           
}
