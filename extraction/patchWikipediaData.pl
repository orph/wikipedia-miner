	#!/usr/bin/perl -w

	# saves pages, pagelinks (with redirects resolved), category links, disambiguation links, link counts, anchors, translations
	 
	use strict ;
	use File::Copy;
	no warnings 'utf8';

	binmode(STDOUT, ':utf8') ;	

  # gather arguments
  my $args = join(' ',@ARGV);
	my $data_dir = shift(@ARGV) or die "You must specify a writable data directory containing a single WikiMedia dump file\n" ;
	
	my $passes = 2 ; 
	if ($args =~ m/-passes[\s\=]*(\d+)/i) {
	 $passes = $1 ;
	}
	
	print "data will be split into $passes passes for memory-intesive operations. Try using more passes if you run into problems.\n" ;
	
	#check that anchor and page (mandatory files) exist
	
	if (not (-e "$data_dir/anchor.csv" and -W "$data_dir/anchor.csv")) { 
		die "'$data_dir/anchor.csv' could not be found\n" ;
	}
	
	if (not (-e "$data_dir/redirect.csv" and -r "$data_dir/redirect.csv")) {
		die "'$data_dir/redirect.csv' could not be found or is not readable\n" ;
	}
	
	if (not (-e "$data_dir/page.csv" and -r "$data_dir/page.csv")) {
		die "'$data_dir/page.csv' could not be found or is not readable\n" ;
	}
	
	
	#check that anchor needs patching
	
	open(ANCHOR, "$data_dir/anchor.csv") ;
	binmode(ANCHOR, ':utf8') ;
	my $peekline=<ANCHOR> ;
	close(ANCHOR) ;

	if ($peekline =~ m/\"(.+?)\",(\d+),(\d+),(\d+)/) {
			print "'$data_dir/anchor.csv' does not need to be patched\n" ;
	} else {
	
		print "Patching anchors" ;
	
		#move anchor to a safe place
		move("$data_dir/anchor.csv", "$data_dir/anchor.csv.old") or die ("could not back up anchor file. '$data_dir' is not writeable.");
		print " - backing up anchors to '$data_dir/anchor.csv.old'\n" ;
		
		#load redirects into memory
		my %redirects = () ;
		
		my $start_time = time  ;
    my $parts_total = -s "$data_dir/redirect.csv" ; 
    my $parts_done = 0 ;
    
    open(REDIRECT, "$data_dir/redirect.csv") ;
    
    while (defined (my $line = <REDIRECT>)) {
			$parts_done = $parts_done + length $line ;    
			chomp($line) ;
    
			if ($line =~ m/^(\d+),(\d+)$/) {
			
				my $rd_from = int $1 ;
				my $rd_to = int $2 ;
				
				$redirects{$rd_from} = $rd_to ;
			}
			print_progress(" - loading redirects", $start_time, $parts_done, $parts_total) ;  
		}	
		
		close REDIRECT ;
		
		print_progress(" - loading redirects", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;
		
		
		
		#load anchors into memory
		my %anchors = () ;  #\"anchor\":id -> (freq:flag)
    my $anchorCount = 0 ;
		
		open(ANCHOR, "$data_dir/anchor.csv.old") ;
		binmode(ANCHOR, ':utf8') ;
		
		$start_time = time  ;

		$parts_total = -s "$data_dir/anchor.csv.old";
		$parts_done = 0 ;

		while (defined(my $line=<ANCHOR>)) {
	  	$parts_done = $parts_done + length $line ;

	  	chomp($line) ;
	    
	  	if ($line =~ m/\"(.+?)\",(\d+),(\d+)/) {
				my $anchor_text = $1 ;
				my $target_id = $2 ;
				my @array = ($3,0) ;
			
				$anchors{"\"$anchor_text\":$target_id"} = \@array ;
				$anchorCount ++ ;
	  	}
	  	
	  	print_progress(" - loading old anchors", $start_time, $parts_done, $parts_total) ;
		}
		close ANCHOR ;

		print_progress(" - loading old anchors", $start_time, $parts_total, $parts_total) ;
		print "\n" ;
		
		close ANCHOR ;
		
		 #flag any anchor:dest combinations that are mirrored by redirects or article titles, and add titles and redirects if they havent been used as anchors yet.
    
    $start_time = time ;
    $parts_total = -s "$data_dir/page.csv" ;
    $parts_done = 0 ;
    
    open (PAGE, "$data_dir/page.csv") ;
    binmode (PAGE, ':utf8') ;
    
    while (defined (my $line = <PAGE>)) {
			$parts_done = $parts_done + length $line ;    
			chomp($line) ;
    
			if ($line =~ m/^(\d+),\"(.+)\",(\d+)$/) {
			
				my $page_id = int $1 ;
				my $page_title = $2 ;
				my $page_type = int $3 ;				
				
				my $flag = 0 ;
				
				if ($page_type == 3) {
					#this is a redirect, need to resolve it
					
					my %redirects_seen = () ;
    			while (defined($page_id) and defined($redirects{$page_id})){
							
						if (defined $redirects_seen{$page_id}) {
							$page_id = undef ;
			    		last ;
						} else {
			    		$redirects_seen{$page_id} = 1 ;
			    		$page_id = $redirects{$page_id} ;
    				}
    			}
    			
    			if (defined $page_id) {
    				$flag = 1 ;    			
    			}
				}
					
				if ($page_type == 1) {
					#this is a page title
					$flag = 2 ;
				}
				
				if ($flag > 0) {
					my $ref = $anchors{"\"$page_title\":$page_id"} ;
					my @array ;
					
					if (defined $ref) {
						#this has already been used as an anchor, needs to be flagged.
	    			@array = @{$ref} ;
	    			$array[1] = $flag ;	    			
					}else {
						#this has never been used as an anchor
						$anchorCount ++ ;
	    			@array = (0,$flag) ;
					}
	 
	 				$anchors{"\"$page_title\":$page_id"} = \@array ;
				}
			}
			print_progress(" - adding titles and redirects to anchors", $start_time, $parts_done, $parts_total) ;
		}
		
		print_progress(" - adding titles and redirects to anchors", $start_time, $parts_total, $parts_total) ;
		print "\n" ;
		
    close PAGE ;
    
    #now we need to save the anchors we have gathered
    
    $start_time = time ;
    $parts_total = $anchorCount ;
    $parts_done = 0 ;
    
    open(ANCHOR, "> $data_dir/anchor.csv") ;
    binmode(ANCHOR, ':utf8');

    while (my ($key, $ref) = each(%anchors)) {
    	$parts_done++ ;
    
			if ($key =~ m/\"(.+?)\":(\d+)/) {
	    	my $anchor = clean_text($1) ;
	    	my $target_id = $2 ;
	    	
	    	my @array = @{$ref} ;
	    	print ANCHOR "\"$anchor\",$target_id,$array[0],$array[1]\n" ;
			}
			print_progress(" - saving anchors", $start_time, $parts_done, $parts_total) ;
		}
		print_progress(" - saving anchors", $start_time, $parts_total, $parts_total) ;
		print "\n" ;
		
    close(ANCHOR) ;
    
    undef %anchors ;
    undef %redirects ;
	}
	
	
	#check that anchor summary needs patching
	
	open(ANCHOR_SUMMARY, "$data_dir/anchor_summary.csv") ;
	binmode(ANCHOR_SUMMARY, ':utf8') ;
	$peekline=<ANCHOR_SUMMARY> ;
	close(ANCHOR_SUMMARY) ;

	if ($peekline =~ m/\"(.+?)\",\"((\d+):(\d+):(\d+);)*(\d+):(\d+):(\d+)\"/) {
			print "'$data_dir/anchor_summary.csv' does not need to be patched\n" ;
	} else {
	
		# resummarize anchors
		
		print "Patching anchor summary" ;
		
		#move anchor to a safe place
		move("$data_dir/anchor_summary.csv", "$data_dir/anchor_summary.csv.old") or die ("could not back up anchor summary. '$data_dir' is not writeable.\n") ;
		print " - backing up anchor_summary to '$data_dir/anchor_summary.csv.old'\n" ;
		
		open(ANCHOR_SUMMARY, "> $data_dir/anchor_summary.csv") or die "'$data_dir' is not writable/\n" ;
		binmode(ANCHOR_SUMMARY, ":utf8") ;
			
		# split data into seperate passes, since we have issues trying to fit all of the anchors into memory
		my $pass = 0 ;
			
		while ($pass < $passes) {	
			
			my %anchors = () ; #ngram-> reference to array of "id:count:flag" strings of senses for anchor

			open(ANCHOR, "$data_dir/anchor.csv") 
				or die "cannot find '$data_dir/anchor.csv'. You must run extractCoreTables.pl first\n" ;
				
			binmode(ANCHOR, ":utf8") ;
				
			my $start_time = time  ;
			my $parts_total = -s "$data_dir/anchor.csv" ; 
				
			my $anchor_count = 0 ;
			my $parts_done = 0 ;

			while (defined (my $line = <ANCHOR>)) {
	   		$parts_done = $parts_done + length $line ;  
	    		  
	   		chomp($line) ;
	   		if ($line =~ m/^\"(.+)\",(\d+),(\d+),(\d+)$/) {
	    			    		
					my $ngram = $1 ;
										
					#generate a very simple hash for this ngram
					my $hash = 0 ;
					foreach (unpack("C*",$ngram)) {
	         	$hash = $hash + int($_);
	      	}
						
					#naively split the data on this hash value, and hope they are evenly spread enough to fit in memory.
					if ($hash % $passes == $pass) {
							
						my @sense = map(int,($2,$3,$4)) ;
						my $ref = $anchors{$ngram} ;
						my @array ;
						
						if (defined $ref) {
		    			@array = @{$ref} ;
						}else {
		    			@array = () ;
		    			$anchor_count++ ; 
						}
		 
						push (@array, \@sense) ;
						$anchors{$ngram} = \@array ;
	    		}
	    		print_progress(" - reorganizing anchors (pass ".($pass+1)." of $passes)", $start_time, $parts_done, $parts_total) ;    
				}
			}

			print_progress(" - reorganizing anchors (pass ".($pass+1)." of $passes)", $start_time, $parts_total, $parts_total) ;    
			print("\n") ;

			close(ANCHOR) ;
			
			$start_time = time ;
			$parts_total = $anchor_count ;
			$parts_done = 0 ;
					
			while ((my $anchor, my $ref) = each %anchors) {
				$parts_done ++ ;

			 	my $line = "" ;
	    	my @senses = @{$ref} ;
	    	@senses = reverse sort { @{$a}[1] <=> @{$b}[1] } @senses ; 

	    	for my $sense (@senses) {
					my @s = @{$sense} ;
					$line = $line."$s[0]:$s[1]:$s[2];" ;
	    	}
	    	
	    	undef @{$anchors{$anchor}} ;
	    	
	    	print ANCHOR_SUMMARY "\"$anchor\",\"".substr($line,0,length($line)-1)."\"\n" ;
	    	print_progress(" - saving anchor summary (pass ".($pass+1)." of $passes)", $start_time, $parts_done, $parts_total) ;
			}
			
			print_progress(" - saving anchor summary (pass ".($pass+1)." of $passes)", $start_time, $parts_total, $parts_total) ;
			print "\n" ;
			
			undef %anchors ;
			$pass++ ;
		}
			
		close ANCHOR_SUMMARY ;
	}
	
	
	# cleans the given text so that it can be safely inserted into database
	sub clean_text {
	    my $text = shift ; 
	    
	    $text =~ s/\\/\\\\/g; #escape backslashes
	    $text =~ s/\"/\\\"/g; #escape quotes
	    $text =~ s/\n/\\n/g; #escape newlines
	    #$text =~ s/\{/\\\{/g ;  #escape curly braces
	    #$text =~ s/\}/\\\}/g ;  #escape curly braces

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
