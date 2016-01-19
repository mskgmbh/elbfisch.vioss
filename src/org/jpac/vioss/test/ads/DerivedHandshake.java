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

import java.net.URISyntaxException;
import org.jpac.AbstractModule;
import org.jpac.InconsistencyException;
import org.jpac.SignalAlreadyExistsException;
import org.jpac.SignedInteger;
import org.jpac.WrongUseException;

/**
 *
 * @author berndschuster
 */
public class DerivedHandshake extends HandshakeModule{
    SignedInteger testDerivedClass = new SignedInteger(this, "testDerivedClass");

    public DerivedHandshake(AbstractModule containingModule) throws SignalAlreadyExistsException, InconsistencyException, WrongUseException, URISyntaxException {
        super(containingModule);
    }

}
