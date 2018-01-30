package org.forester.archaeopteryx;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.forester.archaeopteryx.tools.InferenceManager;
import org.forester.archaeopteryx.tools.ProcessPool;

public interface AptxFrame extends ActionListener {

    NHFilter            nhfilter           = new NHFilter();
    NHXFilter           nhxfilter          = new NHXFilter();
    XMLFilter           xmlfilter          = new XMLFilter();
    TolFilter           tolfilter          = new TolFilter();
    NexusFilter         nexusfilter        = new NexusFilter();
    PdfFilter           pdffilter          = new PdfFilter();
    GraphicsFileFilter  graphicsfilefilter = new GraphicsFileFilter();
    MsaFileFilter       msafilter          = new MsaFileFilter();
    SequencesFileFilter seqsfilter         = new SequencesFileFilter();
    DefaultFilter       defaultfilter      = new DefaultFilter();

    /**
     * Action performed.
     */
    void actionPerformed( ActionEvent e );

    Configuration getConfiguration();

    /**
     * This method returns the current external node data which
     * has been selected by the user by clicking the "Return ..."
     * menu item. This method is expected to be called from Javascript or
     * something like it.
     *
     * @return current external node data as String
     */
    String getCurrentExternalNodesDataBuffer();

    int getCurrentExternalNodesDataBufferChangeCounter();

    int getCurrentExternalNodesDataBufferLength();

    InferenceManager getInferenceManager();

    MainPanel getMainPanel();

    Options getOptions();

    ProcessPool getProcessPool();

    void showTextFrame( String s, String title );

    void showWhole();

    void updateProcessMenu();

    void activateSaveAllIfNeeded();
}