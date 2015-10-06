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

package org.jpac.vioss.test;

import java.net.URI;
import java.net.URISyntaxException;
import org.jpac.ImpossibleEvent;
import org.jpac.InconsistencyException;
import org.jpac.InputInterlockException;
import org.jpac.Module;
import org.jpac.OutputInterlockException;
import org.jpac.ProcessException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.WrongUseException;
import org.jpac.plc.IoDirection;
import org.jpac.vioss.modbus.IoLogical;

/**
 *
 * @author berndschuster
 */
public class TestModule extends Module{

    public TestModule() throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException{
        super(null,"test");
        IoLogical  lip0  = new IoLogical(this, "lip0", new URI("modbus://localhost:1002/PhysicalDiscreteInputs/0"), IoDirection.INPUT);
        IoLogical  lip1  = new IoLogical(this, "lip1", new URI("modbus://localhost:1002//PhysicalDiscreteInputs/1999"), IoDirection.INPUT);
        IoLogical  lip2  = new IoLogical(this, "lip2", new URI("modbus://localhost:1002/InternalBits/500"), IoDirection.INPUT);
        IoLogical  lip3  = new IoLogical(this, "lip3", new URI("modbus://localhost:1002/InternalBits/32"), IoDirection.INPUT);
        IoLogical  lip4  = new IoLogical(this, "lip4", new URI("modbus://localhost:1002/PhysicalCoils/16"), IoDirection.INPUT);
        IoLogical  lip5  = new IoLogical(this, "lip5", new URI("modbus://localhost:1002/InternalBits/13"), IoDirection.INPUT);
        IoLogical  lip6  = new IoLogical(this, "lip6", new URI("modbus://www.mskgmbh.com:1002/PhysicalDiscreteInputs/0"), IoDirection.INPUT);
        IoLogical  lip7  = new IoLogical(this, "lip7", new URI("modbus://localhost:1003//PhysicalDiscreteInputs/1999"), IoDirection.INPUT);
        IoLogical  lop0  = new IoLogical(this, "lop0", new URI("modbus://localhost:1003//PhysicalDiscreteInputs/1999"), IoDirection.OUTPUT);
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
