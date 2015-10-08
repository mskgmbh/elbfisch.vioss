/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpac.vioss.test.ads;

import org.jpac.vioss.test.*;
import java.net.URI;
import org.jpac.hmi.DashboardLauncher;
import org.jpac.vioss.IOHandler;

/**
 *
 * @author berndschuster
 */
public class IoTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
       boolean handles = false;
       try{
            new MainModule().start();
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
