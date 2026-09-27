// forester -- software libraries and applications
// for evolutionary biology and genomics.
// Copyright (C) 2026 Christian M. Zmasek
// All rights reserved
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// Contact: czmasek at jcvi dot org

package org.forester.archaeopteryx;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;

public final class EmbeddedNodeDialogTest {

    public static void main( final String[] args ) throws Exception {
        SwingUtilities.invokeAndWait( () -> {
            try {
                for( final String method : new String[] { "addEmptyNode", "pasteSubtree", "deleteNodeOrSubtree",
                        "reRoot", "midpointRoot", "madRoot" } ) {
                    for( final String response : new String[] { "accept", "cancel", "replace", "close" } ) {
                        exercise( method, response );
                    }
                }
            }
            catch ( final Exception error ) {
                throw new AssertionError( error );
            }
        } );
        System.out.println( "EmbeddedNodeDialog: OK." );
    }

    private static void exercise( final String method, final String response ) throws Exception {
        final Phylogeny original = Phylogeny
                .createInstanceFromNhxString( "deleteNodeOrSubtree".equals( method ) ? "(A:1,B:2)R;"
                        : "(((A:1,B:1)ab:1,C:6)abc:1,(D:1,E:1)de:1);" );
        final MainFrameApplication viewer = MainFrameApplication
                .createEmbeddedInstance( new Phylogeny[] { original }, new Configuration(), "Node dialog owner" );
        final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
        final Method clipboard = TreePanel.class.getDeclaredMethod( "setCutOrCopiedTree", Phylogeny.class );
        clipboard.setAccessible( true );
        clipboard.invoke( panel, Phylogeny.createInstanceFromNhxString( "(X:1,Y:1);" ) );
        final String before = original.toNewHampshire();
        final String title = "addEmptyNode".equals( method ) ? "Addition of Empty New Node"
                : "pasteSubtree".equals( method ) ? "Paste Subtree"
                        : "deleteNodeOrSubtree".equals( method ) ? "Delete Node/Subtree" : "Re-root tree";
        final String accepted = method.endsWith( "Root" ) ? "Re-root"
                : "deleteNodeOrSubtree".equals( method ) ? "Node only" : "As descendant";
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final boolean[] answered = { false };
        final long deadline = System.nanoTime() + 10_000_000_000L;
        final Timer respond = new Timer( 20, event -> {
            for( final Window window : Window.getWindows() ) {
                if ( window instanceof JDialog && window.isShowing()
                        && title.equals( ( ( JDialog ) window ).getTitle() ) ) {
                    final JOptionPane pane = optionPane( ( Container ) window );
                    if ( pane != null ) {
                        ( ( Timer ) event.getSource() ).stop();
                        answered[ 0 ] = true;
                        try {
                            if ( "replace".equals( response ) ) {
                                panel.setTree( original.copy() );
                            }
                            else if ( "close".equals( response ) ) {
                                viewer.end();
                                if ( window.isDisplayable() ) {
                                    throw new AssertionError( "Viewer close left " + title + " open" );
                                }
                            }
                            pane.setValue( "cancel".equals( response ) ? "Cancel" : accepted );
                        }
                        catch ( final Throwable error ) {
                            failure.set( error );
                        }
                        finally {
                            window.dispose();
                        }
                        return;
                    }
                }
            }
            if ( System.nanoTime() > deadline ) {
                ( ( Timer ) event.getSource() ).stop();
                failure.set( new AssertionError( title + " did not appear" ) );
                for( final Window window : Window.getWindows() ) {
                    if ( window instanceof JDialog && title.equals( ( ( JDialog ) window ).getTitle() ) ) {
                        window.dispose();
                    }
                }
            }
        } );
        respond.start();
        try {
            if ( "midpointRoot".equals( method ) || "madRoot".equals( method ) ) {
                final Method action = TreePanel.class.getDeclaredMethod( method );
                action.setAccessible( true );
                action.invoke( panel );
            }
            else {
                final Method action = TreePanel.class.getDeclaredMethod( method, PhylogenyNode.class );
                action.setAccessible( true );
                action.invoke( panel, original.getNode( "deleteNodeOrSubtree".equals( method ) ? "A" : "C" ) );
            }
            if ( !answered[ 0 ] || failure.get() != null ) {
                throw new AssertionError( method + "/" + response, failure.get() );
            }
            if ( "accept".equals( response ) ) {
                if ( before.equals( original.toNewHampshire() ) || !panel.undo()
                        || !before.equals( panel.getPhylogeny().toNewHampshire() ) ) {
                    throw new AssertionError( method + " must apply one undoable edit" );
                }
            }
            else if ( !before.equals( original.toNewHampshire() )
                    || ( "replace".equals( response ) && !before.equals( panel.getPhylogeny().toNewHampshire() ) ) ) {
                throw new AssertionError( method + "/" + response + " changed a stale or cancelled tree" );
            }
        }
        finally {
            respond.stop();
            viewer.end();
        }
    }

    private static JOptionPane optionPane( final Container parent ) {
        for( final Component child : parent.getComponents() ) {
            if ( child instanceof JOptionPane ) {
                return ( JOptionPane ) child;
            }
            if ( child instanceof Container ) {
                final JOptionPane found = optionPane( ( Container ) child );
                if ( found != null ) {
                    return found;
                }
            }
        }
        return null;
    }
}
