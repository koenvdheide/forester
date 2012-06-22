// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2008-2009 Christian M. Zmasek
// Copyright (C) 2008-2009 Burnham Institute for Medical Research
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
// Contact: phylosoft @ gmail . com
// WWW: www.phylosoft.org/forester

package org.forester.sdi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.Event;
import org.forester.phylogeny.data.Taxonomy;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.util.ForesterUtil;

/*
 * Implements our algorithm for speciation - duplication inference (SDI). <p>
 * The initialization is accomplished by: </p> <ul> <li>method
 * "linkExtNodesOfG()" of class SDI: setting the links for the external nodes of
 * the gene tree <li>"preorderReID(int)" from class Phylogeny: numbering of
 * nodes of the species tree in preorder <li>the optional stripping of the
 * species tree is accomplished by method "stripTree(Phylogeny,Phylogeny)" of
 * class Phylogeny </ul> <p> The recursion part is accomplished by this class'
 * method "geneTreePostOrderTraversal(PhylogenyNode)". <p> Requires JDK 1.5 or
 * greater.
 * 
 * @see SDI#linkNodesOfG()
 * 
 * @see Phylogeny#preorderReID(int)
 * 
 * @see
 * PhylogenyMethods#taxonomyBasedDeletionOfExternalNodes(Phylogeny,Phylogeny)
 * 
 * @see #geneTreePostOrderTraversal(PhylogenyNode)
 * 
 * @author Christian M. Zmasek
 */
public final class GSDI extends SDI {

    private final HashMap<PhylogenyNode, Integer> _transversal_counts;
    private final boolean                         _most_parsimonious_duplication_model;
    private final boolean                         _strip_gene_tree;
    private final boolean                         _strip_species_tree;
    private int                                   _speciation_or_duplication_events_sum;
    private int                                   _speciations_sum;
    private final List<PhylogenyNode>             _stripped_gene_tree_nodes;
    private final List<PhylogenyNode>             _stripped_species_tree_nodes;
    private final Set<PhylogenyNode>              _mapped_species_tree_nodes;

    /**
     * Constructor which sets the gene tree and the species tree to be compared.
     * species_tree is the species tree to which the gene tree gene_tree will be
     * compared to - with method "infer(boolean)". Both Trees must be completely
     * binary and rooted. The actual inference is accomplished with method
     * "infer(boolean)". The mapping cost L can then be calculated with method
     * "computeMappingCost()".
     * <p>
     * 
     * @see #infer(boolean)
     * @see SDI#computeMappingCostL()
     * @param gene_tree
     *            reference to a rooted gene tree to which assign duplication vs
     *            speciation, must have species names in the species name fields
     *            for all external nodes
     * @param species_tree
     *            reference to a rooted binary species tree which might get
     *            stripped in the process, must have species names in the
     *            species name fields for all external nodes
     * 
     * @param most_parsimonious_duplication_model
     *            set to true to assign nodes as speciations which would
     *            otherwise be assiged as unknown because of polytomies in the
     *            species tree.
     * @throws SdiException 
     * 
     */
    public GSDI( final Phylogeny gene_tree,
                 final Phylogeny species_tree,
                 final boolean most_parsimonious_duplication_model,
                 final boolean strip_gene_tree,
                 final boolean strip_species_tree ) throws SdiException {
        super( gene_tree, species_tree );
        _speciation_or_duplication_events_sum = 0;
        _speciations_sum = 0;
        _most_parsimonious_duplication_model = most_parsimonious_duplication_model;
        _transversal_counts = new HashMap<PhylogenyNode, Integer>();
        _duplications_sum = 0;
        _strip_gene_tree = strip_gene_tree;
        _strip_species_tree = strip_species_tree;
        _stripped_gene_tree_nodes = new ArrayList<PhylogenyNode>();
        _stripped_species_tree_nodes = new ArrayList<PhylogenyNode>();
        _mapped_species_tree_nodes = new HashSet<PhylogenyNode>();
        getSpeciesTree().preOrderReId();
        linkNodesOfG();
        geneTreePostOrderTraversal( getGeneTree().getRoot() );
    }

