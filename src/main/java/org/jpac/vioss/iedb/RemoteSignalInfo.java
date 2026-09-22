/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : RemoteSignalInfo.java
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

import org.jpac.BasicSignalType;
import org.jpac.InconsistencyException;
import org.jpac.Signal;
import org.jpac.LogicalValue;
import org.jpac.SignedIntegerValue;
import org.jpac.Value;
import org.jpac.DecimalValue;
import org.jpac.CharStringValue;
import org.jpac.vioss.IoSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo{
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.iedb");

	static protected String   DATABLOCKTAG        = "/dp/";
	protected static String   PARAMETER_USEQUOTES = "useQuotes";
	protected static String   PARAMETERVALUE_TRUE = "true";
    protected boolean         checkInFaultLogged;
    protected boolean         checkOutFaultLogged;
    protected Boolean         monitoredItemUpdated;
    protected boolean         remotelyAvailable;
    protected boolean		  useQuotes;
    protected Signal          ioSignal;
	protected Value           monitoredItemValue;
	protected Value           remoteItemValue;
	protected String          topic;
	protected String          topicId;

	protected org.jpac.vioss.iedb.json.Value jsonValue;
	protected ArrayList<Object> values;

    public RemoteSignalInfo(Signal ioSignal){
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
    	this.ioSignal         = ioSignal;
        this.useQuotes        = ((IoSignal)ioSignal).getParameters().containsKey(PARAMETER_USEQUOTES) && ((IoSignal)ioSignal).getParameters().get(PARAMETER_USEQUOTES).equals(PARAMETERVALUE_TRUE);//use quotes for accessing S7 plc's
        String path           = ((IoSignal)ioSignal).getUri().getPath().trim().substring(1);//strip leading slash
    
        if (path == null){
            throw new InconsistencyException(("missing topic '" + ((IoSignal)ioSignal).getUri() + "'"));            
        }
		//take the remainder after DATABLOCKTAG as topic
		if (path.contains(DATABLOCKTAG)){
			this.topic = path.substring(path.indexOf(DATABLOCKTAG) + DATABLOCKTAG.length());
		}
		else {
            throw new InconsistencyException(("missing data block tag '" + DATABLOCKTAG + "' in topic '" + ((IoSignal)ioSignal).getUri() + "'"));            
		}
        this.topic                     = useQuotes ? encloseWithQuotes(this.topic) : this.topic;
        this.checkInFaultLogged        = false;
        this.checkOutFaultLogged       = false;
        this.monitoredItemUpdated      = false;
        this.remotelyAvailable         = false;
		if (getType().newValue() != null){
			this.value                     = getType().newValue();
			this.monitoredItemValue        = getType().newValue();
			this.remoteItemValue           = getType().newValue();
		} else if (ioSignal instanceof IoByteArray){
			this.value                     = new ByteArrayValue();
			this.monitoredItemValue        = new ByteArrayValue();	
			this.remoteItemValue           = new ByteArrayValue();
		} else if (ioSignal instanceof IoShortArray){
			this.value                     = new ShortArrayValue();
			this.monitoredItemValue        = new ShortArrayValue();	
			this.remoteItemValue           = new ShortArrayValue();
		} else if (ioSignal instanceof IoIntArray){
			this.value                     = new IntArrayValue();
			this.monitoredItemValue        = new IntArrayValue();	
			this.remoteItemValue           = new IntArrayValue();
		} else if (ioSignal instanceof IoFloatArray){
			this.value                     = new FloatArrayValue();
			this.monitoredItemValue        = new FloatArrayValue();	
			this.remoteItemValue           = new FloatArrayValue();
		} else if (ioSignal instanceof IoDoubleArray){
			this.value                     = new DoubleArrayValue();
			this.monitoredItemValue        = new DoubleArrayValue();	
			this.remoteItemValue           = new DoubleArrayValue();
		}
		this.jsonValue                     = new org.jpac.vioss.iedb.json.Value();
		this.values                        = new ArrayList<>();
    }  
    
    protected String encloseWithQuotes(String identifier) {	
    	return "\"" + identifier.replace (".", "\".\"") + "\"";
    }

	public boolean isCheckInFaultLogged() {
		return checkInFaultLogged;
	}

	public void setCheckInFaultLogged(boolean checkInFaultLogged) {
		this.checkInFaultLogged = checkInFaultLogged;
	}

	public boolean isCheckOutFaultLogged() {
		return checkOutFaultLogged;
	}

	public void setCheckOutFaultLogged(boolean checkOutFaultLogged) {
		this.checkOutFaultLogged = checkOutFaultLogged;
	}

	public boolean isRemotelyAvailable() {
		return remotelyAvailable;
	}

	public void setRemotelyAvailable(boolean remotelyAvailable) {
		this.remotelyAvailable = remotelyAvailable;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}

	public boolean isMonitoredValueUpdated() {
		return monitoredItemUpdated;
	}

	public void setMonitoredValueUpdated(boolean monitoredValueUpdated) {
		this.monitoredItemUpdated = monitoredValueUpdated;
	}

	public Value getMonitoredItemValue() {
		return this.monitoredItemValue;
	}

	public Value getRemoteItemValue(){
		return this.remoteItemValue;
	}

	public String getTopic(){
		return this.topic;
	}

	public String getTopicId(){
		return this.topicId;
	}

	public void setTopicId(String topicId){
		this.topicId = topicId;
	}

	public org.jpac.vioss.iedb.json.Value computeJsonValue(){
		this.jsonValue.setId(topicId);
		this.jsonValue.setQualityCode(this.ioSignal.isValid() ? 3 : 0);//check, if siemens industrial edge OPC connector accepts quality code
		this.jsonValue.setTimestamp(java.time.Instant.now().toString());
		this.jsonValue.setValue(computeValue());
		return this.jsonValue;
	}

	public ArrayList<Object> computeValue(){
		synchronized(this){
			values.clear();
			switch(getType()){
				case Logical:
					values.add(((LogicalValue)remoteItemValue).get() ? 1 : 0);
					break;
				case SignedInteger:
					values.add(((SignedIntegerValue)remoteItemValue).get());
					break;
				case Decimal:
					values.add(((DecimalValue)remoteItemValue).get());
					break;
				case CharString:
					values.add(((CharStringValue)remoteItemValue).get());
					break;
				case Unknown:
					if (ioSignal instanceof IoByteArray) {
						values.add(((ByteArrayValue)remoteItemValue).get());
					} else if (ioSignal instanceof IoShortArray) {
						values.add(((ShortArrayValue)remoteItemValue).get());
					} else if (ioSignal instanceof IoIntArray) {
						values.add(((IntArrayValue)remoteItemValue).get());
					} else if (ioSignal instanceof IoFloatArray) {
						values.add(((FloatArrayValue)remoteItemValue).get());
					} else if (ioSignal instanceof IoDoubleArray) {
						values.add(((DoubleArrayValue)remoteItemValue).get());
					}
					break;
				default:
			}
		}
		return values;
	}

	protected String valueToString(){
		String ret = "";
		switch(getType()){
			case Logical:
				ret = ((LogicalValue)getRemoteItemValue()).toString();
				break;
			case SignedInteger:
				ret = ((SignedIntegerValue)getRemoteItemValue()).toString();
				break;
			case Decimal:
				ret = ((DecimalValue)getRemoteItemValue()).toString();
				break;
			case CharString:
				ret = ((CharStringValue)getRemoteItemValue()).toString();
				break;
			case Unknown:
					if (ioSignal instanceof IoByteArray) {
						ret = ((ByteArrayValue)getRemoteItemValue()).toString();
					} else if (ioSignal instanceof IoShortArray) {
						ret = ((ShortArrayValue)getRemoteItemValue()).toString();
					} else if (ioSignal instanceof IoIntArray) {
						ret = ((IntArrayValue)getRemoteItemValue()).toString();
					} else if (ioSignal instanceof IoFloatArray) {
						ret = ((FloatArrayValue)getRemoteItemValue()).toString();
					} else if (ioSignal instanceof IoDoubleArray) {
						ret = ((DoubleArrayValue)getRemoteItemValue()).toString();
					}
					break;
			default:
		}
		return ret;
	}

	public byte[] genPayload(){
		if (ioSignal instanceof IoByteArray) {
			return ((ByteArrayValue)getRemoteItemValue()).get();
		} else if (ioSignal instanceof IoShortArray) {
			// Convert short array to byte array
			short[] shorts = ((ShortArrayValue)getRemoteItemValue()).get();
			byte[] bytes = new byte[shorts.length * 2];
			for (int i = 0; i < shorts.length; i++) {
				bytes[i * 2] = (byte)(shorts[i] >> 8);
				bytes[i * 2 + 1] = (byte)(shorts[i] & 0xFF);
			}
			return bytes;
		} else if (ioSignal instanceof IoIntArray) {
			// Convert int array to byte array
			int[] ints = ((IntArrayValue)getRemoteItemValue()).get();
			byte[] bytes = new byte[ints.length * 4];
			for (int i = 0; i < ints.length; i++) {
				bytes[i * 4] = (byte)(ints[i] >> 24);
				bytes[i * 4 + 1] = (byte)(ints[i] >> 16);
				bytes[i * 4 + 2] = (byte)(ints[i] >> 8);
				bytes[i * 4 + 3] = (byte)(ints[i] & 0xFF);
			}
			return bytes;
		} else if (ioSignal instanceof IoFloatArray) {
			// Convert float array to byte array (4 bytes per float)
			float[] floats = ((FloatArrayValue)getRemoteItemValue()).get();
			byte[] bytes = new byte[floats.length * 4];
			for (int i = 0; i < floats.length; i++) {
				int bits = Float.floatToIntBits(floats[i]);
				bytes[i * 4] = (byte)(bits >> 24);
				bytes[i * 4 + 1] = (byte)(bits >> 16);
				bytes[i * 4 + 2] = (byte)(bits >> 8);
				bytes[i * 4 + 3] = (byte)(bits & 0xFF);
			}
			return bytes;
		} else if (ioSignal instanceof IoDoubleArray) {
			// Convert double array to byte array (8 bytes per double)
			double[] doubles = ((DoubleArrayValue)getRemoteItemValue()).get();
			byte[] bytes = new byte[doubles.length * 8];
			for (int i = 0; i < doubles.length; i++) {
				long bits = Double.doubleToLongBits(doubles[i]);
				bytes[i * 8] = (byte)(bits >> 56);
				bytes[i * 8 + 1] = (byte)(bits >> 48);
				bytes[i * 8 + 2] = (byte)(bits >> 40);
				bytes[i * 8 + 3] = (byte)(bits >> 32);
				bytes[i * 8 + 4] = (byte)(bits >> 24);
				bytes[i * 8 + 5] = (byte)(bits >> 16);
				bytes[i * 8 + 6] = (byte)(bits >> 8);
				bytes[i * 8 + 7] = (byte)(bits & 0xFF);
			}
			return bytes;
		}
		else{
			return valueToString().getBytes();
		}
    }
	
	@Override
    public String toString(){
        return super.toString();
    }    
}
