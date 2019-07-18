/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : RequestSet.java
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

package org.jpac.vioss.s7;

import org.jpac.plc.WrongOrdinaryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import org.jpac.plc.Request;
import org.jpac.plc.TooManyRequestsException;

/**
 * represents a set of requests handled be the {@link ReadMultipleData} or {@link WriteMultipleData}
 */
class RequestSet{
	static Logger Log = LoggerFactory.getLogger("jpac.plc.s7");

    private final int MAXSIZE = 255; //maximum number of requests per PDU

    private ArrayList<Request> requests;

    boolean debug;

    public RequestSet(){
        requests = new ArrayList<Request>();
        this.debug = false;
    }
    /**
     * used to delete all requests contained inside the request set
     */
    void clear(){
        requests.clear();
    }

    /**
     * used to add a request to the request set
     * @param request
     * @throws TooManyRequestsException thrown, if the overall size of the request would exceed the user data space inside
     * a pdu, if the given request would be added.
     */
    void add(org.jpac.plc.Request request) throws TooManyRequestsException{
        if (requests.size() > MAXSIZE){
            throw new TooManyRequestsException("try to send more than 255 requests inside one PDU");
        }
        requests.add(request);
    }

    /**
     * used to remove the given request from the request set
     * @param request
     */
    void remove(Request request){
        requests.remove(request);
    }
    /**
     * @return the number of requests stored inside the request set
     */
    int size(){
        return requests.size();
    }
    /**
     * @return the number of bytes consumed by the request set while being streamed out
     */
    int getSendParameterLength(){
        int len = 0;
        for(int i = 0; i < requests.size(); i++){
            len += requests.get(i).getSendParameterLength();
        }
        return len;
    }
    /**
     * @return the number of bytes consumed by the request set while being streamed out
     */
    int getReceiveParameterLength(){
        int len = 0;
        for(int i = 0; i < requests.size(); i++){
            len += requests.get(i).getReceiveParameterLength();
        }
        return len;
    }
    /**
     * @return the number of bytes consumed by the request set while being streamed out
     */
    int getSendDataLength(){
        int len = 0;
        for(int i = 0; i < requests.size(); i++){
            len += requests.get(i).getSendDataLength();
        }
        return len;
    }
    /**
     * @return the number of bytes consumed by the request set while being streamed out
     */
    int getReceiveDataLength(){
        int len = 0;
        for(int i = 0; i < requests.size(); i++){
            len += requests.get(i).getReceiveDataLength();
        }
        return len;
    }
    /**
     * used to read the replies of the plc according to the requests, sent to it.
     * @param conn
     * @throws IOException
     * @throws WrongOrdinaryException
     */
    void read(Connection conn) throws IOException, WrongOrdinaryException{
        if (debug) Log.debug("     reading request records ...");
        for(int i = 0; i < requests.size(); i++){
            requests.get(i).read(conn);
        }
        if (debug) Log.debug("     request records read");
    }

    /**
     * used to write the set of requests to the plc as part of a ISO data packet
     * @param conn
     * @throws IOException
     */
    void write(Connection conn) throws IOException{
        for(int i = 0; i < requests.size(); i++){
            requests.get(i).write(conn);
        }
    }

    /**
     * used to write the data portion of the contained requests  to the plc as part of a ISO data packet
     * @param conn
     * @throws IOException
     */
    void writeData(Connection conn) throws IOException{
        for(int i = 0; i < requests.size(); i++){
            requests.get(i).writeData(conn);
        }
    }

    /**
     * used to switch on/off debugging
     * @param debug
     */
    void setDebug(boolean debug){
        this.debug = debug;
    }
    boolean isDebug(){
        return this.debug;
    }
}