    GSDI( final Phylogeny gene_tree, final Phylogeny species_tree, final boolean most_parsimonious_duplication_model )
            throws SdiException {
        this( gene_tree, species_tree, most_parsimonious_duplication_model, false, false );
    }

    private final Event createDuplicationEvent() {
        final Event event = Event.createSingleDuplicationEvent();
        ++_duplications_sum;
        return event;
    }

    private final Event createSingleSpeciationOrDuplicationEvent() {
        final Event event = Event.createSingleSpeciationOrDuplicationEvent();
        ++_speciation_or_duplication_events_sum;
        return event;
    }

    private final Event createSpeciationEvent() {
        final Event event = Event.createSingleSpeciationEvent();
        ++_speciations_sum;
        return event;
    }

    // s is the node on the species tree g maps to.
    private final void determineEvent( final PhylogenyNode s, final PhylogenyNode g ) {
        Event event = null;
        // Determine how many children map to same node as parent.
        int sum_g_childs_mapping_to_s = 0;
        for( final PhylogenyNodeIterator iter = g.iterateChildNodesForward(); iter.hasNext(); ) {
            if ( iter.next().getLink() == s ) {
                ++sum_g_childs_mapping_to_s;
            }
        }
        // Determine the sum of traversals.
        int traversals_sum = 0;
        int max_traversals = 0;
        PhylogenyNode max_traversals_node = null;
        if ( !s.isExternal() ) {
            for( final PhylogenyNodeIterator iter = s.iterateChildNodesForward(); iter.hasNext(); ) {
                final PhylogenyNode current_node = iter.next();
                final int traversals = getTraversalCount( current_node );
                traversals_sum += traversals;
                if ( traversals > max_traversals ) {
                    max_traversals = traversals;
                    max_traversals_node = current_node;
                }
            }
        }
        // System.out.println( " sum=" + traversals_sum );
        // System.out.println( " max=" + max_traversals );
        // System.out.println( " m=" + sum_g_childs_mapping_to_s );
        if ( sum_g_childs_mapping_to_s > 0 ) {
            if ( traversals_sum == 2 ) {
                event = createDuplicationEvent();
            }
            else if ( traversals_sum > 2 ) {
                if ( max_traversals <= 1 ) {
                    if ( _most_parsimonious_duplication_model ) {
                        event = createSpeciationEvent();
                    }
                    else {
                        event = createSingleSpeciationOrDuplicationEvent();
                    }
                }
                else {
                    event = createDuplicationEvent();
                    _transversal_counts.put( max_traversals_node, 1 );
                }
            }
            else {
                event = createDuplicationEvent();
            }
        }
        else {
            event = createSpeciationEvent();
        }
        g.getNodeData().setEvent( event );
    }

    /**
     * Traverses the subtree of PhylogenyNode g in postorder, calculating the
     * mapping function M, and determines which nodes represent speciation
     * events and which ones duplication events.
     * <p>
     * Preconditions: Mapping M for external nodes must have been calculated and
     * the species tree must be labeled in preorder.
     * <p>
     * 
     * @param g
     *            starting node of a gene tree - normally the root
     */
    final void geneTreePostOrderTraversal( final PhylogenyNode g ) {
        if ( !g.isExternal() ) {
            for( final PhylogenyNodeIterator iter = g.iterateChildNodesForward(); iter.hasNext(); ) {
                geneTreePostOrderTraversal( iter.next() );
            }
            final PhylogenyNode[] linked_nodes = new PhylogenyNode[ g.getNumberOfDescendants() ];
            for( int i = 0; i < linked_nodes.length; ++i ) {
                if ( g.getChildNode( i ).getLink() == null ) {
                    System.out.println( "link is null for " + g.getChildNode( i ) );
                    System.exit( -1 );
                }
                linked_nodes[ i ] = g.getChildNode( i ).getLink();
            }
            final int[] min_max = obtainMinMaxIdIndices( linked_nodes );
            int min_i = min_max[ 0 ];
            int max_i = min_max[ 1 ];
            // initTransversalCounts();
            while ( linked_nodes[ min_i ] != linked_nodes[ max_i ] ) {
                increaseTraversalCount( linked_nodes[ max_i ] );
                linked_nodes[ max_i ] = linked_nodes[ max_i ].getParent();
                final int[] min_max_ = obtainMinMaxIdIndices( linked_nodes );
                min_i = min_max_[ 0 ];
                max_i = min_max_[ 1 ];
            }
            final PhylogenyNode s = linked_nodes[ max_i ];
            g.setLink( s );
            // Determines whether dup. or spec.
            determineEvent( s, g );
            // _transversal_counts.clear();
        }
    }

