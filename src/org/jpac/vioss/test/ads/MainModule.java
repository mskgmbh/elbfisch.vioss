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
public class MainModule extends Module{
    CountModule      countModule;
    //HandshakeModule  handshakeModule;
    DerivedHandshake handshakeModule;
    
    public MainModule() throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException{
        super(null,"main");
        countModule = new CountModule(this);
//        handshakeModule = new HandshakeModule(this);
//        handshakeModule = new DerivedHandshake(this);
    }
    
    @Override
    public void start(){
        countModule.start();
//        handshakeModule.start();
        super.start();
    }
    
    @Override
    protected void work() throws ProcessException {
        new ImpossibleEvent().await();
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
