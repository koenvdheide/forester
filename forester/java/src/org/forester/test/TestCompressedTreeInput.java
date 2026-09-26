// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// All rights reserved
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
//

package org.forester.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;

public final class TestCompressedTreeInput {

    private static final String[] SUFFIXES = { ".nwk", ".nex", ".xml" };
    private static final String[] TREES    = { "(A\u03b1:1,B:2);",
            "#NEXUS\nBEGIN TREES;\nTREE t = (A\u03b1:1,B:2);\nEND;\n",
            "<?xml version=\"1.0\"?><phyloxml xmlns=\"http://www.phyloxml.org\">"
                    + "<phylogeny rooted=\"true\"><clade><clade><name>A\u03b1</name>"
                    + "<branch_length>1</branch_length></clade><clade><name>B</name>"
                    + "<branch_length>2</branch_length></clade></clade></phylogeny></phyloxml>" };
    public static void main( final String[] args ) {
        if ( !test() ) {
            throw new AssertionError( "Compressed tree input failed" );
        }
    }

    public static boolean test() {
        boolean passed = true;
        for( int format = 0; format < TREES.length; ++format ) {
            for( int compression = 0; compression < 3; ++compression ) {
                final String suffix = compression == 2 ? ".data"
                        : SUFFIXES[ format ] + ( compression == 1 ? ".gz" : "" );
                File file = null;
                try {
                    file = File.createTempFile( "forester-tree-", suffix );
                    try (OutputStream output = compression == 0 ? Files.newOutputStream( file.toPath() )
                            : new GZIPOutputStream( Files.newOutputStream( file.toPath() ) )) {
                        output.write( TREES[ format ].getBytes( StandardCharsets.UTF_8 ) );
                    }
                    checkTree( file );
                    System.out.println( SUFFIXES[ format ] + " input " + compression + ": OK" );
                }
                catch ( final Exception e ) {
                    System.err.println( SUFFIXES[ format ] + " input " + compression + ": " + e );
                    passed = false;
                }
                finally {
                    if ( file != null && !file.delete() ) {
                        file.deleteOnExit();
                    }
                }
            }
        }
        return passed;
    }

    private static void checkTree( final File file ) throws IOException {
        final Phylogeny[] trees = PhylogenyMethods
                .readPhylogenies( ParserUtils.createParserDependingOnFileType( file, true ), file );
        if ( trees.length != 1 || trees[ 0 ].getNumberOfExternalNodes() != 2 || trees[ 0 ].getNode( "A\u03b1" ) == null
                || trees[ 0 ].getNode( "B" ) == null || trees[ 0 ].getNode( "A\u03b1" ).getDistanceToParent() != 1
                || trees[ 0 ].getNode( "B" ).getDistanceToParent() != 2 ) {
            throw new IOException( "Expected one tree with A\u03b1:1 and B:2" );
        }
    }
}
