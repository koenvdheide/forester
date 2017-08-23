// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2017 Christian M. Zmasek
// Copyright (C) 2017 J. Craig Venter Institute
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
// Contact: phyloxml @ gmail . com
// WWW: https://sites.google.com/site/cmzmasek/home/software/forester

package org.forester.clade_analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.forester.util.ForesterUtil;

public final class ResultMulti {

    private final String       _separator;
    private final List<Prefix> _greatest_common_prefixes      = new ArrayList<Prefix>();
    private final List<Prefix> _greatest_common_prefixes_up   = new ArrayList<Prefix>();
    private final List<Prefix> _greatest_common_prefixes_down = new ArrayList<Prefix>();
    private List<Prefix>       _all                           = null;
    private List<Prefix>       _collapsed                     = null;
    private List<Prefix>       _cleaned_spec                  = null;
    private boolean            _has_specifics                 = false;
    private List<Prefix>       _all_up                        = null;
    private List<Prefix>       _collapsed_up                  = null;
    private List<Prefix>       _cleaned_spec_up               = null;
    private boolean            _has_specifics_up              = false;
    private List<Prefix>       _all_down                      = null;
    private List<Prefix>       _collapsed_down                = null;
    private List<Prefix>       _cleaned_spec_down             = null;
    private boolean            _has_specifics_down            = false;

    ResultMulti( final String separator ) {
        _separator = separator;
        reset();
    }

    ResultMulti() {
        _separator = AnalysisMulti.DEFAULT_SEPARATOR;
        reset();
    }

    public List<Prefix> getAllMultiHitPrefixesUp() {
        return _all_up;
    }

    public List<Prefix> getCollapsedMultiHitPrefixesUp() {
        return _collapsed_up;
    }

    public List<Prefix> getSpecificMultiHitPrefixesUp() {
        return _cleaned_spec_up;
    }

    public boolean isHasSpecificMultiHitsPrefixesUp() {
        return _has_specifics_up;
    }

    public List<Prefix> getAllMultiHitPrefixesDown() {
        return _all_down;
    }

    public List<Prefix> getCollapsedMultiHitPrefixesDown() {
        return _collapsed_down;
    }

    public List<Prefix> getSpecificMultiHitPrefixesDown() {
        return _cleaned_spec_down;
    }

    public boolean isHasSpecificMultiHitsPrefixesDown() {
        return _has_specifics_down;
    }

    public List<Prefix> getAllMultiHitPrefixes() {
        return _all;
    }

    public List<Prefix> getCollapsedMultiHitPrefixes() {
        return _collapsed;
    }

    public List<Prefix> getSpecificMultiHitPrefixes() {
        return _cleaned_spec;
    }

