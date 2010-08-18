	#!/usr/bin/perl -w

	use strict ;
	no warnings 'utf8';

	binmode(STDOUT, ':utf8');

	my $data_dir = shift(@ARGV) or die "You must specify a writable data directory containing 'anchor.csv' and the various anchorOccurance files produced by extractAnchorOccurances\n" ;
	my $file_count = int shift(@ARGV) or die "You must specify the number of files the data was split data into \n" ;
	
	# check that all files are there ===============================================================================================
	
	for (my $i = 0 ; $i<$file_count ; $i++) {
	
		my $file = "$data_dir/anchor_occurance_$i.csv" ;
		
		if (not -e $file) {
			die "'$file' is missing\n" ;
		}
	}
	
	# read anchors =================================================================================================================
	
	my %link_counts = () ;
	my %occ_counts = () ;

	open(ANCHOR, "$data_dir/anchor.csv") || die "'$data_dir/anchor.csv' is missing\n" ;
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
			
			my $linkCount = $link_counts{$anchor} ;
			
			if (defined $linkCount) {
				$link_counts{$anchor} = $linkCount + $count ;
			} else {
				$link_counts{$anchor} = $count ;
			}
			
			$occ_counts{$anchor} = 0 ;
	  }
	  print_progress("loading anchors", $start_time, $parts_done, $parts_total) ;
	}
	close ANCHOR ;

	print_progress("loading anchors", $start_time, $parts_total, $parts_total) ;
	print "\n" ;
	
	# read occurance counts ==========================================================================================================
	
	for (my $i = 0 ; $i<$file_count ; $i++) {
	
		my $file = "$data_dir/anchor_occurance_$i.csv" ;
		
		open(OCCURANCES, $file) ;
		binmode(OCCURANCES, ':utf8') ;
		
		my $start_time = time  ;
		my $parts_total = -s $file ;
		my $parts_done = 0 ;
		
		while (defined(my $line=<OCCURANCES>)) {
	  	$parts_done = $parts_done + length $line ;
	  	
	  	if ($line =~ m/^\"(.+?)\",(\d+)$/) {
	  		my $anchor = $1 ;
				my $count = $2 ;
				
				my $occCount = $occ_counts{$anchor} ;
	  	
	  		if (defined $occCount) {
					$occ_counts{$anchor} = $occCount + $count ;
				} else {
					$occ_counts{$anchor} = $count ;
				}
	  	}
	  	print_progress("merging anchor occurances (pass ".($i+1)." of $file_count)", $start_time, $parts_done, $parts_total) ;
	  }
		print_progress("merging anchor occurances (pass ".($i+1)." of $file_count)", $start_time, $parts_total, $parts_total) ;
		print "\n" ;
		
		close OCCURANCES ;
	}
	
	# print occurance counts ==========================================================================================================
	
	$start_time = time ;

	open(OCCURANCES, "> $data_dir/anchor_occurance.csv") ;
	binmode(OCCURANCES, ':utf8');

	$parts_total = scalar keys %occ_counts ; 
	$parts_done = 0 ;

	while (my ($anchor, $occCount) = each(%occ_counts) ) {
		$parts_done++ ;
		
		my $linkCount = $link_counts{$anchor} ;
		
		if (defined $linkCount) {
    	print OCCURANCES "\"$anchor\",$linkCount,$occCount\n" ;
    }
    print_progress("saving merged anchor occurances", $start_time, $parts_done, $parts_total) ;
	}

	print_progress("saving merged anchor occurances", $start_time, $parts_total, $parts_total) ;
	print "\n" ;

	close(OCCURANCES) ;
	
	
	
	
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
