/**
 * PROJECT   : Elbfisch - versatile input output subsystem (vioss) for the Revolution Pi
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

package org.jpac.vioss.revpi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import org.jpac.Address;
import static org.jpac.vioss.IOHandler.Log;
import org.jpac.vioss.IllegalUriException;
import org.jpac.vioss.IoSignal;

/**
 *
 * @author berndschuster
 */
public class IOHandler extends org.jpac.vioss.IOHandler{

    public enum RunnerState {CONNECTING, TRANSMITTING, ERROR};

    final static String HANDLEDSCHEME       = "REVPI";
    final static String DEVPICONTROL        = "/dev/piControl0";   //TODO pi control driver TEST: change to /dev/piControl0
    
    protected RandomAccessFile piControl;
    protected ProcessImage     processImage;
    protected boolean          runningOnRevPi;
                
    public IOHandler(URI uri) throws IllegalUriException, IOException{
        super(uri);
        if (!getHandledScheme().equals(uri.getScheme().toUpperCase())){
            throw new IllegalUriException("scheme '" + uri.getScheme() + "' not handled by " + toString());
        }
        this.runningOnRevPi = new File(DEVPICONTROL).exists();
        this.processImage = new ProcessImage(DEVPICONTROL, this.runningOnRevPi);
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
        Log.info("starting up " + this + (this.runningOnRevPi ? "" : " (simulated)"));
        try{
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }
    }

    @Override
    public void stop(){
        Log.info("shutting down " + this + " ...");
        //TODO close file for RevPi's process image
        if (piControl != null){
            try{piControl.close();}catch(IOException exc){/*ignore*/};
        }
    }
        
    @Override
    public void run(){
        try{
            if (!isProcessingAborted()){
                //invoke data interchange
                putSignalsToOutputProcessImage();
                transceiveProcessImage();
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
            setProcessingAborted(true);
        }
    }

    @Override
    public boolean isFinished() {
        return true;
    }
    
    protected void transceiveProcessImage() {
        try{
            //get actual input process image from peripherals
            //check for changes of output signals
            for (IoSignal s: getOutputSignals()) s.checkOut();
            processImage.update();
            //update input signals
            for (IoSignal s: getInputSignals()) s.checkIn();
            //prepare output image to be transferred to the peripherals
        }
        catch(Exception | Error exc){
            Log.error("Error: ", exc);
        }
    }

    @Override
    public String getHandledScheme() {
        return this.HANDLEDSCHEME;
    }
    
    public ProcessImage getProcessImage(){
        return this.processImage;
    }        
}
