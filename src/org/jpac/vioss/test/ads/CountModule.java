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
public class CountModule extends Module{
    IoSignedInteger  otestDINT1;
    IoSignedInteger  itestDINT2;
    IoSignedInteger  itestDINT3;
    IoLogical        cmd;
    IoLogical        ack;
    IoLogical        active;
    IoSignedInteger  result;
    Handshake        handshake;
    IoSignedInteger  iParam;
    
    public CountModule(AbstractModule containingModule) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException{
        super(containingModule,"count");
        try{
            otestDINT1  = new IoSignedInteger(this, "MAIN.testDINT1", new URI("ads://192.168.0.68/MAIN.testDINT1"), IoDirection.OUTPUT);
            itestDINT2  = new IoSignedInteger(this, "MAIN.testDINT2", new URI("ads://192.168.0.68/MAIN.testDINT2"), IoDirection.INPUT);
            itestDINT3  = new IoSignedInteger(this, "MAIN.testDINT3", new URI("ads://192.168.0.68/MAIN.testDINT3"), IoDirection.INPUT);
        }
        catch(Exception exc){
            Log.error("Error:",exc);
        }
    }
    
    @Override
    protected void work() throws ProcessException {
        boolean done = false;
        NextCycle nextCycle = new NextCycle();
        int i = 0;
        do{
            otestDINT1.set(i++);
            nextCycle.await();
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
