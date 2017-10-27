/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsReleaseHandle.java (versatile input output subsystem)
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

import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
public class AdsReleaseHandle extends AdsWrite{
    private final static int INTLENGTH = 4;
    
    public AdsReleaseHandle(Long handle){
        super(IndexGroup.ADSIGRP_SYM_RELEASEHND, 0, INTLENGTH, new Data(new byte[INTLENGTH], Data.Endianness.LITTLEENDIAN));
        try{((AdsWriteRequest)getAdsRequest()).getData().setDWORD(0, handle);}catch(Exception exc){/*cannot happen*/}
    }
}
