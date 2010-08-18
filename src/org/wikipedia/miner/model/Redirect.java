/*
 *    Redirect.java
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

import java.sql.*;
import java.util.*;

/**
 * This class represents redirects in Wikipedia; the links that have been defined to connect synonyms to the correct article
 * (i.e <em>Farming</em> redirects to <em>Agriculture</em>).  
 * It is intended to contain all properties and methods that are relevant for a redirect. 
 * 
 * @author David Milne
 */
public class Redirect extends Page {
	
	/**
	 * Initializes a newly created Redirect so that it represents the page given by <em>id</em> and <em>title</em>.
	 * 
	 * This is the most efficient constructor as no database lookup is required.
	 * 
	 * @param database	an active WikipediaDatabase 
	 * @param id	the unique identifier of the redirect
	 * @param title	the (case dependent) title of the redirect
	 */
	public Redirect(WikipediaDatabase database, int id, String title) {
		super(database, id, title, REDIRECT) ;
	}
	
	/**
	 * Initializes a newly created Redirect so that it represents the redirect given by <em>id</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param id	the unique identifier of the redirect
	 * @throws SQLException	if no page is defined for the id, or if it is not an article.
	 */
	public Redirect(WikipediaDatabase database, int id) throws SQLException{
		super(database, id) ;
		
		if (type != REDIRECT)
			throw new SQLException("The page given by id: " + id + " is not a category") ;
	}
	
	/**
	 * Initialises a newly created Redirect so that it represents the redirect given by <em>title</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param title	the (case dependent) title of the redirect
	 * @throws SQLException	if no redirect is defined for the title.
	 */
	public Redirect(WikipediaDatabase database, String title) throws SQLException {
		super(database, title, REDIRECT) ;
	}

	/**
	 * Returns the Page that this redirect points to. 
	 * 
	 * @return	the equivalent Page for this redirect.
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */	
	public Article getTarget() throws SQLException{
		
		Article target = null;
		
		int currId = id ;
		
		HashSet<Integer> redirectsFollowed = new HashSet<Integer>() ;
		
		while (target == null && !redirectsFollowed.contains(currId)) {
			redirectsFollowed.add(currId) ;
			
			Statement stmt = getWikipediaDatabase().createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type FROM redirect, page WHERE rd_to=page_id AND rd_from=" + id) ;
		
			if (rs.first()) {
				try {
					switch(rs.getInt(3)) {
				
					case ARTICLE: 
						target = new Article(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
						break ;
					case REDIRECT:
						currId = rs.getInt(1) ; 
						break ;
					case DISAMBIGUATION:
						target = new Disambiguation(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
						break ;		
					}
				} catch (Exception e) {} ;
			}
		
			rs.close() ;
			stmt.close() ;
		}
		
		return target ;		
	}	
	
	/**
	 * For testing.
	 * E.g. Gavialis_gangeticus -> Gharial
	 */
	public static void main(String[] args) {
		try {
			Wikipedia wikipedia = new Wikipedia("pohutukawa", "enwiki_20071120", "root", "");
			
			Redirect r = new Redirect(wikipedia.getDatabase(), 1945087) ;
			//Redirect r= new Redirect(wikipedia.getDatabase(), "Gavialis gangeticus");
			
			System.out.println("Redirect: " + r) ;
			System.out.println("Target: " + r.getTarget()) ;
			
			System.out.println("") ;
			
			System.out.println(wikipedia.getDatabase().getStatementsIssuedSinceStartup() + " statements issued.") ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
}
