/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Handshake.java (versatile input output subsystem)
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

package org.jpac.vioss.opcua;

import com.digitalpetri.opcua.stack.core.types.builtin.ExtensionObject;
import java.net.URI;
import java.net.URISyntaxException;
import org.jpac.Event;
import org.jpac.Logical;
import org.jpac.Module;
import org.jpac.ProcessEvent;
import org.jpac.SignedInteger;
import org.jpac.opc.Opc;
import static org.jpac.opc.Opc.AccessLevel.READ_WRITE;
import org.jpac.plc.IoDirection;

/**
 * handshake used on the client side of an opc connection
 * @author berndschuster
 */
public class Handshake {
    public final static int DONTCAREINTEGER = Integer.MIN_VALUE;
    
    @Opc(accessLevel=READ_WRITE)
    protected final Logical       request;
    @Opc(accessLevel=READ_WRITE)
    protected final Logical       ready;
    @Opc(accessLevel=READ_WRITE)
    protected final Logical       ack;
    @Opc(accessLevel=READ_WRITE)
    protected final Logical       active;
    @Opc(accessLevel=READ_WRITE)
    protected final SignedInteger command;
    @Opc(accessLevel=READ_WRITE)
    protected final SignedInteger result;
    
    protected final String          identifier;
    protected final URI             uri;

    protected final Event           acknowledged;
    protected final Event           acknowledgementRemoved;
    
    protected final Event           requested;
    protected final Event           requestRemoved;
    
    protected final Event           valid;

    public Handshake(Module containingModule, String identifier, URI uri) throws URISyntaxException {
        this(containingModule, identifier, uri,  100.0, null, 10, false);
    }

    public Handshake(Module containingModule, String identifier, URI uri,  double samplingRate, ExtensionObject extensionObject, int queueSize, boolean discardOldest) throws URISyntaxException {
        this.identifier = identifier;
        this.uri        = uri;
        
        if (uri != null){
            //handshake is used to access signals on a remote opc server
            this.request     = new IoLogical(containingModule, identifier + ".Request", new URI(uri + ".Request"), IoDirection.OUTPUT, samplingRate, extensionObject, queueSize, discardOldest);
            this.ready       = new IoLogical(containingModule, identifier + ".Ready",   new URI(uri + ".Ready"), IoDirection.INPUT, samplingRate, extensionObject, queueSize, discardOldest);
            this.ack         = new IoLogical(containingModule, identifier + ".Ack",     new URI(uri + ".Ack"), IoDirection.INPUT, samplingRate, extensionObject, queueSize, discardOldest);
            this.active      = new IoLogical(containingModule, identifier + ".Active",  new URI(uri + ".Active"), IoDirection.INPUT, samplingRate, extensionObject, queueSize, discardOldest);
            this.command     = new IoSignedInteger(containingModule, identifier + ".Command", new URI(uri + ".Command"), IoDirection.OUTPUT, samplingRate, extensionObject, queueSize, discardOldest);
            this.result      = new IoSignedInteger(containingModule, identifier + ".Result", new URI(uri + ".Result"), IoDirection.INPUT, samplingRate, extensionObject, queueSize, discardOldest);
            //initialize output signals
            this.request.setDeferred(false);
            this.command.setDeferred(DONTCAREINTEGER);
        }
        else{
            //handshake is used to give a remote opc client access to own signals
            this.request     = new Logical(containingModule, identifier + ".Request");
            this.ready       = new Logical(containingModule, identifier + ".Ready");
            this.ack         = new Logical(containingModule, identifier + ".Ack");
            this.active      = new Logical(containingModule, identifier + ".Active");
            this.command     = new SignedInteger(containingModule, identifier + ".Command");
            this.result      = new SignedInteger(containingModule, identifier + ".Result");
            //initialize output signals
            this.ack.setDeferred(false);
            this.ready.setDeferred(false);
            this.active.setDeferred(false);
            this.result.setDeferred(DONTCAREINTEGER);
        }
        

        this.acknowledged           = new Event(()-> isAcknowledged());
        this.acknowledgementRemoved = new Event(()-> isAcknowledgementRemoved());
        this.requested              = new Event(()-> isRequested());
        this.requestRemoved         = new Event(()-> isRequestRemoved());
        this.valid                  = new Event(()-> isValid());
    }
    
    /**
     * used to initiate a request. All application specific parameters must have been set prior to this call
     * @param command
     */
    public void request(int command){
//        if (!areRequestParameterSignalsValid() || areRequestParameterValuesDontCare()){
//            throw new InconsistencyException("application specific parameters must have been set prior to this call");
//        }
        this.command.setDeferred(command);
        this.request.setDeferred(true);
    }

    /**
     * used to reset all request related signals to "Dontcare"
     * CAUTION: Must be overridden by a derived class to include additional signals
     */
    public void resetRequest(){
        this.command.setDeferred(DONTCAREINTEGER);
        this.request.setDeferred(false);
    }

    /**
     * used to acknowledge a pending request. All application specific response valued must have been set prior to this call
     * @param result
     */
    public void acknowledge(int result){
        this.result.setDeferred(result);
    //    this.active.setDeferred(false);
        this.ack.setDeferred(true);
    }

    /**
     * used to reset all acknowledgement related signals to "Dontcare"
     * CAUTION: Must be overridden by a derived class to include additional signals
     */
    public void resetAcknowledgement(){
        this.result.setDeferred(DONTCAREINTEGER);
        this.ack.setDeferred(false);
        this.active.setDeferred(false);
    }
    
    public void setReady(boolean state){
        this.ready.setDeferred(state);
    }

    public void setActive(boolean state){
        this.active.setDeferred(state);
    }

    public boolean isReady(){
        return ready.isValid() ? ready.is(true) : false;
    }
 
    public boolean isActive(){
        return active.isValid() ? active.is(true) : false;
    }
    
    /**
     * 
     * @return true, if all request related signals ar valid and not "dontcare"
     *               CAUTION: Must be overridden, if a derived class uses own request parameters
     */
    public boolean isValid(){
        boolean valid = false;
        if (uri == null){
            //server side
            valid = request.isValid() && command.isValid();
        }
        else{
            //client side
            valid = ack.isValid() && result.isValid() && ready.isValid() && active.isValid();
        }
        return valid;
    }
    
    public boolean isRequested(){
        return request.isValid() && command.isValid() ? request.is(true) && command.get() != DONTCAREINTEGER : false;
    }

    public boolean isRequestRemoved(){
        return request.isValid() && command.isValid() ? request.is(false) && command.get() == DONTCAREINTEGER : false;
    }

    public boolean isAcknowledged(){
        return ack.isValid() && result.isValid() ? ack.is(true) && result.get() != DONTCAREINTEGER : false;
    }

    public boolean isAcknowledgementRemoved(){
        return ack.isValid() && result.isValid() ? ack.is(false) && result.get() == DONTCAREINTEGER : false;
    }
    
    public int getResult(){
        return result.get();
    }
    
    public int getCommand(){
        return command.get();
    }    

    public ProcessEvent requested(){
        return requested;
    }
    
    public ProcessEvent requestRemoved(){
        return requestRemoved;
    }

    public ProcessEvent valid(){
        return valid;
    }
    
    public ProcessEvent ready(){
        return ready.state(true);
    }
    
    public ProcessEvent active(){
        return active.state(true);
    }
    
    public ProcessEvent acknowledged(){
        return acknowledged;
    }
    
    public ProcessEvent acknowledgementRemoved(){
        return acknowledgementRemoved;
    }
}