    public final int getSpeciationOrDuplicationEventsSum() {
        return _speciation_or_duplication_events_sum;
    }

    public final int getSpeciationsSum() {
        return _speciations_sum;
    }

    private final int getTraversalCount( final PhylogenyNode node ) {
        if ( _transversal_counts.containsKey( node ) ) {
            return _transversal_counts.get( node );
        }
        return 0;
    }

    private final void increaseTraversalCount( final PhylogenyNode node ) {
        if ( _transversal_counts.containsKey( node ) ) {
            _transversal_counts.put( node, _transversal_counts.get( node ) + 1 );
        }
        else {
            _transversal_counts.put( node, 1 );
        }
        // System.out.println( "count for node " + node.getID() + " is now "
        // + getTraversalCount( node ) );
    }

    /**
     * This allows for linking of internal nodes of the species tree (as opposed
     * to just external nodes, as in the method it overrides.
     * @throws SdiException 
     * 
     */
    @Override
    //    final void linkNodesOfG() {
    //        final HashMap<Taxonomy, PhylogenyNode> speciestree_ext_nodes = createTaxonomyToNodeMap();
    //        if ( _strip_gene_tree ) {
    //            stripGeneTree( speciestree_ext_nodes );
    //            if ( ( _gene_tree == null ) || ( _gene_tree.getNumberOfExternalNodes() < 2 ) ) {
    //                throw new IllegalArgumentException( "species tree does not contain any"
    //                        + " nodes matching species in the gene tree" );
    //            }
    //        }
    //        // Retrieve the reference to the PhylogenyNode with a matching species.
    //        for( final PhylogenyNodeIterator iter = _gene_tree.iteratorExternalForward(); iter.hasNext(); ) {
    //            final PhylogenyNode g = iter.next();
    //            if ( !g.getNodeData().isHasTaxonomy() ) {
    //                throw new IllegalArgumentException( "gene tree node " + g + " has no taxonomic data" );
    //            }
    //            final PhylogenyNode s = speciestree_ext_nodes.get( g.getNodeData().getTaxonomy() );
    //            if ( s == null ) {
    //                throw new IllegalArgumentException( "species " + g.getNodeData().getTaxonomy()
    //                        + " not present in species tree" );
    //            }
    //            g.setLink( s );
    //        }
    //    }
    final void linkNodesOfG() throws SdiException {
        final Map<String, PhylogenyNode> species_to_node_map = new HashMap<String, PhylogenyNode>();
        final List<PhylogenyNode> species_tree_ext_nodes = new ArrayList<PhylogenyNode>();
        final TaxonomyComparisonBase tax_comp_base = determineTaxonomyComparisonBase( _gene_tree );
        System.out.println( "comp base is: " + tax_comp_base );
        // Stringyfied taxonomy is the key, node is the value.
        for( final PhylogenyNodeIterator iter = _species_tree.iteratorExternalForward(); iter.hasNext(); ) {
            final PhylogenyNode s = iter.next();
            species_tree_ext_nodes.add( s );
            final String tax_str = taxonomyToString( s, tax_comp_base );
            if ( !ForesterUtil.isEmpty( tax_str ) ) {
                if ( species_to_node_map.containsKey( tax_str ) ) {
                    throw new SdiException( "taxonomy \"" + s + "\" is not unique in species tree" );
                }
                species_to_node_map.put( tax_str, s );
            }
        }
        // Retrieve the reference to the node with a matching stringyfied taxonomy.
        for( final PhylogenyNodeIterator iter = _gene_tree.iteratorExternalForward(); iter.hasNext(); ) {
            final PhylogenyNode g = iter.next();
            if ( !g.getNodeData().isHasTaxonomy() ) {
                if ( _strip_gene_tree ) {
                    _stripped_gene_tree_nodes.add( g );
                }
                else {
                    throw new SdiException( "gene tree node \"" + g + "\" has no taxonomic data" );
                }
            }
            else {
                final String tax_str = taxonomyToString( g, tax_comp_base );
                if ( ForesterUtil.isEmpty( tax_str ) ) {
                    if ( _strip_gene_tree ) {
                        _stripped_gene_tree_nodes.add( g );
                    }
                    else {
                        throw new SdiException( "gene tree node \"" + g + "\" has no appropriate taxonomic data" );
                    }
                }
                else {
                    final PhylogenyNode s = species_to_node_map.get( tax_str );
                    if ( s == null ) {
                        if ( _strip_gene_tree ) {
                            _stripped_gene_tree_nodes.add( g );
                        }
                        else {
                            throw new SdiException( "taxonomy \"" + g.getNodeData().getTaxonomy()
                                    + "\" not present in species tree" );
                        }
                    }
                    else {
                        g.setLink( s );
                        _mapped_species_tree_nodes.add( s );
                        System.out.println( "setting link of " + g + " to " + s );
                    }
                }
            }
        } // for loop
        if ( _strip_gene_tree ) {
            for( final PhylogenyNode g : _stripped_gene_tree_nodes ) {
                _gene_tree.deleteSubtree( g, true );
            }
        }
        if ( _strip_species_tree ) {
            for( PhylogenyNode x : _mapped_species_tree_nodes ) {
                System.out.println( ">>" + x );
            }
            for( final PhylogenyNode s : species_tree_ext_nodes ) {
                System.out.print( ">>>>>>>>>" + s );
                if ( !_mapped_species_tree_nodes.contains( s ) ) {
                    _species_tree.deleteSubtree( s, true );
                    System.out.println( " DELETING" );
                }
                else {
                    System.out.println();
                }
            }
            for( PhylogenyNode x : _mapped_species_tree_nodes ) {
                System.out.println( ">>" + x );
            }
        }
    }

