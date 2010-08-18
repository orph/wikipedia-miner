/*
 *    Page.java
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

package org.wikipedia.miner.model;

import java.io.*;
import java.sql.*;
import java.util.Vector;

import org.wikipedia.miner.model.WikipediaDatabase.CachedPage;
import org.wikipedia.miner.util.*;


/**
 * This class provides properties and methods that are relevant for all pages in Wikipedia. 
 * It may relate to an article, a redirect to an article, a disambiguation page or a category. 
 * <p>
 * The type of a page can be examined so that an instance of the appropriate sub-class can be obtained 
 * for additional functionality: e.g. if (page.getType == Page.ARTICLE) Article art = (Article)page ;
 * 
 * @author David Milne
 */
public abstract class Page implements Comparable<Page>{

	protected WikipediaDatabase database ;

	protected int id ;
	protected int type ;
	protected String title ;

	protected String titleWithoutScope ;
	protected String scope ;

	protected double weight = -1 ;

	/**
	 * the page type corresponding to Articles
	 */
	public static final int ARTICLE = 1 ;

	/**
	 * the page type corresponding to Categories
	 */
	public static final int CATEGORY = 2 ;

	/**
	 * the page type corresponding to Redirects
	 */
	public static final int REDIRECT = 3 ;

	/**
	 * the page type corresponding to DisambiguationPages
	 */
	public static final int DISAMBIGUATION = 4 ;

	/**
	 * Initialises a newly created Page so that it represents the page given by <em>id</em>, <em>title</em> and <em>type</em>.
	 * 
	 * This is the most efficient page constructor as no database lookup is required.
	 * 
	 * @param	database	an active WikipediaDatabase
	 * @param	id	the unique identifier of the page
	 * @param	title	the (case dependent) title of the page
	 * @param	type	the type of the page (ARTICLE, CATEGORY, REDIRECT or DISAMBIGUATION_PAGE)
	 */
	public Page(WikipediaDatabase database, int id, String title, int type)  {
		this.database = database ;
		this.id = id ;
		this.title = title ;
		this.type = type ;
	}

	/**
	 * Initializes a newly created Page so that it represents the page given by <em>id</em>.
	 * 
	 * @param	database	an active WikipediaDatabase
	 * @param	id	the unique identifier of the Wikipedia page
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public Page(WikipediaDatabase database, int id) throws SQLException{
		this.database = database ;
		this.id = id ;

		boolean detailsSet = false ;

		if (database.arePagesCached()) {
			CachedPage p = database.cachedPages.get(id) ;

			if (p != null) {
				this.title = p.title ;
				this.type = p.type ;
				detailsSet = true ;
			}
		} else {
			Statement stmt = database.createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT page_title, page_type FROM page WHERE page_id=" + id) ;

			if (rs.first()) {
				try {
					title = new String(rs.getBytes(1), "UTF-8") ;
				} catch (Exception e) {} ;	

				type = rs.getInt(2) ;

				detailsSet = true ;
			}

			rs.close() ;
			stmt.close();
		}

		if (!detailsSet)
			throw new SQLException("No page defined for id:" + id) ;	
	}

	/**
	 * Initialises a newly created Page so that it represents the page given by <em>title</em> and <em>type</em>.
	 * 
	 * @param	database	an active WikipediaDatabase
	 * @param	title	the (case sensitive) title of the page.
	 * @param    type 	the type of the page (ARTICLE, CATEGORY, REDIRECT or DISAMBIGUATION_PAGE)
	 * @throws 	SQLException	if no page is defined for the given title and type 
	 */
	public Page(WikipediaDatabase database, String title, int type) throws SQLException{
		this.database = database ;
		this.title = title ;
		this.type = type ;

		boolean detailsSet = false ;

		Statement stmt = database.createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id FROM page WHERE page_title=\"" + title + "\" AND page_type=" + type) ;

		if (rs.first()) {
			id = rs.getInt(1) ;
			detailsSet = true ;
		}

		rs.close() ;
		stmt.close();