    public boolean isHasSpecificMultiHitsPrefixes() {
        return _has_specifics;
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
      //  sb.append( "Cleaned:" );
      //  sb.append( ForesterUtil.LINE_SEPARATOR );
      //  for( final Prefix prefix : _all ) {
       //     sb.append( prefix );
       //     sb.append( ForesterUtil.LINE_SEPARATOR );
       // }
       // sb.append( ForesterUtil.LINE_SEPARATOR );
        sb.append( "Collapsed:" );
        sb.append( ForesterUtil.LINE_SEPARATOR );
        for( final Prefix prefix : _collapsed ) {
            sb.append( prefix );
            sb.append( ForesterUtil.LINE_SEPARATOR );
        }
        if ( _has_specifics ) {
            sb.append( ForesterUtil.LINE_SEPARATOR );
            sb.append( "Specifics:" );
            sb.append( ForesterUtil.LINE_SEPARATOR );
            for( final Prefix prefix : _cleaned_spec ) {
                sb.append( prefix );
                sb.append( ForesterUtil.LINE_SEPARATOR );
            }
            sb.append( ForesterUtil.LINE_SEPARATOR );
            sb.append( "Collapsed With Specifics:" );
            sb.append( ForesterUtil.LINE_SEPARATOR );
            for( final Prefix prefix : _collapsed ) {
                sb.append( prefix );
                sb.append( ForesterUtil.LINE_SEPARATOR );
                for( final Prefix spec : _cleaned_spec ) {
                    if ( spec.getPrefix().startsWith( prefix.getPrefix() ) ) {
                        sb.append( "    " + spec );
                        sb.append( ForesterUtil.LINE_SEPARATOR );
                    }
                }
            }
        }
        if ( !ForesterUtil.isEmpty( _all_down ) ) {
            sb.append( ForesterUtil.LINE_SEPARATOR );
        //    sb.append( "Cleaned Down:" );
        //    sb.append( ForesterUtil.LINE_SEPARATOR );
        //    for( final Prefix prefix : _all_down ) {
        //        sb.append( prefix );
        //        sb.append( ForesterUtil.LINE_SEPARATOR );
         //   }
           // sb.append( ForesterUtil.LINE_SEPARATOR );
            sb.append( "Collapsed Down:" );
            sb.append( ForesterUtil.LINE_SEPARATOR );
            for( final Prefix prefix : _collapsed_down ) {
                sb.append( prefix );
                sb.append( ForesterUtil.LINE_SEPARATOR );
            }
          /*  if ( _has_specifics_down ) {
                sb.append( ForesterUtil.LINE_SEPARATOR );
                sb.append( "Specifics Down:" );
                sb.append( ForesterUtil.LINE_SEPARATOR );
                for( final Prefix prefix : _cleaned_spec_down ) {
                    sb.append( prefix );
                    sb.append( ForesterUtil.LINE_SEPARATOR );
                }
                sb.append( ForesterUtil.LINE_SEPARATOR );
                sb.append( "Collapsed With Specifics Down:" );
                sb.append( ForesterUtil.LINE_SEPARATOR );
                for( final Prefix prefix : _collapsed_down ) {
                    sb.append( prefix );
                    sb.append( ForesterUtil.LINE_SEPARATOR );
                    for( final Prefix spec : _cleaned_spec_down ) {
                        if ( spec.getPrefix().startsWith( prefix.getPrefix() ) ) {
                            sb.append( "    " + spec );
                            sb.append( ForesterUtil.LINE_SEPARATOR );
                        }
                    }
                }
            }*/
        }
        if ( !ForesterUtil.isEmpty( _all_up ) ) {
            sb.append( ForesterUtil.LINE_SEPARATOR );
        //    sb.append( "Cleaned Up:" );
        //    sb.append( ForesterUtil.LINE_SEPARATOR );
        //    for( final Prefix prefix : _all_up ) {
        //        sb.append( prefix );
         //       sb.append( ForesterUtil.LINE_SEPARATOR );
         //   }
         //   sb.append( ForesterUtil.LINE_SEPARATOR );
            sb.append( "Collapsed Up:" );
            sb.append( ForesterUtil.LINE_SEPARATOR );
            for( final Prefix prefix : _collapsed_up ) {
                sb.append( prefix );
                sb.append( ForesterUtil.LINE_SEPARATOR );
            }
          /*  if ( _has_specifics ) {
                sb.append( ForesterUtil.LINE_SEPARATOR );
                sb.append( "Specifics Up:" );
                sb.append( ForesterUtil.LINE_SEPARATOR );
                for( final Prefix prefix : _cleaned_spec_up ) {
                    sb.append( prefix );
                    sb.append( ForesterUtil.LINE_SEPARATOR );
                }
                sb.append( ForesterUtil.LINE_SEPARATOR );
                sb.append( "Collapsed With Specifics Up:" );
                sb.append( ForesterUtil.LINE_SEPARATOR );
                for( final Prefix prefix : _collapsed_up ) {
                    sb.append( prefix );
                    sb.append( ForesterUtil.LINE_SEPARATOR );
                    for( final Prefix spec : _cleaned_spec_up ) {
                        if ( spec.getPrefix().startsWith( prefix.getPrefix() ) ) {
                            sb.append( "    " + spec );
                            sb.append( ForesterUtil.LINE_SEPARATOR );
                        }
                    }
                }
            }*/
        }
        return sb.toString();
    }

