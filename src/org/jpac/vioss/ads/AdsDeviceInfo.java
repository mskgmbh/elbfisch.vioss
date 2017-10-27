/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsRead.java (versatile input output subsystem)
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
public class AdsDeviceInfo extends AmsPacket{
    
    protected AdsDeviceInfoRequest  adsDeviceInfoRequest;
    protected AdsDeviceInfoResponse adsDeviceInfoResponse;
    
    protected int                   majorVersion;
    protected int                   minorVersion;
    protected int                   versionBuild;
    protected String                deviceName;        
        
    public AdsDeviceInfo(){
        super();
    }
    
    @Override
    public AdsDeviceInfoRequest getAdsRequest() {
        if (adsDeviceInfoRequest == null){
            adsDeviceInfoRequest = new AdsDeviceInfoRequest();
        }
        return adsDeviceInfoRequest;
    }

    @Override
    public AdsDeviceInfoResponse getAdsResponse() {
        if (adsDeviceInfoResponse == null){
            adsDeviceInfoResponse = new AdsDeviceInfoResponse();
        }
        return adsDeviceInfoResponse;
    }

    /**
     * @return the majorVersion
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * @return the minorVersion
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * @return the versionBuild
     */
    public int getVersionBuild() {
        return versionBuild;
    }

    /**
     * @return the deviceName
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    @Override
    public String toString(){
       return getClass().getSimpleName() + "(" + majorVersion + ", " + minorVersion + ", " + versionBuild + ", '" + deviceName + "')";
    }        

    public class AdsDeviceInfoRequest extends AdsRequest{

        public AdsDeviceInfoRequest(){
            super(CommandId.AdsReadDeviceInfo);
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
        public void write(Connection connection) throws IOException {
            //nothing to write
        }

        @Override
        public int size() {
           return 0;
        }
    } 
    
    public class AdsDeviceInfoResponse extends AdsResponse{
        private final static int MAJORVERSIONSIZE = 1;
        private final static int MINORVERSIONSIZE = 1;
        private final static int VERSIONBUILDSIZE = 2;
        private final static int DEVICENAMESIZE   = 16;

        public AdsDeviceInfoResponse(){
            super();
        }

        @Override
        public void readData(Connection connection) throws IOException {
            byte[] dn = new byte[16];            
            majorVersion = 0xff   & connection.getInputStream().read();
            minorVersion = 0xff   & connection.getInputStream().read();
            versionBuild = 0xffff & connection.getInputStream().readShort();
            StringBuilder sb = new StringBuilder();
            connection.getInputStream().read(dn, 0, dn.length);
            for (int i = 0; i < dn.length && dn[i] != 0; i++){
                sb.append((char)dn[i]);
            }
            deviceName = sb.toString();
            if (Log.isDebugEnabled())Log.debug("  " + this);            
        }
        
        @Override
        public int size(){
            return super.size() + MAJORVERSIONSIZE + MINORVERSIONSIZE + VERSIONBUILDSIZE + DEVICENAMESIZE;
        }
    }  
}
