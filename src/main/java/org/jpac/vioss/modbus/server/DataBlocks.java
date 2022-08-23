/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : DataBlocks.java (versatile input output subsystem)
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
package org.jpac.vioss.modbus.server;

import org.jpac.vioss.modbus.DataBlock;

/**
 *
 * @author berndschuster
 */
public class DataBlocks {

    private DataBlock inputDatablock;
    private DataBlock outputDatablock;

    /**
     * @return the inputDatablock
     */
    public DataBlock getInputDatablock() {
        return inputDatablock;
    }

    /**
     * @param inputDatablock the inputDatablock to set
     */
    public void setInputDatablock(DataBlock inputDatablock) {
        this.inputDatablock = inputDatablock;
    }

    /**
     * @return the outputDatablock
     */
    public DataBlock getOutputDatablock() {
        return outputDatablock;
    }

    /**
     * @param outputDatablock the outputDatablock to set
     */
    public void setOutputDatablock(DataBlock outputDatablock) {
        this.outputDatablock = outputDatablock;
    }
}
