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


package org.jpac.vioss.modbus.wago750;

import org.jpac.vioss.iec61131_3.AbsoluteAddress;
import org.jpac.vioss.InvalidAddressException;
import org.jpac.vioss.modbus.*;

/**
 * 
 * @author berndschuster
 */

public class DataBlock extends org.jpac.vioss.modbus.DataBlock{
        private enum DataBlocks {BIT_PFCOUT             (7 ,0x0000,0x0000,null                               ,null                               ,false),
                                 BIT_PFCIN              (8 ,0x0000,0x0000,FunctionCode.READINPUTDISCRETES    ,null                               ,true),
                                 BIT_NOVRAM             (9 ,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 BIT_PHYSICALINPUTAREA1 (10,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 BIT_PHYSICALINPUTAREA2 (11,0x0000,0x0000,FunctionCode.READINPUTREGISTERS    ,null                               ,false),
                                 BIT_PHYSICALOUTPUTAREA1(12,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false),
                                 BIT_PHYSICALOUTPUTAREA2(13,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false),
                                 REG_PFCOUT             (14 ,0x0000,0x0000,null                               ,null                               ,false),
                                 REG_PFCIN              (15 ,0x0000,0x0000,FunctionCode.READINPUTDISCRETES    ,null                               ,true),
                                 REG_NOVRAM             (16 ,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 REG_PHYSICALINPUTAREA1 (17,0x0000,0x0000,FunctionCode.READCOILS             ,FunctionCode.WRITEMULTIPLECOILS    ,true),
                                 REG_PHYSICALINPUTAREA2 (18,0x0000,0x0000,FunctionCode.READINPUTREGISTERS    ,null                               ,false),
                                 REG_PHYSICALOUTPUTAREA1(19,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false),
                                 REG_PHYSICALOUTPUTAREA2(20,0x0000,0x0000,FunctionCode.READHOLDINGREGISTERS  ,FunctionCode.WRITEMULTIPLEREGISTERS,false);
                             
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
    
    private DataBlocks      dataBlock;
    private AbsoluteAddress absoluteAddress;

    
    public DataBlock(int dataBlock){
        super();
        this.dataBlock       = DataBlocks.fromValue(dataBlock);
        this.absoluteAddress = null;
    }
    
    public DataBlock(String string) throws InvalidAddressException{
        super();
        DataBlocks fromString = null;
        if(AbsoluteAddress.mightBeAnIEC61131Address(string)){
            absoluteAddress = new AbsoluteAddress(string);
            switch(absoluteAddress.getArea()){
                case INPUT:
                    fromString = computeInputDataBlock();
                    break;
                case OUTPUT:
                    fromString = computeOutputDataBlock();
                    break;
                case MERKER:
                    fromString = computeMerkerDataBlock();
                    break;
                default:
                    throw new AssertionError(absoluteAddress.getArea().name());
            }
        }
        else {
            fromString = DataBlocks.fromString(string);
        };
        this.dataBlock = fromString;
    }
    
    private DataBlocks computeInputDataBlock() {
        DataBlocks retDataBlock = null;
        switch(absoluteAddress.getType()){
            case BIT:
                if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALINPUTAREA1.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALINPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALINPUTAREA1;
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA1.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA1;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PFCIN.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PFCIN.endAddress){
                    retDataBlock = DataBlocks.BIT_PFCIN;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALINPUTAREA2.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALINPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALINPUTAREA2;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA2.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA2;                    
                }
                break;
            case WORD:
                if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PHYSICALINPUTAREA1.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PHYSICALINPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.REG_PHYSICALINPUTAREA1;
                }else if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PFCIN.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PFCIN.endAddress){
                    retDataBlock = DataBlocks.REG_PFCIN;                    
                }else if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PHYSICALINPUTAREA2.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PHYSICALINPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.REG_PHYSICALINPUTAREA2;                    
                }
                break;
        }
        return retDataBlock;
    }

    private DataBlocks computeOutputDataBlock() {
        DataBlocks retDataBlock = null;
        switch(absoluteAddress.getType()){
            case BIT:
                if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA1.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA1;
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA1.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA1;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PFCIN.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PFCIN.endAddress){
                    retDataBlock = DataBlocks.BIT_PFCIN;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA2.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA2;                    
                }else if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_PHYSICALOUTPUTAREA2.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_PHYSICALOUTPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.BIT_PHYSICALOUTPUTAREA2;                    
                }
                break;
            case WORD:
                if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PHYSICALOUTPUTAREA1.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PHYSICALOUTPUTAREA1.endAddress){
                    retDataBlock = DataBlocks.REG_PHYSICALOUTPUTAREA1;
                }else if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PFCOUT.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PFCOUT.endAddress){
                    retDataBlock = DataBlocks.REG_PFCOUT;                    
                }else if (absoluteAddress.getWordAddress() >= DataBlocks.REG_PHYSICALOUTPUTAREA2.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_PHYSICALOUTPUTAREA2.endAddress){
                    retDataBlock = DataBlocks.REG_PHYSICALOUTPUTAREA2;                    
                }
                break;
        }
        return retDataBlock;
    }

    private DataBlocks computeMerkerDataBlock() {
        DataBlocks retDataBlock = null;
        switch(absoluteAddress.getType()){
            case BIT:
                if (absoluteAddress.getBitAddress() >= DataBlocks.BIT_NOVRAM.baseAddress && absoluteAddress.getBitAddress() <= DataBlocks.BIT_NOVRAM.endAddress){
                    retDataBlock = DataBlocks.BIT_NOVRAM;
                }
                break;
            case WORD:
                if (absoluteAddress.getWordAddress() >= DataBlocks.REG_NOVRAM.baseAddress && absoluteAddress.getWordAddress() <= DataBlocks.REG_NOVRAM.endAddress){
                    retDataBlock = DataBlocks.REG_NOVRAM;
                }
                break;
        }
        return retDataBlock;
    }
                
    @Override
    public FunctionCode getReadFunctionCode(){
        return dataBlock.readFunctionCode;
    }
    
    @Override
    public FunctionCode getWriteFunctionCode(){
        return dataBlock.writeFunctionCode;
    }                
}
