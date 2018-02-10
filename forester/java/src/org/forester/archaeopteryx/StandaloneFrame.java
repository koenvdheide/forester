package org.forester.archaeopteryx;


import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;


public class StandaloneFrame extends JFrame implements AptxFrame{
    
    @Override
    public Container getThisFrame() {
    return this;
    }

    @Override
    public void addFrameListener( FrameListener frameListener ) {
        WindowListener actualListener = new WindowListener() {

            @Override
            public void windowActivated( WindowEvent e ) {
                frameListener.FrameActivated();
                
            }

            @Override
            public void windowClosed( WindowEvent e ) {
                frameListener.FrameClosed();
                
            }

            @Override
            public void windowClosing( WindowEvent e ) {
                frameListener.FrameClosing();
                
            }

            @Override
            public void windowDeactivated( WindowEvent e ) {
               frameListener.FrameDeactivated();
                
            }

            @Override
            public void windowDeiconified( WindowEvent e ) {
                frameListener.FrameDeiconified();
                
            }

            @Override
            public void windowIconified( WindowEvent e ) {
                frameListener.FrameIconified();
                
            }

            @Override
            public void windowOpened( WindowEvent e ) {
                frameListener.FrameOpened();
                
            }
            
        };
        this.addWindowListener( actualListener );
        
        
    }
  
    
}
