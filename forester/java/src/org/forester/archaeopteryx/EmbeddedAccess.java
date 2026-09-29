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

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.forester.archaeopteryx.tools.NodeDataImporter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.NodeVisualData.NodeShape;

/** Typed access to the controls and display settings used by an embedding host. */
public final class EmbeddedAccess {

    public static final FileFilter NH_FILTER = MainFrame.nhfilter;
    public static final FileFilter NHX_FILTER = MainFrame.nhxfilter;
    public static final FileFilter XML_FILTER = MainFrame.xmlfilter;
    public static final FileFilter TOL_FILTER = MainFrame.tolfilter;
    public static final FileFilter NEXUS_FILTER = MainFrame.nexusfilter;
    public static final FileFilter DEFAULT_FILTER = MainFrame.defaultfilter;

    private EmbeddedAccess() {
    }

    public static JMenu viewMenu(final MainFrame frame) {
        return frame._view_jmenu;
    }

    public static JMenuItem exitItem(final MainFrame frame) {
        return frame._exit_item;
    }

    public static JMenuItem openItem(final MainFrame frame) {
        return frame._open_item;
    }

    public static JMenuItem newItem(final MainFrame frame) {
        return frame._new_item;
    }

    public static JMenuItem epsItem(final MainFrame frame) {
        return frame._write_to_eps_item;
    }

    public static void showWhole(final MainPanel main) {
        main.getControlPanel().showWhole();
    }

    public static List<TreePanel> treePanels(final MainPanel main) {
        return new ArrayList<>(main.getTreePanels());
    }

    /** Captures an EDT-owned view onto the host's detached tree copy. */
    public static void copyViewState(final TreePanel source, final Phylogeny target) {
        FigureSpec.writeToTree(target, FigureSpec.capture(source));
        final TimeAxisConfig time = source.currentTimeAxisConfig();
        TimeAxisConfig.writeToTree(target, time.isDefault() ? null : time);
        NodeDataImporter.writeProfileToTree(target, source.getLastImportProfile());
    }

    public static void activateSaveAll(final MainFrame frame) {
        frame.activateSaveAllIfNeeded();
    }

    public static File treeFile(final TreePanel panel) {
        return panel.getTreeFile();
    }

    public static void setTreeFile(final TreePanel panel, final File file) {
        panel.setTreeFile(file);
    }

    public static PhylogenyNode findNode(final TreePanel panel, final int x, final int y) {
        return panel.findNode(x, y);
    }

    public static Set<Long> selectedNodes(final TreePanel panel) {
        return panel.getFoundNodes0();
    }

    public static void setSelectedNodes(final TreePanel panel, final Set<Long> nodes) {
        panel.setFoundNodes0(nodes);
    }

    public static boolean isSubtree(final TreePanel panel) {
        return panel.isCurrentTreeIsSubtree();
    }

    public static void selectNodesOnClick(final MainPanel main) {
        main.getControlPanel().selectNodesOnClick();
    }

    public static void setDisplayOption(final TreePanel panel, final DisplayOption option, final boolean show) {
        panel.setShows(option, show);
        if (panel.getMainPanel().getCurrentTreePanel() == panel) {
            panel.getControlPanel().reseedDisplayDataFromCurrentTab();
        }
    }

    public static boolean shows(final TreePanel panel, final DisplayOption option) {
        return panel.shows(option);
    }

    public static void setDomainStructuresFollowTreeZoom(final TreePanel panel, final boolean follow) {
        panel.setDomainStructuresFollowTreeZoom(follow);
    }

    public static void setFont(final MainFrame frame, final Font font) {
        frame.getOptions().setBaseFont(font);
        frame.getMainPanel().getTreeFontSet().setBaseFont(font);
    }

    public static void applyDisplayColours(final MainFrame frame) {
        frame.getMainPanel().setTreeColorSet(TreeColorSet.createInstance(frame.getConfiguration()));
    }

    public static void setMinConfidenceFraction(final MainFrame frame, final double fraction) {
        frame.getOptions().setMinConfidenceFraction(fraction);
    }

    public static void setCladogramType(final MainFrame frame, final Options.CLADOGRAM_TYPE type) {
        frame.getOptions().setCladogramType(type);
    }

    public static void setNodeShape(final MainFrame frame, final NodeShape shape) {
        frame.getOptions().setDefaultNodeShape(shape);
    }

    public static void setShowInternalNodeShapes(final MainFrame frame, final boolean show) {
        frame.getOptions().setShowDefaultNodeShapesInternal(show);
    }

    public static void setShowExternalNodeShapes(final MainFrame frame, final boolean show) {
        frame.getOptions().setShowDefaultNodeShapesExternal(show);
    }

    public static void setShowMarkedNodeShapes(final MainFrame frame, final boolean show) {
        frame.getOptions().setShowDefaultNodeShapesForMarkedNodes(show);
    }

    public static void setShowScale(final MainFrame frame, final boolean show) {
        frame.getOptions().setShowScale(show);
    }

    public static void setOverviewPlacement(final MainFrame frame, final Options.OVERVIEW_PLACEMENT_TYPE placement) {
        frame.getOptions().setOvPlacement(placement);
    }
}
