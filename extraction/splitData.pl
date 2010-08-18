#!/usr/bin/perl -w

	use strict ;
	use Parse::MediaWikiDump;
	no warnings 'utf8';

	binmode(STDOUT, ':utf8');

	my $data_dir = shift(@ARGV) or die "You must specify a writable data directory containing a single WikiMedia dump file\n" ;
	my $file_count = int shift(@ARGV) or die "You must specify the number of files to split the data in to\n" ;
	
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

	# setup filehandlers ========================================================================================================
	
	my @filehandles;

	for(my $i=0; $i<$file_count; $i++)
	{
    local *FILE;
    open(FILE, "> $data_dir/split_$i.csv") || die "the data directory '$data_dir' is not writable.\n" ;
    binmode(FILE, ':utf8');

    push(@filehandles, *FILE);
	}
	

	# save splitfiles ============================================================================================================

	my $start_time = time ;
	my $parts_total = -s $dump_file ;

	my $pages = Parse::MediaWikiDump::Pages->new($dump_file);
	my $page;

	my $articles = 0 ;

	while(defined($page = $pages->page)) {
    
    print_progress("splitting dump file", $start_time, $pages->current_byte, $parts_total) ;
    
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
    
    #print $page->title.", $namespace($namespace_key), $title\n" ;
    
    if ($namespace_key==0 and not defined($page->redirect)) {
			$articles ++ ;
			my $index = $articles%$file_count ;
	
			my $content = clean_text($$text) ;
	
			my $file = $filehandles[$index] ;
	
			print $file "$id,\"$content\"\n" ;
    }
	}

	foreach my $file (@filehandles){
    close $file;
	}
	
	print_progress("splitting dump file", $start_time, $parts_total, $parts_total) ;
	print "\n" ;
	

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
