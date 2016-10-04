/**
 * PROJECT   : jPac PLC communication library
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

package org.jpac.vioss.modbus;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import org.jpac.IndexOutOfRangeException;
import org.jpac.AsynchronousTask;
import org.jpac.Module;
import org.jpac.ProcessException;
import org.jpac.Timer;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import org.jpac.vioss.IllegalUriException;
import org.jpac.Address;
import org.jpac.vioss.IoSignal;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    final static String HANDLEDSCHEME = "MODBUS";

    public enum RunnerState {CONNECTING, TRANSMITTING, ERROR};

    protected final static int    MAXIMUMDATARANGE    = 256;
    protected final static int    CONNECTIONRETRYTIME = 5000; //ms
    protected final static int    TRANSMISSIONTIMEOUT = 1000; //ms
    
    
    protected DataBlock modbusDataBlock;
    
    protected int   minInputByteIndex;
    protected int   maxInputByteIndex;
    protected int   minOutputByteIndex;
    protected int   maxOutputByteIndex;   
    
    private Data    inputData;
    private Data    outputData;
    
    private Data    lastValidInputData;    
    private Data    pendingOutputData;
    
    private Integer assignedChunkOfData;
        
    private boolean inputDataValid;
    
    private InputOutputProcessImageRunner inputOutputProcessImageRunner;
    
    private Timer   inputOutputWatchdog;
        
    private String  toString;
        
    public IOHandler(URI uri) throws IllegalUriException{
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.maxInputByteIndex  = 0;
        this.maxOutputByteIndex = 0;
        this.minInputByteIndex  = Integer.MAX_VALUE;
        this.minOutputByteIndex = Integer.MAX_VALUE;
        
        this.inputOutputWatchdog   = new Timer();
        
        analyzePath();
    }

    private void analyzePath() throws IllegalUriException{
        try{
            modbusDataBlock = seizeModbusDataBlock(getUri());
        }
        catch(InvalidDataBlockException exc){
            throw new IllegalUriException("invalid datablock specified: " + getUri());
        }
    }
    
    protected DataBlock seizeModbusDataBlock(URI uri) throws IllegalUriException, InvalidDataBlockException{
        DataBlock         dataBlock = null;
        String            token;
        StringTokenizer   tokenizer = new StringTokenizer(uri.getPath(),"/");
        token     = tokenizer.nextToken();
        if (token == null){
            throw new IllegalUriException("missing datablock specification in " + uri);
        }
        dataBlock = new DataBlock(token.toUpperCase());
        return dataBlock;
    }
        
    @Override
    public boolean handles(Address address, URI uri) {
        boolean isHandledByThisInstance = false;
        try{
            isHandledByThisInstance  = uri != null;
            isHandledByThisInstance &= this.getUri().getScheme().equals(uri.getScheme());
            InetAddress[] ia =  InetAddress.getAllByName(this.getUri().getHost());
            InetAddress[] ib =  InetAddress.getAllByName(uri.getHost());
            isHandledByThisInstance &= ia[0].equals(ib[0]);
            isHandledByThisInstance &= this.getUri().getPort() == uri.getPort();
            isHandledByThisInstance &= this.modbusDataBlock.getValue() == ((org.jpac.vioss.modbus.Address)address).getDataBlock().getValue();
            if (assignedChunkOfData == null){
                //the first registered signal determines the chunk of data handled by this IOHandler
                assignedChunkOfData = ((org.jpac.vioss.modbus.Address)address).getChunkOfData();
            }
            else{
                //every subsequently registered signal must fit into the chunk of data handled by this IOHandler
                isHandledByThisInstance &= ((org.jpac.vioss.modbus.Address)address).getChunkOfData() == assignedChunkOfData;
            }
        }
        catch(UnknownHostException exc){};
        return isHandledByThisInstance;
    }
    
    @Override
    public void prepare(){
        setProcessingStarted(true);
        Log.info("starting up " + this);
        try{
            inputOutputProcessImageRunner = new InputOutputProcessImageRunner(this.toString(), getInputData(), getOutputData());
            inputOutputProcessImageRunner.start();
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }
    }

    @Override
    public void stop(){
        Log.info("shutting down " + this);
    }
    
    @Override
    public void registerInputSignal(IoSignal signal) throws WrongUseException {
       super.registerInputSignal(signal);
       int firstByte = signal.getAddress().getByteIndex();
       int lastByte  = firstByte + signal.getAddress().getSize() - 1;
       //adjust the range of data to fetch from remote modbus device.
       if (firstByte < minInputByteIndex){
           minInputByteIndex = firstByte;
       }
       if (lastByte > maxInputByteIndex){
           maxInputByteIndex = lastByte;
       }
    }

    @Override
    public void registerOutputSignal(IoSignal signal) throws WrongUseException {
       super.registerOutputSignal(signal);
       int firstByte = signal.getAddress().getByteIndex();
       int lastByte  = firstByte + signal.getAddress().getSize() - 1;
       //adjust the range of data to transfer to the remote modbus device.
       if (firstByte < minOutputByteIndex){
           minOutputByteIndex = firstByte;
       }
       if (lastByte > maxOutputByteIndex){
           maxOutputByteIndex = lastByte;
       }
    }

    protected Data getInputData() throws IndexOutOfRangeException{
        if (inputData == null && !getInputSignals().isEmpty()){
            if (maxInputByteIndex - minInputByteIndex <= MAXIMUMDATARANGE){
                inputData = new Data(new byte[maxInputByteIndex - minInputByteIndex + 1]);
            }
            else{
                throw new org.jpac.IndexOutOfRangeException("modbus address range exceeded: " + (maxInputByteIndex - minInputByteIndex));
            }
        }
        return inputData;
    }
    
    protected Data getOutputData() throws IndexOutOfRangeException{
        if (outputData == null && !getOutputSignals().isEmpty()){
            if (maxOutputByteIndex - minOutputByteIndex <= MAXIMUMDATARANGE){
                outputData = new Data(new byte[maxOutputByteIndex - minOutputByteIndex + 1]);
            }
            else{
                throw new org.jpac.IndexOutOfRangeException("modbus address range exceeded: " + (maxOutputByteIndex - minOutputByteIndex));
            }
        }
        return outputData;
    }
    
    protected Data getLastValidInputData() throws IndexOutOfRangeException{
        if (lastValidInputData == null){
            if (maxInputByteIndex - minInputByteIndex <= MAXIMUMDATARANGE){
                lastValidInputData = new Data(new byte[maxInputByteIndex - minInputByteIndex + 1]);
            }
            else{
                throw new org.jpac.IndexOutOfRangeException("modbus address range exceeded: " + (maxInputByteIndex - minInputByteIndex));
            }
        }
        return lastValidInputData;
    }

    protected Data getPendingOutputData() throws IndexOutOfRangeException{
        if (pendingOutputData == null){
            if (maxInputByteIndex - minInputByteIndex <= MAXIMUMDATARANGE){
                lastValidInputData = new Data(new byte[maxInputByteIndex - minInputByteIndex + 1]);
            }
            else{
                throw new org.jpac.IndexOutOfRangeException("modbus address range exceeded: " + (maxInputByteIndex - minInputByteIndex));
            }
        }
        return lastValidInputData;
    }

    @Override
    public void run(){
        if (!isProcessingAborted()){
            //invoke data interchange
            inputOutputProcessImage();
        }
    }
    
    protected boolean inputOutputProcessImage() {
        boolean inputOutputFinished = false;
        try{
            inputOutputFinished = inputOutputProcessImageRunner.isFinished();
            if (inputOutputFinished){
                //check, if the inputProcessImageRunner has fetched an actual process image, yet
                inputDataValid = inputOutputProcessImageRunner.isInputDataValid();//only valid in finished state of the runner !
                if (inputDataValid){
                    //if so, make a copy of the data for further processing
                    getLastValidInputData().copy(getInputData());
                    //and transfer it to the connected input signals
                    seizeSignalsFromInputProcessImage();
                }
                //transfer actual states of output signals to the output image
                putSignalsToOutputProcessImage();
                //and prepare it for transmission to the plc
                if (getOutputData() != null){
                    getOutputData().copy(getPendingOutputData());
                }
                //Finally restart the runner to let it transceive the process images (in/out)
                //HINT: First transmission after connection is not supervised by this implementation
                inputOutputProcessImageRunner.start();
                if (inputOutputProcessImageRunner.state == RunnerState.TRANSMITTING){
                    //retrigger watchdog for process image transmission 
                    inputOutputWatchdog.start(TRANSMISSIONTIMEOUT * Module.ms);
                }
            }
            else{
                if (inputOutputProcessImageRunner.state == RunnerState.TRANSMITTING && !inputOutputWatchdog.isRunning()){
                    Log.error("process image transmission timed out. Reestablishing connection ...");
                    inputOutputProcessImageRunner = new InputOutputProcessImageRunner(this.toString(), getInputData(), getOutputData());
                    inputOutputProcessImageRunner.start();
                }
            }
        }
        catch(Error exc){
            Log.error("Error: ", exc);
        }
        catch(Exception exc){
            Log.error("Error: ", exc);            
        }
        return inputOutputFinished;
    }
    
    protected Request getReadRequest() throws IndexOutOfRangeException, AddressException{
        Request request = null;
        if (modbusDataBlock.isBitAccess()){
            request = new ReadBits(getReadAddress());
        }else if (modbusDataBlock.isRegisterAccess()){
            request = new ReadRegisters(getReadAddress());            
        }
        return request;
    }

    protected Request getWriteRequest() throws IndexOutOfRangeException, AddressException{
        Request request = null;
        if (modbusDataBlock.isBitAccess()){
            request = new WriteBits(getWriteAddress(), outputData);
        }else if (modbusDataBlock.isRegisterAccess()){
            request = new WriteRegisters(getWriteAddress(), outputData);
        }
        return request;
    }

    protected org.jpac.vioss.modbus.Address getReadAddress() throws IndexOutOfRangeException, AddressException{
        return new org.jpac.vioss.modbus.Address(modbusDataBlock, minInputByteIndex, maxInputByteIndex - minInputByteIndex + 1);
    }

    protected org.jpac.vioss.modbus.Address getWriteAddress() throws IndexOutOfRangeException, AddressException{
        return new org.jpac.vioss.modbus.Address(modbusDataBlock, minOutputByteIndex, maxOutputByteIndex - minOutputByteIndex + 1);
    }
    
    @Override
    public boolean isFinished() {
        return inputOutputProcessImageRunner.isFinished();
    }

    @Override
    public String getHandledScheme() {
        return this.HANDLEDSCHEME;
    }
    
    public DataBlock getHandledDataBlock() {
        return this.modbusDataBlock;
    }

    @Override
    public String toString(){
        if (toString == null){
            toString = getClass().getCanonicalName() + "(" +getUri().getScheme() + "://" + getUri().getHost() + ":" + getUri().getPort() + "/" + modbusDataBlock + ")";
        }
        return toString;
    }

    public Boolean getBOOL(org.jpac.Address address) throws AddressException{
        Boolean  value = null;//return null, if the input process image is invalid
        if (inputDataValid){
            try{value = getLastValidInputData().getBIT(address.getByteIndex() - minInputByteIndex, address.getBitIndex());}catch(IndexOutOfRangeException exc){/*cannot happen*/};
        }
        return value;
    }

    public Integer getINT(org.jpac.Address address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Integer getDINT(org.jpac.Address address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Double getFLOAT(org.jpac.Address address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Boolean getSTRING(org.jpac.Address address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setBOOL(org.jpac.Address address, boolean value) throws AddressException {
        try{getPendingOutputData().setBIT(address.getByteIndex() - minOutputByteIndex, address.getBitIndex(), value);}catch(IndexOutOfRangeException exc){/*cannot happen*/};
    }

    public void setINT(org.jpac.Address address, int value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setDINT(org.jpac.Address address, int value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setFLOAT(org.jpac.Address address, double value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setSTRING(org.jpac.Address address, String value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    class InputOutputProcessImageRunner extends AsynchronousTask{
        private boolean            connected;
        private String             instanceIdentifier;

        private Data               inputData;
        private boolean            inputDataValid;

        private Data               outputData;
        private boolean            outputDataValid;

        private Request            readRequest;
        private Address            readAddress;
        private Request            writeRequest;
        private Address            writeAddress;
        
        private Connection         connection;
        public  RunnerState        state;
        
        public  int                count;
        
        
        public InputOutputProcessImageRunner(String instanceIdentifier, Data inputData, Data outputData){
            super(instanceIdentifier);
            this.instanceIdentifier = instanceIdentifier;
            this.inputData          = inputData;
            this.outputData         = outputData;
            this.state              = RunnerState.CONNECTING;
            try{
                if (!getInputSignals().isEmpty()){
                    this.readAddress = getReadAddress();            
                    this.readRequest = getReadRequest();
                }
                if (!getOutputSignals().isEmpty()){
                    this.writeAddress = getWriteAddress();
                    this.writeRequest = getWriteRequest();
                }
            }
            catch(Exception exc){
                Log.error("Error:", exc);
            }
        }
        
        @Override
        public void doIt() throws ProcessException {            
            try{
                outputDataValid = false;//only valid during finished state of the runner
                inputDataValid  = false;//only valid during finished state of the runner
                do{ 
                    if (!connected){
                        state = RunnerState.CONNECTING;
                    }
                    switch(state){
                        case CONNECTING:
                            Log.info("establishing connection ...");
                            do{
                                try{
                                    connection = new Connection(getUri().getHost(), getUri().getPort());
                                    connected  = true;
                                }
                                catch(Exception exc){
                                    Thread.sleep(CONNECTIONRETRYTIME);
                                }
                            }
                            while(!connected);
                            Log.info("... connection established");
                            state = RunnerState.TRANSMITTING;
                            break;
                        case TRANSMITTING:
                            if (writeRequest != null){
                                writeRequest.write(connection);
                                writeRequest.read(connection);
                                outputDataValid = true;
                            }
                            if (readRequest != null){
                                readRequest.write(connection);
                                readRequest.read(connection);
                                inputDataValid = true;
                            }
                            break;
                    }
                }
                while((readRequest != null && !inputDataValid) || (writeRequest != null && !outputDataValid));
            }
            catch(Error exc){
                Log.error("Error", exc);
                connected = false;
                outputDataValid = false;
                inputDataValid  = false;
            }
            catch(Exception exc){
                Log.error("Error", exc);
                connected = false;
                outputDataValid = false;
                inputDataValid  = false;
            }
        }
        
        public RunnerState getState(){
            return this.state;
        }
        
        public boolean isInputDataValid(){
            return this.inputDataValid;
        }
    }

}
