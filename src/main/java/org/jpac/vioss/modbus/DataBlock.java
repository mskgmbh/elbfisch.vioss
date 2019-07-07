/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : DataBlock.java
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
import org.jpac.plc.Data;
import org.jpac.vioss.modbus.Iec61131Address.AccessMode;

/**
 * 
 * @author berndschuster
 */

public class DataBlock {
        protected int             address;			 //address of the data block inside the modbus device [word]
        protected int             size;				 //size [word]
        protected FunctionCode    readFunctionCode;  //block read command for accessing the data block
        protected FunctionCode    writeFunctionCode; //block write command for accessing the data block
        protected Iec61131Address iec61131Address;   //IEC61131 address of this data block (1st word)
        protected Data            data;              //contence of this data block

        public DataBlock(int address, int size, FunctionCode readFunctionCode, FunctionCode writeFunctionCode, Iec61131Address iec61131Address){
            this.address           = address;
            this.size              = size;
            this.readFunctionCode  = readFunctionCode;
            this.writeFunctionCode = writeFunctionCode;
            this.iec61131Address   = iec61131Address;
            
            if (size > 256) {
            	throw new WrongUseException("Error: modbus datablock size must not exceed 256 words");
            }
            if (readFunctionCode != FunctionCode.UNDEFINED && iec61131Address.accessMode == AccessMode.OUTPUT) {
            	throw new WrongUseException("read function code cannot be applied to output datablock: " + iec61131Address.getAddressSpecifier());
            }
            if (writeFunctionCode != FunctionCode.UNDEFINED && iec61131Address.accessMode == AccessMode.INPUT) {
            	throw new WrongUseException("Write function code cannot be applied to input datablock: " + iec61131Address.getAddressSpecifier());
            }
            if (readFunctionCode != FunctionCode.UNDEFINED && !readFunctionCode.isReadFunctionBlock()) {
            	throw new WrongUseException("applied readFunctionCode must be a read FC for " + iec61131Address.getAddressSpecifier());
            }

            if (writeFunctionCode != FunctionCode.UNDEFINED && !writeFunctionCode.isWriteFunctionBlock()) {
            	throw new WrongUseException("applied writeFunctionCode must be a write FC for "+ iec61131Address.getAddressSpecifier());
            }
            this.data = new Data(new byte[2 * this.size]);
        }

		public int getAddress() {
			return address;
		}

		public int getSize() {
			return size;
		}
		
		public Data getData() {
			return this.data;
		}

		public FunctionCode getReadFunctionCode() {
			return readFunctionCode;
		}

		public FunctionCode getWriteFunctionCode() {
			return writeFunctionCode;
		}

		public Iec61131Address getIec61131Address() {
			return iec61131Address;
		}  
		
		public boolean contains(Iec61131Address iec61131Address) {
			boolean contained = false;
			contained  = this.iec61131Address.accessMode == iec61131Address.accessMode   &&
			             this.iec61131Address.getAddress() <= iec61131Address.getAddress() &&
			             this.iec61131Address.getAddress() + this.size > iec61131Address.getAddress();
			return contained;
		}
		
		@Override
		public String toString() {
			return getClass().getName() + "(" + address + ", " + getSize() + ", "  + readFunctionCode + ", " + writeFunctionCode + ", " + iec61131Address +")";			
		}
}
