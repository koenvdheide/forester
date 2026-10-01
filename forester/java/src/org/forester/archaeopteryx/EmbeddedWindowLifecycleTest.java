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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.forester.io.parsers.nhx.NHXParser;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;

public final class EmbeddedWindowLifecycleTest {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrameApplication first = null;
            MainFrameApplication second = null;
            TanglegramFrame firstComparison = null;
            TanglegramFrame secondComparison = null;
            try {
                first = viewer();
                second = viewer();
                firstComparison = openComparison(first);
                secondComparison = openComparison(second);
                first.end();
                if (firstComparison.isDisplayable()) {
                    throw new AssertionError("Closing a viewer left its tanglegram open");
                }
                if (!secondComparison.isDisplayable()) {
                    throw new AssertionError("Closing one viewer closed another viewer's tanglegram");
                }
                second.end();
                if (secondComparison.isDisplayable()) {
                    throw new AssertionError("Closing the second viewer left its tanglegram open");
                }
            } catch (final Exception e) {
                throw new AssertionError(e);
            } finally {
                if (firstComparison != null) {
                    firstComparison.dispose();
                }
                if (secondComparison != null) {
                    secondComparison.dispose();
                }
                if (first != null) {
                    first.end();
                }
                if (second != null) {
                    second.end();
                }
            }
        });
        checkSettingsOwnership();
        System.out.println("EmbeddedWindowLifecycle: OK.");
    }

    private static void checkSettingsOwnership() throws Exception {
        final MainFrameApplication[] viewers = new MainFrameApplication[3];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    for (int i = 0; i < viewers.length; ++i) {
                        viewers[i] = viewer();
                        for (final javax.swing.event.MenuListener listener : viewers[i]._settings_jmenu.getMenuListeners()) {
                            listener.menuSelected(new javax.swing.event.MenuEvent(viewers[i]._settings_jmenu));
                        }
                    }
                    viewers[2].end();
                } catch (final Exception e) {
                    throw new AssertionError(e);
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                if (!viewers[0]._open_settings_dialog.isDisplayable()
                        || !viewers[1]._open_settings_dialog.isDisplayable()) {
                    throw new AssertionError("The Settings menu did not open both dialogs");
                }
                viewers[0].end();
                if (viewers[0]._open_settings_dialog.isDisplayable()) {
                    throw new AssertionError("Closing a viewer left its Settings dialog open");
                }
                if (!viewers[1]._open_settings_dialog.isDisplayable()) {
                    throw new AssertionError("Closing a viewer closed another viewer's Settings");
                }
                if (viewers[2]._open_settings_dialog != null && viewers[2]._open_settings_dialog.isDisplayable()) {
                    throw new AssertionError("A queued Settings callback reopened an ended viewer");
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (final MainFrameApplication viewer : viewers) {
                    if (viewer != null) {
                        if (viewer._open_settings_dialog != null) {
                            viewer._open_settings_dialog.dispose();
                        }
                        viewer.end();
                    }
                }
            });
        }
    }

    private static MainFrameApplication viewer() throws Exception {
        final Phylogeny tree = ParserBasedPhylogenyFactory.getInstance()
                .create("(A:1,B:2)R;", new NHXParser())[0];
        return MainFrameApplication.createEmbeddedInstance(new Phylogeny[] { tree, tree.copy() },
                new Configuration(), "Comparison owner");
    }

    private static TanglegramFrame openComparison(final MainFrameApplication viewer) {
        final Set<Window> before = new HashSet<>(Arrays.asList(Window.getWindows()));
        final long deadline = System.nanoTime() + 10_000_000_000L;
        final Timer confirm = new Timer(50, event -> {
            for (final Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()
                        && "Create Tanglegram".equals(((JDialog) window).getTitle())) {
                    final JButton ok = okButton((Container) window);
                    if (ok != null) {
                        ((Timer) event.getSource()).stop();
                        ok.doClick();
                        return;
                    }
                }
            }
            if (System.nanoTime() > deadline) {
                ((Timer) event.getSource()).stop();
                for (final Window window : Window.getWindows()) {
                    if (!before.contains(window) && window instanceof JDialog) {
                        window.dispose();
                    }
                }
            }
        });
        confirm.start();
        try {
            viewer.createTanglegram();
        } finally {
            confirm.stop();
        }
        for (final Window window : Window.getWindows()) {
            if (!before.contains(window) && window instanceof TanglegramFrame && window.isDisplayable()) {
                return (TanglegramFrame) window;
            }
        }
        throw new AssertionError("The real Create Tanglegram dialog did not open a comparison");
    }

    private static JButton okButton(final Container parent) {
        for (final Component child : parent.getComponents()) {
            if (child instanceof JButton && "OK".equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                final JButton found = okButton((Container) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
