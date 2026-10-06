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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.io.writers.PhylogenyWriter;

public final class EmbeddedTreeLifecycleTest {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                deleteSingleChildRoot();
                enrichmentAfterNavigation();
                collapsedCladeNavigation();
                propertyCandidatesAfterSubtreeUndo();
                pasteAtDisplayedRoot();
                rerootFromCompleteTree();
                subtreeHistoryAndSaving();
                nestedSubtrees();
                collapsedRootKeepsBranchLength();
                completedChanges();
                completedTabChanges();
                cladogramIsNotToScale();
            } catch (final Exception e) {
                throw new AssertionError(e);
            }
        });
        System.out.println("EmbeddedTreeLifecycle: OK.");
    }


    private static void deleteSingleChildRoot() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:2,B:3)X:4,C:5)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            panel.subTree(tree.getNode("X"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), true);
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getRoot(), true);
            check(complete(panel).getNode("A").getDistanceToParent() == 6,
                    "Deleting a single-child root must add the incoming length only once");
            panel.undo();
            check(complete(panel).getNode("X").getDistanceToParent() == 4, "Undo must restore the clade edge");
            panel.redo();
            check(complete(panel).getNode("A").getDistanceToParent() == 6, "Redo must retain the promoted edge");
            assertSnapshot(panel, "A", "C");
            panel.superTree();
            assertParents(panel.getPhylogeny());
        } finally {
            frame.end();
        }
    }

    private static void enrichmentAfterNavigation() throws Exception {
        for (final boolean ancestors : new boolean[] { false, true }) {
            final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
            final MainFrameApplication frame = embedded(tree);
            try {
                final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
                final Phylogeny result = tree.copy();
                panel.subTree(tree.getNode("X"));
                final List<TreeChangeListener.Kind> changes = new ArrayList<>();
                panel.setTreeChangeListener((source, kind) -> changes.add(kind));
                if (ancestors) {
                    new org.forester.archaeopteryx.tools.AncestralTaxonomyInferrer(frame, panel, result, false).commit(1);
                } else {
                    new org.forester.archaeopteryx.tools.SequenceAndTaxonomyDataObtainer(frame, panel, result).commit(
                            new org.forester.ws.seqdb.SequenceTaxonomyResolver.Result(
                                    new java.util.TreeSet<String>(), 1, 0, false, null));
                }
                check(!panel.isCurrentTreeIsSubtree(), "A complete-tree result must return to the complete view");
                check(panel.getPhylogeny() == result, "The completed result must be installed as the complete tree");
                check(changes.equals(Arrays.asList(TreeChangeListener.Kind.NAVIGATION, TreeChangeListener.Kind.HISTORY_RESTORE)),
                        "Returning and installing a result must report the actual completed operations");
                assertSnapshot(panel, "A", "B", "D", "C");
                check(panel.undo(), "The enrichment must remain undoable");
                assertSnapshot(panel, "A", "B", "D", "C");
            } finally {
                frame.end();
            }
        }
    }

    private static void collapsedCladeNavigation() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            tree.getNode("X").setCollapse(true);
            panel.subTree(tree.getNode("X"));
            check(!panel.getPhylogeny().getRoot().isCollapse(), "The displayed subtree must be expanded");
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            panel.undo();
            panel.superTree();
            check(panel.getPhylogeny().getNode("X").isCollapse(), "Returning must restore the original collapse flag");
            panel.subTree(panel.getPhylogeny().getNode("X"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("D"), false);
            panel.superTree();
            check(!panel.getPhylogeny().getNode("A").isCollapse(), "A promoted leaf must not inherit clade collapse");
        } finally {
            frame.end();
        }
    }

    private static void propertyCandidatesAfterSubtreeUndo() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
        for (final PhylogenyNode node : tree.getExternalNodes()) {
            final org.forester.phylogeny.data.PropertiesList properties = new org.forester.phylogeny.data.PropertiesList();
            properties.addProperty(new org.forester.phylogeny.data.Property("test:host",
                    "B".equals(node.getName()) ? "dog" : "cat", "", "xsd:string",
                    org.forester.phylogeny.data.Property.AppliesTo.NODE));
            node.getNodeData().setProperties(properties);
        }
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            check(panel.visualizationCandidate("test:host") != null, "Two host values must offer a candidate");
            panel.setColorByPropertyRef("test:host");
            panel.subTree(tree.getNode("X"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            check(panel.visualizationCandidate("test:host")._kept, "One value must retain only the chosen field");
            panel.undo();
            check(panel.visualizationCandidate("test:host") != null && !panel.visualizationCandidate("test:host")._kept,
                    "Undo must derive candidates from the restored complete topology");
        } finally {
            frame.end();
        }
    }

    private static void pasteAtDisplayedRoot() throws Exception {
        for (final boolean navigated : new boolean[] { false, true }) {
            final Phylogeny tree = Phylogeny.createInstanceFromNhxString(navigated ? "((A:2)X:4,C:5)R;" : "A:6;");
            final MainFrameApplication frame = embedded(tree);
            try {
                final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
                if (navigated) {
                    panel.subTree(tree.getNode("X"));
                    panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getRoot(), true);
                }
                final List<TreeChangeListener.Kind> changes = new ArrayList<>();
                panel.setTreeChangeListener((source, kind) -> changes.add(kind));
                frame.getMainPanel().setCutOrCopiedTree(Phylogeny.createInstanceFromNhxString("(P:1,Q:2)Paste;"));
                panel.pasteSubtreeConfirmed(panel.getPhylogeny().getRoot(), false);
                check(changes.equals(Arrays.asList(navigated ? TreeChangeListener.Kind.EDIT
                        : TreeChangeListener.Kind.REPLACE)), "Root paste must distinguish subtree edit from replacement");
                if (navigated) {
                    check(complete(panel).getNode("Paste").getDistanceToParent() == 6,
                            "Subtree root paste must keep the incoming edge");
                    assertSnapshot(panel, "P", "Q", "C");
                } else {
                    assertSnapshot(panel, "P", "Q");
                }
                check(panel.undo(), "Root paste must be undoable");
                if (navigated) {
                    assertSnapshot(panel, "A", "C");
                } else {
                    assertSnapshot(panel, "A");
                }
            } finally {
                frame.end();
            }
        }
    }

    private static void rerootFromCompleteTree() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
        final MainFrameApplication frame = MainFrameApplication.createEmbeddedInstance(new Phylogeny[] { tree },
                new Configuration(), "Subtree rooting");
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            if (panel.rerootRefusal() != null) {
                throw new AssertionError("The complete tree should be rerootable");
            }
            panel.subTree(tree.getNode("X"));
            final String why = panel.rerootRefusal();
            if (why == null || frame._midpoint_root_item.isEnabled() || frame._mad_root_item.isEnabled()
                    || panel.getControlPanel().clickToEntryForTest(
                            panel.getControlPanel().rerootClickToIndexForTest()).isEnabled()) {
                throw new AssertionError("Subtree rerooting must be refused by actions and controls");
            }
            final List<String> messages = new ArrayList<>();
            panel.setRerootDialogsForTest(messages::add, warning -> true);
            final String before = panel.getPhylogeny().toNewHampshire();
            panel.midpointRoot();
            panel.madRoot();
            panel.reRoot(panel.getPhylogeny().getNode("B"));
            if (messages.size() != 3 || !messages.stream().allMatch(why::equals)
                    || panel.canUndo() || !before.equals(panel.getPhylogeny().toNewHampshire())) {
                throw new AssertionError("Refused rerooting must leave the subtree and history unchanged");
            }
            panel.superTree();
            if (panel.rerootRefusal() != null || !frame._midpoint_root_item.isEnabled()
                    || !frame._mad_root_item.isEnabled()) {
                throw new AssertionError("Returning to the complete tree must restore rooting controls");
            }
        } finally {
            frame.end();
        }
    }
    private static void subtreeHistoryAndSaving() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            final PhylogenyNode x = tree.getNode("X");
            final PhylogenyNode c = tree.getNode("C");
            final long aId = tree.getNode("A").getId();
            tree.getNumberOfExternalNodes();
            tree.getNode(aId);
            panel.subTree(x);
            check(panel.getPhylogeny().getRoot() == x, "Navigation must preserve the clade root identity");
            check(x.getParent() == null, "The displayed root must be detached");
            check(Arrays.equals(panel.getPhylogeny().getAllExternalNodeNames(), new String[] { "A", "B", "D" }),
                    "Displayed iteration must stay inside the subtree");
            assertSnapshot(panel, "A", "B", "D", "C");
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            check(complete(panel).getNumberOfExternalNodes() == 3, "Complete leaf cache must follow subtree edits");
            check(panel.undo(), "Deletion must be undoable");
            check(complete(panel).getRoot().getChildNode(0) == panel.getPhylogeny().getRoot(),
                    "Undo must replace the complete tree's child slot before returning");
            check(complete(panel).getNode(aId) == panel.getPhylogeny().getNode(aId),
                    "Complete ID cache must contain the restored nodes");
            assertSnapshot(panel, "A", "B", "D", "C");
            check(panel.redo(), "Deletion must be redoable");
            assertSnapshot(panel, "A", "D", "C");
            check(panel.undo(), "Deletion must remain undoable after saving");
            panel.pushUndoCheckpoint("Rename");
            panel.getPhylogeny().getNode("B").setName("Changed");
            panel.setEdited(true);
            check(panel.undo(), "Rename must be undoable");
            assertSnapshot(panel, "A", "B", "D", "C");
            panel.superTree();
            assertSnapshot(panel, "A", "B", "D", "C");
            check(panel.getPhylogeny().getNode("C") == c && c.getParent() == tree.getRoot(),
                    "An unrelated sibling and its parent must retain identity");
            assertParents(panel.getPhylogeny());
        } finally {
            frame.end();
        }
    }

    private static void nestedSubtrees() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("(((A:1,B:2,D:1)Y:2,E:1)X:1,C:3)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            panel.subTree(tree.getNode("X"));
            panel.subTree(tree.getNode("Y"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            check(panel.undo(), "Nested deletion must be undoable");
            assertSnapshot(panel, "A", "B", "D", "E", "C");
            panel.superTree();
            check("X".equals(panel.getPhylogeny().getRoot().getName()), "One return must pop one navigation frame");
            assertSnapshot(panel, "A", "B", "D", "E", "C");
            panel.superTree();
            assertParents(panel.getPhylogeny());
            panel.subTree(tree.getNode("Y"));
            panel.superTreeOneLevel();
            check("X".equals(panel.getPhylogeny().getRoot().getName()), "R1 must use the recorded parent");
            panel.superTree();
            assertSnapshot(panel, "A", "B", "D", "E", "C");
        } finally {
            frame.end();
        }
    }

    private static void collapsedRootKeepsBranchLength() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:2,B:3)X:4,C:5)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            panel.subTree(tree.getNode("X"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            check(complete(panel).getNode("A").getDistanceToParent() == 6,
                    "Root collapse must retain both lengths of the connecting path");
            assertSnapshot(panel, "A", "C");
            check(panel.undo(), "Root collapse must be undoable");
            check(complete(panel).getNode("X").getDistanceToParent() == 4, "Undo must restore the incoming length");
            check(panel.redo(), "Root collapse must be redoable");
            check(complete(panel).getNode("A").getDistanceToParent() == 6, "Redo must not add the length twice");
            panel.superTree();
            assertParents(panel.getPhylogeny());
        } finally {
            frame.end();
        }
    }

    private static void completedChanges() throws Exception {
        final Phylogeny tree = Phylogeny.createInstanceFromNhxString("((A:1,B:2,D:1)X:1,C:3)R;");
        final MainFrameApplication frame = embedded(tree);
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            final List<TreeChangeListener.Kind> changes = new ArrayList<>();
            panel.setTreeChangeListener((source, kind) -> {
                check(SwingUtilities.isEventDispatchThread(), "Completed changes belong to the event thread");
                check(source == panel, "An event must identify its own panel");
                final Phylogeny complete = source.getCompletePhylogeny();
                assertLiveTopology(complete.getRoot(), source.isCurrentTreeIsSubtree()
                        ? source.getPhylogeny().getRoot() : null);
                check(complete.getNumberOfExternalNodes() == complete.copy().getNumberOfExternalNodes(),
                        "Listener must see current complete-tree counts");
                if (source.isCurrentTreeIsSubtree()) {
                    check(complete.getRoot().getChildNode(0) == source.getPhylogeny().getRoot(),
                            "Listener must see the current subtree root in the complete topology");
                }
                changes.add(kind);
            });
            panel.setEdited(false);
            check(changes.isEmpty(), "Saving must not emit a mutation");
            panel.subTree(tree.getNode("X"));
            panel.deleteNodeOrSubtreeConfirmed(panel.getPhylogeny().getNode("B"), false);
            panel.undo();
            panel.redo();
            panel.superTree();
            final Phylogeny replacement = Phylogeny.createInstanceFromNhxString("(Q:1,Z:1)Other;");
            panel.setTree(replacement);
            check(changes.size() == 5, "A bare setter must not report a completed replacement");
            panel.setEdited(true);
            check(changes.equals(Arrays.asList(TreeChangeListener.Kind.NAVIGATION, TreeChangeListener.Kind.EDIT,
                    TreeChangeListener.Kind.HISTORY_RESTORE, TreeChangeListener.Kind.HISTORY_RESTORE,
                    TreeChangeListener.Kind.NAVIGATION, TreeChangeListener.Kind.REPLACE)),
                    "Completed operations must have distinct, ordered change kinds");
            panel.setTreeChangeListener(null);
        } finally {
            frame.end();
        }
    }

    private static void completedTabChanges() throws Exception {
        final MainFrameApplication frame = embedded(Phylogeny.createInstanceFromNhxString("(A,B)R;"));
        try {
            final MainPanel main = frame.getMainPanel();
            final List<Integer> counts = new ArrayList<>();
            main.setTreeViewsChangedListener(() -> {
                check(main.getTreePanels().size() == main.getTabbedPane().getTabCount(),
                        "Tab callback must follow both panel and tab updates");
                counts.add(main.getTreePanels().size());
            });
            main.addPhylogenyInNewTab(Phylogeny.createInstanceFromNhxString("(Q,Z)Analysis;"),
                    new Configuration(), "Analysis", "");
            main.closeCurrentPane();
            main.closeCurrentPane();
            check(counts.equals(Arrays.asList(2, 1, 0)), "Added and removed views must notify exactly once");
            main.setTreeViewsChangedListener(null);
        } finally {
            frame.end();
        }
    }

    private static void cladogramIsNotToScale() throws Exception {
        final MainFrameApplication frame = embedded(Phylogeny.createInstanceFromNhxString("(A:1,B:2)R;"));
        try {
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            check(panel.drawsDistancesToScale(), "A phylogram is drawn to scale");
            panel.getControlPanel().getDisplayAsCladogramRb().doClick();
            check(!panel.drawsDistancesToScale(), "A cladogram is not drawn to scale");
        } finally {
            frame.end();
        }
    }

    private static MainFrameApplication embedded(final Phylogeny tree) {
        return MainFrameApplication.createEmbeddedInstance(new Phylogeny[] { tree }, new Configuration(), "Lifecycle");
    }

    private static Phylogeny complete(final TreePanel panel) throws Exception {
        return panel.getCompletePhylogeny();
    }

    private static void assertSnapshot(final TreePanel panel, final String... names) throws Exception {
        final Phylogeny copy = complete(panel).copy();
        assertParents(copy);
        check(Arrays.equals(copy.getAllExternalNodeNames(), names), "Saved leaf order must match the complete tree");
        final PhyloXmlParser parser = PhyloXmlParser.createPhyloXmlParser();
        parser.setSource(new PhylogenyWriter().toPhyloXML(copy, 0));
        final Phylogeny restored = parser.parse()[0];
        assertParents(restored);
        check(Arrays.equals(restored.getAllExternalNodeNames(), names), "PhyloXML round-trip must retain every leaf");
    }

    private static void assertParents(final Phylogeny tree) {
        for (final PhylogenyNodeIterator it = tree.iteratorPreorder(); it.hasNext();) {
            final PhylogenyNode node = it.next();
            for (final PhylogenyNode child : node.getDescendants()) {
                check(child.getParent() == node, "Every copied or restored child must have its actual parent");
            }
        }
    }

    private static int assertLiveTopology(final PhylogenyNode node, final PhylogenyNode detached) {
        int descendants = 0;
        for (final PhylogenyNode child : node.getDescendants()) {
            check(child.getParent() == (child == detached ? null : node),
                    "Only a recorded navigation root may have no parent");
            descendants += assertLiveTopology(child, detached);
        }
        final int expected = node.isExternal() || node.isCollapse() ? 1 : descendants;
        check(node.getNumberOfExternalNodes() == expected, "Listeners must see current per-node descendant counts");
        return expected;
    }

    private static void check(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

}
