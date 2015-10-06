/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsResponse.java (versatile input output subsystem)
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
import org.jpac.plc.Data;

/**
 *
 * @author berndschuster
 */
abstract public class AdsResponse extends AdsData{
    protected AdsErrorCode errorCode;
    protected int          length;
    protected Data         data;
    
    public AdsResponse(){
        this(0);
    }
    
    public AdsResponse(int length){
        this.length = length;
    }
            
    @Override
    public void read(Connection connection) throws IOException{
        readMetaData(connection);
        if (errorCode != AdsErrorCode.NoError){
            //disard remaining data in input stream
            connection.getInputStream().skip(connection.getInputStream().available());
        }
        else{
            readData(connection);
        }
        
    }
    
    protected void readMetaData(Connection connection) throws IOException{
        errorCode = AdsErrorCode.getValue(connection.getInputStream().readInt());        
    }
       
    @Override
    public void write(Connection connection) throws IOException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public AdsErrorCode getErrorCode(){
        return this.errorCode;
    }
    
    public void setErrorCode(AdsErrorCode errorCode){
        this.errorCode = errorCode;
    }
    
    public void setLength(int length){
        this.length = length;
    }

    public int getLength(){
        return this.length;
    }

    public Data getData(){
        if (data == null){
            data = new Data(new byte[getLength()], Data.Endianness.LITTLEENDIAN);
        }
        return this.data;
    }
    
    public void setData(Data data){
        this.data = data;
    }

    @Override
    public int size(){
        return AdsErrorCode.size();
    }
           
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + errorCode + ")";
    }
    
    abstract public void readData(Connection connection) throws IOException;
}