		if (!detailsSet)
			throw new SQLException("No page defined for title:" + title + " and type:" + type) ;
	}

	/**
	 * Sets the weight by which this page will be compared to others.
	 * 
	 * @param weight  the weight by which this page will be compared to others.
	 */
	public void setWeight(double weight) {
		this.weight = weight ;
	}

	/**
	 * Gets the weight by which this page is compared to others.
	 * 
	 * @return the weight by which this page is compared to others.
	 */	
	public double getWeight() {
		return weight ;
	}

	public boolean equals(Object o) {
		Page p = (Page) o ;
		return p.getId() == id ;
	}

	/**
	 * Compares this page to another. If weights are defined for both pages, then the page with the larger 
	 * weight will be considered smaller (and thus appear earlier in sorted lists). Otherwise, the comparison is made based on their unique identifiers. 
	 * 
	 * @param	p	the Page to be compared
	 * @return	see above.
	 */
	public int compareTo(Page p) {

		if (p.getId() == id)
			return 0 ;

		if (p.weight >= 0 && weight >= 0 && p.weight != weight)
			return -1 * (new Double(weight)).compareTo(p.weight) ;

		return (new Integer(id)).compareTo(p.getId()) ;
	}

	/**
	 * Returns a string representation of this page, in the format "<em>id</em> - <em>title</em>".
	 * 
	 * @return a string representation of the page
	 */
	public String toString() {
		String s = id + ": " + title ;
		return s ;
	}

	/**
	 * Returns the database in which details of this page is stored.
	 * 
	 * @return the database
	 */
	protected WikipediaDatabase getWikipediaDatabase() {
		return database;
	}

	/**
	 * Returns the unique identifier for this page.
	 * 
	 * @return the unique identifier
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the title of this page. 
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the type of the page, which may be an ARTICLE, CATEGORY, REDIRECT, or DISAMBUGATION
	 * 
	 * @return	the type of the page
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the generality of the page (a function of how far down the category tree it is located).
	 * 
	 * @return	the generality of the page
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public double getGenerality() throws SQLException {

		int depth = -1 ;

		if (database.cachedGenerality != null) {
			if (database.cachedGenerality.containsKey(id))
				depth = database.cachedGenerality.get(id) ;
		} else {
			Statement stmt = database.createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT gn_depth FROM generality WHERE gn_id=" + id) ;

			if (rs.first()) {
				try {
					depth = rs.getInt(1) ; 
				} catch (Exception e) {} ;
			}

			rs.close() ;
			stmt.close() ;
		}

		if (depth < 0) {
			return -1 ;
		} else {
			return 1-((double)depth/database.getMaxPageDepth()) ; 
		}
	}

	/**
	 * Returns the content of the page, marked up in raw media wiki form. You can then use Markup stripper to
	 * get the parts you are interested in. 
	 * 
	 * @return	content of the page, in raw media wiki format.
	 * @throws	SQLException if page content has not been stored in the database. 
	 */
	public String getContent() throws SQLException {

		if (!database.isContentImported()) {
			throw new SQLException("Page content has not been imported") ;
		} else {
			String content = null ;

			Statement stmt = database.createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT co_content FROM content WHERE co_id=" + id) ;

			if (rs.first()) {
				try {
					content = new String(rs.getBytes(1), "UTF-8") ;
				} catch (Exception e) {} ;
			}

			rs.close() ;
			stmt.close() ;

			if (content != null) {
				//content.replaceAll("\\{","{") ;
				//content.replaceAll("\\}","}") ;
			}
			return content ;
		}
	}

	/**
	 * Returns the first sentence from the content of this page, cleaned of all markup except links and 
	 * basic formating. 
	 * This generally serves as a definition of the concept or concepts for which this article, 
	 * disambiguation page or category was written.
	 * 
	 * @param paragraph this is more efficient if you have already gathered the firstParagraph. If not, just use null.
	 * @param ss this is more efficient if you have already constructed a sentence splitter. If not, just use null.
	 * @return the first sentence on this page.
	 * @throws SQLException if page content has not been imported, or if there is another problem with the Wikipedia database
	 * @throws Exception if there is a problem splitting the text into sentences.
	 */
	public String getFirstSentence(String paragraph, SentenceSplitter ss) throws SQLException, Exception {
		if (!database.isContentImported()) 
			throw new SQLException("Page content has not been imported") ;

		if (database.areDefinitionsSummarized()) {
			String fs = "" ;
			
			Statement stmt = database.createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT df_firstSentence FROM definition WHERE df_id=" + id) ;

			if (rs.first()) {
				try {
					fs = new String(rs.getBytes(1), "UTF-8") ;
				} catch (Exception e) {} ;	
			}

			rs.close() ;
			stmt.close() ;

			return fs ;	
		}

		if (paragraph==null)
			paragraph = getFirstParagraph() ;

		if (paragraph.equals(""))
			return "" ;

		if (ss==null)
			ss = new SentenceSplitter() ;
		
		StringBuffer sb = new StringBuffer() ;

		Vector<String> sentences = ss.getSentences(paragraph, SentenceSplitter.MULTIPLE_NEWLINES) ;

		for (String sentence:sentences) {
			sb.append(sentence.trim()) ;

			if (sb.length() > 30)
				break ;
		}

		return sb.toString() ;

	}

	/**
	 * Returns the first paragraph from the content of this page, cleaned of all markup except links and 
	 * basic formating. 
	 * This generally serves as a more specific definition of the concept or concepts for which this 
	 * article, disambiguation page or category was written.
	 * 
	 * @return the first paragraph on this page.
	 * @throws SQLException if page content has not been imported, or if there is another problem with the Wikipedia database
	 */
	public String getFirstParagraph() throws SQLException {
		if (!database.isContentImported()) 
			throw new SQLException("Page content has not been imported") ;

		String fp = null ;
		
		if (database.areDefinitionsSummarized()) {
			
			Statement stmt = database.createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT df_firstParagraph FROM definition WHERE df_id=" + id) ;

			if (rs.first()) {
				try {
					fp = new String(rs.getBytes(1), "UTF-8") ;
				} catch (Exception e) {} ;	
			}

			rs.close() ;
			stmt.close() ;
		}

		if (fp == null) {
		
			String content = getContent() ;

			content = content.replaceAll("={2,}(.+)={2,}", "\n") ; //clear section headings completely - not just formating, but content as well.			
			content = MarkupStripper.stripTemplates(content) ;
			content = MarkupStripper.stripImages(content) ;
			content = MarkupStripper.stripExternalLinks(content) ;
			content = MarkupStripper.stripIsolatedLinks(content) ;
			content = MarkupStripper.stripTables(content) ;
			content = MarkupStripper.stripHTML(content) ;
			content = MarkupStripper.stripMagicWords(content) ;
			content = MarkupStripper.stripListItems(content) ;
			content = MarkupStripper.stripOrphanedBrackets(content) ;
			content = MarkupStripper.stripIndentedStart(content) ;
			content = MarkupStripper.stripExcessNewlines(content) ;
	
			fp = "" ;
			int pos = content.indexOf("\n\n") ;
	
			while (pos>=0) {
				fp = content.substring(0, pos) ;
	
				if (pos > 150) 
					break ;
	
				pos = content.indexOf("\n\n", pos+2) ;
			}
	
			fp = fp.replaceAll("\n", " ") ;
			fp = fp.replaceAll("\\s+", " ") ;  //turn all whitespace into spaces, and collapse them.
			fp = fp.trim();
		}
		return fp ;
	}

	/**
	 * Returns the title without the text used to differentiate between ambiguous terms.
	 * e.g. returns <em>Plane</em> for both <em>Plane (mathematics)</em> and <em>Plane (vehicle)</em>
	 * 
	 * @return the title without scope text
	 */
	public String getTitleWithoutScope() {
		if (titleWithoutScope==null)
			setTitleParts() ;

		return titleWithoutScope ;	
	}

	/**
	 * Returns the text found within parenthesis in the title.
	 * This typically indicates scope; to differentiate an ambiguous title: 
	 * 
	 * e.g. Plane (mathematics) v.s. Plane (vehicle). 
	 * 
	 * @return the parenthesisText, or null if none is found.
	 */
	public String getScope() {
		if (titleWithoutScope==null) 
			setTitleParts() ;

		return scope ;	
	}

	private void setTitleParts() {

		int pos1 = title.lastIndexOf('(') ;
		int pos2 = title.lastIndexOf(')') ;

		if (pos1 > 0 && pos2==title.length()-1) {
			titleWithoutScope = title.substring(0, pos1).trim() ;
			scope = title.substring(pos1+1, pos2).trim() ;
		} else {
			titleWithoutScope = title ;
			scope = null ;
		}
	}

	/**
	 * Provides a demo of functionality available to Pages
	 * 
	 * @param args an array of arguments for connecting to a wikipedia datatabase: server and database names at a minimum, and optionally a username and password
	 * @throws Exception if there is a problem with the wikipedia database.
	 */
	public static void main(String[] args) throws Exception{
		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));	

		while (true) {
			System.out.println("Enter article title (or press ENTER to quit): ") ;
			String title = in.readLine() ;

			if (title == null || title.equals(""))
				break ;

			Article art = wikipedia.getMostLikelyArticle(title, null) ;

			System.out.println("Page: " + art) ;
			System.out.println(" - title without scope: \"" + art.getTitleWithoutScope() + "\"") ;
			System.out.println(" - scope: \"" + art.getScope() + "\"") ;
			System.out.println(" - generality: " + art.getGenerality()) ;
			System.out.println("") ;

			if (wikipedia.getDatabase().isContentImported()) {
				//System.out.println("Page Content: \n") ;
				//System.out.println(art.getContent()) ;

				System.out.println(art.getFirstSentence(null, null)) ;
				System.out.println() ;
				System.out.println(art.getFirstParagraph()) ;
			}
		}
	}

	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 * @param database an active Wikipedia database
	 * @param id the id of the page
	 * @param title the title of the page
	 * @param type the type of the page (ARTICLE, CATEGORY, REDIRECT or DISAMBIGUATION_PAGE)
	 * @return the instantiated page, which can be safely cast as appropriate
	 */
	public static Page createPage(WikipediaDatabase database, int id, String title, int type) {

		Page p = null ;

		switch (type) {
		case Page.ARTICLE:
			p = new Article(database, id, title) ;
			break ;
		case Page.REDIRECT:
			p = new Redirect(database, id, title) ;
			break ;
		case Page.DISAMBIGUATION:
			p = new Disambiguation(database, id, title) ;
			break ;
		case Page.CATEGORY:
			p = new Category(database, id, title) ;
			break ;
		}

		return p ;
	}
}
