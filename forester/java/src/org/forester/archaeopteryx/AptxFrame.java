package org.forester.archaeopteryx;



import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.util.EventListener;

import javax.swing.JMenuBar;

import org.forester.archaeopteryx.tools.InferenceManager;
import org.forester.archaeopteryx.tools.ProcessPool;

public interface AptxFrame {
    
    void addComponentListener(ComponentListener compListener);
    
    
    Container getThisFrame();


    void dispose();


    Container getContentPane();


    void repaint();


    void setSize( int x, int y );


    void addFrameListener(FrameListener frameListener);


    Container getJMenuBar();


    void setDefaultCloseOperation( int doNothingOnClose );


    Container getParent();


    void setVisible( boolean visible );


    boolean requestFocusInWindow();


    void setJMenuBar( JMenuBar jmenubar );


    void setLocationRelativeTo( Component component );


    void setTitle( String title );
    
    void validate();
}