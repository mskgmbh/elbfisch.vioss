/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpac.vioss.test;

import java.net.URI;
import org.jpac.hmi.DashboardLauncher;
import org.jpac.vioss.IOHandler;

/**
 *
 * @author berndschuster
 */
public class LogicalTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
       boolean handles = false;
       try{
//           URI uri = new URI("modbus://localhost:1003/coils/1234");
//           IOHandler ioh = org.jpac.vioss.IOHandlerFactory.getHandlerFor(uri);
//           handles = ioh.handles(new URI("modbus://localhost:1003/coils/1234"));
//           handles = ioh.handles(new URI("modbus://127.0.0.1:1003/coils/1234"));
//           handles = ioh.handles(new URI("modbus://192.168.99.4:1003/coils/1234"));
//           handles = ioh.handles(new URI("modbusX://localhost:1003/coils/1234"));
//           handles = ioh.handles(new URI("modbus://192.168.0.127:1003/coils/1234"));
//           handles = ioh.handles(new URI("modbus://localhost:1004/coils/1234"));
//           handles = ioh.handles(new URI("modbus://localhost:1003/inputdiscretes/1234"));
//           handles = ioh.handles(new URI("modbus://localhost:1003/coils/4321"));
        new TestModule().start();
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                DashboardLauncher dialog = new DashboardLauncher(new java.awt.Frame(), false);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });        
           
       }
       catch(Exception exc){
           exc.printStackTrace();
       }
    }
}
