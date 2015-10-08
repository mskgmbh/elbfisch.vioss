/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsReadState.java (versatile input output subsystem)
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

package org.jpac.vioss.ads;

import java.io.IOException;
/**
 *
 * @author berndschuster
 */
public class AdsReadState extends AmsPacket{
    AdsReadStateRequest  adsReadStateRequest;
    AdsReadStateResponse adsReadStateResponse;
    
    public AdsReadState(){
        super();
        setAdsRequest(new AdsReadStateRequest());
        setAdsResponse(new AdsReadStateResponse());
    }
    
    public class AdsReadStateRequest extends AdsRequest{

        public AdsReadStateRequest(){
            super(CommandId.AdsReadState);
        }

        @Override
        public void write(Connection connection) throws IOException {
            //nothing to write
        }

        @Override
        public void writeMetaData(Connection connection) throws IOException {
            //nothing to write
        }

        @Override
        public void writeData(Connection connection) throws IOException {
            //nothing to write
        }

        @Override
        public int size() {
            return 0;
        }
    } 
    
    public class AdsReadStateResponse extends AdsResponse{
        private final static int ADSSTATESIZE = 2;

        protected AdsState     adsState;
        protected DeviceState  deviceState;

        public AdsReadStateResponse(){
            super();
            adsState    = AdsState.Undefined;
            deviceState = new DeviceState();
        }

        @Override
        public void readData(Connection connection) throws IOException {
            adsState = AdsState.getValue(connection.getInputStream().readShort());
            deviceState.read(connection);
        }

        @Override
        public int size(){
            return super.size() + ADSSTATESIZE + DeviceState.size();
        }
    }    
}
