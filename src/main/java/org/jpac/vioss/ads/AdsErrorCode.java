/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : AdsErrorCode.java (versatile input output subsystem)
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
public enum AdsErrorCode {
    Undefined                   (-1,"Undefined"),//added by Elbfisch
    ProtocolError               (-2,"protocol error encountered"),//added by Elbfisch
    SomeReadWritesFailed        (-3,"some AdsReadWrite operations failed while performing an AdsReadWriteMultiple operation"),//added by Elbfisch
    NoError                     ( 0,"No Error"),
    InternalError               ( 1,"Internal Error"),
    NoRTime                     ( 2,"No Rtime"),
    LockedMemoryError           ( 3,"Allocation locked memory error"),
    MailBoxError                ( 4,"Insert mailbox error"),
    WrongHMsg                   ( 5,"Wrong receive HMSG"),
    TargetPortNotFound          ( 6,"Target port not found"),
    TargetMachineNotFound       ( 7,"Target machine not found"),
    UnknownCommandID            ( 8,"Unknown command ID"),
    BadTaskID                   ( 9,"Bad task ID"),
    NoIO                        (10,"No IO"),
    UnknownAmsCommand           (11,"Unknown AMS command"),
    Win32Error                  (12,"Win 32 error"),
    PortNotConnected            (13,"Port is not connected"),
    InvalidAmsLength            (14,"Invalid AMS length"),
    InvalidAmsNetID             (15,"Invalid AMS Net ID"),
    LowInstallLevel             (16,"Low Installation level"),
    NoDebug                     (17,"No debug available"),
    PortDisabled                (18,"Port disabled"),
    PortConnected               (19,"Port is already connected"),
    AmsSyncWin32Error           (20,"AMS Sync Win32 error"),
    SyncTimeOut                 (21,"AMS Sync timeout"),
    AmsSyncAmsError             (22,"AMS Sync AMS error"),
    AmsSyncNoIndexMap           (23,"AMS Sync no index map"),
    InvalidAmsPort              (24,"Invalid AMS port"),
    NoMemory                    (25,"No memory"),
    TCPSendError                (26,"TCP send error"),
    HostUnreachable             (27,"Host unreachable"),
    NoLockedMemory              (1280,"Router: no locked memory"),
    MailboxFull                 (1282,"Router: mailbox full"),
    DeviceError                 (1792,"error class <device error>"),
    DeviceServiceNotSupported 	(1793,"Service is not supported by server"),
    DeviceInvalidGroup          (1794,"Invalid index group"),
    DeviceInvalidOffset 	(1795,"Invalid index offset"),
    DeviceInvalidAccess 	(1796,"Reading/writing not permitted"),
    DeviceInvalidSize           (1797,"Parameter size not correct"),
    DeviceInvalidData           (1798,"Invalid parameter value(s)"),
    DeviceNotReady              (1799,"Device is not in a ready state"),
    DeviceBusy                  (1800,"Device is busy"),
    DeviceInvalidContext 	(1801,"Invalid context (must be in Windows)"),
    DeviceNoMemory              (1802,"Out of memory"),
    DeviceInvalidParam          (1803,"Invalid parameter value(s)"),
    DeviceNotFound              (1804,"Not found(files, ...)"),
    DeviceSyntaxError           (1805,"Syntax error in command or file"),
    DeviceIncompatible          (1806,"Objects do not match"),
    DeviceExists                (1807,"Object already exists"),
    DeviceSymbolNotFound 	(1808,"Symbol not found"),
    DeviceSymbolVersionInvalid 	(1809,"Symbol version is invalid"),
    DeviceInvalidState          (1810,"Server is not i a valid state"),
    DeviceTransModeNotSupported (1811,"ADS transmode is not supported"),
    DeviceNotifyHandleInvalid 	(1812,"Notification handle is invalid"),
    DeviceClientUnknown 	(1813,"Notification client not registered"),
    DeviceNoMoreHandles 	(1814,"No more notification handles"),
    DeviceInvalidWatchsize 	(1815,"Size for watch to big"),
    DeviceNotInitialized 	(1816,"Device is not initialized"),
    DeviceTimeOut               (1817,"Device has a timeout"),
    DeviceNoInterface           (1818,"Query interface has failed"),
    DeviceInvalidInterface 	(1819,"Wrong interface required"),
    DeviceInvalidCLSID          (1820,"Class ID is invalid"),
    DeviceInvalidObjectID 	(1821,"Object ID is invalid"),
    ClientError                 (1856,"Error class <client error>"),
    ClientInvalidParameter 	(1857,"Parameter at service is invalid"),
    ClientListEmpty             (1858,"Polling list is empty"),
    ClientVaraiableInUse 	(1859,"Variable connection is already in use"),
    ClientDuplicateInvokeID 	(1860,"Invoke ID already in use"),
    ClientSyncTimeOut           (1861,"Timeout has elapsed"),
    ClientW32OR                 (1862,"Error in win32 subsystem"),
    ClientTimeoutInvalid 	(1863,"Timeout value is invalid"),
    ClientPortNotOpen           (1864,"Ads port is not opened"),
    ClientNoAmsAddr             (1865,"No AMS addr"),
    ClientSyncInternal          (1872,"An internal in ADS sync has occured"),
    ClientAddHash               (1873,"Hash table overflow"),
    ClientRemoveHash            (1874,"There are no more symbols in the hash table"),
    ClientNoMoreSymbols 	(1875,"There are no more symbols in cache"),
    ClientSyncResInvalid 	(1876,"An invalid response has been received"),
    ClientSyncPortLocked 	(1877,"Sync port is locked"),
    ClientQueueFull             (1878,"Client queue is full");    
     
    int    errorCode;
    String description;
    
    AdsErrorCode(int errorCode, String description){
       this.errorCode   = errorCode;
       this.description = description;
    }
    
    public void write(Connection connection) throws IOException{
        connection.getOutputStream().writeInt(errorCode);
    }
    
    public boolean equals(AdsErrorCode ec){
        return this.errorCode == ec.errorCode;
    }    
    
    public static int size(){
        return 4;
    }
    
    public int getErrorCode(){
        return this.errorCode;
    }
    
    public static AdsErrorCode getValue(int errorCode){
        boolean found = false;
        int     idx   = 0;
        AdsErrorCode[] ecs = AdsErrorCode.values();
        for(int i = 0; i < ecs.length && !found; i++){
            found = ecs[i].errorCode == errorCode;
            if (found){
                idx = i;
            }
        }
        return ecs[idx];
    }    
   
   @Override
   public String toString(){
       return super.toString() + "(" + errorCode + " ,'" + description + "')";
   }
}
