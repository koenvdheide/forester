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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

    public static JMenuItem loadAlignmentItem(final MainFrame frame) {
        return frame._load_alignment_item;
    }

    public static JMenuItem exportSequencesItem(final MainFrame frame) {
        return frame._export_seqs_fasta_item;
    }

    public static JMenuItem epsItem(final MainFrame frame) {
        return frame._write_to_eps_item;
    }

    public static void addInternalPropertyRefs(final String... refs) {
        Collections.addAll(TreePanelUtil.HOST_INTERNAL_PROPERTY_REFS, refs);
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

    public static Point2D.Double logicalPoint(final TreePanel panel, final int x, final int y) {
        return panel.toLogicalPoint(x, y);
    }

    public static Rectangle2D.Double logicalVisibleRect(final TreePanel panel) {
        return panel.logicalVisibleRect();
    }

    public static Set<Long> selectedNodes(final TreePanel panel) {
        return panel.getFoundNodes0();
    }

    public static void setSelectedNodes(final TreePanel panel, final Set<Long> nodes) {
        panel.setFoundNodes0(nodes);
    }

    /** What the panel highlights: both search sets, without duplicates. */
    public static List<PhylogenyNode> foundNodes(final TreePanel panel) {
        return panel.getFoundNodesAsListOfPhylogenyNodes();
    }

    /** Re-reads the Color-by and Size-by candidates after the host added node data. */
    public static void refreshVisualizationCandidates(final TreePanel panel) {
        panel.rederiveVisualizationCandidates();
    }

    /** The colour a node is drawn in that is its own, or null (see TreePanel.nodeDisplayColor). */
    public static Color nodeDisplayColor(final TreePanel panel, final PhylogenyNode node) {
        return panel.nodeDisplayColor(node);
    }

    /** Fades the labels of the given tips like search non-matches; an empty set clears it. */
    public static void setDimmedTips(final TreePanel panel, final Set<Long> tip_ids) {
        panel.setDimmedTips(tip_ids);
    }

    public static boolean drawsDistancesToScale(final TreePanel panel) {
        return panel.drawsDistancesToScale();
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

    public static void setDomainLabelsLeftAligned(final TreePanel panel, final boolean left_aligned) {
        panel.setDomainLabelsLeftAligned(left_aligned);
    }

    public static void setFont(final MainFrame frame, final Font font) {
        frame.getOptions().setBaseFont(font);
        frame.getMainPanel().getTreeFontSet().setBaseFont(font);
    }

    public static void applyDisplayColours(final MainFrame frame) {
        final Configuration.UI ui = frame.getConfiguration().getUi();
        frame.getMainPanel().setTreeColorSet(TreeColorSet.createInstance(frame.getConfiguration(),
                ui == Configuration.UI.FLAT_LIGHT ? TreeColorSet.LIGHT_COLOR_SCHEME : TreeColorSet.DARK_COLOR_SCHEME));
        frame.updateTreeCanvasColors(ui);
    }

    public static void setFoundColors(final MainFrame frame, final Color first, final Color second, final Color both) {
        frame.getMainPanel().getTreeColorSet().setFoundColors(first, second, both);
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

    public static void setPulseFoundNodes(final MainFrame frame, final boolean pulse) {
        frame.getOptions().setPulseFoundNodes(pulse);
    }

    public static void setDimNonMatches(final MainFrame frame, final boolean dim) {
        frame.getOptions().setDimNonMatches(dim);
    }

    public static void setAutoColorNewTrees(final MainFrame frame, final boolean auto) {
        frame.getOptions().setAutoColorNewTrees(auto);
    }

    public static void setUseItalicScientificNames(final MainFrame frame, final boolean italic) {
        frame.getOptions().setUseItalicScientificNames(italic);
    }

    public static void setShowTreeName(final MainFrame frame, final boolean show) {
        frame.getOptions().setShowTreeName(show);
    }

    public static void setInternalLabelsAboveBranch(final MainFrame frame, final boolean above) {
        frame.getOptions().setInternalLabelsAboveBranch(above);
    }

    public static void setOutlineFontsInVectorExport(final MainFrame frame, final boolean outline) {
        frame.getOptions().setOutlineFontsInVectorExport(outline);
    }

    public static void setShowMsa(final TreePanel panel, final boolean show) {
        panel.getOptions().setShowMsa(show);
    }
}
