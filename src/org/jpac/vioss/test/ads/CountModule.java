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

import java.net.URI;
import java.net.URISyntaxException;
import org.jpac.AbstractModule;
import org.jpac.CharString;
import org.jpac.Handshake;
import org.jpac.InconsistencyException;
import org.jpac.InputInterlockException;
import org.jpac.Module;
import org.jpac.NextCycle;
import org.jpac.OutputInterlockException;
import org.jpac.PeriodOfTime;
import org.jpac.ProcessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignedInteger;
import org.jpac.WrongUseException;
import org.jpac.alarm.Alarm;
import org.jpac.opc.Opc;
import org.jpac.opc.OpcNone;
import org.jpac.opc.OpcReadOnly;
import org.jpac.opc.OpcReadWrite;
import org.jpac.plc.IoDirection;
import org.jpac.vioss.ads.IoLogical;
import org.jpac.vioss.ads.IoSignedInteger;

/**
 *
 * @author berndschuster
 */
public class CountModule extends Module{
    @OpcNone      private IoSignedInteger  otestDINT1;
    @OpcReadWrite private SignedInteger  opcTestDint;
    private IoSignedInteger  itestDINT2;
    private IoSignedInteger  itestDINT3;
    private IoLogical        cmd;
    private IoLogical        ack;
    private IoLogical        active;
    private IoSignedInteger  result;
    private Handshake        handshake;
    private IoSignedInteger  iParam;
    private CharString       charString;
    private Alarm            alarm;
    
    public CountModule(AbstractModule containingModule) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException{
        super(containingModule,"count");
        try{
            otestDINT1  = new IoSignedInteger(this, "MAIN.testDINT1", new URI("ads://192.168.0.68/MAIN.testDINT1"), IoDirection.OUTPUT);
            itestDINT2  = new IoSignedInteger(this, "MAIN.testDINT2", new URI("ads://192.168.0.68/MAIN.testDINT2"), IoDirection.INPUT);
//            itestDINT3  = new IoSignedInteger(this, "MAIN.testDINT3", new URI("ads://192.168.0.68/MAIN.testDINT3"), IoDirection.INPUT);
//            opcTestDint = new SignedInteger(this,"opcTestDint");
//            charString  = new CharString(this,"charString");
            alarm       = new Alarm(this,"alarm","this is an alarm",true);
        }
        catch(Exception exc){
            Log.error("Error:",exc);
        }
    }
    
    @Override
    protected void work() throws ProcessException {
        boolean done = false;
        NextCycle nextCycle = new NextCycle();
        PeriodOfTime pot    = new PeriodOfTime(100 * ms);
        int i = 0;
        do{
            otestDINT1.set(i++);
//            opcTestDint.set(i);
//            charString.set("this is a string #" + i);
//            if (i % 10 == 0){
//                opcTestDint.invalidate();
//                alarm.raise();
//            }
//            else{
//                alarm.acknowledge();
//            }
            pot.await();
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
