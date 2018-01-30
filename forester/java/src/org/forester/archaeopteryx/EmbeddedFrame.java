package org.forester.archaeopteryx;


import java.awt.event.ActionEvent;

import javax.swing.JInternalFrame;

import org.forester.archaeopteryx.tools.InferenceManager;
import org.forester.archaeopteryx.tools.ProcessPool;

public class EmbeddedFrame extends JInternalFrame implements AptxFrame{
    
    MainFrame aptx;
    
    public EmbeddedFrame(MainFrame aptxInstance) {
        aptx = aptxInstance;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        aptx.actionPerformed( e );
        
    }

    @Override
    public Configuration getConfiguration() {
        return aptx.getConfiguration();
    }

    @Override
    public String getCurrentExternalNodesDataBuffer() {
        return aptx.getCurrentExternalNodesDataBuffer();
    }

    @Override
    public int getCurrentExternalNodesDataBufferChangeCounter() {
        return aptx.getCurrentExternalNodesDataBufferChangeCounter();
    }

    @Override
    public int getCurrentExternalNodesDataBufferLength() {
        return aptx.getCurrentExternalNodesDataBufferLength();
    }

    @Override
    public InferenceManager getInferenceManager() {
        return aptx.getInferenceManager();
    }

    @Override
    public MainPanel getMainPanel() {
        return aptx.getMainPanel();
    }

    @Override
    public Options getOptions() {
        return aptx.getOptions();
    }

    @Override
    public ProcessPool getProcessPool() {
        return aptx.getProcessPool();
    }

    @Override
    public void showTextFrame( String s, String title ) {
        aptx.showTextFrame( s, title );
        
    }

    @Override
    public void showWhole() {
        aptx.showWhole();
        
    }

    @Override
    public void updateProcessMenu() {
        aptx.updateProcessMenu();
        
    }

    @Override
    public void activateSaveAllIfNeeded() {
        aptx.activateSaveAllIfNeeded();
        
    }

    
}