    void addGreatestCommonPrefix( final String prefix, final double confidence ) {
        _greatest_common_prefixes.add( new Prefix( prefix, confidence, _separator ) );
    }

    void addGreatestCommonPrefixUp( final String prefix_up, final double confidence ) {
        _greatest_common_prefixes_up.add( new Prefix( prefix_up, confidence, _separator ) );
    }

    void addGreatestCommonPrefixDown( final String prefix_down, final double confidence ) {
        _greatest_common_prefixes_down.add( new Prefix( prefix_down, confidence, _separator ) );
    }

    final void analyze( final double cutoff_for_specifics ) {
        reset();
        analyzeGreatestCommonPrefixes( _greatest_common_prefixes, _separator, cutoff_for_specifics );
        analyzeGreatestCommonPrefixesUp( _greatest_common_prefixes_up, _separator, cutoff_for_specifics );
        analyzeGreatestCommonPrefixesDown( _greatest_common_prefixes_down, _separator, cutoff_for_specifics );
    }

    private final void reset() {
        _all = new ArrayList<Prefix>();
        _collapsed = new ArrayList<Prefix>();
        _cleaned_spec = new ArrayList<Prefix>();
        _has_specifics = false;
        _all_up = new ArrayList<Prefix>();
        _collapsed_up = new ArrayList<Prefix>();
        _cleaned_spec_up = new ArrayList<Prefix>();
        _has_specifics_up = false;
        _all_down = new ArrayList<Prefix>();
        _collapsed_down = new ArrayList<Prefix>();
        _cleaned_spec_down = new ArrayList<Prefix>();
        _has_specifics_down = false;
    }

    private final void analyzeGreatestCommonPrefixes( final List<Prefix> greatest_common_prefixes,
                                                      final String separator,
                                                      final double cutoff ) {
        final List<Prefix> l = obtainAllPrefixes( greatest_common_prefixes, separator );
        if ( !ForesterUtil.isEmpty( l ) ) {
            sortPrefixesAccordingToConfidence( l );
            _all = removeLessSpecificPrefixes( l );
            _collapsed = collapse( _all );
            _has_specifics = false;
            if ( cutoff >= 0 ) {
                _cleaned_spec = obtainSpecifics( cutoff, _all, _collapsed );
                if ( !ForesterUtil.isEmpty( _cleaned_spec ) ) {
                    _has_specifics = true;
                }
            }
        }
    }

    private final void analyzeGreatestCommonPrefixesUp( final List<Prefix> greatest_common_prefixes_up,
                                                        final String separator,
                                                        final double cutoff ) {
        final List<Prefix> l = obtainAllPrefixes( greatest_common_prefixes_up, separator );
        if ( !ForesterUtil.isEmpty( l ) ) {
            sortPrefixesAccordingToConfidence( l );
            _all_up = removeLessSpecificPrefixes( l );
            _collapsed_up = collapse( _all_up );
            _has_specifics_up = false;
            if ( cutoff >= 0 ) {
                _cleaned_spec_up = obtainSpecifics( cutoff, _all_up, _collapsed_up );
                if ( !ForesterUtil.isEmpty( _cleaned_spec_up ) ) {
                    _has_specifics_up = true;
                }
            }
        }
    }

    final void analyzeGreatestCommonPrefixesDown( final List<Prefix> greatest_common_prefixes_down,
                                                  final String separator,
                                                  final double cutoff ) {
        final List<Prefix> l = obtainAllPrefixes( greatest_common_prefixes_down, separator );
        if ( !ForesterUtil.isEmpty( l ) ) {
            sortPrefixesAccordingToConfidence( l );
            _all_down = removeLessSpecificPrefixes( l );
            _collapsed_down = collapse( _all_down );
            _has_specifics_down = false;
            if ( cutoff >= 0 ) {
                _cleaned_spec_down = obtainSpecifics( cutoff, _all_down, _collapsed_down );
                if ( !ForesterUtil.isEmpty( _cleaned_spec_down ) ) {
                    _has_specifics_down = true;
                }
            }
        }
    }

