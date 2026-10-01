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

import org.forester.io.parsers.nhx.NHXParser;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;

public final class EmbeddedDeletionDialogTest {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                exercise("accept", (viewer, pane) -> pane.setValue("Node only"));
                exercise("cancel", (viewer, pane) -> pane.setValue("Cancel"));
                exercise("close", (viewer, pane) -> {
                    final Window dialog = SwingUtilities.getWindowAncestor(pane);
                    viewer.end();
                    if (dialog.isDisplayable()) {
                        throw new AssertionError("Closing the viewer left its delete confirmation open");
                    }
                    pane.setValue("Node only");
                });
                exercise("replace", (viewer, pane) -> {
                    final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
                    panel.setTree(panel.getPhylogeny().copy());
                    pane.setValue("Node only");
                });
            } catch (final Exception e) {
                throw new AssertionError(e);
            }
        });
        System.out.println("EmbeddedDeletionDialog: OK.");
    }

    private static void exercise(final String operation,
                                 final java.util.function.BiConsumer<MainFrameApplication, JOptionPane> action)
            throws Exception {
        final Phylogeny original = ParserBasedPhylogenyFactory.getInstance()
                .create("(A:1,B:2)R;", new NHXParser())[0];
        final MainFrameApplication viewer = MainFrameApplication.createEmbeddedInstance(
                new Phylogeny[] {original}, new Configuration(), "Delete owner");
        final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final boolean[] answered = {false};
        final long deadline = System.nanoTime() + 10_000_000_000L;
        final Timer respond = new Timer(20, event -> {
            for (final Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()
                        && "Delete Node/Subtree".equals(((JDialog) window).getTitle())) {
                    final JOptionPane pane = optionPane((Container) window);
                    if (pane != null) {
                        ((Timer) event.getSource()).stop();
                        answered[0] = true;
                        try {
                            action.accept(viewer, pane);
                        } catch (final Throwable error) {
                            failure.set(error);
                        } finally {
                            window.dispose();
                        }
                        return;
                    }
                }
            }
            if (System.nanoTime() > deadline) {
                ((Timer) event.getSource()).stop();
                failure.set(new AssertionError("Delete confirmation did not appear"));
                for (final Window window : Window.getWindows()) {
                    if (window instanceof JDialog
                            && "Delete Node/Subtree".equals(((JDialog) window).getTitle())) {
                        window.dispose();
                    }
                }
                viewer.end();
            }
        });
        respond.start();
        try {
            final Method delete = TreePanel.class.getDeclaredMethod("deleteNodeOrSubtree", PhylogenyNode.class);
            delete.setAccessible(true);
            delete.invoke(panel, original.getNode("A"));
            if (!answered[0]) {
                throw new AssertionError("Desktop deletion returned before its modal response");
            }
            if (failure.get() != null) {
                throw new AssertionError(operation, failure.get());
            }
            final int expected = "accept".equals(operation) ? 1 : 2;
            if (original.getNumberOfExternalNodes() != expected) {
                throw new AssertionError(operation + " changed the wrong tree");
            }
            if ("accept".equals(operation)
                    && (!panel.undo() || panel.getPhylogeny().getNumberOfExternalNodes() != 2)) {
                throw new AssertionError("Confirmed deletion could not be undone");
            }
            if ("replace".equals(operation) && panel.getPhylogeny().getNumberOfExternalNodes() != 2) {
                throw new AssertionError("A stale confirmation changed the replacement tree");
            }
        } finally {
            respond.stop();
            viewer.end();
        }
    }

    private static JOptionPane optionPane(final Container parent) {
        for (final Component child : parent.getComponents()) {
            if (child instanceof JOptionPane) {
                return (JOptionPane) child;
            }
            if (child instanceof Container) {
                final JOptionPane found = optionPane((Container) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
