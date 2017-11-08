/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : ProcessImage.java
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


package org.jpac.vioss.revpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.io.file.LinuxFile;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.jpac.plc.AddressException;
import org.jpac.plc.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class ProcessImage {
    protected static Logger Log = LoggerFactory.getLogger("jpac.vioss.revpi");

    private final String DEVICES          = "/Devices";
    private final int    PROCESSIMAGESIZE = 4096;
    
    final static String CONFIGFILE          = "/etc/revpi/config.rsc"; //piCtory config file
    final static String SIMCONFIGFILE       = "./cfg/config1.json";     //piCtory config file in non revpi environments


    protected LinuxFile         piControl0;
    protected ArrayList<Device> devices;
    protected static Data       simulatedProcessImage;
    protected static boolean    simulation;

    protected String            piCtoryConfigFileName; 
    
    public ProcessImage(String piControl0File, boolean runningOnRevPi) throws FileNotFoundException, IOException{
        if (runningOnRevPi){
            this.piControl0            = new LinuxFile(piControl0File,"rw");
            this.simulatedProcessImage = null;
            this.simulation            = false;
            this.piCtoryConfigFileName = CONFIGFILE;
        } else{
            this.piControl0            = null;
            this.simulatedProcessImage = new Data(new byte[PROCESSIMAGESIZE]);
            this.simulation            = true;
            this.piCtoryConfigFileName = SIMCONFIGFILE;
        }
        ObjectMapper mapper      = new ObjectMapper();
        JsonNode     rootNode    = mapper.readTree(new FileReader(piCtoryConfigFileName));
        Iterator     deviceNodes = rootNode.at(DEVICES).elements();
        
        this.devices             = new ArrayList<>();
        deviceNodes.forEachRemaining((devNode) -> this.devices.add(new Device((JsonNode)devNode, this.piControl0)));
    }
    
    public void update(){
        devices.forEach((dev) -> dev.updateProcessImage());
    }
    
    public Device getDevice(String identifier){
        return devices.stream().filter((dev)-> dev.getIdentifier().equals(identifier)).findFirst().get();
    }
    
    public ProcessImageItem getItem(String identifier) throws AddressException{
        ProcessImageItem searchedProcessImageItem = null;
        Iterator<Device> devs  = devices.iterator();
        while(devs.hasNext() && searchedProcessImageItem == null){
            searchedProcessImageItem = devs.next().getProcessImageItem(identifier);
        }
        return searchedProcessImageItem;
    }
        
    public ArrayList<Device> getDevices(){
        return this.devices;
    }
}