    public Set<PhylogenyNode> getMappedExternalSpeciesTreeNodes() {
        return _mapped_species_tree_nodes;
    }

    //    final private HashMap<Taxonomy, PhylogenyNode> createTaxonomyToNodeMap() {
    //        final HashMap<Taxonomy, PhylogenyNode> speciestree_ext_nodes = new HashMap<Taxonomy, PhylogenyNode>();
    //        for( final PhylogenyNodeIterator iter = _species_tree.iteratorLevelOrder(); iter.hasNext(); ) {
    //            final PhylogenyNode n = iter.next();
    //            if ( n.getNodeData().isHasTaxonomy() ) {
    //                if ( speciestree_ext_nodes.containsKey( n.getNodeData().getTaxonomy() ) ) {
    //                    throw new IllegalArgumentException( "taxonomy [" + n.getNodeData().getTaxonomy()
    //                            + "] is not unique in species phylogeny" );
    //                }
    //                speciestree_ext_nodes.put( n.getNodeData().getTaxonomy(), n );
    //            }
    //        }
    //        return speciestree_ext_nodes;
    //    }
    //    private final void stripGeneTree( final HashMap<Taxonomy, PhylogenyNode> speciestree_ext_nodes ) {
    //        //  final Set<PhylogenyNode> to_delete = new HashSet<PhylogenyNode>();
    //        for( final PhylogenyNodeIterator iter = _gene_tree.iteratorExternalForward(); iter.hasNext(); ) {
    //            final PhylogenyNode g = iter.next();
    //            if ( !g.getNodeData().isHasTaxonomy() ) {
    //                throw new IllegalArgumentException( "gene tree node " + g + " has no taxonomic data" );
    //            }
    //            if ( !speciestree_ext_nodes.containsKey( g.getNodeData().getTaxonomy() ) ) {
    //                _stripped_gene_tree_nodes.add( g );
    //            }
    //        }
    //        for( final PhylogenyNode n : _stripped_gene_tree_nodes ) {
    //            _gene_tree.deleteSubtree( n, true );
    //        }
    //    }
    //    private final void stripGeneTree2( final HashMap<Taxonomy, PhylogenyNode> speciestree_ext_nodes ) {
    //        //  final Set<PhylogenyNode> to_delete = new HashSet<PhylogenyNode>();
    //        for( final PhylogenyNodeIterator iter = _gene_tree.iteratorExternalForward(); iter.hasNext(); ) {
    //            final PhylogenyNode g = iter.next();
    //            if ( !g.getNodeData().isHasTaxonomy() ) {
    //                _stripped_gene_tree_nodes.add( g );
    //            }
    //            else {
    //                if ( !speciestree_ext_nodes.containsKey( g.getNodeData().getTaxonomy() ) ) {
    //                    _stripped_gene_tree_nodes.add( g );
    //                }
    //            }
    //        }
    //        for( final PhylogenyNode n : _stripped_gene_tree_nodes ) {
    //            _gene_tree.deleteSubtree( n, true );
    //        }
    //    }
    public static TaxonomyComparisonBase determineTaxonomyComparisonBase( final Phylogeny gene_tree ) {
        int with_id_count = 0;
        int with_code_count = 0;
        int with_sn_count = 0;
        int max = 0;
        for( final PhylogenyNodeIterator iter = gene_tree.iteratorExternalForward(); iter.hasNext(); ) {
            final PhylogenyNode g = iter.next();
            if ( g.getNodeData().isHasTaxonomy() ) {
                final Taxonomy tax = g.getNodeData().getTaxonomy();
                if ( ( tax.getIdentifier() != null ) && !ForesterUtil.isEmpty( tax.getIdentifier().getValue() ) ) {
                    if ( ++with_id_count > max ) {
                        max = with_id_count;
                    }
                }
                if ( !ForesterUtil.isEmpty( tax.getTaxonomyCode() ) ) {
                    if ( ++with_code_count > max ) {
                        max = with_code_count;
                    }
                }
                if ( !ForesterUtil.isEmpty( tax.getScientificName() ) ) {
                    if ( ++with_sn_count > max ) {
                        max = with_sn_count;
                    }
                }
            }
        }
        if ( max == 0 ) {
            throw new IllegalArgumentException( "gene tree has no taxonomic data" );
        }
        else if ( max == 1 ) {
            throw new IllegalArgumentException( "gene tree has only one node with taxonomic data" );
        }
        else if ( max == with_sn_count ) {
            return SDI.TaxonomyComparisonBase.SCIENTIFIC_NAME;
        }
        else if ( max == with_id_count ) {
            return SDI.TaxonomyComparisonBase.ID;
        }
        else {
            return SDI.TaxonomyComparisonBase.CODE;
        }
    }

