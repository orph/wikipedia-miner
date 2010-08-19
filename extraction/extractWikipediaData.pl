	#!/usr/bin/perl -w

	# saves pages, pagelinks (with redirects resolved), category links, disambiguation links, link counts, anchors, translations
	 
	use strict ;
	use Parse::MediaWikiDump;
	no warnings 'utf8';

	binmode(STDOUT, ':utf8');

  # gather arguments

	my $data_dir = shift(@ARGV) or die "You must specify a writable data directory containing a single WikiMedia dump file\n" ;
	
	
	my $args = join(' ',@ARGV);
	my $contentFlag = 1 ;
	
	if ($args =~ m/-nocontent/i) {
	 $contentFlag = 0 ;
	}
	
	 	
	my $passes = 2 ; 
	if ($args =~ m/-passes[\s\=]*(\d+)/i) {
	 $passes = $1 ;
	}	
	
	if ($contentFlag) {
		print "page content will be extracted.\n" ;
	} else {
		print "page content will not be extracted.\n" ;
	}
	print "data will be split into $passes passes for memory-intesive operations. Try using more passes if you run into problems.\n" ;
	
	# tweaking for different versions of wikipedia ==================================================================================
	
	# as far as I know, this is the only part of the import process that depends on the version of wikipedia being imported. If you 
	# want this to work on anything other than the en (english) dump file, then you must find out how disambiguation pages are 
	# identified in that language, and which category forms the root of all non-system pages, and modify the follwing values accordingly. 
	
	my @disambig_templates = ("disambig", "disambig-cleanup", "geodis", "hndis", "numberdis") ;
	my @disambig_categories = ("disambiguation") ;
	
	my $root_category = "Fundamental" ;  # for enwiki
	#my $root_category = "Main page" ; # for simple wiki

	# logging===================================================================================================================
	
	open (LOG, "> $data_dir/log.txt") or die "data dir '$data_dir' is not writable. \n" ;
	binmode(LOG, ':utf8');

	# get dump file ============================================================================================================

	my $dump_file ;
	my @files = <$data_dir/*>;
	foreach my $file (@files) {
	   if ($file =~ m/pages-articles.xml/i) {
	      if (defined $dump_file) {
		  die "the data directory '$data_dir' contains multiple dump files\n" ;
	      } else {
		  $dump_file = $file ;
	      }
	   }
	}

	if (not defined $dump_file) {
	    die "the data directory '$data_dir' does not contain a WikiMedia dump file\n" ;
	}

	# get namespaces ============================================================================================================

	my %namespaces = () ;

	open(DUMP, $dump_file) or die "dump file '$dump_file' is not readable.\n" ;
	while (defined (my $line = <DUMP>)) {

	    $line =~ s/\s//g ;  #clean whitespace

	    if ($line =~ m/<\/namespaces>/i) {
		    last ;
	    }
		
	    if ($line =~ m/<namespaceKey=\"(\d+)\">(.*)<\/namespace>/i){
		    $namespaces{lc($2)} = $1 ;
	    }
		
	    if ($line =~ m/<namespaceKey=\"(\d+)\"\/>/i) {
		    $namespaces{""} = $1 ;
	    }
	}
	close DUMP ;
	
	# get progress ===========================================================================================================	
	
	my $progress = 0 ;
	my $progressFile = "$data_dir/progress.csv" ;
	
	if (-e $progressFile) {	
		open (PROGRESS, $progressFile) ;
		
		foreach (<PROGRESS>) {
			$progress = $_ ;
		}
		close PROGRESS ;
	}
	
	sub save_progress() {
		open (PROGRESS, "> $progressFile") ;
		print PROGRESS $progress ;
		close PROGRESS ;	
	}
	

	# disambig tests =========================================================================================================
	
	my $dt_test  ;
	if (scalar @disambig_templates == 1) {
		$dt_test = $disambig_templates[0] ;
	}else {
		$dt_test = "(".join("|", @disambig_templates).")" ;
	}
	$dt_test = "\\{\\{".lc($dt_test)."\\}\\}" ;
	
	my $dc_test = join("|", @disambig_categories) ;	
	if (scalar @disambig_categories == 1) {
		$dc_test = $disambig_categories[0] ;
	}else {
		$dc_test = "(".join("|", @disambig_categories).")" ;
	}
	$dc_test = "\\[\\[category:".lc($dc_test)."\\]\\]" ;


	# page summary ===========================================================================================================

	my @ids = () ;          #ordered array of page ids
	my %pages_ns0 = () ;    #case normalized title -> id
	my %pages_ns14 = () ;   #case normalized title -> id
	
	sub extractPageSummary() {
		if ($progress >= 1) {
			readPageSummaryFromCsv() ;
		} else {
			extractPageSummaryFromDump();
			$progress = 1 ;
			save_progress() ;
		}
	}
	
	sub readPageSummaryFromCsv() {
		
    my $start_time = time  ;
    my $parts_total = -s "$data_dir/page.csv" ; 
    my $parts_done = 0 ;
    
    open(PAGE, "$data_dir/page.csv") ;
    binmode (PAGE, ':utf8') ;
    
    while (defined (my $line = <PAGE>)) {
			$parts_done = $parts_done + length $line ;    
			chomp($line) ;
    
			if ($line =~ m/^(\d+),\"(.+)\",(\d+)$/) {
			
				my $page_id = int $1 ;
				my $page_title = $2 ;
				my $page_type = int $3 ;
				
				$page_title = normalize_casing(clean_title($page_title)) ;
				
				push(@ids, $page_id) ;
				
				if ($page_type == 2) {
					$pages_ns14{$page_title} = $page_id ;
				} else {
					$pages_ns0{$page_title} = $page_id ;	
				}
			}
			
			print_progress("reading page summary from csv file", $start_time, $parts_done, $parts_total) ;  
		}	
		
		close PAGE ;
		
		print_progress("reading page summary from csv file", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;
	}
	
	sub extractPageSummaryFromDump() {
		my $start_time = time ;
    my $parts_total = -s $dump_file ;

    open (PAGE, "> $data_dir/page.csv") ;
    binmode (PAGE, ':utf8') ;
    open (STATS, "> $data_dir/stats.csv") ;

    my $article_count = 0 ;
    my $redirect_count = 0 ;
    my $category_count = 0 ;
    my $disambig_count = 0 ;

    my $pages = Parse::MediaWikiDump::Pages->new($dump_file) ;
    my $page ;

    $pages = Parse::MediaWikiDump::Pages->new($dump_file);

    while(defined($page = $pages->next)) {
	
			print_progress("extracting page summary from dump file", $start_time, $pages->current_byte, $parts_total) ;

			my $id = int($page->id) ;
			my $title = $page->title ;
			my $text = $page->text ;
			$text = lc($$text) ;

			my $namespace = $page->namespace;
			my $namespace_key = $namespaces{lc($namespace)} ;
   
			# check if namespace is valid
			if ($page->namespace ne "" && defined $namespace_key) {
	    	$title = substr $title, (length $page->namespace) + 1;
			} else {
	    	$namespace = "" ;
	    	$namespace_key = 0 ;
			}
    
			#identify the type of the page (1=Article,2=Category,3=Redirect,4=Disambig)
			my $type ;
			if ($namespace_key == 0) {
	    	if (defined $page->redirect) {
					$type = 3 ;
					$redirect_count ++ ;
	    	} else {
					if($text =~ m/$dt_test/ or $text =~ m/$dc_test/) {
		    		$type = 4 ;
		    		$disambig_count ++ ;
					} else {
		    		$type = 1 ;
		    		$article_count ++ ;
					}
	    	}
			}
			if ($namespace_key ==14) {
	    	if (defined $page->redirect) {
					$type = 3 ;
					$redirect_count ++ ;
	    	} else {
					$type = 2 ;
					$category_count ++ ;
	    	}
			}

    	if (defined $type) { 
	    	$title = clean_title($title) ;
	    
	    	my $normalized_title = normalize_casing($title) ;

	    	if ($namespace_key==0) {
					my $curr_id = $pages_ns0{$normalized_title} ;
					if (defined $curr_id) {
		    		# we have a collision
		    		if ($type != 3) {
							# only replace with non-redirect
							$pages_ns0{$normalized_title} = $id ; 
		    		}
					} else {
		    		$pages_ns0{$normalized_title} = $id ;
					}
	    	} else {
					my $curr_id = $pages_ns14{$normalized_title} ;
					if (defined $curr_id) {
		    		#we have a collision
		    		if ($type != 3) {
            	# only replace with non-redirect
							$pages_ns14{$normalized_title} = $id ; 
		    		}
					} else {
		    		$pages_ns14{$normalized_title} = $id ;
					}
	    	}
 		    print PAGE "$id,\"$title\",$type\n" ;
			}
    }

    print STATS "$article_count,$category_count,$redirect_count,$disambig_count\n" ;
    close STATS ;
 
    close PAGE ;
    
    print_progress("extracting page summary from dump file", $start_time, $parts_total, $parts_total) ;    
    print "\n" ;
	}

	# redirect summary ===========================================================================================================

	my %redirects = () ;    #from_id -> to_id

	sub extractRedirectSummary() {
		
		if ($progress >= 2) {
			readRedirectSummaryFromCsv() ;
		} else {
			extractRedirectSummaryFromDump();
			$progress = 2 ;
			save_progress() ;
		}
	}
	
	sub readRedirectSummaryFromCsv() {
		
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
			print_progress("reading redirect summary from csv file", $start_time, $parts_done, $parts_total) ;  
		}	
		
		close REDIRECT ;
		
		print_progress("reading redirect summary from csv file", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;
	}
	
	sub extractRedirectSummaryFromDump() {
	
		my $start_time = time ;
    my $parts_total = -s $dump_file ;

    open (REDIRECT, "> $data_dir/redirect.csv") ;

    my $pages = Parse::MediaWikiDump::Pages->new($dump_file) ;
    my $page ;

    while(defined($page = $pages->next)) {

			print_progress("extracting redirect summary from dump file", $start_time, $pages->current_byte, $parts_total) ;

			my $id = int($page->id) ;
			my $title = $page->title ;
			my $text = $page->text ;
			my $namespace = $page->namespace;
			my $namespace_key = $namespaces{lc($namespace)} ;
		   
			# check if namespace is valid
			if ($page->namespace ne "" && defined $namespace_key) {
				$title = substr $title, (length $page->namespace) + 1;
			} else {
			  $namespace = "" ;
			  $namespace_key = 0 ;
			}
    
			if ($namespace_key==0 or $namespace_key==14) {

	    	if (defined $page->redirect) {
					my $link_markup = $page->redirect ;
					my $target_lang ="";
					
					if ($link_markup =~ m/^([a-z]{1}.+?):(.+)/) {
		    		#check that someone hasnt put a valid namespace here
		    		if (not defined $namespaces{lc($1)}) {
							$target_lang = clean_text($1) ;
							$link_markup = $2 ;
		    		}
					}
		
					my $target_namespace = "" ; 
					my $target_ns_key = 0 ;
					if ($link_markup =~ m/^(.+?):(.+)/) {
			    	$target_namespace = lc($1) ;
			    	$target_ns_key = $namespaces{$target_namespace} ;
			    	if (defined $target_ns_key) {
							$target_namespace = lc($1) ;
							$link_markup = $2 ;
			    	} else {
							$target_namespace = "" ;
							$target_ns_key = 0 ;
			    	}
					}

					my $target_title = "";
					my $anchor_text = "" ;
					if ($link_markup =~ m/^(.+?)\|(.+)/) {
			    	$target_title = clean_title($1) ;
			    	$anchor_text = clean_text($2) ;
					} else {
			    	$target_title = clean_title($link_markup) ;
			    	$anchor_text = clean_text($link_markup) ;
					}
			
					my $target_id ;
					if ($target_ns_key == 0) {
			    	$target_id = $pages_ns0{normalize_casing($target_title)} ;
					}
		
					if ($target_ns_key == 14) {
			    	$target_id = $pages_ns14{normalize_casing($target_title)} ;
					}
		    
					if (defined($target_id)) {
			    	$redirects{$id} = $target_id ;
			    	print REDIRECT "$id,$target_id\n" ;
					} else {
			    	print LOG "Problem with redirect $target_ns_key:$target_title\n";
					}
				}
    	} 
  	}    

		print_progress("extracting redirect summary from dump file", $start_time, $parts_total, $parts_total) ;
    print "\n" ;

    close REDIRECT ;
    
    $progress = 2 ;
		save_progress() ;
	}
	
	# other core tables =============================================================================================================	
	
	sub extractCoreSummaries() {
		if ($progress < 3) {
			extractCoreSummariesFromDump();
			$progress = 3 ;
			save_progress() ;
		}
	}
	
	sub extractCoreSummariesFromDump() {
	
		my $start_time = time ;
    my $parts_total = -s $dump_file ;

    open (PAGELINK, "> $data_dir/pagelink.csv") ;
    open (CATLINK, "> $data_dir/categorylink.csv") ;
    open (TRANSLATION, "> $data_dir/translation.csv") ;
    binmode(TRANSLATION, ':utf8') ;
    open (DISAMBIG, "> $data_dir/disambiguation.csv") ;
    binmode(DISAMBIG, ':utf8') ;
    open (EQUIVALENCE, "> $data_dir/equivalence.csv") ;

    my $pages = Parse::MediaWikiDump::Pages->new($dump_file);
    my $page ;
    
    my %anchors = () ;  #\"anchor\":id -> (freq:flag)
    my $anchorCount = 0 ;

    while(defined($page = $pages->next)) {
    
			print_progress("extracting core summaries from dump file", $start_time, $pages->current_byte, $parts_total) ;
			
			#print ("anchors".scalar keys %anchors) ;

			my $id = int($page->id) ;
			my $title = $page->title ;
			my $text = $page->text ;	
			$text = $$text ;
			my $namespace = $page->namespace;
			my $namespace_key = $namespaces{lc($namespace)} ;
		   
			# check if namespace is valid
			if ($page->namespace ne "" && defined $namespace_key) {
			    $title = substr $title, (length $page->namespace) + 1;
			} else {
			    $namespace = "" ;
			    $namespace_key = 0 ;
			}

			if ($namespace_key==14) {
			    #find this category's equivalent article
			    my $t = $title ;
			    my $equivalent_id = resolve_link(clean_title($t), 0) ;

			    if (defined $equivalent_id) {
						print EQUIVALENCE "$id,$equivalent_id\n" ;
			    }
			}
    
			if ($namespace_key==0 or $namespace_key==14) {

	    	my $stripped_text = strip_templates($text) ;
	    	
	    	my $lc_text = lc($text) ;

	    	if($lc_text =~ m/$dt_test/ or $lc_text =~ m/$dc_test/) {
					# process disambiguation page
		
					my $index = 0 ;
    
					while($stripped_text =~ m/(.*?)\n/gi) {	    
		    		my $line = $1 ;
		    
		    		if ($line =~ m/\={2,}\s*See Also/i) {
							# down to "see also" links, which we want to ignore
							last ;
		    		}
		    
		    		if ($line =~ m/\[\[\s*([^\]]+)\]\]/) {
							#only interested in first link in the line
			
							my $pos_of_link = index($line, "[[") ;
							my $pos_of_title = index(lc($line), lc($title)) ;
			
							if ($pos_of_title < 0 || $pos_of_title > $pos_of_link) { 
			    			#only interested if title of page isnt found before the link

			    			my $link_markup = $1 ;
			    
			    			my $target_lang ="";
			    			if ($link_markup =~ m/^([a-z]{1}.+?):(.+)/) {
									#check that someone hasnt put a valid namespace here
									if (not defined $namespaces{lc($1)}) {
				    				$target_lang = clean_text($1) ;
				    				$link_markup = $2 ;
									}
			    			}

			    			if ($target_lang ne "") {
									#down to language links, which we want to ignore
									last ;
			    			}
			    
			    			my $target_namespace = "" ; 
			    			my $target_ns_key = 0 ;
			    			if ($link_markup =~ m/^(.+?):(.+)/) {
									$target_namespace = lc($1) ;
									$target_ns_key = $namespaces{$target_namespace} ;
									if (defined $target_ns_key) {
									    $target_namespace = lc($1) ;
									    $link_markup = $2 ;
									} else {
									    $target_namespace = "" ;
									    $target_ns_key = 0 ;
									}
			    			}
			    
						    my $target_title = "";
						    my $anchor_text = "" ;
						    if ($link_markup =~ m/^(.+?)\|(.+)/) {
									$target_title = clean_title($1) ;
									$anchor_text = clean_text($2) ;
						    } else {
									$target_title = clean_title($link_markup) ;
									$anchor_text = clean_text($link_markup) ;
						    }
			    
						    #print "$target_lang, ns=$target_namespace($target_ns_key), n=$target_title, a=$anchor_text\n" ;
						    
						    my $target_id = resolve_link($target_title, $target_ns_key) ;
						    
						    if (defined $target_id) {
									$index ++ ;

									my $scope = $line ;
									$scope =~ s/^(\**)//g ;                          #clean list markers
									$scope =~ s/\[\[([^\]\|]+)\|([^\]]+)\]\]/$2/g ;  #clean piped links
									$scope =~ s/\[\[\s*([^\]]+)\]\]/$1/g ;           #clean remaining links
									$scope =~ s/\'{2,}//g ;                          #clean bold and italic stuff 
									$scope = clean_text($scope) ;
																		
									print DISAMBIG "$id,$target_id,$index,\"$scope\"\n" ;
								} else {
									print LOG "problem resolving disambig link to $target_title in ns:$target_ns_key\n" ;
								}
							} 
						}
					}
				}
	    
	    	while($stripped_text =~ m/\[\[\s*([^\]]+)\]\]/gi) {
					my $link_markup = $1 ;
		
					my $target_lang ="";
					if ($link_markup =~ m/^([a-z]{1}.+?):(.+)/) {
		    		#check that someone hasnt put a valid namespace here
		    		if (not defined $namespaces{lc($1)}) {
							$target_lang = clean_text($1) ;
							$link_markup = $2 ;
		    		}
					}
		
					my $target_namespace = "" ; 
					my $target_ns_key = 0 ;
					if ($link_markup =~ m/^(.+?):(.+)/) {
					    $target_namespace = lc($1) ;
					    $target_ns_key = $namespaces{$target_namespace} ;
					    if (defined $target_ns_key) {
								$target_namespace = lc($1) ;
								$link_markup = $2 ;
					    } else {
								$target_namespace = "" ;
								$target_ns_key = 0 ;
					    }
					}
		
					my $target_title = "";
					my $anchor_text = "" ;
					if ($link_markup =~ m/^(.+?)\|(.+)/) {
					    $target_title = clean_title($1) ;
					    $anchor_text = clean_text($2) ;
					} else {
					    $target_title = clean_title($link_markup) ;
					    $anchor_text = clean_text($link_markup) ;
					}
		
					#print "l=$target_lang, ns=$target_namespace($target_ns_key), n=$target_title, a=$anchor_text\n" ;
		
					if ($target_lang ne "") {
					    print TRANSLATION "$id,\"$target_lang\",\"$target_title\"\n" ;
					} else {
					    if ($target_ns_key==0) {
								#page link
								my $target_id = resolve_link($target_title, $target_ns_key) ;
						
								if (defined $target_id) {
						    	print PAGELINK "$id,$target_id\n" ;

									#save this anchor:dest combination as a two element array, with count as first element, and flag (0) as seccond
						    	my $ref = $anchors{"\"$anchor_text\":$target_id"} ;
						    	my @array ;
					
									if (defined $ref) {
	    							@array = @{$ref} ;
									}else {
	    							@array = (0,0) ;
	    							$anchorCount ++ ;
									}
	 
	 								$array[0] = $array[0] + 1 ;
	 
									$anchors{"\"$anchor_text\":$target_id"} = \@array ;
						    
						    	#print LOG "\"$anchor_text\":$target_id, $array[0], $array[1]\n" ;
						    	
								} else {
			    				print LOG "problem resolving page link to $target_title\n" ;
								}
		    			}
		    
		    			if ($target_ns_key==14) {
								#category link
								my $parent_id = resolve_link($target_title, $target_ns_key) ;
			
								if (defined $parent_id) {
			    				print CATLINK "$parent_id,$id\n" ;
								} else {
			    				print LOG "problem resolving category link to $target_title\n" ;
								}
		    			}   
						}
	    		}
				}
    	} 

    print_progress("extracting core summaries from dump file", $start_time, $parts_total, $parts_total) ;
    print "\n" ;

    close PAGELINK ;
    close CATLINK ;
    close TRANSLATION ;
    close DISAMBIG ;
    close EQUIVALENCE ;
    
    #flag any anchor:dest combinations that are mirrored by redirects or article titles, and add titles and redirects if they havent been used as anchors yet.
    
    $start_time = time ;
    $parts_total = -s "$data_dir/page.csv" ;
    my $parts_done = 0 ;
    
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
			print_progress(" - adding titles and redirects to anchor summary", $start_time, $parts_done, $parts_total) ;
		}
		
		print_progress(" - adding titles and redirects to anchor summary", $start_time, $parts_total, $parts_total) ;
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
    
    
    # done with looking up page titles now, so lets free up some memory ;
    undef %pages_ns0 ;
	}
	
	
	sub resolve_link {
    my $title = shift ;
    my $namespace = shift ;

    #print " - resolving link $namespace:$title\n" ;

    my $target_id ;

    if ($namespace == 0) {
			$target_id = $pages_ns0{normalize_casing($title)} ;
    }

    if ($namespace == 14) {
			$target_id = $pages_ns14{normalize_casing($title)} ;
    }

    my %redirects_seen = () ;
    while (defined($target_id) and defined($redirects{$target_id})){
			#print " - - redirect $target_id\n" ;
			if (defined $redirects_seen{$target_id}) {
			    #seen this before, so cant resolve this loop of redirects
			    last ;
			} else {
			    $redirects_seen{$target_id} = 1 ;
			    $target_id = $redirects{$target_id} ;
			}
    }
    return $target_id ;
	}
	
	# anchor summary ==============================================================================================================
	
	sub extractAnchorSummary() {
		if ($progress < 4) {
			summarise_anchors() ;
			$progress = 4 ;
			save_progress() ;
		}
	}
	
	sub summarise_anchors() {
	
		print "summarizing anchors for quick caching\n" ;
	
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
	
	# generality summary ==================================================================================================================
	
	sub extractGenerality() {
		if ($progress < 5) {
			summarize_generality() ;
			$progress = 5 ;
			save_progress() ;
		}
	}
	
	sub summarize_generality() {
	
		print "summarizing generality\n" ;
		
		my $root_id = $pages_ns14{normalize_casing($root_category)} ; 
		
		if (not defined $root_id) {
			die "Could not locate root category. Please configure the script properly. \n Don't panic! You will not have to calculate all of the previous summaries again " ;
		}
				
		my %cat_links = () ;  #parent_id -> ref to array of #child_ids

		my $start_time = time  ;
		my $parts_total = -s "$data_dir/categorylink.csv" ; 
		my $parts_done = 0 ;
		
		open(CATEGORYLINK, "$data_dir/categorylink.csv") ;

		while (defined (my $line = <CATEGORYLINK>)) {
		    $parts_done = $parts_done + length $line ;    
		    chomp($line) ;

		    if ($line =~ m/^(\d+),(\d+)$/) {
					my $parent_id = int $1 ;
					my $child_id = int $2 ;

					my $children_ref = $cat_links{$parent_id} ;
			
					if (defined $children_ref) {
			    	my @children = @{$children_ref} ;
			    	push(@children, $child_id) ;
			    	$cat_links{$parent_id} = \@children ;
					} else {
			    	my @children = () ;
			    	push(@children, $child_id) ;
			    	$cat_links{$parent_id} = \@children ;
					}
		    }
		    print_progress(" - gathering category links", $start_time, $parts_done, $parts_total) ;    
		}

		print_progress(" - gathering category links", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;

		close(CATEGORYLINK) ;
		
		open(GENERALITY, "> $data_dir/generality.csv") ;
		
		my %page_depths = () ;  #$page_id -> depth ;

		my $curr_depth = 0 ;
		my $curr_page = $root_id ;
		my @curr_level = () ;
		my @next_level = () ;
		
		$start_time = time ;
		$parts_total = scalar keys %cat_links ;
		$parts_done = 0 ;

		while (defined $curr_page){
		
		  if (not defined $page_depths{$curr_page}) {
				$page_depths{$curr_page} = $curr_depth ; 
				print GENERALITY "$curr_page, $curr_depth\n" ; 
		    
				my $ref = $cat_links{$curr_page} ;
				if (defined $ref) {
			    my @children = @{$ref} ;
			    
			    for my $child (@children) {
						if (not defined $page_depths{$child}) {
				    	push(@next_level, $child) ; 
						}
			    }			     
				}
				$parts_done++ ;
				print_progress(" - calculating and saving page depths", $start_time, $parts_done, $parts_total) ;   
		  }

		 	$curr_page = pop(@curr_level) ;

		  if (not defined $curr_page) {
				@curr_level = @next_level ;
				@next_level = () ;
				$curr_depth ++ ;

				$curr_page = pop(@curr_level) ;
		  }
		}

		print_progress(" - calculating and saving page depths", $start_time, $parts_done, $parts_total) ;  
		print "\n" ;
		
		close GENERALITY ; 
		
		undef %cat_links ;
		undef %page_depths ; 
	
		# done with looking up category titles now, so lets free up some memory ;
		undef %pages_ns14 ; 

	}
	
	
	
	# link count summary ==================================================================================================================
	
	sub extractLinkCountSummary() {
		if ($progress < 6) {
			summarize_linkcounts() ;
			$progress = 6 ;
			save_progress() ;
		}
	}
	
	sub summarize_linkcounts() {
	
		print "summarizing link counts\n" ;
	
		if (not @ids) {
			extractPageSummary() ;
		} 
		
		my %links_in = () ;    #article id -> count of links in 
		my %links_out = () ;    #article id -> count of links out
		
		my $start_time = time  ;
		my $parts_total = -s "$data_dir/pagelink.csv" ; 
		my $parts_done = 0 ;
		
		open(PAGELINK, "$data_dir/pagelink.csv") ;
		binmode(PAGELINK, ":utf8") ;
		
		my $lastFrom = 0 ;
		my $lastFrom_out = 0 ;
	
		while (defined (my $line = <PAGELINK>)) {
    	$parts_done = $parts_done + length $line ;    
    	chomp($line) ;

    	if ($line =~ m/^(\d+),(\d+)$/) {
				my $pl_from = $1 ;
				my $pl_to = $2 ;
				
				if ($pl_from == $lastFrom) {
					$lastFrom_out ++ ;
				} else {
					$links_out{$lastFrom} = $lastFrom_out ;
					$lastFrom = $pl_from ;
					$lastFrom_out = 1 ;
				}
				
				my $li = $links_in{$pl_to} ;
			  if (defined $li) {
					$links_in{$pl_to} = $li + 1 ;
			  } else {
					$links_in{$pl_to} = 1 ;
			  }
			}
			
			print_progress(" - gathering link counts", $start_time, $parts_done, $parts_total) ; 
		}
		
		print_progress(" - gathering link counts", $start_time, $parts_total, $parts_total) ;
		print "\n" ;
		
		close PAGELINK ;
		
		
    open(LINKCOUNT, "> $data_dir/linkcount.csv") ;

    $start_time = time ;
    $parts_total = $#ids ;
    $parts_done = 0 ;
        
    foreach my $id (@ids) {
			$parts_done ++;
	
			my $li = $links_in{$id} ;
			if (not defined $li) {
	    	$li = 0 ;
			}
	
			my $lo = $links_out{$id} ;
			if (not defined $lo) {
	    	$lo = 0 ;
	    }
	    
			print LINKCOUNT "$id,$li,$lo\n" ;
			
			print_progress(" - saving link counts", $start_time, $parts_done, $parts_total) ;
    }    

    print_progress(" - saving link counts", $start_time, $parts_total, $parts_total) ;
    print "\n" ;		
 
		close LINKCOUNT ;
		
		
		undef %links_in ;  
		undef %links_out ; 
	}
		
	# links out summary =============================================================================================================
	
	sub extractLinksOutSummary() {
		if ($progress < 7) {
			summarize_linksOut() ;
			$progress = 7 ;
			save_progress() ;
		}
	}
	
	sub summarize_linksOut() {
		# summarize links out, with one page per line and frequency appended to each link destination 
		# so that relatedness measures can be calculated quickly
	
		print "summarizing links out from each page\n" ;
	
		my %in_counts = () ;

		my $start_time = time ;
		my $parts_total = -s "$data_dir/linkcount.csv" ;
		my $parts_done = 0 ;
		
		open(LINKCOUNT, "$data_dir/linkcount.csv") ;

		my $count = 0 ;

		while (defined (my $line = <LINKCOUNT>)) {
   		$parts_done = $parts_done + length $line ;    
    	chomp($line) ;

    	if ($line =~ m/^(\d+),(\d+),(\d+)$/) {
				my $lc_id = scalar $1 ;
				my $lc_in = scalar $2 ;
	
				$in_counts{$lc_id} = $lc_in ;
    	}
    	print_progress(" - gathering destination frequencies", $start_time, $parts_done, $parts_total) ;    
		}

		print_progress(" - gathering destination frequencies", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;

		close(LINKCOUNT) ;
		

		$start_time = time ;
		$parts_total = -s "$data_dir/pagelink.csv" ;
		$parts_done = 0 ;
		
		open(PAGELINK, "$data_dir/pagelink.csv") ;
		open(LINKSOUT, "> $data_dir/pagelink_out.csv") ;

		my $last_id = 0 ;
		my @ids = () ; 

		while (defined(my $line = <PAGELINK>)) {
    
    	$parts_done = $parts_done + length $line ;    
    	chomp($line) ;
    
    	if ($line =~ m/^(\d+),(\d+)$/){
				my $pl_from = scalar $1 ;
				my $pl_to = scalar $2 ;
	
				if ($pl_from > $last_id) {
	    		if ($last_id > 0) {		
						my $output = "" ;
						my $prev = 0 ;
		
						for my $id (sort {$a <=> $b} (@ids)) {
		    			if ($id > $prev) {
								my $li = $in_counts{$id} ;
								if (not defined $li) { $li = 0 ; } 
			
								$output = $output."$id:$li;" ;
								$prev = $id ;
		    			}
						}
		
						print LINKSOUT "$last_id,\"".substr($output,0,length($output)-1)."\"\n" ;
	    		}
	    		$last_id = $pl_from ;
	    		@ids = () ;
				}
	
				push(@ids, $pl_to) ;
    	}
    	print_progress(" - saving links", $start_time, $parts_done, $parts_total) ;    
		}

		my $line = "" ;
		my $prev = 0 ;

		for my $id (sort {$a <=> $b} (@ids)) {
    	if ($id > $prev) {
				my $li = $in_counts{$id} ;
				if (not defined $li) { $li = 0 ; } 
	
				$line = $line."$id:$li;" ;
				$prev = $id ;
    	}
		}

		print LINKSOUT "$last_id,\"".substr($line,0,length($line)-1)."\"\n" ;

		print_progress(" - saving links", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;

		close(PAGELINK) ;
		close(LINKSOUT) ;
		
		undef %in_counts ;
		undef @ids ;
	}	
	
	# links in summary ==============================================================================================================
	
	sub extractLinksInSummary() {
		if ($progress < 8) {
			summarize_linksIn() ;
			$progress = 8 ;
			save_progress() ;
		}
	}
	
	sub summarize_linksIn() {
	
		print "summarizing links in to each page\n" ;
		
		open(LINKSIN, "> $data_dir/pagelink_in.csv") ;
		
		#get link counts, so we can initialize arrays to correct size
		
		my %link_count = () ; #id-> count of links in (may be a bit more than we need, because there are duplucates)
		my $total_ids = 0 ;

		open(LINKCOUNT, "$data_dir/linkcount.csv") ;

		my $start_time = time  ;
		my $parts_total = -s "$data_dir/linkcount.csv" ; 
		my $parts_done = 0 ;

		while (defined (my $line = <LINKCOUNT>)) {
    	$parts_done = $parts_done + length $line ;    
    	chomp($line) ;

    	if ($line =~ m/^(\d+),(\d+),(\d+)$/) {
				my $lc_id = int $1 ;
				my $lc_in = int $2 ;
	
				$link_count{$lc_id} = $lc_in ;
				$total_ids ++ ;
    	}
    	print_progress(" - calculating space requirements", $start_time, $parts_done, $parts_total) ;    
		}

		print_progress(" - calculating space requirements", $start_time, $parts_total, $parts_total) ;  
		print("\n") ;

		close(LINKCOUNT) ;
		
		#pen(LINKSIN, "> $data_dir/pagelink_in.csv") ;

		#get pagelinks, but split into passes that we can fit into memory

		for (my $pass=0 ; $pass<$passes ; $pass++) {
		
    	print " - pass ".($pass + 1)." of $passes\n" ;

    	my $start_time = time  ;
    	my $parts_total = $total_ids ; 
    	my $parts_done = 0 ;
    	
    	# allocate space needed to to do this step first, so we can fail fast if we have to. 

    	my %links_in = () ;  #id -> reference to array of unique ids linking to the page, in ascending order ;
                           #first element in array specifies the "size" of array; the index at last id was placed. 
      
      my $keys = 0 ;
      
      while ((my $lc_id, my $lc_in) = each %link_count) {
 				$parts_done ++;
      	
      	if ($lc_id % $passes == $pass) {
      		my @array = @{initialize_array($lc_in+1)} ;
		
					$links_in{$lc_id} = \@array ;
					$keys++ ;
				}
				
				print_progress("   - allocating space", $start_time, $parts_done, $parts_total) ;
    	}

    	print_progress("   - allocating space", $start_time, $parts_total, $parts_total) ;
    	print "\n" ;
 
 			# populate links_in 
    	
    	open(PAGELINK, "$data_dir/pagelink.csv") ;
    	
    	$start_time = time  ;
    	$parts_total = -s "$data_dir/pagelink.csv" ;
    	$parts_done = 0 ;
    	
    	while (defined (my $line = <PAGELINK>)) {
				$parts_done = $parts_done + length $line ;    
				chomp($line) ;
    
				if ($line =~ m/^(\d+),(\d+)$/) {
	    		my $pl_from = int $1 ;
	    		my $pl_to = int $2 ;

	    		if ($pl_to%$passes == $pass) {
		
						if ($pl_from != $pl_to) {

		    			my $ref = $links_in{$pl_to} ;
		    			my @array = @{$ref} ;

							my $size = int $array[0] ;	#number of links we have already stored for this $pl_to		 
							
							if ($size >= @array) {
								die "array not large enough for $pl_to:$size\n";
							}
															
							if ($array[$size]!= $pl_from) {
			    
			    			$size ++ ;
			    			$links_in{$pl_to}[0] = $size ;
			    			$links_in{$pl_to}[$size] = $pl_from ;

			    			#print "array appended to for $pl_to\n" ;
							}
						}
					}
				}
				print_progress("   - gathering links", $start_time, $parts_done, $parts_total) ;
    	}
    	
    	print_progress("   - gathering links", $start_time, $parts_total, $parts_total) ;
    	print "\n" ;
    	
    	close PAGELINK ;
    	
    	#save stored links to file
    	
    	$start_time = time ;
    	$parts_total = $keys ;
    	$parts_done = 0 ;
    	
    	while ((my $pl_to, my $ref) = each %links_in) {
 				$parts_done ++;

				my $line = "" ;
				my @array = @{$ref} ;
				my $size = $array[0] ;
				
				for (my $i = 1 ; $i <= $size ; $i++) {
					my $pl_from = $array[$i] ;
					$line = $line."$pl_from:" ;
				}
				
				undef @{$links_in{$pl_to}} ;

				print LINKSIN "$pl_to,\"".substr($line,0,length($line)-1)."\"\n" ;
				print_progress("   - saving links", $start_time, $parts_done, $parts_total) ;
    	}

    	print_progress("   - saving links", $start_time, $parts_total, $parts_total) ;
    	print "\n" ;
    	
    	undef %links_in ;
		}
		
		close LINKSIN ;
		
		undef %link_count ;
	}

	sub initialize_array{
    my $size = shift ;
   
    my @array = () ;
    $#array = $size ;
    
    for (my $i=0 ; $i<$size ; $i++) {
			$array[$i] = int 0 ;
    }
       
    return \@array ;
	}
	
	
	# page content ==================================================================================================================
	
	sub extractContent() {
	
		my $start_time = time ;
		my $parts_total = -s $dump_file ;

		open (CONTENT, "> $data_dir/content.csv") ;
		binmode(CONTENT, ':utf8');

		my $pages = Parse::MediaWikiDump::Pages->new($dump_file);
		my $page;

		while(defined($page = $pages->next)) {
    
    	my $parts_done = $pages->current_byte ;
    
	    my $id = $page->id ;
	    my $title = $page->title ;
	    my $text = $page->text ;
	    my $namespace = $page->namespace;
	    my $namespace_key = $namespaces{lc($namespace)} ;
    
	    # check if namespace is valid
	    if ($page->namespace ne "" && defined $namespace_key) {
				$title = substr $title, (length $page->namespace) + 1;
    	} else {
				$namespace = "" ;
				$namespace_key = 0 ;
    	}
    
    	if ($namespace_key==0 or $namespace_key==14) {
				my $content = clean_text($$text) ;
				print CONTENT "$id,\"$content\"\n" ;
    	}
    	print_progress("extracting content", $start_time, $parts_done, $parts_total) ;
		}

		print_progress("extracting content", $start_time, $parts_total, $parts_total) ;
		print "\n" ;

		close CONTENT ;
	}	
	
	if ($progress < 8) {
		if ($progress < 4) {
			extractPageSummary() ;
			extractRedirectSummary() ;
			extractCoreSummaries() ;
		}
		
		extractAnchorSummary() ;
		extractGenerality() ;
		extractLinkCountSummary() ;
		extractLinksOutSummary() ;
		extractLinksInSummary() ;
	}
		
	if ($contentFlag) {
		extractContent() ;
	}


	# text cleaning =================================================================================================================
	
	# makes first letter of every word uppercase
	sub normalize_casing {
	    my $title = shift ;
	    $title =~ s/(\w)(\w*)/\u$1$2/g;  
	    return $title;
	}

	# cleans the given title so that it will be matched to entries saved in the page table 
	sub clean_title {  
	    my $title = shift ;
		
	    $title = clean_text($title) ;
	    $title =~ s/_+/ /g; # replace underscores with spaces
	    $title =~ s/\s+/ /g;  # remove multiple spaces
	    $title =~ s/\#.+//; #remove page-internal part of link (the bit after the #)

	    return $title;
	}

	sub strip_templates {
	    my $text = shift ;

	    $text =~ s/\{\{((?:[^{}]+|\{(?!\{)|\}(?!\}))*)\}\}//sxg ;  #remove all templates that dont have any templates in them
	    $text =~ s/\{\{((.|\n)*?)\}\}//g ;                         #repeat to get rid of nested templates
	    $text =~ s/\{\|((.|\n)+?)\|\}//g ;                         #remove {|...|} structures

	    return $text ;
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

