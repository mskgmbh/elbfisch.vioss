/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : IOHandlerFactory.java (versatile input output subsystem)
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

package org.jpac.vioss;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jpac.configuration.StringProperty;
import org.jpac.Address;
import org.jpac.configuration.Configuration;

/**
 * Maintains all IOHandler's used in a given Elbfisch application 
 * @author berndschuster
 */
public class IOHandlerFactory {
    final  static String IOHANDLERPATH = "org..jpac..vioss.IOHandler";
    static public Logger Log = LoggerFactory.getLogger("jpac.vioss.IOHandler");    
    static List<IOHandler> instances;
    
    /**
     * used to return the IOHandler according to the given URI
     * @param address address of the signal to handle
     * @return 
     * @throws java.lang.ClassNotFoundException 
     */
    static public IOHandler getHandlerFor(Address address, URI uri) throws ClassNotFoundException {
        IOHandler ioHandler = null;
        if (instances == null){
            instances = Collections.synchronizedList(new ArrayList<IOHandler>());
        }
        //check,if the desired handler is already instantiated
        for (IOHandler cip: instances){
            if (cip.handles(address, uri)){
                ioHandler = cip;
            }
        }
        //if not, do it now:
        if (ioHandler == null){
            String cyclicInputHandlerClass = null;
            try{
                //seize the name of the input handler from the configuration file
                String scheme = uri.getScheme().replace(".","..");
                cyclicInputHandlerClass = (String)Configuration.getInstance().getProperty(IOHANDLERPATH + "." + scheme);
                if (cyclicInputHandlerClass == null){
                    throw new ClassNotFoundException();
                }
//                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//                Class clazz = systemClassLoader.loadClass(cyclicInputHandlerClass);
                Class clazz = Class.forName(cyclicInputHandlerClass);
                Constructor c = clazz.getConstructor(URI.class);
                //... and instantiate it using the uri provided.
                ioHandler = (IOHandler) c.newInstance(uri);
                //... and finally add it to the list of io handlers
                instances.add(ioHandler);
            }
            catch(Exception exc){
                if (exc instanceof InvocationTargetException && exc.getCause() instanceof IllegalUriException){
                    Log.error("Error:",exc.getCause());
                }
                else{
                    Log.error("Error:", exc);
                }
                throw new ClassNotFoundException(cyclicInputHandlerClass, exc);
            }
        }
        return ioHandler; 
    }
}
