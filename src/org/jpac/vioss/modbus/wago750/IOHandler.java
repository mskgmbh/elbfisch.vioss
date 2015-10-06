/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : IOHandler.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : -
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : -
 *
 * This file is part of the jPac PLC communication library.
 * The jPac PLC communication library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The jPac PLC communication library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac PLC communication library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.jpac.vioss.modbus.wago750;

import java.net.URI;
import java.util.StringTokenizer;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.InvalidAddressException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.modbus.IOHandler{
    final static String HANDLEDSCHEME   = "MODBUS:WAGO750";

    public IOHandler(URI uri) throws IllegalUriException{
        super(uri);
    }

    @Override
    protected DataBlock seizeModbusDataBlock(URI uri) throws IllegalUriException{
        DataBlock dataBlock = null;
        String            token;
        StringTokenizer   tokenizer = new StringTokenizer(uri.getPath(),"/");
        token     = tokenizer.nextToken();
        if (token == null){
            throw new IllegalUriException("missing datablock specification in " + uri);
        }
        try{
            dataBlock = (DataBlock)new org.jpac.vioss.modbus.wago750.DataBlock(token.toUpperCase());
        }
        catch(InvalidAddressException exc){
            throw new IllegalUriException(exc);
        }
        return dataBlock;
    }        
}
