/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : DoubleArrayValue.java (versatile input output subsystem)
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
package org.jpac.vioss.iedb;

import org.jpac.Value;
import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.util.Arrays;

/**
 * represents the value of a double array signal
 * @author berndschuster
 * represents a double array value
 */
public class DoubleArrayValue implements Value, Cloneable, Serializable{
    
    protected double[]            value = new double[0];
    protected transient boolean valid = false;//transient to be compatible to legacy RemoteSignals
    
    
    public void set(double[] value){
        this.value = value;
    }
    
    public double[] get(){
        return value;
    }
    
    @Override
    public Object getValue(){
        return get();
    }

    @Override
    public void setValue(Object value){
        set((double[]) value);
    }
    
    @Override
    public void copy(Value aValue){
        if (((DoubleArrayValue)aValue).get() != null){
            double[] src = ((DoubleArrayValue)aValue).get();
            double[] dst = new double[src.length];
            System.arraycopy(src, 0, dst, 0, src.length);
            set(dst);         
        }
        else{
            set(null);
        }
        this.valid = aValue.isValid();        
    }

    @Override
    public boolean equals(Value aValue) {
        return Arrays.equals(get(), ((DoubleArrayValue)aValue).get());
    }
    
    @Override
    public String toString(){
        String value = "";
        for(double d: get()){
            value += String.format(" %.6f,", d);
        }
        if (value.length() > 0) {
            value = "[ " + value.substring(0, value.length() - 1) + "]";
        } else {
            value = "[]";
        }
        return value;
    }

    @Override
    public Value clone() throws CloneNotSupportedException {
        return (DoubleArrayValue) super.clone();
    }

    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public void encode(ByteBuf byteBuf){
        byteBuf.writeByte(valid ? 1 : 0);
        byteBuf.writeInt(get().length);
        for (double d : get()) {
            byteBuf.writeDouble(d);
        }
    }

    @Override
    public void decode(ByteBuf byteBuf){
        valid = byteBuf.readByte() == 0 ? false : true;
        int length   = byteBuf.readInt();
        double[] doubles = new double[length];
        for (int i = 0; i < length; i++) {
            doubles[i] = byteBuf.readDouble();
        }
        set(doubles);
    }    
}
