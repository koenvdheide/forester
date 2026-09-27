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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;

public final class EmbeddedLabelHitTest {

    public static void main( final String[] args ) throws Exception {
        SwingUtilities.invokeAndWait( () -> {
            try {
                exercise();
            }
            catch ( final Exception error ) {
                throw new AssertionError( error );
            }
        } );
        System.out.println( "EmbeddedLabelHit: OK." );
    }

    @SuppressWarnings( "unchecked")
    private static void exercise() throws Exception {
        final Phylogeny phy = Phylogeny
                .createInstanceFromNhxString( "(Long_species_name_alpha:1,Long_species_name_beta:2)R;" );
        final MainFrameApplication viewer = MainFrameApplication
                .createEmbeddedInstance( new Phylogeny[] { phy }, new Configuration(), "Label hit export" );
        try {
            final TreePanel panel = viewer.getMainPanel().getCurrentTreePanel();
            panel.setSize( 600, 400 );
            panel.calcParametersForPainting( 600, 400 );
            final Graphics2D screen = new BufferedImage( 600, 400, BufferedImage.TYPE_INT_ARGB ).createGraphics();
            panel.paintPhylogeny( screen, false, false, 600, 400, 0, 0 );
            screen.dispose();
            final Field field = TreePanel.class.getDeclaredField( "_label_bounds" );
            field.setAccessible( true );
            final Map<PhylogenyNode, Shape> bounds = ( Map<PhylogenyNode, Shape> ) field.get( panel );
            final Map<PhylogenyNode, Rectangle2D> before = new IdentityHashMap<>();
            bounds.forEach( ( node, shape ) -> before.put( node, shape.getBounds2D() ) );
            final PhylogenyNode leaf = phy.getNode( "Long_species_name_alpha" );
            if ( !before.containsKey( leaf ) ) {
                throw new AssertionError( "Screen paint must establish label bounds" );
            }
            final Rectangle2D box = before.get( leaf );
            final int x = ( int ) box.getCenterX();
            final int y = ( int ) box.getCenterY();
            if ( panel.findNodeOrLabel( x, y ) != leaf ) {
                throw new AssertionError( "Screen label must be pickable before export" );
            }
            for( int mode = 0; mode < 3; ++mode ) {
                final Graphics2D export = new BufferedImage( 900, 700, BufferedImage.TYPE_INT_ARGB ).createGraphics();
                if ( mode == 2 ) {
                    panel.paintPhylogeny( export, true, false, 900, 700, 0, 0 );
                }
                else {
                    panel.paintFile( export, mode == 1, 900, 700, 80, 90, mode == 1 );
                }
                export.dispose();
                final Map<PhylogenyNode, Rectangle2D> after = new IdentityHashMap<>();
                bounds.forEach( ( node, shape ) -> after.put( node, shape.getBounds2D() ) );
                if ( before.size() != after.size()
                        || before.entrySet().stream()
                                .anyMatch( entry -> !entry.getValue().equals( after.get( entry.getKey() ) ) )
                        || panel.findNodeOrLabel( x, y ) != leaf ) {
                    throw new AssertionError( "Export must preserve screen label picking, mode=" + mode );
                }
            }
        }
        finally {
            viewer.end();
        }
    }
}
