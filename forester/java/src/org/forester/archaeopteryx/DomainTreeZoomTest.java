// forester -- software libraries and applications
// for evolutionary biology and genomics.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

package org.forester.archaeopteryx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.forester.archaeopteryx.phylogeny.data.RenderableDomainArchitecture;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.DomainArchitecture;
import org.forester.phylogeny.data.ProteinDomain;
import org.forester.phylogeny.data.Sequence;

public final class DomainTreeZoomTest {

    public static void main(final String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(DomainTreeZoomTest::exercise);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("DOMAIN_TREE_ZOOM_OK");
        System.exit(0);
    }

    private static void exercise() {
        final Phylogeny phy = new Phylogeny();
        final PhylogenyNode root = new PhylogenyNode();
        for (int length : new int[] { 100, 80 }) {
            final PhylogenyNode tip = new PhylogenyNode();
            final DomainArchitecture domains = new DomainArchitecture();
            domains.setTotalLength(length);
            domains.addDomain(new ProteinDomain("Helix", 1, 20));
            domains.addDomain(new ProteinDomain("Sheet", 41, 60));
            final Sequence sequence = new Sequence();
            sequence.setDomainArchitecture(domains);
            tip.getNodeData().setSequence(sequence);
            tip.setDistanceToParent(1);
            root.addAsChild(tip);
        }
        phy.setRoot(root);
        phy.setRooted(true);
        phy.externalNodesHaveChanged();
        final MainFrame viewer = MainFrameApplication.createInstance(new Phylogeny[] { phy },
                new Configuration(), "domain tree zoom");
        final JFrame host = viewer._window;
        try {
            host.setSize(1000, 700);
            host.validate();
            final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
            final ControlPanel controls = panel.getControlPanel();
            panel.setPhylogenyGraphicsType(Options.PHYLOGENY_GRAPHICS_TYPE.RECTANGULAR);
            panel.setTreeOrientation(Options.TREE_ORIENTATION.ROOT_LEFT);
            controls.setCheckbox(DisplayOption.SHOW_DOMAIN_ARCHITECTURES, true);
            controls.setCheckbox(DisplayOption.DYNAMICALLY_HIDE_DATA, false);
            controls.setTreeDisplayType(Options.PHYLOGENY_DISPLAY_TYPE.UNALIGNED_PHYLOGRAM);
            panel.getOptions().setShowDomainGlow(false);
            panel.getOptions().setShowOverview(false);
            panel.getOptions().setDomainLabelMode(Options.DOMAIN_LABEL_MODE.NONE);
            final Map<String, Color> palette = new HashMap<>();
            palette.put("Helix", Color.RED);
            palette.put("Sheet", Color.GREEN);
            RenderableDomainArchitecture.setColorMap(palette);
            controls.showWhole();
            final RenderableDomainArchitecture domains = (RenderableDomainArchitecture)
                    root.getChildNode1().getNodeData().getSequence().getDomainArchitecture();
            paint(panel);
            final double standalone_width = domains.getRenderingFactorWidth();
            near(domains.getRenderingSize().getHeight(), 16, "standalone height");
            controls.zoomInX(1.2f, 1.2f);
            paint(panel);
            near(domains.getRenderingFactorWidth(), standalone_width, "standalone width stays independent");

            EmbeddedAccess.setDomainStructuresFollowTreeZoom(panel, true);
            for (int cycle = 0; cycle < 3; cycle++) {
                controls.showWhole();
                final BufferedImage fitted = paint(panel);
                near(domains.getRenderingSize().getHeight(), 7, "fitted strip height");
                near(domains.getRenderingFactorWidth(), 0.9, "fitted residue scale");
                checkColours(fitted, phy, domains.getRenderingFactorWidth());
                final double width = domains.getRenderingFactorWidth();
                controls.zoomInX(1.2f, 1.2f);
                controls.zoomInY(2f);
                final BufferedImage zoomed = paint(panel);
                near(domains.getRenderingFactorWidth(), width * 1.2, "tree depth zoom scales strip width");
                near(domains.getRenderingSize().getHeight(), 14, "tree breadth zoom scales strip height");
                checkColours(zoomed, phy, domains.getRenderingFactorWidth());
                controls.zoomOutX(1f / 1.2f, 1f / 1.2f);
                controls.zoomOutY(0.5f);
                paint(panel);
                near(domains.getRenderingFactorWidth(), width, "reversed depth zoom");
                near(domains.getRenderingSize().getHeight(), 7, "reversed breadth zoom");
            }
            controls.zoomInY(2f);
            controls.fitWidth();
            paint(panel);
            near(domains.getRenderingSize().getHeight(), 14, "depth fit preserves breadth zoom");
            controls.zoomInX(1.2f, 1.2f);
            paint(panel);
            final float before_export = domains.getRenderingFactorWidth();
            final int[] layout = panel.layoutForExportSize(1100, 750);
            panel.restoreLayoutAfterExport(layout);
            paint(panel);
            near(domains.getRenderingFactorWidth(), before_export, "export restore preserves strip width");
            near(domains.getRenderingSize().getHeight(), 14, "export restore preserves strip height");
            controls.showWhole();
            controls.zoomInX(3f, 3f);
            final BufferedImage unlabelled = paint(panel);
            panel.getOptions().setDomainLabelMode(Options.DOMAIN_LABEL_MODE.ON_DOMAINS);
            final BufferedImage labelled = paint(panel);
            for (int y = 0; y < labelled.getHeight(); y++) {
                for (int x = 0; x < labelled.getWidth(); x++) {
                    if (labelled.getRGB(x, y) != unlabelled.getRGB(x, y)) {
                        throw new AssertionError("labels must not spill from slim strips at " + x + "," + y);
                    }
                }
            }
            panel.getOptions().setDomainLabelMode(Options.DOMAIN_LABEL_MODE.NONE);
            panel.setYdistance(1f);
            paint(panel);
            near(domains.getRenderingSize().getHeight(), 1, "dense strip height floor");
            panel.setTreeOrientation(Options.TREE_ORIENTATION.ROOT_TOP);
            controls.showWhole();
            paint(panel);
            near(domains.getRenderingSize().getHeight(), 7, "vertical orientation strip height");
            controls.zoomInY(2f);
            paint(panel);
            near(domains.getRenderingSize().getHeight(), 14, "vertical orientation breadth zoom");
            controls.fitHeight();
            paint(panel);
            near(domains.getRenderingSize().getHeight(), 14, "vertical depth fit preserves breadth zoom");
            panel.setPhylogenyGraphicsType(Options.PHYLOGENY_GRAPHICS_TYPE.CIRCULAR);
            panel.getOptions().setNodeLabelDirection(Options.NODE_LABEL_DIRECTION.RADIAL);
            controls.showWhole();
            paint(panel);
            final float radial_width = domains.getRenderingFactorWidth();
            controls.zoomInX(2f, 2f);
            paint(panel);
            near(domains.getRenderingFactorWidth(), radial_width, "radial domain zoom remains independent");
        } finally {
            host.dispose();
        }
    }

