/*
 *    ArticleSet.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util;

import java.io.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;

import org.wikipedia.miner.model.*;

/**
 * @author David Milne
 *
 *	A set of Wikipedia articles that can be used to train and test disambiguators, linkDetectors, etc. 
 * Can either be generated randomly from Wikipedia, or loaded from file.
 */
public class ArticleSet {
	private TreeSet<Integer> articleIds = new TreeSet<Integer>() ;
	
	int x = 0 ;
	
	/**
	 * Loads this article set from file. The file must contain a list of article ids, separated by newlines. 
	 * If the file is comma separated, then only the first column is used.
	 * 
	 * @param file the file containing article ids.
	 * @throws IOException if the file cannot be read.
	 */
	public ArticleSet(File file) throws IOException{
		
		articleIds = new TreeSet<Integer>() ;

		BufferedReader reader = new BufferedReader(new FileReader(file)) ;
		String line  ;

		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\t") ;
			int id = new Integer(values[0].trim()) ;
			articleIds.add(id) ;
		}

		reader.close();		
	}
	
	/**
	 * Generates a set of articles randomly from Wikipedia, given some constraints on what is an acceptable article.
	 * <p>
	 * This first gathers all articles that satisfy the minInLink and minOutLink constraints, and then randomly samples from
	 * these to produce the final set of articles which satisfy all constraints.
	 * <p>
	 * The length of time this takes is very variable. It will work fastest if the minInLink and minOutLink constraints are strict, and 
	 * the other constraints are loose.
	 * <p>
	 * You can ignore any of the constraints by setting them to -1 ;
	 * 
	 * @param wikipedia	an instantiated instance of Wikipedia.
	 * @param size	the desired number of articles
	 * @param minInLinks	the minimum number of links that must be made to an article
	 * @param minOutLinks	the minimum number of links that an article must make
	 * @param minLinkProportion the minimum proportion of links (over total words) that articles must contain
	 * @param maxLinkProportion the maximum proportion of links (over total words) that articles must contain
	 * @param minWordCount  the minimum number of words allowed in an article
	 * @param maxWordCount the maximum number of words allowed in an article
	 * @param maxListProportion the maximum proportion of list items (over total line count) that an article may contain. 
	 * @throws SQLException if there is a problem with the wikipedia database.
	 */
	public ArticleSet(Wikipedia wikipedia, int size, int minInLinks, int minOutLinks, double minLinkProportion, double maxLinkProportion, int minWordCount, int maxWordCount, double maxListProportion) throws SQLException{
		
		DecimalFormat df = new DecimalFormat("#0.00 %") ;
		
		Vector<Article> roughCandidates = getRoughCandidates(wikipedia, minInLinks, minOutLinks) ;
		int totalRoughCandidates = roughCandidates.size() ;
				
		articleIds = new TreeSet<Integer>() ;
		
		ProgressNotifier pn = new ProgressNotifier(totalRoughCandidates, "Refining candidates (ETA is worst case)") ;
		
		double lastWarningProgress = 0 ;
		
		while (roughCandidates.size() > 0) {
			
			pn.update() ;
			
			if (articleIds.size() == size)
				break ; //we have enough ids
			
			//pop a random id
			Integer index = (int)Math.floor(Math.random() * roughCandidates.size()) ;
			Article art = roughCandidates.elementAt(index) ;
			roughCandidates.removeElementAt(index) ;
									
			if (isArticleValid(art, minLinkProportion, maxLinkProportion, minWordCount, maxWordCount, maxListProportion)) 
				articleIds.add(art.getId()) ;
			
			
			// warn user if it looks like we wont find enough valid articles
			double roughProgress = 1-((double) roughCandidates.size()/totalRoughCandidates) ;
			if (roughProgress >= lastWarningProgress + 0.01) {
				double fineProgress = (double)articleIds.size()/size ;
			
				if (roughProgress > fineProgress) {
					System.err.println("ArticleSet | Warning : we have exhausted " + df.format(roughProgress) + " of the available pages and only gathered " + df.format(fineProgress*100) + " of the articles needed.") ;
					lastWarningProgress = roughProgress ;
				}
			}
		}
		
		if (articleIds.size() < size)
			System.err.println("ArticleSet | Warning: we could only find " + articleIds.size() + " suitable articles.") ;
	}

	/**
	 * @return the set of article ids, in ascending order.
	 */
	public TreeSet<Integer> getArticleIds() {
		return articleIds ;
	}
	
	/**
	 * Saves this list of article ids in a text file, separated by newlines. 
	 * If the file exists already, it will be overwritten.
	 * 
	 * @param file the file in which this set is to be saved
	 * @throws IOException if the file cannot be written to.
	 */
	public void save(File file) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ;
		
		for (Integer id: articleIds) 
			writer.write(id + "\n") ;
		
		writer.close() ;
	}
		
	private Vector<Article> getRoughCandidates(Wikipedia wikipedia, int minInLinks, int minOutLinks) throws SQLException {
		
		Vector<Article> articles = new Vector<Article>() ;
		ProgressNotifier pn = new ProgressNotifier(wikipedia.getDatabase().getArticleCount(), "Gathering rough candidates") ;
		
		Iterator<Page> i = wikipedia.getPageIterator(Page.ARTICLE) ;
		
		while (i.hasNext()) {
			Article art = (Article)i.next() ;
			pn.update() ;
			
			if (minOutLinks >= 0 && art.getLinksOutCount() < minOutLinks)
				continue ;
			
			if (minInLinks >= 0 && art.getLinksInCount() < minInLinks)
				continue ;
			
			articles.add(art) ;
		}
		
		return articles ;
	}
		
	private boolean isArticleValid(Article art, double minLinkProportion, double maxLinkProportion, int minWordCount, int maxWordCount, double maxListProportion) throws SQLException{
				
		//we don't want any disambiguations
		if (art.getType() == Page.DISAMBIGUATION) 
			return false ;	
		
		//we don't want any list pages
		if (art.getTitle().toLowerCase().startsWith("list")) 
			return false ;	
	
		//check if there are any other constraints
		if (minLinkProportion < 0 && maxLinkProportion < 0 && minWordCount < 0 && maxWordCount < 0 && maxListProportion < 0)
			return true ;
		
		// get and prepare markup
		String markup = art.getContent() ;
		
		if (markup == null)
			return false ;
		
		markup = MarkupStripper.stripTemplates(markup) ;
		markup = MarkupStripper.stripTables(markup) ;
		markup = MarkupStripper.stripLinks(markup) ;
		markup = MarkupStripper.stripHTML(markup) ;
		markup = MarkupStripper.stripExcessNewlines(markup) ;
		
		
		if (maxListProportion >= 0) {
			//we need to count lines and list items
			
			String[] lines = markup.split("\n") ;
			
			int lineCount = 0 ;
			int listCount = 0 ;
			
			for (String line: lines) {
				line = line.replace(':', ' ') ;
				line = line.replace(';', ' ') ;
				
				line = line.trim() ;
				
				if (line.length() > 5) {
					lineCount++ ;

					if (line.startsWith("*") || line.startsWith("#")) 
						listCount++ ;			
				}
			}
			
			double listProportion = ((double)listCount) / lineCount ;
			if (listProportion > 0.50)
				return false ;
		}
		
				
		if (minWordCount >= 0 || maxWordCount >= 0 || minLinkProportion >= 0 || maxLinkProportion >= 0 ) {
			//we need to count words
			
			int wordCount = 0 ;
			markup = MarkupStripper.stripFormatting(markup) ;
		
			Pattern wordPattern = Pattern.compile("\\W(\\w+)\\W") ; 
			Matcher wordMatcher = wordPattern.matcher(markup) ;
		
			while (wordMatcher.find()) 			
				wordCount++ ;
		
			if (wordCount < minWordCount && minWordCount != -1) 
				return false ;
			
			if (wordCount > maxWordCount && maxWordCount != -1) 
				return false ;
			
			int linkCount = art.getLinksOutIds().length ;
			double linkProportion = (double)linkCount/wordCount ;
			if (linkProportion < minLinkProportion || linkProportion > maxLinkProportion)
				return false ;
		}
		
		return true ;
	}
}
