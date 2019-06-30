/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Raspberry Pi
 * MODULE    : IOHandler.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : -
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : -
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

package org.jpac.vioss.raspi.i2c;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.jpac.IoDirection;
import org.jpac.ProcessException;
import org.jpac.Signal;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;
import org.jpac.vioss.IoSignedInteger;


/**
 *
 * @author berndschuster
 * IoHandler for access of the i2c bus of the raspberry pi 3
 * addressing scheme for read/write access to devices:
 * "pi.i2c:/<bus>/<device>/<datasize>/<byte address>[/<bit address>][?endianess=bigendian | littleendian]
 *  Path:
 *   bus         : on raspi always '1'
 *   device      : 7-bit address of the device (can be given decimal or hexadecimal notation)
 *   datasize    : number of bytes comprised by the data item to be accessed. Possible values 1,2,4. 'na' for bit access
 *   byte address: address of the register or of the low byte of a data item to be accessed
 *   bit address : optinal: number of a bit (0..7) to be accessed by an IoLogical
 *  
 *  Parameters:
 *   endianess   : endianess of a multi byte data item. Possible values 'bigendian' or 'littleendian
 *   
 *  Examples:
 *   IoSignedInteger(this, "AccCtrlReg1", new URI("pi.i2c:/1/6b/1/2a)                            , IoDirection.OUTPUT);
 *   IoSignedInteger(this, "AccX"       , new URI("pi.i2c:/1/107/2/42?endianness=littleendian")  , IoDirection.INPUT);
 *   IoLogical      (this, "ReadyFlag"  , new URI("pi.i2c:/1/19/2/32/3)                          , IoDirection.INPUT);
 *   
 */
public class IOHandler extends org.jpac.vioss.IOHandler{

    final static String HANDLEDSCHEME       = "PI.I2C";
    final static String RASPII2CDEVICE      = "/dev/i2c-1";
    
    protected  Map<Integer, Bus> busses; //bus->device->register
    
    protected  boolean runningOnRaspi;
    protected  boolean properlyPrepared;
                
    public IOHandler(URI uri, SubnodeConfiguration subnodeConfiguration) throws IllegalUriException {
        super(uri, subnodeConfiguration);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.busses           = new HashMap<>();
        this.runningOnRaspi   = new File(RASPII2CDEVICE).exists();
        this.properlyPrepared = false;
    }
    
    @Override
    public boolean handles(URI uri) {
        boolean isHandledByThisInstance = false;
        try{
            isHandledByThisInstance  = uri != null;
            isHandledByThisInstance &= this.getUri().getScheme().equals(uri.getScheme());
        }
        catch(Exception exc){};
        return isHandledByThisInstance;
    }
    
    @Override
    public void prepare(){
        for (Signal is: getInputSignals()){
        	IoSignal ioSig = (IoSignal)is;
        	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(is));
        }
        for (Signal os: getOutputSignals()){
        	IoSignal ioSig = (IoSignal)os;
        	ioSig.setRemoteSignalInfo(new RemoteSignalInfo(os));
        }
        //allocate data buffers for all devices handled by this IOHandler
        busses.values().forEach(b -> b.getDevices().values().forEach(d -> d.allocateDataBuffers()));
        //... and assign them to associated signals
        for (Signal is: getInputSignals()){
        	((RemoteSignalInfo)((IoSignal)is).getRemoteSignalInfo()).assignDevicesReadBuffer();
        }
        for (Signal os: getOutputSignals()){
        	((RemoteSignalInfo)((IoSignal)os).getRemoteSignalInfo()).assignDevicesWriteBuffer();
        }
        setProcessingStarted(true);
        Log.info("starting up " + this);
        if (!isRunningOnRaspi()) {
        	Log.info(this + " started in simulation mode (host system is not a Raspberry Pi). Bytes put out are reflected on corresponding input bytes");
        }
    }

    @Override
    public void stop(){
        Log.info("shutting down ..." + this);
        setProcessingAborted(true);
    }
        
    @Override
    public void run(){
        try{
            if (isProcessingStarted() && !isProcessingAborted()){
                inputOutputProcessImage();
            }
        }
        catch(Error exc){
            Log.error("Error: ", exc);
            Log.error("processing aborted for IOHandler " + this);
            //abort processing of this io handler
            setProcessingAborted(true);
        }
        catch(Exception exc){
            Log.error("Error: ", exc);
            //abort processing of this io handler
            setProcessingAborted(true);
        }
    }

    @Override
    public boolean isFinished() {
        return isProcessingAborted();
    }
    
    protected void inputOutputProcessImage() {
        try{
        	//check out changed output signals
            for (Signal os: getOutputSignals()){
            	IoSignal ioSig = (IoSignal)os;
                ioSig.checkOut();
            	((RemoteSignalInfo)ioSig.getRemoteSignalInfo()).pushValueToDevice();
            }
        	//transceive process image
        	for (Bus b: getBusses().values()) {
        		for (Device d : b.getDevices().values()) {
        			d.readWrite();
        		}
        	}
        	//check in changed input signals
            for (Signal is: getInputSignals()){
            	IoSignal ioSig = (IoSignal)is;
            	((RemoteSignalInfo)ioSig.getRemoteSignalInfo()).pullValueFromDevice();
                ioSig.checkIn();        
            }
            Log.info("in data " + ((RemoteSignalInfo)((IoSignal)getInputSignals().get(0)).getRemoteSignalInfo()).readData);//TODO
        } catch(Error exc){
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }
            throw new ProcessException(exc);                
        } catch(Exception exc){
            for (Signal is: getInputSignals()){
                is.invalidate();                                    
            }
            throw new ProcessException(exc);
        }
    }

    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }
    
    public Map<Integer, Bus> getBusses(){
    	return busses;
    }
    
    public boolean isRunningOnRaspi() {
    	return runningOnRaspi;
    }
}
