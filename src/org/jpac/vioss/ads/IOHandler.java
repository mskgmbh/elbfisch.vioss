/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : IOHandler.java (versatile input output subsystem)
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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.jpac.Address;
import org.jpac.AsynchronousTask;
import org.jpac.InconsistencyException;
import org.jpac.Module;
import org.jpac.ProcessException;
import org.jpac.Timer;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{
    private final static String HANDLEDSCHEME = "ADS";
    
    public enum State {UNDEFINED, CONNECTING, GETTINGHANDLES, TRANSCEIVING, RELEASINGHANDLES, STOPPED};
    
    private final static int    TRANSMISSIONTIMEOUT = 1000;//ms
    private final static int    CONNECTIONRETRYTIME = 1000;//ms 
         
    private Integer assignedChunkOfData;
        
    private boolean inputDataValid;
    
    private InputOutputProcessImageRunner inputOutputProcessImageRunner;
    
    private Timer   inputOutputWatchdog;
        
    private String  toString;
    

    public IOHandler(URI uri) throws IllegalUriException {
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }       
        this.inputOutputWatchdog   = new Timer();
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
                    // TODO replace : getLastValidInputData().copy(getInputData());
                    //and transfer it to the connected input signals
                    seizeSignalsFromInputProcessImage();
                }
                //transfer actual states of output signals to the output image
                putSignalsToOutputProcessImage();
                //and prepare it for transmission to the plc
                //TODO replace : if (getOutputData() != null){
                //                    getOutputData().copy(getPendingOutputData());
                //                 }
                //Finally restart the runner to let it transceive the process images (in/out)
                //HINT: First transmission after connection is not supervised by this implementation
                inputOutputProcessImageRunner.start();
                if (inputOutputProcessImageRunner.state == State.TRANSCEIVING){
                    //retrigger watchdog for process image transmission 
                    inputOutputWatchdog.start(TRANSMISSIONTIMEOUT * Module.ms);
                }
            }
            else{
                if (inputOutputProcessImageRunner.state == State.TRANSCEIVING && !inputOutputWatchdog.isRunning()){
                    Log.error("process image transmission timed out. Reestablishing connection ...");
                    inputOutputProcessImageRunner = new InputOutputProcessImageRunner(this.toString());
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
        }
        catch(UnknownHostException exc){};
        return isHandledByThisInstance;
    }

    @Override
    public void prepare() {
        setProcessingStarted(true);
        Log.info("starting up " + this);
        try{
            inputOutputProcessImageRunner = new InputOutputProcessImageRunner(this.toString());
            inputOutputProcessImageRunner.start();
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }
    }

    @Override
    public void stop() {
        Log.info("shutting down " + this);
    }
 
    @Override
    public String getHandledScheme() {
        return HANDLEDSCHEME;
    }

    @Override
    public boolean isFinished() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    class InputOutputProcessImageRunner extends AsynchronousTask{
        private boolean            connected;

        private boolean            inputDataValid;

        private boolean            outputDataValid;
        
        private Connection         connection;
        public  State        state;
        
        public  int                count;
        
        
        public InputOutputProcessImageRunner(String instanceIdentifier){
            super(instanceIdentifier);
            this.state = State.CONNECTING;
        }
        
        @Override
        public void doIt() throws ProcessException {            
            try{
                outputDataValid = false;//only valid during finished state of the runner
                inputDataValid  = false;//only valid during finished state of the runner
                do{ 
                    if (!connected){
                        state = State.CONNECTING;
                    }
                    switch(state){
                        case CONNECTING:
                            Log.info("establishing connection ...");
                            do{
                                try{
                                    connection = new Connection(getUri().getHost());
                                    connected  = true;
                                }
                                catch(Exception exc){
                                    Thread.sleep(CONNECTIONRETRYTIME);
                                }
                            }
                            while(!connected);
                            Log.info("... connection established");
                            state = State.GETTINGHANDLES;
                            break;
                        case GETTINGHANDLES:
                            Log.info("getting handles ...");
                            //must be implemented
                            state = State.TRANSCEIVING;
                            Log.info("... handles received");
                            break;
                        case TRANSCEIVING:
                            //do transceiving here
                            outputDataValid = true;
                            inputDataValid = true;
                            break;
                        case RELEASINGHANDLES:
                            Log.info("disconnecting ...");
                            //must be implemented
                            //deregistering handles
                            state = State.STOPPED;
                            Log.info("... disconnected");
                            break;
                        case STOPPED:
                            throw new InconsistencyException(this + " might not be called in STOPPED state");
                    }
                }
                while(!inputDataValid || !outputDataValid);
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
        
        public State getState(){
            return this.state;
        }
        
        public boolean isInputDataValid(){
            return this.inputDataValid;
        }
    }
}
