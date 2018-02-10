package org.forester.archaeopteryx;


import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.EventListener;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;


public class EmbeddedFrame extends JInternalFrame implements AptxFrame{

 
    @Override
    public Container getThisFrame() {
       return this;
    }

    @Override
    public void addFrameListener( FrameListener frameListener ) {
      InternalFrameListener actualListener = new InternalFrameListener() {

        @Override
        public void internalFrameActivated( InternalFrameEvent e ) {
            frameListener.FrameActivated( );
            
        }

        @Override
        public void internalFrameClosed( InternalFrameEvent e ) {
            frameListener.FrameClosed();
            
        }

        @Override
        public void internalFrameClosing( InternalFrameEvent e ) {
            frameListener.FrameClosing();
            
        }

        @Override
        public void internalFrameDeactivated( InternalFrameEvent e ) {
            frameListener.FrameDeactivated();
            
        }

        @Override
        public void internalFrameDeiconified( InternalFrameEvent e ) {
            frameListener.FrameDeiconified();
            
        }

        @Override
        public void internalFrameIconified( InternalFrameEvent e ) {
            frameListener.FrameIconified();
            
        }

        @Override
        public void internalFrameOpened( InternalFrameEvent e ) {
            frameListener.FrameOpened();
            
        }
          
      };
      this.addInternalFrameListener( actualListener );
       
    }

    @Override
    public void setLocationRelativeTo( Component component ) {
        //is this actually equivalent?
        this.setLocation( component.getX(), component.getY() );
        
    }


  

 

    


    
}