    final static List<Prefix> obtainSpecifics( final double cutoff,
                                               final List<Prefix> cleaned,
                                               final List<Prefix> collapsed ) {
        final List<Prefix> cleaned_spec = new ArrayList<>();
        final Set<String> collapsed_set = new HashSet<>();
        for( final Prefix prefix : collapsed ) {
            collapsed_set.add( prefix.getPrefix() );
        }
        final List<Prefix> spec = new ArrayList<>();
        for( final Prefix prefix : cleaned ) {
            if ( ( prefix.getConfidence() >= cutoff ) && !collapsed_set.contains( prefix.getPrefix() ) ) {
                spec.add( prefix );
            }
        }
        for( final Prefix o : spec ) {
            boolean ok = true;
            for( final Prefix i : spec ) {
                if ( ( !o.getPrefix().equals( i.getPrefix() ) ) && ( i.getPrefix().startsWith( o.getPrefix() ) ) ) {
                    ok = false;
                    break;
                }
            }
            if ( ok ) {
                cleaned_spec.add( o );
            }
        }
        return cleaned_spec;
    }

    private final static List<Prefix> collapse( final List<Prefix> cleaned ) {
        final List<Prefix> collapsed = new ArrayList<>();
        final Set<String> firsts = new HashSet<>();
        double confidence_sum = 0;
        for( final Prefix prefix : cleaned ) {
            final String f = prefix.getPrefixFirstElement();
            if ( !firsts.contains( f ) ) {
                firsts.add( f );
                collapsed.add( prefix );
                confidence_sum += prefix.getConfidence();
            }
        }
        if ( !ForesterUtil.isEqual( confidence_sum, 1.0, 1E-5 ) ) {
            throw new IllegalArgumentException( "Confidences add up to " + confidence_sum + " instead of 1.0" );
        }
        return collapsed;
    }

    /*
     * This replaces (by way of example)
     * A.1.1 0.9
     * A.1   0.9
     * with
     * A.1.1 0.9
     *
     * I.e. it removes less specific prefixes.
     *
     */
    private final static List<Prefix> removeLessSpecificPrefixes( final List<Prefix> l ) {
        final List<Prefix> cleaned = new ArrayList<>();
        for( final Prefix o : l ) {
            boolean ok = true;
            for( final Prefix i : l ) {
                if ( ( !o.getPrefix().equals( i.getPrefix() ) ) && ( i.getPrefix().startsWith( o.getPrefix() ) )
                        && ForesterUtil.isEqual( i.getConfidence(),
                                                 o.getConfidence() ) ) {
                    ok = false;
                    break;
                }
            }
            if ( ok ) {
                cleaned.add( o );
            }
        }
        return cleaned;
    }

    private final static void sortPrefixesAccordingToConfidence( final List<Prefix> l ) {
        Collections.sort( l, new Comparator<Prefix>() {

            @Override
            public int compare( final Prefix x, final Prefix y ) {
                return compare( x.getConfidence(), y.getConfidence() );
            }

            private int compare( final double a, final double b ) {
                return a > b ? -1 : a > b ? 1 : 0;
            }
        } );
    }

    private final static List<Prefix> obtainAllPrefixes( final List<Prefix> greatest_common_prefixes,
                                                         final String separator ) {
        final SortedMap<String, Double> map = new TreeMap<>();
        for( final Prefix prefix : greatest_common_prefixes ) {
            final List<String> prefixes = ForesterUtil.spliIntoPrefixes( prefix.getPrefix(), separator );
            for( final String p : prefixes ) {
                map.put( p, 0.0 );
            }
        }
        for( final String key : map.keySet() ) {
            for( final Prefix prefix : greatest_common_prefixes ) {
                if ( prefix.getPrefix().startsWith( key ) ) {
                    map.put( key, map.get( key ) + prefix.getConfidence() );
                }
            }
        }
        final List<Prefix> l = new ArrayList<>();
        for( final Entry<String, Double> entry : map.entrySet() ) {
            l.add( new Prefix( entry.getKey(), entry.getValue(), separator ) );
        }
        return l;
    }
}
