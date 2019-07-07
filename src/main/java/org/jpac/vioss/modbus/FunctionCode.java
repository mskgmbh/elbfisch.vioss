/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : FunctionCodes.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : -
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
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

package org.jpac.vioss.modbus;

import org.jpac.WrongUseException;

/**
 * modbus function codes implemented in this package
 * @author berndschuster
 */
public enum FunctionCode {
	UNDEFINED             (0x00),
    READCOILS             (0x01),
    READDISCRETEINPUTS    (0x02),
    READHOLDINGREGISTERS  (0x03),
    READINPUTREGISTERS    (0x04),
    WRITEMULTIPLECOILS    (0x0F),
    WRITEMULTIPLEREGISTERS(0x10),
    READWRITEREGISTERS    (0x17);
    
    int fc; 
    
    FunctionCode(int fc){
        this.fc = fc;
    }
    
    public int getValue(){
        return this.fc;
    }
    
    public boolean isReadFunctionBlock() {
    	return this.equals(READCOILS) || this.equals(READDISCRETEINPUTS) || this.equals(READHOLDINGREGISTERS) || this.equals(READINPUTREGISTERS) || this.equals(READWRITEREGISTERS);
    }
    
    public boolean isWriteFunctionBlock() {
    	return this.equals(WRITEMULTIPLECOILS) || this.equals(WRITEMULTIPLEREGISTERS);
    }

    static public FunctionCode fromInt(int ifc) {
    	switch(ifc) {
    		case 0x01: return READCOILS;
    		case 0x02: return READDISCRETEINPUTS;
    		case 0x03: return READHOLDINGREGISTERS;
    		case 0x04: return READINPUTREGISTERS;
    		case 0x0F: return WRITEMULTIPLECOILS;
    		case 0x10: return WRITEMULTIPLEREGISTERS;
    		case 0x17: return READWRITEREGISTERS;
    	
    		default:
    			return UNDEFINED;
    	}
    }
}
