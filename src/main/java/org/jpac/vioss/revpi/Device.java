/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : Device.java
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
import com.pi4j.io.file.LinuxFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.jpac.Address;
import org.jpac.plc.Data;
import org.jpac.plc.IoDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class Device {
    protected static Logger Log = LoggerFactory.getLogger("jpac.vioss.revpi");
    
    private final String ID           = "/id";
    private final String OFFSET       = "/offset";
    private final String INPUT        = "/inp";
    private final String OUTPUT       = "/out";
    private final String MEMORY       = "/mem";
    private final String EXTEND       = "/extend";
    private final byte[] DUMMY        = new byte[0];
    
    protected String identifier;
    protected int    offset;
    
    protected Data inputImage;
    protected Data outputImage;
    protected Data memoryImage;
    protected Data extendImage;
    

    protected ArrayList<ProcessImageItem> inputs;
    protected ArrayList<ProcessImageItem> outputs;
    protected ArrayList<ProcessImageItem> memoryItems;
    protected ArrayList<ProcessImageItem> extendItems;
    
    protected ProcessImageItem            searchedProcessImageItem;
    protected LinuxFile                   piControl0;
    
    protected byte[]                      snapshot;
    
    protected boolean                     bit;

    public Device(JsonNode deviceNode, LinuxFile piControl0){
        ProcessImageItem  pii = null;
        int numberOfBytes = 0;
        int imageOffset   = 0;
        this.identifier   = deviceNode.at(ID).asText();
        this.offset       = deviceNode.at(OFFSET).asInt();

        Log.debug("added device {} at offset {}", this.identifier, this.offset);
        imageOffset   = 0;
        numberOfBytes = 0;
        this.inputs   = new ArrayList<>();
        Iterator<JsonNode> inputNodes = deviceNode.at(INPUT).elements();
        this.inputImage = new Data(DUMMY, Data.Endianness.LITTLEENDIAN);
        while (inputNodes.hasNext()){
            pii = new ProcessImageItem(this, imageOffset, inputNodes.next(), IoDirection.INPUT, this.inputImage);
            this.inputs.add(pii);
            Log.debug("   added input {}", pii);
        }
        if (pii != null){
            //get size of this image from offset of last data item. If it is not a bit item add its size.
            numberOfBytes = pii.getAddress().getByteIndex() + (pii.getAddress().getSize() != Address.NA ? pii.getAddress().getSize(): 0);
            this.inputImage.setBytes(new byte[numberOfBytes]);
        }
        
        imageOffset   = numberOfBytes;
        numberOfBytes = 0;
        this.outputs  = new ArrayList<>();
        Iterator<JsonNode> outputNodes = deviceNode.at(OUTPUT).elements();
        this.outputImage = new Data(DUMMY, Data.Endianness.LITTLEENDIAN);
        while (outputNodes.hasNext()){
            pii = new ProcessImageItem(this, imageOffset, outputNodes.next(), IoDirection.OUTPUT, this.outputImage);
            this.outputs.add(pii);
            numberOfBytes += pii.getAddress().getSize();
            Log.debug("   added output {}", pii);
        }                            
        if (pii != null){
            //get size of this image from offset of last data item. If it is not a bit item add its size.
            numberOfBytes = pii.getAddress().getByteIndex() + (pii.getAddress().getSize() != Address.NA ? pii.getAddress().getSize(): 0);
            this.outputImage.setBytes(new byte[numberOfBytes]);
        }    
        imageOffset      = numberOfBytes;
        numberOfBytes    = 0;
        this.memoryItems = new ArrayList<>();
        Iterator<JsonNode> memoryNodes = deviceNode.at(MEMORY).elements();
        this.memoryImage = new Data(DUMMY, Data.Endianness.LITTLEENDIAN);
        while (memoryNodes.hasNext()){
            pii = new ProcessImageItem(this, imageOffset, memoryNodes.next(), IoDirection.INPUT, this.memoryImage);
            this.memoryItems.add(pii);
            numberOfBytes += pii.getAddress().getSize();
            Log.debug("   added memory item {}", pii);
        }                            
        if (pii != null){
            //get size of this image from offset of last data item. If it is not a bit item add its size.
            numberOfBytes = pii.getAddress().getByteIndex() + (pii.getAddress().getSize() != Address.NA ? pii.getAddress().getSize(): 0);
            this.memoryImage.setBytes(new byte[numberOfBytes]);
        }    

        imageOffset      = numberOfBytes;
        numberOfBytes    = 0;
        this.extendItems = new ArrayList<>();
        Iterator<JsonNode> extendNodes = deviceNode.at(EXTEND).elements();
        this.extendImage = new Data(DUMMY, Data.Endianness.LITTLEENDIAN);
        while (extendNodes.hasNext()){
            pii = new ProcessImageItem(this, imageOffset, extendNodes.next(), IoDirection.INPUT, this.extendImage);
            this.extendItems.add(pii);
            numberOfBytes += pii.getAddress().getSize();
            Log.debug("   added extend item {}", pii);
        }                            
        if (pii != null){
            //get size of this image from offset of last data item. If it is not a bit item add its size.
            numberOfBytes = pii.getAddress().getByteIndex() + (pii.getAddress().getSize() != Address.NA ? pii.getAddress().getSize(): 0);
            this.extendImage.setBytes(new byte[numberOfBytes]);
        }    
        this.snapshot   = new byte[inputImage.getBytes().length + outputImage.getBytes().length + memoryImage.getBytes().length + extendImage.getBytes().length];
        this.piControl0 = piControl0;
//        //initialize memory/extend portion of this device with the state of the associated portion of the process image on startup of this application
//        try{
//            piControl0.seek(offset + inputImage.getBytes().length + outputImage.getBytes().length);//skip input/output area
//            piControl0.read(memoryImage.getBytes(), 0, memoryImage.getBytes().length);
//            piControl0.read(extendImage.getBytes(), 0, extendImage.getBytes().length);        
//        } catch(IOException exc){
//            Log.error("Error: failed to initialize process image for device " + identifier, exc);
//        }
    }
    
    public ProcessImageItem getProcessImageItem(String identifier){
        searchedProcessImageItem = null;
        inputs.forEach((pii) -> {if (pii.getIdentifier().equals(identifier)) searchedProcessImageItem = pii;});
        if (searchedProcessImageItem == null){
            outputs.forEach((pii) -> {if (pii.getIdentifier().equals(identifier)) searchedProcessImageItem = pii;});        
        }
        if (searchedProcessImageItem == null){
            memoryItems.forEach((pii) -> {if (pii.getIdentifier().equals(identifier)) searchedProcessImageItem = pii;});        
        }
        if (searchedProcessImageItem == null){
            extendItems.forEach((pii) -> {if (pii.getIdentifier().equals(identifier)) searchedProcessImageItem = pii;});        
        }
        return searchedProcessImageItem;
    }
        
    public void updateProcessImage(){
        try{
            if (!ProcessImage.simulation){
                //write writable portions of the process image
                piControl0.seek(offset + inputImage.getBytes().length);//skip input area
                piControl0.write(outputImage.getBytes(), 0, outputImage.getBytes().length);
                //piControl0.write(memoryImage.getBytes(), 0, memoryImage.getBytes().length);
                //piControl0.write(extendImage.getBytes(), 0, extendImage.getBytes().length);        
                //get snapshot of actual process image
                piControl0.seek(offset);
                piControl0.read(inputImage.getBytes(), 0, inputImage.getBytes().length);
                piControl0.seek(piControl0.getFilePointer() + outputImage.getBytes().length);//skip output area
                piControl0.read(memoryImage.getBytes(), 0, memoryImage.getBytes().length);                
                piControl0.read(extendImage.getBytes(), 0, extendImage.getBytes().length);
            } else {
                //write writable portions of the process image
                int byteOffset = offset + inputImage.getBytes().length;
                System.arraycopy(outputImage.getBytes(), 0, ProcessImage.simulatedProcessImage.getBytes(), byteOffset, outputImage.getBytes().length);
                byteOffset += outputImage.getBytes().length;
                System.arraycopy(memoryImage.getBytes(), 0, ProcessImage.simulatedProcessImage.getBytes(), byteOffset, memoryImage.getBytes().length);
                byteOffset += memoryImage.getBytes().length;
                System.arraycopy(extendImage.getBytes(), 0, ProcessImage.simulatedProcessImage.getBytes(), byteOffset, extendImage.getBytes().length);
                //get snapshot of actual process image
                byteOffset = offset;
                System.arraycopy(ProcessImage.simulatedProcessImage.getBytes(), byteOffset, inputImage.getBytes(), 0, inputImage.getBytes().length);
                byteOffset += inputImage.getBytes().length;
                System.arraycopy(ProcessImage.simulatedProcessImage.getBytes(), byteOffset, outputImage.getBytes(), 0, outputImage.getBytes().length);
                byteOffset += outputImage.getBytes().length;
                System.arraycopy(ProcessImage.simulatedProcessImage.getBytes(), byteOffset, memoryImage.getBytes(), 0, memoryImage.getBytes().length);
                byteOffset += memoryImage.getBytes().length;
                System.arraycopy(ProcessImage.simulatedProcessImage.getBytes(), byteOffset, extendImage.getBytes(), 0, extendImage.getBytes().length);
            }
        }
        catch(Exception exc){
            Log.error("Error: failed to seize process image for " + this, exc);
        }
    }
    
    
    public String getIdentifier(){
        return identifier;
    }
    
    public int getOffset() {
        return offset;
    }

    public Data getInputImage() {
        return inputImage;
    }

    public Data getOutputImage() {
        return outputImage;
    }

    public Data getMemoryImage() {
        return memoryImage;
    }
    
    @Override
    public String toString(){
        return this.getClass().getCanonicalName() + "(" + identifier + ", " + offset + ")";
    }
}
