#!/usr/bin/perl -w

	use strict ;
	no warnings 'utf8';

	binmode(STDOUT, ':utf8');

	my $data_dir = shift(@ARGV) or die "You must specify a writable data directory containing a single split file, and the anchor.csv file produced by extractWikipediaData\n" ;
	
	my $max_ngram_length = 10 ;
	my $report_rate = 0.001;
	

	
	
	
	# logging===================================================================================================================
	
	open (LOG, "> $data_dir/log.txt") or die "data dir '$data_dir' is not writable. \n" ;
	binmode(LOG, ':utf8');
	
	# get data files ============================================================================================================

	my $split_file ;
	my $split_index ;
	
	my @files = <$data_dir/*>;
	foreach my $file (@files) {
	   if ($file =~ m/split_(\d+)\.csv$/) {
	      if (defined $split_file) {
		  		die "the data directory '$data_dir' contains multiple split files\n" ;
	      } else {
		  		$split_file = $file ;
		  		$split_index = $1 ;
	      }
	   }
	}

	if (not defined $split_file) {
	    die "the data directory '$data_dir' does not contain any split files\n" ;
	}

	# get anchors - just set count=0 ==============================================================================================

	my %anchorFreq = () ;             #anchor text -> freq

	open(ANCHOR, "$data_dir/anchor.csv") || die "'$data_dir/anchor.csv' could not be found\n" ;
	binmode(ANCHOR, ':utf8') ;

	my $start_time = time  ;

	my $parts_total = -s "$data_dir/anchor.csv";
	my $parts_done = 0 ;

	while (defined(my $line=<ANCHOR>)) {
	  $parts_done = $parts_done + length $line ;

	  chomp($line) ;
	    
	  if ($line =~ m/\"(.+?)\",(\d+),(\d+)(,\d+)?/) {
			my $anchor = $1 ;
			my $id = $2 ;
			my $count = $3 ;
						
			$anchorFreq{$anchor} = 0 ;
	  }
	  print_progress("Loading anchors", $start_time, $parts_done, $parts_total) ;
	}
	close ANCHOR ;

	print_progress("Loading anchors", $start_time, $parts_total, $parts_total) ;
	print "\n" ;

	# get number of documents in which these anchors occur (as plain text or within links, we dont care) ============================

	$start_time = time ;
	$parts_total = -s $split_file ;
	$parts_done = 0 ;
	
	open(SPLIT, $split_file) ;
	binmode(SPLIT, ':utf8') ;
	
	my %ngrams_seen ;

	while (defined(my $line=<SPLIT>)) {
	
		%ngrams_seen = () ;
    
   	$parts_done = $parts_done + length $line ;
    
    if ($line =~ m/^(\d+),\"(.+?)\"$/) {
    	my $id = $1 ;
    	my $content = $2 ;
    	
    	$content = unescape_text($content) ;
    	
    	$content = strip_text($content) ;

			while($content =~ m/(.*?)\n/gi) {	

		    my $line = $1 ;
		    #print LOG "LINE: $line\n" ;
		    
		    get_ngrams($line, %ngrams_seen) ;	
			}
    }
    print_progress("Gathering anchor occurances", $start_time, $parts_done, $parts_total) ;
	}

	close SPLIT ;

	print_progress("Gathering anchor occurances", $start_time, $parts_total, $parts_total) ;
	print("\n") ;

	# save anchor frequencies...................................................................

	$start_time = time ;

	open(OCCURANCES, "> $data_dir/anchor_occurance_".$split_index.".csv") ;
	binmode(OCCURANCES, ':utf8');

	$parts_total = scalar keys %anchorFreq ; 
	$parts_done = 0 ;

	while (my ($anchor, $freq) = each(%anchorFreq) ) {
    $parts_done++ ;

    print OCCURANCES "\"$anchor\",$freq\n" ;
    print_progress("Printing anchor occurances", $start_time, $parts_done, $parts_total) ;
	}

	print_progress("Printing n-grams and frequencies", $start_time, $parts_total, $parts_total) ;
	print "\n" ;

	close(OCCURANCES) ;
 

	sub get_ngrams {
  	my $line = shift;
    my @words = split(" ",$line);
    #my $max = 5 ;
    
    for (my $i = 0;$i <= $#words; $i++) {
			for (my $j = $i+$max_ngram_length; $j >= $i; $j--) {
	    
	    	if ($j > $#words) {
					$j = $#words;
	    	}
    
	    	my $ngram = subarray($i, $j, @words);
	    	
	    	if (not defined $ngrams_seen{$ngram}) {
	    	
	    		my $freq = $anchorFreq{$ngram} ;
	    
	    		if (defined $freq) {
						$anchorFreq{$ngram} = $freq + 1 ;
						#print LOG "$ngram,$freq\n" ;
	    		}
	    		$ngrams_seen{$ngram} = 1 ;
	    	}
			}
    }
	}

	sub subarray ($$@) {
    my $start = shift;
    my $end = shift;
    my @array = @_;
    my @subarray = ();
    
    for (my $i = $start; $i <= $end; $i++) {
			push(@subarray,$array[$i]);
    }
    return join(" ",@subarray);
	}
	
	sub unescape_text {
    my $text = shift ;
    
    $text =~ s/\\\\/\\/g ;
    $text =~ s/\\\"/\"/g ;
    $text =~ s/\\n/\n/g ;

    return $text ;
	}	

	# removes all markup
	sub strip_text {

    my $text = shift ;

    $text =~ s/<!-{2,}((.|\n)*?)-{2,}>//g ;             #remove comments

    #formatting
    $text =~ s/\'{2,}//g ;                              #remove all bold and italic markup
    $text =~ s/\={2,}//g ;                              #remove all header markup

    #templates
    $text =~ s/\{\{((?:[^{}]+|\{(?!\{)|\}(?!\}))*)\}\}//sxg ;  #remove all templates that dont have any templates in them
    $text =~ s/\{\{((.|\n)*?)\}\}//g ;                         #repeat to get rid of nested templates
    $text =~ s/\{\|((.|\n)+?)\|\}//g ;                         #remove {|...|} structures
    
    #links
    $text =~ s/\[\[([^\[\]\:]*?)\|([^\[\]]*?)\]\]/$2/g ;   #replace piped links with anchor texts, as long as they dont contain other links ;
    $text =~ s/\[\[([^\[\]\:]*?)\]\]/$1/g ;                #replace unpiped links with content, as long as they dont contain other links ;

    $text =~ s/\[\[wiktionary\:(.+?)\|(.+?)\]\]/$2/gi ;       #retain piped wiktionary links
    $text =~ s/\[\[wiktionary\:(.*?)\]\]/$1/gi ;              #retain unpiped wiktionary links
    
    $text =~ s/\[\[(.*?)\]\]//g ;                #remove remaining links (they must have unwanted namespaces).

    $text =~ s/\[(.*?)\s(.*?)\]/$2/g ;           #replace external links with anchor text

    #references 
    $text =~ s/\<ref\/\>//gi ;                          #remove simple ref tags
    $text =~ s/\<ref\>((.|\n)*?)\<\/ref\>//gi ;         #remove ref tags and all content between them. 
    $text =~ s/\<ref\s(.+?)\>((.|\n)*?)\<\/ref\>//gi ;  #remove ref tags and all content between them (with attributes).
    
    #whitespace
    $text =~ s/\n{3,}/\n\n/g ;   #collapse multiple newlines

    #html tags
    $text =~ s/\<(.+?)\>//g ;   

    return $text ;
	}


	# cleans the given text so that it can be safely inserted into database
	sub clean_text {
    my $text = shift ; 
    
    $text =~ s/\\/\\\\/g;     #escape escape chars
    $text =~ s/\"/\\\"/g;     #escape double quotes
    $text =~ s/\n/\\n/g ;  #escape newlines 
    $text =~ s/^\s+|\s+$//g;  #remove leading & trailing spaces

    return $text ;
	}
	
	
	# displaying progress ============================================================================================================

	my $msg ;
	my $last_report_time ;
	
	sub format_percent {
    return sprintf("%.2f",($_[0] * 100))."%" ;
	}

	sub format_time {
    my @t = gmtime($_[0]) ;

    my $hr = $t[2] + (24*$t[7]) ;
    my $min = $t[1] ;
    my $sec = $t[0] ;
	
    return sprintf("%02d:%02d:%02d",$hr, $min, $sec) ; 
	}

	sub print_progress {
	
    my $message = shift ;
    my $start_time = shift ;
    my $parts_done = shift ;
    my $parts_total = shift ;
    
    if (not defined $last_report_time) {
    	$last_report_time = $start_time
    }
    
    if (time == $last_report_time && $parts_done < $parts_total) {
			#do not report if we reported less than a second ago, unless we have finished.
			return ;
		}

    my $work_done = $parts_done/$parts_total ;    
    my $time_elapsed = time - $start_time ;
    my $time_expected = (1/$work_done) * $time_elapsed ;
    my $time_remaining = $time_expected - $time_elapsed ;
    $last_report_time = time ;

		#clear 
    if (defined $msg) {
			$msg =~ s/./\b/g ;
			print $msg ;
    }
    
    #flush output, so we definitely see this message
    $| = 1 ;
    
    if ($parts_done >= $parts_total) {
    	$msg = $message.": done in ".format_time($time_elapsed)."                          " ;
    } else {
    	$msg = $message.": ".format_percent($work_done)." in ".format_time($time_elapsed).", ETA:".format_time($time_remaining) ;
    }
    
    print $msg ;
	}
