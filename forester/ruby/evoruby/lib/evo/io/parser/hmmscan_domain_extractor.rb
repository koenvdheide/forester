#
# = lib/evo/io/parser/hmmscan_domain_extractor.rb - HmmscanDomainExtractor class
#
# Copyright::    Copyright (C) 2017 Christian M. Zmasek
# License::      GNU Lesser General Public License (LGPL)
#
# Last modified: 2017/02/20

require 'lib/evo/util/constants'
require 'lib/evo/msa/msa_factory'
require 'lib/evo/io/msa_io'
require 'lib/evo/io/writer/fasta_writer'
require 'lib/evo/io/parser/fasta_parser'
require 'lib/evo/io/parser/hmmscan_parser'

module Evoruby
  class HmmscanDomainExtractor

    ADD_TO_CLOSE_PAIRS = 0
    def initialize
    end

    # raises ArgumentError, IOError, StandardError
    def parse( domain_id,
      hmmscan_output,
      fasta_sequence_file,
      outfile,
      passed_seqs_outfile,
      failed_seqs_outfile,
      e_value_threshold,
      length_threshold,
      add_position,
      add_domain_number,
      add_species,
      min_linker,
      log )

      Util.check_file_for_readability( hmmscan_output )
      Util.check_file_for_readability( fasta_sequence_file )
      Util.check_file_for_writability( outfile + ".fasta" )
      Util.check_file_for_writability( passed_seqs_outfile )
      Util.check_file_for_writability( failed_seqs_outfile )

      in_msa = nil
      factory = MsaFactory.new()
      in_msa = factory.create_msa_from_file( fasta_sequence_file, FastaParser.new() )

      if ( in_msa == nil || in_msa.get_number_of_seqs() < 1 )
        error_msg = "could not find fasta sequences in " + fasta_sequence_file
        raise IOError, error_msg
      end

      out_msa = Msa.new

      failed_seqs = Msa.new
      passed_seqs = Msa.new
      out_msa_pairs = nil
      out_msa_isolated = nil
      out_msa_singles = nil
      out_msa_single_domains_protein_seqs = nil
      out_msa_close_pairs_protein_seqs = nil
      out_msa_close_pairs_only_protein_seqs = nil
      out_msa_isolated_protein_seqs = nil
      out_msa_isolated_only_protein_seqs = nil
      out_msa_isolated_and_close_pair_protein_seqs = nil
      if min_linker
        out_msa_pairs = Msa.new
        out_msa_isolated = Msa.new
        out_msa_singles = Msa.new
        out_msa_single_domains_protein_seqs = Msa.new
        out_msa_close_pairs_protein_seqs = Msa.new
        out_msa_close_pairs_only_protein_seqs = Msa.new
        out_msa_isolated_protein_seqs = Msa.new
        out_msa_isolated_only_protein_seqs = Msa.new
        out_msa_isolated_and_close_pair_protein_seqs  = Msa.new
      end

      ld = Constants::LINE_DELIMITER

      domain_pass_counter                = 0
      domain_fail_counter                = 0
      passing_domains_per_protein        = 0
      proteins_with_failing_domains      = 0
      domain_not_present_counter         = 0
      protein_counter                    = 1
      max_domain_copy_number_per_protein = -1
      max_domain_copy_number_sequence    = ""
      passing_target_length_sum          = 0
      overall_target_length_sum          = 0
      overall_target_length_min          = 10000000
      overall_target_length_max          = -1
      passing_target_length_min          = 10000000
      passing_target_length_max          = -1

      overall_target_ie_min          = 10000000
      overall_target_ie_max          = -1
      passing_target_ie_min          = 10000000
      passing_target_ie_max          = -1

      hmmscan_datas = []

      hmmscan_parser = HmmscanParser.new( hmmscan_output )
      results = hmmscan_parser.parse

      prev_query = nil
      saw_target = false

      results.each do | r |

        if ( prev_query != nil ) && ( r.query != prev_query )
          protein_counter += 1
          passing_domains_per_protein = 0
          if !saw_target
            log << domain_not_present_counter.to_s + ": " + prev_query.to_s + " lacks target domain" + ld
            domain_not_present_counter += 1
          end
          saw_target = false
        end

        prev_query = r.query

        if domain_id != r.model
          next
        end

        saw_target = true

        sequence  = r.query
        number    = r.number
        out_of    = r.out_of
        env_from  = r.env_from
        env_to    = r.env_to
        i_e_value = r.i_e_value
        prev_query = r.query

        length = 1 + env_to - env_from

        overall_target_length_sum += length
        if length > overall_target_length_max
          overall_target_length_max = length
        end
        if length < overall_target_length_min
          overall_target_length_min = length
        end

        if i_e_value > overall_target_ie_max
          overall_target_ie_max = i_e_value
        end
        if i_e_value < overall_target_ie_min
          overall_target_ie_min = i_e_value
        end

        if ( ( ( e_value_threshold < 0.0 ) || ( i_e_value <= e_value_threshold ) ) &&
        ( ( length_threshold <= 0 ) || ( length >= length_threshold.to_f ) ) )
          hmmscan_datas << HmmsearchData.new( sequence, number, out_of, env_from, env_to, i_e_value )
          passing_target_length_sum += length
          passing_domains_per_protein += 1
          if length > passing_target_length_max
            passing_target_length_max = length
          end
          if length < passing_target_length_min
            passing_target_length_min = length
          end
          if i_e_value > passing_target_ie_max
            passing_target_ie_max = i_e_value
          end
          if i_e_value < passing_target_ie_min
            passing_target_ie_min = i_e_value
          end
          if ( passing_domains_per_protein > max_domain_copy_number_per_protein )
            max_domain_copy_number_sequence    = sequence
            max_domain_copy_number_per_protein = passing_domains_per_protein
          end
        else # no pass
          log << domain_fail_counter.to_s + ": " + sequence.to_s + " fails threshold(s)"
          if ( ( e_value_threshold.to_f >= 0.0 ) && ( i_e_value > e_value_threshold ) )
            log << " iE=" + i_e_value.to_s
          end
          if ( ( length_threshold.to_f > 0 ) && ( env_to - env_from + 1 ) < length_threshold.to_f )
            le = env_to - env_from + 1
            log << " l=" + le.to_s
          end
          log << ld
          domain_fail_counter += 1
        end

        if number > out_of
          error_msg = "number > out_of (this should not have happened)"
          raise StandardError, error_msg
        end

        if number == out_of
          if !hmmscan_datas.empty?
            process_hmmscan_datas( hmmscan_datas,
            in_msa,
            add_position,
            add_domain_number,
            add_species,
            out_msa,
            out_msa_singles,
            out_msa_pairs,
            out_msa_isolated,
            min_linker,
            out_msa_single_domains_protein_seqs,
            out_msa_close_pairs_protein_seqs,
            out_msa_close_pairs_only_protein_seqs,
            out_msa_isolated_protein_seqs,
            out_msa_isolated_only_protein_seqs,
            out_msa_isolated_and_close_pair_protein_seqs )
            domain_pass_counter += hmmscan_datas.length
            if passed_seqs.find_by_name_start( sequence, true ).length < 1
              add_sequence( sequence, in_msa, passed_seqs )
            else
              error_msg = "this should not have happened"
              raise StandardError, error_msg
            end
          else # no pass
            if failed_seqs.find_by_name_start( sequence, true ).length < 1
              add_sequence( sequence, in_msa, failed_seqs )
              proteins_with_failing_domains += 1
            else
              error_msg = "this should not have happened"
              raise StandardError, error_msg
            end
          end
          hmmscan_datas.clear
        end

      end # results.each do | r |

      if (prev_query != nil) && (!saw_target)
        log << domain_not_present_counter.to_s + ": " + prev_query.to_s + " lacks target domain" + ld
        domain_not_present_counter += 1
      end

      if domain_pass_counter < 1
        error_msg = "no domain sequences were extracted"
        raise IOError, error_msg
      end

      if ( domain_not_present_counter + passed_seqs.get_number_of_seqs + proteins_with_failing_domains ) != protein_counter
        error_msg = "not present + passing + not passing != proteins in sequence (fasta) file (this should not have happened)"
        raise StandardError, error_msg
      end

      puts
      log << ld

      log << ld
      avg_pass = ( passing_target_length_sum / domain_pass_counter )
      puts( "Passing target domain lengths: average: " + avg_pass.to_s  )
      log << "Passing target domain lengths: average: " + avg_pass.to_s
      log << ld
      puts( "Passing target domain lengths: min-max: " + passing_target_length_min.to_s + " - "  + passing_target_length_max.to_s)
      log << "Passing target domain lengths: min-max: " + passing_target_length_min.to_s + " - "  + passing_target_length_max.to_s
      log << ld
      puts( "Passing target domain iE:      min-max: " + passing_target_ie_min.to_s + " - "  + passing_target_ie_max.to_s)
      log << "Passing target domain iE:      min-max: " + passing_target_ie_min.to_s + " - "  + passing_target_ie_max.to_s
      log << ld
      puts( "Passing target domains:            sum: " + domain_pass_counter.to_s  )
      log << "Passing target domains:            sum: " + domain_pass_counter.to_s
      log << ld
      log << ld
      puts
      sum = domain_pass_counter + domain_fail_counter
      avg_all = overall_target_length_sum / sum
      puts( "All target domain lengths:     average: " + avg_all.to_s  )
      log << "All target domain lengths:     average: " + avg_all.to_s
      log << ld
      puts( "All target domain lengths:     min-max: " + overall_target_length_min.to_s + " - "  + overall_target_length_max.to_s)
      log << "All target domain lengths:     min-max: " + overall_target_length_min.to_s + " - "  + overall_target_length_max.to_s
      log << ld
      puts( "All target target domain iE:   min-max: " + overall_target_ie_min.to_s + " - "  + overall_target_ie_max.to_s)
      log << "All target target domain iE:   min-max: " + overall_target_ie_min.to_s + " - "  + overall_target_ie_max.to_s
      log << ld
      puts( "All target domains:                sum: " + sum.to_s  )
      log << "All target domains:                sum: " + sum.to_s

      puts
      puts( "Proteins with passing target domain(s): " + passed_seqs.get_number_of_seqs.to_s )
      puts( "Proteins with no passing target domain: " + proteins_with_failing_domains.to_s )
      puts( "Proteins with no target domain        : " + domain_not_present_counter.to_s )

      log << ld
      log << ld
      puts
      puts( "Max target domain copy number per protein: " + max_domain_copy_number_per_protein.to_s )
      log << "Max target domain copy number per protein: " + max_domain_copy_number_per_protein.to_s
      log << ld

      if ( max_domain_copy_number_per_protein > 1 )
        puts( "First target protein with this copy number: " + max_domain_copy_number_sequence )
        log << "First target protein with this copy number: " + max_domain_copy_number_sequence
        log << ld
      end

      write_msa( out_msa, outfile + ".fasta"  )
      write_msa( passed_seqs, passed_seqs_outfile )
      write_msa( failed_seqs, failed_seqs_outfile )

      if out_msa_pairs
        write_msa( out_msa_pairs, outfile + "_" + min_linker.to_s + ".fasta")
      end

      if out_msa_singles
        write_msa( out_msa_singles, outfile + "_singles.fasta")
      end

      if out_msa_isolated
        write_msa( out_msa_isolated, outfile + "_" + min_linker.to_s + "_isolated.fasta");
      end

      if out_msa_single_domains_protein_seqs
        write_msa( out_msa_single_domains_protein_seqs, outfile + "_proteins_with_singles.fasta" )
      end

      if out_msa_close_pairs_protein_seqs
        write_msa( out_msa_close_pairs_protein_seqs, outfile + "_" + min_linker.to_s + "_proteins_close_pairs.fasta" )
      end

      if out_msa_close_pairs_only_protein_seqs
        write_msa( out_msa_close_pairs_only_protein_seqs, outfile + "_" + min_linker.to_s + "_proteins_with_close_pairs_only.fasta" )
      end

      if  out_msa_isolated_protein_seqs
        write_msa(  out_msa_isolated_protein_seqs, outfile + "_" + min_linker.to_s + "_proteins_with_isolated_domains.fasta" )
      end

      if out_msa_isolated_only_protein_seqs
        write_msa( out_msa_isolated_only_protein_seqs, outfile + "_" + min_linker.to_s + "_proteins_with_isolated_domains_only.fasta" )
      end

      if out_msa_isolated_and_close_pair_protein_seqs
        write_msa( out_msa_isolated_and_close_pair_protein_seqs, outfile + "_" + min_linker.to_s + "_proteins_with_isolated_and_close_pairs.fasta" )
      end

      if min_linker
        if ( out_msa_single_domains_protein_seqs.get_number_of_seqs +
        out_msa_close_pairs_only_protein_seqs.get_number_of_seqs +
        out_msa_isolated_only_protein_seqs.get_number_of_seqs +
        out_msa_isolated_and_close_pair_protein_seqs.get_number_of_seqs ) != passed_seqs.get_number_of_seqs
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      end

      log << ld
      log << "passing target domains                       : " + domain_pass_counter.to_s + ld
      log << "failing target domains                       : " + domain_fail_counter.to_s + ld
      log << "proteins in sequence (fasta) file            : " + in_msa.get_number_of_seqs.to_s + ld
      log << "proteins in hmmscan outputfile               : " + protein_counter.to_s + ld
      log << "proteins with passing target domain(s)       : " + passed_seqs.get_number_of_seqs.to_s + ld
      log << "proteins with no passing target domain       : " + proteins_with_failing_domains.to_s + ld
      log << "proteins with no target domain               : " + domain_not_present_counter.to_s + ld
      if min_linker
        log << "min linker length                            : " + min_linker.to_s + ld
        log << "single domains                               : " + out_msa_singles.get_number_of_seqs.to_s + ld
        log << "domains in close pairs                       : " + (2 * out_msa_pairs.get_number_of_seqs).to_s + ld
        log << "isolated domains                             : " + out_msa_isolated.get_number_of_seqs.to_s + ld
        log << "proteins with single domains                 : " + out_msa_single_domains_protein_seqs.get_number_of_seqs.to_s + ld
        log << "proteins with close pair domains             : " + out_msa_close_pairs_protein_seqs.get_number_of_seqs.to_s + ld
        log << "proteins with close pair domains only        : " + out_msa_close_pairs_only_protein_seqs.get_number_of_seqs.to_s + ld
        log << "proteins with isolated domains               : " + out_msa_isolated_protein_seqs.get_number_of_seqs.to_s + ld
        log << "proteins with isolated domains only          : " + out_msa_isolated_only_protein_seqs.get_number_of_seqs.to_s + ld
        log << "proteins with close pair and isolated domains: " + out_msa_isolated_and_close_pair_protein_seqs.get_number_of_seqs.to_s + ld
      end

      log << ld

      return domain_pass_counter

    end # parse

    private

    def write_msa( msa, filename )
      io = MsaIO.new()
      w = FastaWriter.new()
      w.set_line_width( 60 )
      w.clean( true )
      begin
        io.write_to_file( msa, filename, w )
      rescue Exception
        error_msg = "could not write to \"" + filename + "\""
        raise IOError, error_msg
      end
    end

    def add_sequence( sequence_name, in_msa, add_to_msa )
      seqs = in_msa.find_by_name_start( sequence_name, true )
      if ( seqs.length < 1 )
        error_msg = "sequence \"" + sequence_name + "\" not found in sequence file"
        raise StandardError, error_msg
      end
      if ( seqs.length > 1 )
        error_msg = "sequence \"" + sequence_name + "\" not unique in sequence file"
        raise StandardError, error_msg
      end
      seq = in_msa.get_sequence( seqs[ 0 ] )
      add_to_msa.add_sequence( seq )
    end

    def process_hmmscan_datas( hmmscan_datas,
      in_msa,
      add_position,
      add_domain_number,
      add_species,
      out_msa,
      out_msa_singles,
      out_msa_pairs,
      out_msa_isolated,
      min_linker,
      out_msa_single_domains_protein_seqs,
      out_msa_close_pairs_protein_seqs,
      out_msa_close_pairs_only_protein_seqs,
      out_msa_isolated_protein_seqs,
      out_msa_isolated_only_protein_seqs,
      out_msa_isolated_and_close_pair_protein_seqs )

      actual_out_of = hmmscan_datas.size
      saw_close_pair = false
      saw_isolated = false

      seq_name = ""
      prev_seq_name = nil

      hmmscan_datas.each_with_index do |hmmscan_data, index|
        if hmmscan_data.number < ( index + 1 )
          error_msg = "hmmscan_data.number < ( index + 1 ) (this should not have happened)"
          raise StandardError, error_msg
        end

        seq_name =  hmmscan_data.seq_name

        extract_domain( seq_name,
        index + 1,
        actual_out_of,
        hmmscan_data.env_from,
        hmmscan_data.env_to,
        in_msa,
        out_msa,
        add_position,
        add_domain_number,
        add_species )

        if min_linker
          if actual_out_of == 1
            extract_domain( seq_name,
            1,
            1,
            hmmscan_data.env_from,
            hmmscan_data.env_to,
            in_msa,
            out_msa_singles,
            add_position,
            add_domain_number,
            add_species )
            if out_msa_single_domains_protein_seqs.find_by_name_start( seq_name, true ).length < 1
              add_sequence( seq_name, in_msa, out_msa_single_domains_protein_seqs )
            else
              error_msg = "this should not have happened"
              raise StandardError, error_msg
            end

          else
            first = index == 0
            last = index == hmmscan_datas.length - 1

            if ( ( first && ( ( hmmscan_datas[ index + 1 ].env_from - hmmscan_data.env_to ) > min_linker) )  ||
            ( last && ( ( hmmscan_data.env_from - hmmscan_datas[ index - 1 ].env_to ) > min_linker ) ) ||
            ( !first && !last &&  ( ( hmmscan_datas[ index + 1 ].env_from - hmmscan_data.env_to ) > min_linker ) &&
            ( ( hmmscan_data.env_from - hmmscan_datas[ index - 1 ].env_to ) > min_linker ) ) )

              extract_domain( seq_name,
              index + 1,
              actual_out_of,
              hmmscan_data.env_from,
              hmmscan_data.env_to,
              in_msa,
              out_msa_isolated,
              add_position,
              add_domain_number,
              add_species )
              saw_isolated = true

            elsif !first

              from = hmmscan_datas[ index - 1 ].env_from
              to = hmmscan_data.env_to

              if ADD_TO_CLOSE_PAIRS > 0
                from = from - ADD_TO_CLOSE_PAIRS
                if from < 1
                  from = 1
                end
                to = to + ADD_TO_CLOSE_PAIRS
                temp_seqs = in_msa.find_by_name_start( seq_name, true )
                temp_seq = in_msa.get_sequence( temp_seqs[ 0 ] )
                if to >  temp_seq.get_length
                  to =  temp_seq.get_length
                end
              end

              extract_domain( seq_name,
              index.to_s  + "+" + ( index + 1 ).to_s,
              actual_out_of,
              from,
              to,
              in_msa,
              out_msa_pairs,
              add_position,
              add_domain_number,
              add_species )
              saw_close_pair = true
            end
          end
        end
        if prev_seq_name && prev_seq_name != seq_name
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
        prev_seq_name = seq_name
      end # each
      if saw_isolated
        if out_msa_isolated_protein_seqs.find_by_name_start( seq_name, true ).length < 1
          add_sequence( seq_name, in_msa, out_msa_isolated_protein_seqs )
        else
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      end
      if saw_close_pair
        if out_msa_close_pairs_protein_seqs.find_by_name_start( seq_name, true ).length < 1
          add_sequence( seq_name, in_msa, out_msa_close_pairs_protein_seqs )
        else
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      end
      if saw_close_pair && saw_isolated
        if out_msa_isolated_and_close_pair_protein_seqs.find_by_name_start( seq_name, true ).length < 1
          add_sequence( seq_name, in_msa, out_msa_isolated_and_close_pair_protein_seqs )
        else
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      elsif saw_close_pair
        if out_msa_close_pairs_only_protein_seqs.find_by_name_start( seq_name, true ).length < 1
          add_sequence( seq_name, in_msa, out_msa_close_pairs_only_protein_seqs )
        else
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      elsif saw_isolated
        if out_msa_isolated_only_protein_seqs.find_by_name_start( seq_name, true ).length < 1
          add_sequence( seq_name, in_msa, out_msa_isolated_only_protein_seqs )
        else
          error_msg = "this should not have happened"
          raise StandardError, error_msg
        end
      end
    end # def process_hmmscan_data

    def extract_domain( sequence,
      number,
      out_of,
      seq_from,
      seq_to,
      in_msa,
      out_msa,
      add_position,
      add_domain_number,
      add_species )
      if number.is_a?( Fixnum ) && ( number < 1 || out_of < 1 || number > out_of )
        error_msg = "number=" + number.to_s + ", out of=" + out_of.to_s
        raise StandardError, error_msg
      end
      if seq_from < 1 || seq_to < 1 || seq_from >= seq_to
        error_msg = "impossible: seq-from=" + seq_from.to_s + ", seq-to=" + seq_to.to_s
        raise StandardError, error_msg
      end
      seqs = in_msa.find_by_name_start( sequence, true )
      if seqs.length < 1
        error_msg = "sequence \"" + sequence + "\" not found in sequence file"
        raise IOError, error_msg
      end
      if seqs.length > 1
        error_msg = "sequence \"" + sequence + "\" not unique in sequence file"
        raise IOError, error_msg
      end
      # hmmscan is 1 based, whereas sequences are 0 bases in this package.
      seq = in_msa.get_sequence( seqs[ 0 ] ).get_subsequence( seq_from - 1, seq_to - 1 )

      orig_name = seq.get_name

      seq.set_name( orig_name.split[ 0 ] )

      if add_position
        seq.set_name( seq.get_name + "_" + seq_from.to_s + "-" + seq_to.to_s )
      end

      if out_of != 1 && add_domain_number
        seq.set_name( seq.get_name + "~" + number.to_s + "-" + out_of.to_s )
      end

      if add_species
        a = orig_name.rindex "["
        b = orig_name.rindex "]"
        unless a && b
          error_msg = "species not found in " + orig_name
          raise StandardError, error_msg
        end
        species = orig_name[ a .. b ]
        seq.set_name( seq.get_name + " " + species )
      end
      out_msa.add_sequence( seq )
    end

    def is_ignorable?( line )
      return ( line !~ /[A-Za-z0-9-]/ || line =~/^#/ )
    end

  end # class HmmscanDomainExtractor

  class HmmsearchData
    def initialize( seq_name, number, out_of, env_from, env_to, i_e_value )
      @seq_name = seq_name
      @number = number
      @out_of = out_of
      @env_from = env_from
      @env_to = env_to
      @i_e_value = i_e_value
    end
    attr_reader :seq_name, :number, :out_of, :env_from, :env_to, :i_e_value
  end

end # module Evoruby

