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

package org.jpac.vioss.mqttv3;

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
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author berndschuster
 */
public class RemoteSignalInfo extends org.jpac.vioss.RemoteSignalInfo implements IMqttMessageListener{
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.mqttv3");
    
	protected static String   PARAMETER_USEQUOTES ="useQuotes";
	protected static String   PARAMETERVALUE_TRUE ="true";
    protected boolean         checkInFaultLogged;
    protected boolean         checkOutFaultLogged;
    protected Boolean         monitoredItemUpdated;
    protected boolean         remotelyAvailable;
    protected boolean		  useQuotes;
    protected Signal          ioSignal;
	protected Value           monitoredItemValue;
	protected Value           remoteItemValue;
	protected String          topic;

    public RemoteSignalInfo(Signal ioSignal){
    	super(ioSignal.getIdentifier(), BasicSignalType.fromSignal(ioSignal));
    	this.ioSignal         = ioSignal;
        this.useQuotes        = ((IoSignal)ioSignal).getParameters().containsKey(PARAMETER_USEQUOTES) && ((IoSignal)ioSignal).getParameters().get(PARAMETER_USEQUOTES).equals(PARAMETERVALUE_TRUE);//use quotes for accessing S7 plc's
        String path           = ((IoSignal)ioSignal).getUri().getPath().trim().substring(1);//strip leading slash
        
        if (path == null){
            throw new InconsistencyException(("missing topic '" + ((IoSignal)ioSignal).getUri() + "'"));            
        }
        this.topic                = useQuotes ? encloseWithQuotes(path) : path;
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

	@Override
	public void messageArrived(java.lang.String topic, MqttMessage message) throws java.lang.Exception{
		String msg = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
		Log.debug("message arrived " + msg);
		synchronized(this){
			switch(getType()){
				case Logical:
					((LogicalValue)monitoredItemValue).set(Boolean.parseBoolean(msg));
					monitoredItemUpdated = true;
					break;
				case SignedInteger:
					((SignedIntegerValue)monitoredItemValue).set(Integer.parseInt(msg));
					monitoredItemUpdated = true;
					break;
				case Decimal:
					((DecimalValue)monitoredItemValue).set(Double.parseDouble(msg));
					monitoredItemUpdated = true;
					break;
				case CharString:
					((CharStringValue)monitoredItemValue).set(msg);
					monitoredItemUpdated = true;
					break;
				case Unknown:
					if (ioSignal instanceof IoByteArray) {
						((ByteArrayValue)monitoredItemValue).set(message.getPayload());
						monitoredItemUpdated = true;
					} else if (ioSignal instanceof IoShortArray) {
						// Convert byte payload to short array
						byte[] payload = message.getPayload();
						short[] shorts = new short[payload.length / 2];
						for (int i = 0; i < shorts.length; i++) {
							shorts[i] = (short)(((payload[i * 2] & 0xFF) << 8) | (payload[i * 2 + 1] & 0xFF));
						}
						((ShortArrayValue)monitoredItemValue).set(shorts);
						monitoredItemUpdated = true;
					} else if (ioSignal instanceof IoIntArray) {
						// Convert byte payload to int array
						byte[] payload = message.getPayload();
						int[] ints = new int[payload.length / 4];
						for (int i = 0; i < ints.length; i++) {
							ints[i] = ((payload[i * 4] & 0xFF) << 24) | ((payload[i * 4 + 1] & 0xFF) << 16) | ((payload[i * 4 + 2] & 0xFF) << 8) | (payload[i * 4 + 3] & 0xFF);
						}
						((IntArrayValue)monitoredItemValue).set(ints);
						monitoredItemUpdated = true;
					} else if (ioSignal instanceof IoFloatArray) {
						// Convert byte payload to float array (4 bytes per float)
						byte[] payload = message.getPayload();
						float[] floats = new float[payload.length / 4];
						for (int i = 0; i < floats.length; i++) {
							int bits = ((payload[i * 4] & 0xFF) << 24) | ((payload[i * 4 + 1] & 0xFF) << 16) | ((payload[i * 4 + 2] & 0xFF) << 8) | (payload[i * 4 + 3] & 0xFF);
							floats[i] = Float.intBitsToFloat(bits);
						}
						((FloatArrayValue)monitoredItemValue).set(floats);
						monitoredItemUpdated = true;
					} else if (ioSignal instanceof IoDoubleArray) {
						// Convert byte payload to double array (8 bytes per double)
						byte[] payload = message.getPayload();
						double[] doubles = new double[payload.length / 8];
						for (int i = 0; i < doubles.length; i++) {
							long bits = ((long)(payload[i * 8] & 0xFF) << 56) | ((long)(payload[i * 8 + 1] & 0xFF) << 48) | 
							           ((long)(payload[i * 8 + 2] & 0xFF) << 40) | ((long)(payload[i * 8 + 3] & 0xFF) << 32) |
							           ((long)(payload[i * 8 + 4] & 0xFF) << 24) | ((long)(payload[i * 8 + 5] & 0xFF) << 16) |
							           ((long)(payload[i * 8 + 6] & 0xFF) << 8) | ((long)(payload[i * 8 + 7] & 0xFF));
							doubles[i] = Double.longBitsToDouble(bits);
						}
						((DoubleArrayValue)monitoredItemValue).set(doubles);
						monitoredItemUpdated = true;
					}
					break;
				default:
			}
		}
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
