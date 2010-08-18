/*
 *    Disambiguation.java
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

import org.wikipedia.miner.util.* ;
import java.sql.*;
import java.util.* ;

/**
 * This class represents disambiguation pages in Wikipedia; the pages that list the various articles that an
 * ambiguous term may refer to.
 *  <p>
 * On top of the functionality provided by Article, it attempts to identify the linked articles which relate to
 * alternative senses for the term in question. This is done through the following heuristics:
 * <ul>
 * <li> Only the first link in each line or list item is used. </li>
 * <li> This link is only used if the ambiguous term does not occur before it on the same line. </li>
 * <li> All links within the "See Also" section are discarded.</li>
 * </ul>
 * 
 * @author David Milne
 */
public class Disambiguation extends Article{

	/**
	 * Initializes a newly created DisambiguationPage so that it represents the page given by <em>id</em> and <em>title</em>.
	 * 
	 * This is the most efficient constructor as no database lookup is required.
	 * 
	 * @param database	an active WikipediaDatabase 
	 * @param id	the unique identifier of the disambiguation page
	 * @param title	the (case dependent) title of the disambiguation page
	 */
	public Disambiguation(WikipediaDatabase database, int id, String title) {
		super(database, id, title) ;
		type = DISAMBIGUATION ;
	}
	
	/**
	 * Initializes a newly created DisambiguationPage so that it represents the disambiguation page given by <em>id</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param id	the unique identifier of the disambiguation page
	 * @throws SQLException	if no page is defined for the id, or if it is not a disambiguation page.
	 */
	public Disambiguation(WikipediaDatabase database, int id) throws SQLException{
		super(database, id) ;
		
		if (type != DISAMBIGUATION)
			throw new SQLException("The page given by id: " + id + " is not a disambiguation page") ;
	}
	
	/**
	 * Initializes a newly created DisambiguationPage so that it represents the disambiguation page given by <em>title</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param title	the (case dependent) title of the disambiguation page
	 * @throws SQLException	if no disambiguation page is defined for the title.
	 */
	public Disambiguation(WikipediaDatabase database, String title) throws SQLException {
		super(database, title, DISAMBIGUATION) ;
	}
		
	/**
	 * Returns the most obvious or most common sense of the ambiguous term, by selecting the first article that the 
	 * disambiguation page links to. 
	 * 
	 * @return the most obvious (first) sense listed.
	 * @throws SQLException	if there is a problem with the Wikipedia database.
	 */
	public SensePage getMostObviousSense() throws SQLException{
		SensePage sense = null ;
		
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type, da_index, da_scope FROM disambiguation, page WHERE da_to=page_id AND da_from=" + id + " AND da_index=1") ;

		if (rs.first()) {
			try {
				sense = new SensePage(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8"), rs.getInt(3), rs.getInt(4), new String(rs.getBytes(5), "UTF-8")) ;
			} catch (Exception e) {} ;
		}
		
		rs.close() ;
		stmt.close() ;
		
		return sense ;
	}
	
	/**
	 * @return all senses of the ambiguous term, ordered by page id.
	 * @throws SQLException	if there is a problem with the Wikipedia database.
	 */
	public SortedVector<SensePage> getSenses() throws SQLException{
		
		SortedVector<SensePage> senses = new SortedVector<SensePage>() ;
		
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type, da_index, da_scope FROM disambiguation, page WHERE da_to=page_id AND da_from=" + id + " ORDER BY page_id") ;

		while (rs.next()) {
			try {
				SensePage sense = new SensePage(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8"), rs.getInt(3), rs.getInt(4), new String(rs.getBytes(5), "UTF-8")) ;
				senses.add(sense, true) ;
			} catch (Exception e) {} ;
		}
		
		rs.close() ;
		stmt.close() ;
		
		return senses ;
	}
	
	/**
	 * Returns all senses of the ambiguous term, in the order they were found on the page.
	 * This order usually correlates with how obvious or well known each sense is. 
	 * 
	 * @return see above
	 * @throws SQLException	if there is a problem with the wikipedia database.
	 */
	public Vector<SensePage> getSensesInPageOrder() throws SQLException{
		
		Vector<SensePage> senses = new Vector<SensePage>() ;
		
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type, da_index, da_scope FROM disambiguation, page WHERE da_to=page_id AND da_from=" + id + " ORDER BY da_index") ;

		while (rs.next()) {
			try {
				SensePage sense = new SensePage(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8"), rs.getInt(3), rs.getInt(4), new String(rs.getBytes(5), "UTF-8")) ;
				senses.add(sense) ;
			} catch (Exception e) {} ;
		}
		
		rs.close() ;
		stmt.close() ;
		
		return senses ;
	}
	
	
	
	/**
	 * Provides a demo of functionality available to Disambiguations
	 * 
	 * @param args an array of arguments for connecting to a wikipedia database: server and database names at a minimum, and optionally a username and password
	 * @throws Exception if there is a problem with the wikipedia database.
	 */
	public static void main(String[] args) throws Exception {
		
		Wikipedia wikipedia = new Wikipedia("kia", "enwiki_20071120", "root", "");
			
		Disambiguation dp = new Disambiguation(wikipedia.getDatabase(), "Texas (disambiguation)") ;
		System.out.println("Disambiguation Page: " + dp) ;
		System.out.println("") ;
			
		System.out.println("Most obvious Sense: " + dp.getMostObviousSense().getArticle()) ;
		System.out.println("") ;
			
		for (SensePage sp: dp.getSensesInPageOrder()) {
			System.out.println(" - Sense (" + sp.getIndex() + "): " + sp) ;
			System.out.println("     " + sp.getScopeNote()) ;
		}
		System.out.println("") ;
	}
}
