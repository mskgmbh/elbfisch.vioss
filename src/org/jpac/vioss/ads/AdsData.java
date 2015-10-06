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

package org.jpac.vioss.ads;

import java.io.IOException;

/**
 *
 * @author berndschuster
 */
abstract public class AdsData {
    private int size;
    
    protected void setSize(int size){
        this.size = size;
    }
    
    abstract public int size();
    abstract public void read(Connection connection) throws IOException;
    abstract public void write(Connection connection) throws IOException;
}
