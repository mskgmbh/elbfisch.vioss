/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AbsoluteAddress.java (versatile input output subsystem)
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

package org.jpac.vioss.iec61131_3;

import org.jpac.vioss.InvalidAddressException;
import org.jpac.Address;

/**
 * represents an absolute address according to the notation of IEC61131-3
 * @author berndschuster
 */
public class AbsoluteAddress {
   public enum Area {UNDEFINED, INPUT,OUTPUT,MERKER};
   public enum Type {UNDEFINED, BIT, BYTE, WORD, DOUBLEWORD};
   private final String ABSOLUTEADDRESSTAG = "%";
   private final int    AREATAGPOS         = 1;
   private final int    TYPETAGPOS         = 2;
   private final char   INPUTTAG           = 'I';
   private final char   OUTPUTTAG          = 'O';
   private final char   MERKERTAG          = 'M';
   private final char   BITTAG             = 'X';
   private final char   BYTETAG            = 'B';
   private final char   WORDTAG            = 'W';
   private final char   DOUBLEWORDTAG      = 'D';
   private final char   DOT                = '.';

   private int     address;
   private int     doubleWordAddress;
   private int     wordAddress;
   private int     byteAddress;
   private int     bitAddress;
   private Area    area;
   private Type    type;
   
   public AbsoluteAddress(Area area, Type type, int address){
       this.area    = area;
       this.type    = type;
       this.address = address;
   }
   
   public AbsoluteAddress(String string) throws InvalidAddressException{
       address = 0;
       area    = Area.UNDEFINED;
       type    = Type.UNDEFINED;
       
       int addressPos = 0;
       string = string.trim().toUpperCase();
       if (!string.startsWith(ABSOLUTEADDRESSTAG)){
           throw new InvalidAddressException("absolute address identifiers must start with a '%'");
       }
       switch(string.charAt(AREATAGPOS)){
           case INPUTTAG:
               area = Area.INPUT;
               break;
           case OUTPUTTAG:
               area = Area.OUTPUT;
               break;
           case MERKERTAG:
               area = Area.MERKER;
               break;
           default:
               throw new InvalidAddressException("area of absolute address not specified correctly : '" + string.charAt(AREATAGPOS) + "'. Must be 'I','O' or 'M'");           
       }
       switch(string.charAt(TYPETAGPOS)){
           case BITTAG:
               type = Type.BIT;
               addressPos = 3;
               break;
           case BYTETAG:
               type = Type.BYTE;
               addressPos = 3;
               break;
           case WORDTAG:
               type = Type.WORD;
               addressPos = 3;
               break;
           case DOUBLEWORDTAG:
               type = Type.DOUBLEWORD;               
               addressPos = 3;
               break;
           default:
               if (string.charAt(TYPETAGPOS) < '0' || string.charAt(TYPETAGPOS) > '9'){
                  throw new InvalidAddressException("type of absolute address not specified correctly : '" + string.charAt(TYPETAGPOS) + "'. Must be 'X','B','W' or 'D'");           
               }
               //'tag' is a digit. Assume a bit address
               type       = Type.BIT;
               addressPos = 2;
       }
       if (type == Type.BIT){
           if (!string.contains(".")){
              throw new InvalidAddressException("address part of absolute address not specified correctly. Must be of kind <integer>[.<integer] ");                          
           }
           int dotPos      = string.indexOf(DOT);
           try{
                wordAddress = Integer.decode(string.substring(addressPos, dotPos));
                bitAddress  = Integer.decode(string.substring(dotPos + 1));
           }
           catch(NumberFormatException exc){
              throw new InvalidAddressException("address part of absolute address not specified correctly. Must be of kind <integer>[.<integer] ");                                         
           }
           if (bitAddress < 0 || bitAddress > 15){
              throw new InvalidAddressException("address part of absolute address not specified correctly. Bit address out of range (0..15)");                                         
           }
           address     = wordAddress * 16 + bitAddress;
           byteAddress = wordAddress * 2;
       }
       else{
           try{
               address = Integer.decode(string.substring(addressPos));
           }
           catch(NumberFormatException exc){
              throw new InvalidAddressException("address part of absolute address not specified correctly. Must be an integer.");                                         
           }
           switch(type){
               case BYTE:
                   byteAddress       = address;
                   wordAddress       = address / 2;
                   doubleWordAddress = address / 4;
                   break;
               case WORD:
                   byteAddress       = address * 2;
                   wordAddress       = address;
                   doubleWordAddress = address / 2;
                   break;
               case DOUBLEWORD:
                   byteAddress       = address * 4;
                   wordAddress       = address * 2;
                   doubleWordAddress = address;
                   break;
           }
       }
    }

    /**
     * returns true, if the given string starts with a '%' 
     */
    public static boolean mightBeAnIEC61131Address(String string){
        return string.trim().startsWith("%");
    }
   
    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(int address) {
        this.address = address;
    }

    /**
     * @return the wordAddress
     */
    public int getWordAddress() {
        return wordAddress;
    }

    /**
     * @param wordAddress the wordAddress to set
     */
    public void setWordAddress(int wordAddress) {
        this.wordAddress = wordAddress;
    }

    /**
     * @return the bitAddress
     */
    public int getBitAddress() {
        return bitAddress;
    }

    /**
     * @param bitAddress the bitAddress to set
     */
    public void setBitAddress(int bitAddress) {
        this.bitAddress = bitAddress;
    }

    /**
     * @return the area
     */
    public Area getArea() {
        return area;
    }

    /**
     * @param area the area to set
     */
    public void setArea(Area area) {
        this.area = area;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the doubleWordAddress
     */
    public int getDoubleWordAddress() {
        return doubleWordAddress;
    }

    /**
     * @return the byteAddress
     */
    public int getByteAddress() {
        return byteAddress;
    }
}
