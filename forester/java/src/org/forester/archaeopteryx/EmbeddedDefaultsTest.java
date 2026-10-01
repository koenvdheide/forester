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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.forester.archaeopteryx.phylogeny.data.RenderableDomainArchitecture;
import org.forester.archaeopteryx.tools.NodeDataImporter;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.data.DomainArchitecture;
import org.forester.phylogeny.data.ProteinDomain;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;

public final class EmbeddedDefaultsTest {

    public static void main(final String[] args) throws Exception {
        final Path directory = Files.createTempDirectory("forester-embedded-defaults-");
        final Path settings = directory.resolve(GuiPreferences.SETTINGS_FILE);
        final byte[] original = "show_scale=false\n".getBytes(StandardCharsets.UTF_8);
        Files.write(settings, original);
        final String previous = System.getProperty(GuiPreferences.DIR_PROPERTY);
        System.setProperty(GuiPreferences.DIR_PROPERTY, directory.toString());
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    checkDefaults();
                    checkDetachedViewState();
                    checkLoadOrder();
                    checkAddedTreeFitsLikeConstructedTree();
                    checkClickToFallbackAfterHostSelection();
                } catch (final Exception e) {
                    throw new AssertionError(e);
                }
            });
            if (!Arrays.equals(original, Files.readAllBytes(settings))) {
                throw new AssertionError("Embedded reset changed standalone preferences");
            }
        } finally {
            if (previous == null) {
                System.clearProperty(GuiPreferences.DIR_PROPERTY);
            } else {
                System.setProperty(GuiPreferences.DIR_PROPERTY, previous);
            }
            Files.deleteIfExists(settings);
            Files.delete(directory);
        }
        System.out.println("EmbeddedDefaults: OK.");
    }

    private static void checkLoadOrder() throws Exception {
        for (final boolean embedded : new boolean[] { true, false }) {
            final Configuration config = new Configuration();
            final MainFrameApplication frame = embedded
                    ? MainFrameApplication.createEmbeddedInstance(new Phylogeny[0], config, "Load order")
                    : (MainFrameApplication) MainFrameApplication.createInstance(new Phylogeny[0], config, "Load order");
            try {
                final Phylogeny source = tree();
                source.getFirstExternalNode().getBranchData().addConfidence(
                        new org.forester.phylogeny.data.Confidence(0.9, "bootstrap"));
                final MainPanel main = frame.getMainPanel();
                main.addPhylogenyInNewTab(source, config, "original", "");
                main.getCurrentTreePanel().setShows(DisplayOption.WRITE_CONFIDENCE_VALUES, false);
                final Phylogeny saved = source.copy();
                FigureSpec.writeToTree(saved, FigureSpec.capture(main.getCurrentTreePanel()));
                AptxUtil.addPhylogeniesToTabs(new Phylogeny[] { saved }, "restored", null, config, main);
                if (main.getCurrentTreePanel().shows(DisplayOption.WRITE_CONFIDENCE_VALUES) == embedded) {
                    throw new AssertionError("Embedded saved state and ordinary upstream load order must be distinct");
                }
            } finally {
                frame.end();
                frame.dispose();
            }
        }
    }

    private static void checkDefaults() throws Exception {
        final Object laf = UIManager.getLookAndFeel();
        final Configuration config = new Configuration();
        final MainFrameApplication frame = MainFrameApplication.createEmbeddedInstance(new Phylogeny[0], config,
                "Embedded defaults");
        try {
            final MainPanel main = frame.getMainPanel();
            final int menus = frame.getJMenuBar().getMenuCount();
            frame.showErrorIndicator();
            if (frame.getJMenuBar().getMenuCount() != menus) {
                throw new AssertionError("Standalone error broadcasts must not change embedded menus");
            }
            final int[] initializations = { 0 };
            main.setTreeInitializer(panel -> {
                ++initializations[0];
                panel.setShows(DisplayOption.SHOW_NODE_NAMES, false);
            });
            frame.getOptions().setShowScale(false);
            frame._show_scale_cbmi.setSelected(false);
            frame.setEmbeddedDefaults(() -> frame.getOptions().setShowScale(true));
            if (!frame.getOptions().isShowScale() || !frame._show_scale_cbmi.isSelected()) {
                throw new AssertionError("Embedded defaults must apply at once and reach the menus");
            }
            main.addPhylogenyInNewTab(tree(), config, "first", "");
            final TreePanel first = main.getCurrentTreePanel();
            if (!main.getControlPanel().isDrawPhylogram()) {
                throw new AssertionError("Branch-length trees must use upstream's detected display type");
            }
            checkDomainColours(first);
            if (initializations[0] != 1 || first.shows(DisplayOption.SHOW_NODE_NAMES)
                    || main.getControlPanel().isShowNodeNames()) {
                throw new AssertionError("New-tab defaults and controls disagree");
            }
            first.setShows(DisplayOption.SHOW_NODE_NAMES, true);
            final Phylogeny saved = tree();
            main.getControlPanel().setTreeDisplayType(Options.PHYLOGENY_DISPLAY_TYPE.CLADOGRAM);
            FigureSpec.writeToTree(saved, FigureSpec.capture(first));
            main.addPhylogenyInNewTab(saved, config, "restored", "");
            if (initializations[0] != 2 || main.getControlPanel().isDrawPhylogram()
                    || !main.getCurrentTreePanel().shows(DisplayOption.SHOW_NODE_NAMES)
                    || !main.getControlPanel().isShowNodeNames()) {
                throw new AssertionError("Saved state must override defaults and update controls");
            }
            frame.getOptions().setShowScale(false);
            frame.resetToDefaults();
            if (UIManager.getLookAndFeel() != laf || !frame.getOptions().isShowScale()) {
                throw new AssertionError("Embedded reset lost host theme or defaults");
            }
            if (initializations[0] != 4 || first.shows(DisplayOption.SHOW_NODE_NAMES)
                    || main.getCurrentTreePanel().shows(DisplayOption.SHOW_NODE_NAMES)
                    || main.getControlPanel().isShowNodeNames()) {
                throw new AssertionError("Reset must apply defaults independently to every tab");
            }
        } finally {
            frame.end();
        }
    }

    private static void checkDetachedViewState() throws Exception {
        final Configuration config = new Configuration();
        final MainFrameApplication frame = MainFrameApplication.createEmbeddedInstance(new Phylogeny[0], config,
                "Detached state");
        try {
            final MainPanel main = frame.getMainPanel();
            main.addPhylogenyInNewTab(tree(), config, "first", "");
            final TreePanel first = main.getCurrentTreePanel();
            first.setPhylogenyGraphicsType(Options.PHYLOGENY_GRAPHICS_TYPE.CIRCULAR);
            first.setShows(DisplayOption.SHOW_NODE_NAMES, false);
            final TimeAxisConfig time = new TimeAxisConfig(Options.TIME_AXIS_TYPE.CALENDAR, 8, 2021, true, true);
            first.applyTimeAxisConfig(time);
            final NodeDataImporter.Table table = NodeDataImporter.parseTable("name\treads\nA\t3\n");
            final NodeDataImporter.ImportProfile profile = NodeDataImporter.ImportProfile.from(table, 0,
                    NodeDataImporter.MatchBy.TIP_NAME, NodeDataImporter.ColumnPlan.importAll(table), "annotations.tsv", false);
            first.setLastImportProfile(profile);
            main.addPhylogenyInNewTab(tree(), config, "second", "");
            final String figure = FigureSpec.capture(first).toPropertyValue();
            final PhylogenyWriter writer = new PhylogenyWriter();
            final String before = writer.toPhyloXML(first.getPhylogeny(), 0).toString();
            final Phylogeny copy = first.getCompletePhylogeny().copy();
            EmbeddedAccess.copyViewState(first, copy);
            if (!before.equals(writer.toPhyloXML(first.getPhylogeny(), 0).toString())) {
                throw new AssertionError("Capturing view state must not modify the live tree");
            }
            if (!time.serialize().equals(TimeAxisConfig.readFromTree(copy).serialize())
                    || !profile.serialize().equals(NodeDataImporter.readProfileFromTree(copy).serialize())) {
                throw new AssertionError("Detached state must retain time-axis and import-profile settings");
            }
            main.addPhylogenyInNewTab(copy, config, "restored", "");
            final TreePanel restored = main.getCurrentTreePanel();
            if (!figure.equals(FigureSpec.capture(restored).toPropertyValue())
                    || !time.serialize().equals(restored.currentTimeAxisConfig().serialize())
                    || !profile.serialize().equals(restored.getLastImportProfile().serialize())) {
                throw new AssertionError("Inactive-tab view state must survive the ordinary load path");
            }
        } finally {
            frame.end();
        }
    }

    private static void checkAddedTreeFitsLikeConstructedTree() throws Exception {
        final Configuration config = new Configuration();
        final Phylogeny tree = longLabelTree();
        final Phylogeny copy = tree.copy();
        final MainFrameApplication constructed = MainFrameApplication.createEmbeddedInstance(new Phylogeny[] { tree },
                config, "a");
        final MainFrameApplication added = MainFrameApplication.createEmbeddedInstance(new Phylogeny[0], config, "b");
        try {
            added.getMainPanel().addPhylogenyInNewTab(copy, config, "b", "");
            if (constructed.getMainPanel().getTreeFontSet().getLargeFont().getSize() != added.getMainPanel()
                    .getTreeFontSet().getLargeFont().getSize()) {
                throw new AssertionError(
                        "An embedded tree added to an empty viewer must fit like one passed to the constructor");
            }
        } finally {
            constructed.end();
            added.end();
        }
    }

    // Node names are shortened to 18 characters, so the width that forces the font down comes from the species name
    private static Phylogeny longLabelTree() throws Exception {
        final String species = "[&&NHX:S=" + "W".repeat(72) + "]";
        return ParserBasedPhylogenyFactory.getInstance()
                .create("(A:1" + species + ",B:2" + species + ",C:3" + species + ");", new NHXParser())[0];
    }

    private static void checkClickToFallbackAfterHostSelection() throws Exception {
        final MainFrameApplication frame = MainFrameApplication.createEmbeddedInstance(new Phylogeny[0],
                new Configuration(), "Click to");
        try {
            EmbeddedAccess.selectNodesOnClick(frame.getMainPanel());
            if (privateInt(frame, "_last_allowed_click_to_index") != privateInt(frame, "_select_nodes_item")) {
                throw new AssertionError("A host's click-to choice must also be the reroot fallback");
            }
        } finally {
            frame.end();
        }
    }

    private static int privateInt(final MainFrameApplication frame, final String name) throws Exception {
        final java.lang.reflect.Field field = ControlPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(frame.getMainPanel().getControlPanel());
    }

    private static Phylogeny tree() throws Exception {
        return ParserBasedPhylogenyFactory.getInstance().create("(A:1,B:2);", new NHXParser())[0];
    }

    private static void checkDomainColours(final TreePanel panel) {
        panel.setDomainColorProvider(name -> "Helix".equals(name) ? Color.RED : null);
        RenderableDomainArchitecture.setColorMap(new java.util.HashMap<String, Color>(
                java.util.Collections.singletonMap("Helix", Color.BLUE)));
        if (!Color.RED.equals(panel.getDomainColor("Helix"))
                || !RenderableDomainArchitecture.colorFor("Sheet").equals(panel.getDomainColor("Sheet"))) {
            throw new AssertionError("A view's domain override must survive palette replacement with a fallback");
        }
        final DomainArchitecture domains = new DomainArchitecture();
        domains.setTotalLength(100);
        domains.addDomain(new ProteinDomain("Helix", 0, 99));
        final RenderableDomainArchitecture rendered = new RenderableDomainArchitecture(domains);
        rendered.setRenderingHeight(10);
        final BufferedImage image = new BufferedImage(150, 30, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            rendered.render(0, 5, graphics, panel, false, false);
        } finally {
            graphics.dispose();
        }
        final Color box = new Color(image.getRGB(60, 10));
        if (box.getRed() <= box.getBlue()) {
            throw new AssertionError("The domain renderer ignored the view's red override");
        }
        panel.setDomainColorProvider(null);
        if (!Color.BLUE.equals(panel.getDomainColor("Helix"))) {
            throw new AssertionError("Removing the override must restore the upstream palette");
        }
    }
}
