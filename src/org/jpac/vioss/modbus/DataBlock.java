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

/**
 * 
 * @author berndschuster
 */

public class DataBlock {
        public  enum DataBlocks {UNDEFINED              (0,0x0000,0x0000,null                               ,null                               ,false),
                                 PHYSICALDISCRETEINPUTS (1,0x0000,0x0000,FunctionCode.READINPUTDISCRETES    ,null                               ,true),
                                 INTERNALBITS           (2,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 PHYSICALCOILS          (3,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 PHYSICALINPUTREGISTERS (4,0x0000,0x0000,FunctionCode.READINPUTREGISTERS    ,null                               ,false),
                                 INTERNALREGISTERS      (5,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false),
                                 PHYSICALOUTPUTREGISTERS(6,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false);
                             
        private final int              value;
        private final int              baseAddress;
        private final int              endAddress;
        private final FunctionCode     readFunctionCode;
        private final FunctionCode     writeFunctionCode;
        private final boolean          bitAccess;

        DataBlocks(int value, int baseAddress, int endAddress, FunctionCode readFunctionCode,  FunctionCode writeFunctionCode, boolean bitAccess){
            this.value              = value;
            this.baseAddress        = baseAddress;
            this.endAddress         = endAddress;
            this.readFunctionCode   = readFunctionCode;
            this.writeFunctionCode  = writeFunctionCode;
            this.bitAccess          = bitAccess;
        }
        
        public static DataBlocks fromValue(int value){
            DataBlocks dbs = null;
            for (DataBlocks d: DataBlocks.values()){
                if (d.value == value){
                    dbs = d;
                    break;
                }
            }
            return dbs;
        }

        public static DataBlocks fromString(String string){
            DataBlocks dbs = null;
            for (DataBlocks d: DataBlocks.values()){
                if (d.toString().equals(string)){
                    dbs = d;
                    break;
                }
            }
            return dbs;
        }
    };   
    
    protected DataBlocks dataBlock;
    
    public DataBlock(){
        this.dataBlock = DataBlocks.UNDEFINED;
    }
    
    public DataBlock(DataBlocks dataBlock) throws InvalidDataBlockException{
        this.dataBlock = dataBlock;
        if (this.dataBlock == null){
            throw new InvalidDataBlockException();
        }
    }
    
    public DataBlock(String string) throws InvalidDataBlockException{
        this.dataBlock = DataBlocks.fromString(string);
        if (this.dataBlock == null){
            throw new InvalidDataBlockException();
        }
    }

    public DataBlocks getValue(){
        return this.dataBlock;
    }
        
    public FunctionCode getReadFunctionCode(){
        return dataBlock.readFunctionCode;
    }
    
    public FunctionCode getWriteFunctionCode(){
        return dataBlock.writeFunctionCode;
    }    
    /**
     * 
     * @return used to check, if the data items contained in this datablock are accessed as bits 
     */  
    public boolean isBitAccess(){
        return dataBlock.bitAccess;
    }

    /**
     * 
     * @return used to check, if the data items contained in this datablock are accessed as registers
     */  
    public boolean isRegisterAccess(){
        return !dataBlock.bitAccess;
    }

    @Override
    public String toString(){
        return dataBlock.toString();
    }
}
