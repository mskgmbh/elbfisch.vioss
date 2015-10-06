/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AmsHeader.java (versatile input output subsystem)
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

import java.io.IOException;
import org.apache.log4j.Logger;
import org.jpac.WrongUseException;

/**
 *
 * @author berndschuster
 */
public class AmsHeader {
    static public Logger Log = Logger.getLogger("jpac.vioss.ads");
    
    protected AmsNetId     targetAmsNetId;
    protected AmsPortNr    targetAmsPortNr;
    protected AmsNetId     sourceAmsNetId;
    protected AmsPortNr    sourceAmsPortNr;
    protected CommandId    commandId;
    protected StateFlags   stateFlags;
    protected int          dataLength;
    protected AdsErrorCode errorCode;
    protected int          invokeId;

    protected boolean  initialized;
    /**
     * constructor used to read an AMS header sent by a peer
     */
    public AmsHeader(){
        initialized = false;
    }
    
    /**
     * constructor used to send an AMS header to a peer
     * @param targetAmsNetId
     * @param targetAmsPort
     * @param sourceAmsNetId
     * @param sourceAmsPort
     * @param commandId
     * @param dataLength 
     */
    public AmsHeader(AmsNetId targetAmsNetId, AmsPortNr targetAmsPort, AmsNetId sourceAmsNetId, AmsPortNr sourceAmsPort, CommandId commandId, int dataLength){
        this.targetAmsNetId  = targetAmsNetId;
        this.targetAmsPortNr = targetAmsPort;
        this.sourceAmsNetId  = sourceAmsNetId;
        this.sourceAmsPortNr = sourceAmsPort;
        this.commandId       = commandId;
        this.dataLength      = dataLength;
        
        this.stateFlags      = new StateFlags(true, true, true);
        this.errorCode       = AdsErrorCode.NoError;
        this.invokeId        = 0;
        initialized          = true;
    }
    
    public void write(Connection connection) throws IOException, WrongUseException{
        if (!initialized){
            throw new IOException("length of the AMS request not set");
        }
        invokeId = (int)System.nanoTime();//send an arbitrary, but (almost) unique value        
        if (Log.isDebugEnabled())Log.debug("  " + this);
        targetAmsNetId.write(connection);
        targetAmsPortNr.write(connection);
        sourceAmsNetId.write(connection);
        sourceAmsPortNr.write(connection);
        commandId.write(connection);
        stateFlags.write(connection);
        connection.getOutputStream().writeInt(dataLength);
        errorCode.write(connection);
        connection.getOutputStream().writeInt(invokeId);
    }

    public void read(Connection connection) throws IOException{
        targetAmsNetId  = new AmsNetId();
        targetAmsNetId.read(connection);
        targetAmsPortNr = AmsPortNr.getValue(connection.getInputStream().readShort());
        sourceAmsNetId  = new AmsNetId();
        sourceAmsNetId.read(connection);
        sourceAmsPortNr = AmsPortNr.getValue(connection.getInputStream().readShort());
        commandId       = CommandId.getValue(connection.getInputStream().readShort());
        stateFlags      = new StateFlags();
        stateFlags.read(connection);
        dataLength      = connection.getInputStream().readInt();
        errorCode       = AdsErrorCode.getValue(connection.getInputStream().readInt());
        invokeId        = connection.getInputStream().readInt();
        if (Log.isDebugEnabled())Log.debug("  " + this);
    }
    
    public static int size(){
        return 32;
    }

    /**
     * @return the targetAmsNetId
     */
    public AmsNetId getTargetAmsNetId() {
        return targetAmsNetId;
    }

    /**
     * @return the targetAmsPortNr
     */
    public AmsPortNr getTargetAmsPort() {
        return targetAmsPortNr;
    }

    /**
     * @return the sourceAmsNetId
     */
    public AmsNetId getSourceAmsNetId() {
        return sourceAmsNetId;
    }

    /**
     * @return the sourceAmsPortNr
     */
    public AmsPortNr getSourceAmsPort() {
        return sourceAmsPortNr;
    }

    /**
     * @return the commandId
     */
    public CommandId getCommandId() {
        return commandId;
    }

    /**
     * @return the stateFlags
     */
    public StateFlags getStateFlags() {
        return stateFlags;
    }

    /**
     * @return the dataLength
     */
    public int getDataLength() {
        return dataLength;
    }

    /**
     * @return the errorCode
     */
    public AdsErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the invokeId
     */
    public int getInvokeId() {
        return invokeId;
    }

    /**
     * @param targetAmsNetId the targetAmsNetId to set
     */
    public void setTargetAmsNetId(AmsNetId targetAmsNetId) {
        this.targetAmsNetId = targetAmsNetId;
    }

    /**
     * @param targetAmsPort the targetAmsPortNr to set
     */
    public void setTargetAmsPortNr(AmsPortNr targetAmsPortNr) {
        this.targetAmsPortNr = targetAmsPortNr;
    }

    /**
     * @param sourceAmsNetId the sourceAmsNetId to set
     */
    public void setSourceAmsNetId(AmsNetId sourceAmsNetId) {
        this.sourceAmsNetId = sourceAmsNetId;
    }

    /**
     * sourceAmsPortNr sourceAmsPortNr the sourceAmsPortNr to set
     */
    public void setSourceAmsPortNr(AmsPortNr sourceAmsPortNr) {
        this.sourceAmsPortNr = sourceAmsPortNr;
    }

    /**
     * @param commandId the commandId to set
     */
    public void setCommandId(CommandId commandId) {
        this.commandId = commandId;
    }

    /**
     * @param stateFlags the stateFlags to set
     */
    public void setStateFlags(StateFlags stateFlags) {
        this.stateFlags = stateFlags;
    }

    /**
     * @param dataLength the dataLength to set
     */
    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(AdsErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @param invokeId the invokeId to set
     */
    public void setInvokeId(int invokeId) {
        this.invokeId = invokeId;
    }

    /**
     * @param initialized the initialized to set
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
        
    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + targetAmsNetId + ", " + targetAmsPortNr + ", " + sourceAmsNetId + ", " + sourceAmsPortNr + ", " + commandId + ")";
    }
    
}
