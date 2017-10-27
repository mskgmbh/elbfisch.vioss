/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpac.vioss.ads;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author berndschuster
 */
public class AdsServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ServerSocket listener = null;
        int          nr       = 0;
        int          length   = 0;
        try {
            listener = new ServerSocket(0xBF02);
            while (true) {
                System.out.println("awaiting connection by client ...");                
                Socket socket = listener.accept();
                System.out.println("connected by client");
                try {
                    DataInputStream in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    nr = 0;
                    in.skip(2);
                    length = in.read();
                    in.skip(3);
                    while(in.available() > 0){
                        System.out.println(++nr + ": " + in.read());
                    }
                    if (length != nr){
                        System.out.println("length mismatch: expected: " + length + " actually received:" + nr);
                    }
                }
                catch(Exception exc){
                    System.out.println("Socket closed:" + exc);
                }
                finally {
                    socket.close();
                }
            }
        }
        catch(Exception exc){
            exc.printStackTrace();
        }
        finally {
            if (listener != null) try{listener.close();}catch(Exception exc){}
        }
        
    }
    
}
