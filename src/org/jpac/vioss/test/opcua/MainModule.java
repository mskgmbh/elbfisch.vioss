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

package org.jpac.vioss.test.opcua;

import java.net.URI;
import org.jpac.ImpossibleEvent;
import org.jpac.InputInterlockException;
import org.jpac.Module;
import org.jpac.OutputInterlockException;
import org.jpac.PeriodOfTime;
import org.jpac.ProcessException;
import org.jpac.opc.Opc;
import org.jpac.plc.IoDirection;
import org.jpac.vioss.opcua.IoLogical;
import org.jpac.vioss.opcua.IoSignedInteger;

/**
 *
 * @author berndschuster
 */
public class MainModule extends Module{
    IoLogical iPause;
    IoLogical iSingleStep;
    IoLogical oProceed;
    IoSignedInteger oCarrierId;
    IoSignedInteger iCarrierId;
    public MainModule(){
        super(null,"main");
        try{
             this.iPause      = new IoLogical(this, "iPause", new URI("opcua://localhost:12685/elbfisch/2/Main.pause"), IoDirection.INPUT);
             this.iSingleStep = new IoLogical(this, "iSingleStep", new URI("opcua://localhost:12685/elbfisch/2/Main.singleStep"), IoDirection.INPUT);
             this.oProceed    = new IoLogical(this, "oProceed", new URI("opcua://localhost:12685/elbfisch/2/Main.proceed"), IoDirection.OUTPUT);
             this.oCarrierId  = new IoSignedInteger(this, "oCarrierId", new URI("opcua://localhost:12685/elbfisch/2/Main.carrierId"), IoDirection.OUTPUT);
             this.iCarrierId  = new IoSignedInteger(this, "iCarrierId", new URI("opcua://localhost:12685/elbfisch/2/Main.carrierId"), IoDirection.INPUT);
        }
        catch(Exception exc){
            Log.error("Error:", exc);
        }
    }
        
    @Override
    protected void work() throws ProcessException {
        PeriodOfTime pot = new PeriodOfTime(1 * sec);
        for (int i = 0; i < 10; i++){
            oProceed.set(i % 2 == 1);
            pot.await();
            oProceed.invalidate();
            pot.await();
        }
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