    public List<PhylogenyNode> getStrippedExternalGeneTreeNodes() {
        return _stripped_gene_tree_nodes;
    }

    @Override
    public final String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Most parsimonious duplication model: " + _most_parsimonious_duplication_model );
        sb.append( ForesterUtil.getLineSeparator() );
        sb.append( "Speciations sum                    : " + getSpeciationsSum() );
        sb.append( ForesterUtil.getLineSeparator() );
        sb.append( "Duplications sum                   : " + getDuplicationsSum() );
        sb.append( ForesterUtil.getLineSeparator() );
        if ( !_most_parsimonious_duplication_model ) {
            sb.append( "Speciation or duplications sum     : " + getSpeciationOrDuplicationEventsSum() );
            sb.append( ForesterUtil.getLineSeparator() );
        }
        sb.append( "mapping cost L                     : " + computeMappingCostL() );
        return sb.toString();
    }

    static final int[] obtainMinMaxIdIndices( final PhylogenyNode[] linked_nodes ) {
        int max_i = 0;
        int min_i = 0;
        int max_i_id = -Integer.MAX_VALUE;
        int min_i_id = Integer.MAX_VALUE;
        for( int i = 0; i < linked_nodes.length; ++i ) {
            final int id_i = linked_nodes[ i ].getId();
            if ( id_i > max_i_id ) {
                max_i = i;
                max_i_id = linked_nodes[ max_i ].getId();
            }
            if ( id_i < min_i_id ) {
                min_i = i;
                min_i_id = linked_nodes[ min_i ].getId();
            }
        }
        return new int[] { min_i, max_i };
    }
    /**
     * Updates the mapping function M after the root of the gene tree has been
     * moved by one branch. It calculates M for the root of the gene tree and
     * one of its two children.
     * <p>
     * To be used ONLY by method "SDIunrooted.fastInfer(Phylogeny,Phylogeny)".
     * <p>
     * (Last modfied: )
     * 
     * @param prev_root_was_dup
     *            true if the previous root was a duplication, false otherwise
     * @param prev_root_c1
     *            child 1 of the previous root
     * @param prev_root_c2
     *            child 2 of the previous root
     * @return number of duplications which have been assigned in gene tree
     */
    // int updateM( final boolean prev_root_was_dup,
    // final PhylogenyNode prev_root_c1, final PhylogenyNode prev_root_c2 ) {
    // final PhylogenyNode root = getGeneTree().getRoot();
    // if ( ( root.getChildNode1() == prev_root_c1 )
    // || ( root.getChildNode2() == prev_root_c1 ) ) {
    // calculateMforNode( prev_root_c1 );
    // }
    // else {
    // calculateMforNode( prev_root_c2 );
    // }
    // Event event = null;
    // if ( prev_root_was_dup ) {
    // event = Event.createSingleDuplicationEvent();
    // }
    // else {
    // event = Event.createSingleSpeciationEvent();
    // }
    // root.getPhylogenyNodeData().setEvent( event );
    // calculateMforNode( root );
    // return getDuplications();
    // } // updateM( boolean, PhylogenyNode, PhylogenyNode )
    // Helper method for updateM( boolean, PhylogenyNode, PhylogenyNode )
    // Calculates M for PhylogenyNode n, given that M for the two children
    // of n has been calculated.
    // (Last modified: 10/02/01)
    // private void calculateMforNode( final PhylogenyNode n ) {
    // if ( !n.isExternal() ) {
    // boolean was_duplication = n.isDuplication();
    // PhylogenyNode a = n.getChildNode1().getLink(), b = n
    // .getChildNode2().getLink();
    // while ( a != b ) {
    // if ( a.getID() > b.getID() ) {
    // a = a.getParent();
    // }
    // else {
    // b = b.getParent();
    // }
    // }
    // n.setLink( a );
    // Event event = null;
    // if ( ( a == n.getChildNode1().getLink() )
    // || ( a == n.getChildNode2().getLink() ) ) {
    // event = Event.createSingleDuplicationEvent();
    // if ( !was_duplication ) {
    // increaseDuplications();
    // }
    // }
    // else {
    // event = Event.createSingleSpeciationEvent();
    // if ( was_duplication ) {
    // decreaseDuplications();
    // }
    // }
    // n.getPhylogenyNodeData().setEvent( event );
    // }
    // } // calculateMforNode( PhylogenyNode )
}
