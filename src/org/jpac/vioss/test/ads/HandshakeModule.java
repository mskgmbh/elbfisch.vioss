/**
 * PROJECT   : <???>
 * MODULE    : <???>.java
 * VERSION   : $Revision$
 * DATE      : $Date$
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log$
 */

package org.jpac.vioss.test.ads;

import org.jpac.vioss.test.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jpac.AbstractModule;
import org.jpac.Handshake;
import org.jpac.ImpossibleEvent;
import org.jpac.InconsistencyException;
import org.jpac.InputInterlockException;
import org.jpac.Module;
import org.jpac.NextCycle;
import org.jpac.OutputInterlockException;
import org.jpac.PeriodOfTime;
import org.jpac.ProcessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.WrongUseException;
import org.jpac.plc.IoDirection;
import org.jpac.vioss.ads.IoLogical;
import org.jpac.vioss.ads.IoSignedInteger;

/**
 *
 * @author berndschuster
 */
public class HandshakeModule extends Module{
    IoSignedInteger  otestDINT1;
    IoSignedInteger  itestDINT2;
    IoSignedInteger  itestDINT3;
    IoLogical        cmd;
    IoLogical        ack;
    IoLogical        active;
    IoSignedInteger  result;
    Handshake        handshake;
    IoSignedInteger  iParam;
    
    public HandshakeModule(AbstractModule containingModule) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException{
        super(containingModule,"handshake");
        try{
            otestDINT1  = new IoSignedInteger(this, "MAIN.testDINT1", new URI("ads://192.168.0.68/MAIN.testDINT1"), IoDirection.OUTPUT);
            itestDINT2  = new IoSignedInteger(this, "MAIN.testDINT2", new URI("ads://192.168.0.68/MAIN.testDINT2"), IoDirection.INPUT);
            itestDINT3  = new IoSignedInteger(this, "MAIN.testDINT3", new URI("ads://192.168.0.68/MAIN.testDINT3"), IoDirection.INPUT);
            cmd         = new IoLogical(this, "MAIN.Cmds.fbCmdNoOp.stCmdAck.Cmd", new URI("ads://192.168.0.68/MAIN.Cmds.fbCmdNoOp.stCmdAck.Cmd"), IoDirection.OUTPUT);
            ack         = new IoLogical(this, "MAIN.Cmds.fbCmdNoOp.stCmdAck.Ack", new URI("ads://192.168.0.68/MAIN.Cmds.fbCmdNoOp.stCmdAck.Ack"), IoDirection.INPUT);
            active      = new IoLogical(this, "MAIN.Cmds.fbCmdNoOp.stCmdAck.Active", new URI("ads://192.168.0.68/MAIN.Cmds.fbCmdNoOp.stCmdAck.Active"), IoDirection.INPUT);
            result      = new IoSignedInteger(this, "MAIN.Cmds.fbCmdNoOp.stCmdAck.Result", new URI("ads://192.168.0.68/MAIN.Cmds.fbCmdNoOp.stCmdAck.Result"), IoDirection.INPUT);
            iParam      = new IoSignedInteger(this, "MAIN.Cmds.fbCmdNoOp.iParam", new URI("ads://192.168.0.68/MAIN.Cmds.fbCmdNoOp.iParam"), IoDirection.OUTPUT);
            handshake   = new Handshake(this,"handshake");
            handshake.getRequest().connect(cmd);
            ack.connect(handshake.getAcknowledge());
            active.connect(handshake.getActive());
//            result.connect(handshake.getResultSig());
        }
        catch(Exception exc){
            Log.error("Error:",exc);
        }
    }
    
    @Override
    protected void work() throws ProcessException {
        boolean done = false;
        PeriodOfTime ldelay = new PeriodOfTime(4 * sec);
        PeriodOfTime sdelay = new PeriodOfTime(2 * sec);
//        NextCycle nextCycle = new NextCycle();
//        cmd.set(false);
//        for(int i = 0; i < 10000; i++){
//            otestDINT1.set(i);
//            cmd.set(cmd.is(false));
//            nextCycle.await();
//        }
//        new ImpossibleEvent().await();
//        cmd.set(false);
        sdelay.await();
        iParam.set(123);
        do{ 
            iParam.set(iParam.get()+1);
            Log.info("requesting");
            handshake.request();
            Log.info("awaiting active state sent by plc");
            handshake.active().await();
            Log.info("awaiting acknowledgement");
            handshake.acknowledged().await();            
            sdelay.await();
            handshake.resetRequest();            
//            Log.info("requesting");
//            cmd.set(true);
//            Log.info("awaiting active state sent by plc");
//            active.state(true).await();
//            Log.info("awaiting acknowledgement");
//            ack.state(true).await();
//            sdelay.await();
//            Log.info("reset cmd");
//            cmd.set(false);
            ldelay.await();
        }
        while(!done);
    
    }

    @Override
    protected void preCheckInterlocks() throws InputInterlockException {
    }

    @Override
    protected void postCheckInterlocks() throws OutputInterlockException {
    }

    @Override
    protected void inEveryCycleDo() throws ProcessException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