    private static BufferedImage paint(final TreePanel panel) {
        final BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void checkColours(final BufferedImage image, final Phylogeny tree, final float scale) {
        int previous_red = -1;
        for (final PhylogenyNode tip : tree.getExternalNodes()) {
            final int y = Math.round(tip.getYcoord());
            int red_start = -1, red_end = -1, green_start = -1;
            for (int x = 0; x < image.getWidth(); x++) {
                final Color c = new Color(image.getRGB(x, y));
                if ((c.getRed() > 150) && (c.getGreen() < 100) && (c.getBlue() < 100)) {
                    if (red_start < 0) {
                        red_start = x;
                    }
                    red_end = x;
                }
                if ((c.getGreen() > 150) && (c.getRed() < 100) && (c.getBlue() < 100) && (green_start < 0)) {
                    green_start = x;
                }
            }
            if ((red_start < 0) || (green_start < 0)
                    || (Math.abs(red_end - red_start + 1 - 20 * scale) > 2)
                    || (Math.abs(green_start - red_start - 40 * scale) > 2)
                    || ((previous_red >= 0) && (red_start != previous_red))) {
                throw new AssertionError("misaligned strip colours: " + red_start + ", " + red_end
                        + ", " + green_start + ", previous " + previous_red + ", scale " + scale);
            }
            previous_red = red_start;
        }
    }

    private static void near(final double actual, final double expected, final String message) {
        if (Math.abs(actual - expected) > 0.001) {
            throw new AssertionError(message + ": " + actual + " != " + expected);
        }
    }
}
