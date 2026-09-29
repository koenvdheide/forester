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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.forester.archaeopteryx.phylogeny.data.RenderableDomainArchitecture;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.DomainArchitecture;
import org.forester.phylogeny.data.ProteinDomain;

public final class DomainLabelStyleTest {

    private static final Color FILL = new Color(240, 240, 180);

    private static final int UPSTREAM_INK = new Color(20, 26, 29).getRGB();

    private static final int BLACK = Color.BLACK.getRGB();

    public static void main(final String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(DomainLabelStyleTest::exercise);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("DOMAIN_LABEL_STYLE_OK");
        System.exit(0);
    }

    private static void exercise() {
        final Phylogeny phy = new Phylogeny();
        final PhylogenyNode root = new PhylogenyNode();
        root.addAsChild(new PhylogenyNode());
        root.addAsChild(new PhylogenyNode());
        phy.setRoot(root);
        phy.setRooted(true);
        phy.externalNodesHaveChanged();
        final Configuration config = new Configuration();
        final MainFrameApplication frame = MainFrameApplication.createEmbeddedInstance(new Phylogeny[0], config,
                "domain labels");
        try {
            frame.getMainPanel().addPhylogenyInNewTab(phy, config, "labels", "");
            final TreePanel panel = frame.getMainPanel().getCurrentTreePanel();
            panel.setSize(600, 400);
            panel.calcParametersForPainting(600, 400);
            panel.setDomainColorProvider(name -> FILL);
            final DomainArchitecture architecture = new DomainArchitecture();
            architecture.setTotalLength(100);
            architecture.addDomain(new ProteinDomain("Helix", 1, 100, 0.0));
            final RenderableDomainArchitecture strip = new RenderableDomainArchitecture(architecture);
            strip.setRenderingFactorWidth(3f);
            strip.setRenderingHeight(30f);
            final BufferedImage upstream = paint(strip, panel);
            require((count(upstream, UPSTREAM_INK) > 0) && (count(upstream, BLACK) == 0),
                    "upstream draws the label in its contrast ink");
            require(firstColumn(upstream, UPSTREAM_INK) > 60, "upstream centres the label");
            EmbeddedAccess.setDomainLabelsLeftAligned(panel, true);
            final BufferedImage host = paint(strip, panel);
            require((count(host, BLACK) > 0) && (count(host, UPSTREAM_INK) == 0),
                    "the host style draws the label in black on a light fill");
            require(firstColumn(host, BLACK) <= 26, "the host style left-aligns the label");
            strip.setRenderingHeight(6f);
            require(count(paint(strip, panel), BLACK) == 0,
                    "the host style drops labels on strips shorter than the label");
        } finally {
            frame.end();
        }
    }

    // render() starts the track 20 px in, so the box spans x 20..320 and y 10..40; samples stay inside its border
    private static BufferedImage paint(final RenderableDomainArchitecture strip, final TreePanel panel) {
        final BufferedImage image = new BufferedImage(360, 60, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            strip.render(0f, 10f, g, panel, false, true);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int count(final BufferedImage image, final int rgb) {
        int n = 0;
        for (int y = 11; y < 39; y++) {
            for (int x = 21; x < 319; x++) {
                if (image.getRGB(x, y) == rgb) {
                    n++;
                }
            }
        }
        return n;
    }

    private static int firstColumn(final BufferedImage image, final int rgb) {
        for (int x = 21; x < 319; x++) {
            for (int y = 11; y < 39; y++) {
                if (image.getRGB(x, y) == rgb) {
                    return x;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
