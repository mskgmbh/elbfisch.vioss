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

package org.jpac.vioss.raspi.gpio;

import java.net.URI;
import org.jpac.Address;
import org.jpac.AsynchronousTask;
import org.jpac.ProcessException;
import org.jpac.WrongUseException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import static org.jpac.vioss.IOHandler.Log;
import org.jpac.vioss.IllegalUriException;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{

    public enum RunnerState {CONNECTING, TRANSMITTING, ERROR};

    final static int    MAXIMUMDATARANGE    = 3;
    final static int    CONNECTIONRETRYTIME = 5000;
    final static String HANDLEDSCHEME       = "RASPIGPIO";
    final static int    DEFAULTSERVERPORT   = 1234;
            
    private boolean inputOutputProcessImageCompleted;
    
    private InputOutputProcessImageRunner inputOutputProcessImageRunner;
        
    private String      toString;
        
    public IOHandler(URI uri) throws IllegalUriException{
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.inputOutputProcessImageCompleted  = false;
    }
    
    @Override
    public boolean handles(Address address, URI uri) {
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
        setProcessingStarted(true);
        Log.info("starting up " + this);
        try{
            inputOutputProcessImageRunner = new InputOutputProcessImageRunner(this.toString());
            inputOutputProcessImageRunner.start();
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }//fetch the input process image for the next cycle
    }

    @Override
    public void stop(){
        Log.info("shutting down ..." + this);
        try{inputOutputProcessImageRunner.terminate();}catch(WrongUseException exc){/*cannot happen*/};
    }
        
    @Override
    public void run(){
        try{
            if (!isProcessingAborted()){
                //invoke data interchange
                putSignalsToOutputProcessImage();
                inputOutputProcessImageCompleted  = inputOutputProcessImage();
                seizeSignalsFromInputProcessImage();
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
            inputOutputProcessImageCompleted = false;
        }
    }

    @Override
    public boolean isFinished() {
        return inputOutputProcessImageRunner.isFinished();
    }
    
    protected boolean inputOutputProcessImage() {
        try{
            //check, if the inputProcessImageRunner has fetched/put an actual process image, yet
            inputOutputProcessImageCompleted = inputOutputProcessImageRunner.isInputOutputCompleted();
            //and restart the runner for the next fetch/put.
            //Hint: It might have finished without valid data, or is still running (to establish new connection, for instance).
            if (inputOutputProcessImageRunner.isFinished()){
                //Immediately start it, if it came to an end
                inputOutputProcessImageRunner.start();
            }
        }
        catch(Error exc){
            Log.error("Error: ", exc);
        }
        catch(Exception exc){
            Log.error("Error: ", exc);            
        }
        return inputOutputProcessImageCompleted;
    }

    @Override
    public String getHandledScheme() {
        return this.HANDLEDSCHEME;
    }
    
    public Boolean getBOOL(org.jpac.Address address) throws AddressException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    public void setBOOL(org.jpac.Address address, boolean value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        private Data               outputData;
        private boolean            inputOutputCompleted;
        
        public  RunnerState        state;        
        
        public InputOutputProcessImageRunner(String instanceIdentifier){
            this.instanceIdentifier = instanceIdentifier;
            this.inputData          = new Data(new byte[MAXIMUMDATARANGE]);
            this.outputData         = new Data(new byte[MAXIMUMDATARANGE]);
            this.state              = RunnerState.CONNECTING;
        }
        
        @Override
        public void doIt() throws ProcessException {            
            try{
                inputOutputCompleted = false;
                for (org.jpac.plc.IoSignal os: getOutputSignals()){
                    os.checkOut();
                }
                for (org.jpac.plc.IoSignal is: getInputSignals()){
                    is.checkIn();                                    
                }
                inputOutputCompleted = true;
            }
            catch(Error exc){
                for (org.jpac.plc.IoSignal is: getInputSignals()){
                    is.invalidate();                                    
                }
                throw new ProcessException(exc);                
            }
            catch(Exception exc){
                for (org.jpac.plc.IoSignal is: getInputSignals()){
                    is.invalidate();                                    
                }
                throw new ProcessException(exc);
            }
        }
        
        public RunnerState getState(){
            return this.state;
        }
        
        public boolean isInputOutputCompleted(){
            return this.inputOutputCompleted;
        }

        /**
         * @return the inputData
         */
        public Data getInputData() {
            return inputData;
        }

        /**
         * @return the outputData
         */
        public Data getOutputData() {
            return outputData;
        }
    }

}
