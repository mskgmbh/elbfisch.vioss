/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : FloatArrayValue.java (versatile input output subsystem)
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
package org.jpac.vioss.mqttv3;

import org.jpac.Value;
import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.util.Arrays;

/**
 * represents the value of a float array signal
 * @author berndschuster
 * represents a float array value
 */
public class FloatArrayValue implements Value, Cloneable, Serializable{
    
    protected float[]            value = new float[0];
    protected transient boolean valid = false;//transient to be compatible to legacy RemoteSignals
    
    
    public void set(float[] value){
        this.value = value;
    }
    
    public float[] get(){
        return value;
    }
    
    @Override
    public Object getValue(){
        return get();
    }

    @Override
    public void setValue(Object value){
        set((float[]) value);
    }
    
    @Override
    public void copy(Value aValue){
        if (((FloatArrayValue)aValue).get() != null){
            float[] src = ((FloatArrayValue)aValue).get();
            float[] dst = new float[src.length];
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
        return Arrays.equals(get(), ((FloatArrayValue)aValue).get());
    }
    
    @Override
    public String toString(){
        String value = "";
        for(float f: get()){
            value += String.format(" %.6f,", f);
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
        return (FloatArrayValue) super.clone();
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
        for (float f : get()) {
            byteBuf.writeFloat(f);
        }
    }

    @Override
    public void decode(ByteBuf byteBuf){
        valid = byteBuf.readByte() == 0 ? false : true;
        int length   = byteBuf.readInt();
        float[] floats = new float[length];
        for (int i = 0; i < length; i++) {
            floats[i] = byteBuf.readFloat();
        }
        set(floats);
    }    
}
