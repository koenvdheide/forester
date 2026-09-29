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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;

public final class PartitionLineRenderTest {

    private static final int MARGIN = 30;

    public static void main(final String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(PartitionLineRenderTest::exercise);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("PARTITION_LINE_RENDER_OK");
        System.exit(0);
    }

    private static void exercise() {
        final Phylogeny phy = new Phylogeny();
        final PhylogenyNode root = new PhylogenyNode();
        final PhylogenyNode tip = new PhylogenyNode();
        tip.setDistanceToParent(2);
        root.addAsChild(tip);
        final PhylogenyNode other = new PhylogenyNode();
        other.setDistanceToParent(1);
        root.addAsChild(other);
        phy.setRoot(root);
        phy.setRooted(true);
        phy.externalNodesHaveChanged();
        final MainFrame viewer = MainFrameApplication.createInstance(new Phylogeny[] { phy },
                new Configuration(), "partition line test");
        final JFrame host = viewer._window;
        try {
            host.setSize(1400, 600);
            host.validate();
            final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
            panel.setPhylogenyGraphicsType(Options.PHYLOGENY_GRAPHICS_TYPE.RECTANGULAR);
            panel.getControlPanel().setTreeDisplayType(Options.PHYLOGENY_DISPLAY_TYPE.UNALIGNED_PHYLOGRAM);
            panel.getOptions().setShowOverview(false);
            for (final Options.TREE_ORIENTATION orientation : Options.TREE_ORIENTATION.values()) {
                try {
                    checkOrientation(panel, orientation, root, tip);
                } catch (final AssertionError e) {
                    throw new AssertionError(orientation + ": " + e.getMessage(), e);
                }
            }
            for (final Options.PHYLOGENY_GRAPHICS_TYPE radial : new Options.PHYLOGENY_GRAPHICS_TYPE[] {
                    Options.PHYLOGENY_GRAPHICS_TYPE.CIRCULAR, Options.PHYLOGENY_GRAPHICS_TYPE.UNROOTED }) {
                panel.setPhylogenyGraphicsType(radial);
                panel.getControlPanel().showWhole();
                panel.setPartitionThreshold(0.5f);
                final BufferedImage active = paint(panel, false);
                panel.setPartitionThreshold(0f);
                final BufferedImage clear = paint(panel, false);
                for (int y = 0; y < active.getHeight(); y++) {
                    for (int x = 0; x < active.getWidth(); x++) {
                        if (active.getRGB(x, y) != clear.getRGB(x, y)) {
                            throw new AssertionError("partition line must be absent in " + radial);
                        }
                    }
                }
            }
        } finally {
            host.dispose();
        }
    }

    private static void checkOrientation(final TreePanel panel, final Options.TREE_ORIENTATION orientation,
                                         final PhylogenyNode root, final PhylogenyNode tip) {
        panel.setTreeOrientation(orientation);
        panel.getControlPanel().showWhole();
        if (panel.getWidth() < (panel.getHeight() + 200)) {
            throw new AssertionError("panel must be wider than tall: " + panel.getWidth() + "x" + panel.getHeight());
        }
        final boolean vertical = panel.isVerticalOrientation();
        final int breadth = vertical ? panel.getWidth() : panel.getHeight();
        for (final float threshold : new float[] { 0.25f, 0.75f }) {
            panel.setPartitionThreshold(threshold);
            final BufferedImage screen = paint(panel, false);
            final int depth = depth(panel, root, tip, threshold);
            for (final int b : new int[] { 10, breadth - 10 }) {
                requireRed(screen, vertical ? b : depth, vertical ? depth : b, true);
            }
            final BufferedImage export = paint(panel, true);
            final int export_depth = depth(panel, root, tip, threshold);
            for (final int b : new int[] { MARGIN + 5, breadth - MARGIN - 5 }) {
                requireRed(export, vertical ? b : export_depth, vertical ? export_depth : b, true);
            }
            for (final int b : new int[] { 10, breadth - 10 }) {
                requireRed(export, vertical ? b : export_depth, vertical ? export_depth : b, false);
            }
            panel.getOptions().setExportBlackAndWhite(true);
            final BufferedImage monochrome = paint(panel, true);
            final int monochrome_depth = depth(panel, root, tip, threshold);
            for (final int b : new int[] { MARGIN + 5, breadth - MARGIN - 5 }) {
                if (monochrome.getRGB(vertical ? b : monochrome_depth,
                        vertical ? monochrome_depth : b) != Color.BLACK.getRGB()) {
                    throw new AssertionError("monochrome partition line must be black");
                }
            }
            requireRed(paint(panel, false), vertical ? 10 : depth, vertical ? depth : 10, true);
            panel.getOptions().setExportBlackAndWhite(false);
            panel.setPartitionThreshold(0f);
            requireRed(paint(panel, false), vertical ? 10 : depth, vertical ? depth : 10, false);
        }
    }

    private static int depth(final TreePanel panel, final PhylogenyNode root, final PhylogenyNode tip,
                             final float threshold) {
        final Point2D.Double r = panel.screenPointFor(root);
        final Point2D.Double t = panel.screenPointFor(tip);
        return panel.isVerticalOrientation() ? Math.round((float) (r.y + ((t.y - r.y) * threshold)))
                : Math.round((float) (r.x + ((t.x - r.x) * threshold)));
    }

    private static BufferedImage paint(final TreePanel panel, final boolean export) {
        final BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            if (export) {
                panel.paintFile(g, false, image.getWidth() - (2 * MARGIN), image.getHeight() - (2 * MARGIN), MARGIN,
                        MARGIN, false);
            } else {
                panel.paint(g);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void requireRed(final BufferedImage image, final int x, final int y, final boolean expected) {
        if ((image.getRGB(x, y) == Color.RED.getRGB()) != expected) {
            throw new AssertionError("partition pixel " + x + "," + y + " expected red=" + expected);
        }
    }
}
