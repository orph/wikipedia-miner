/*
 *    Category.java
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.* ;
import org.wikipedia.miner.util.*; 

/**
 * This class represents categories in Wikipedia; the pages that exist to organize articles that discuss related topics. 
 * It is intended to contain all properties and methods that are relevant for a category, such as its pertinent statistics,
 * the categories and articles it contains, and the categories it belongs to.  
 * 
 * @author David Milne
 */
public class Category extends Page {

	/**
	 * Initialises a newly created Category so that it represents the page given by <em>id</em> and <em>title</em>.
	 * 
	 * This is the most efficient constructor as no database lookup is required.
	 * 
	 * @param database	an active WikipediaDatabase 
	 * @param id	the unique identifier of the category
	 * @param title	the (case dependent) title of the category
	 */
	public Category(WikipediaDatabase database, int id, String title) {
		super(database, id, title, CATEGORY) ;
	}

	/**
	 * Initialises a newly created Category so that it represents the category given by <em>id</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param id	the unique identifier of the category
	 * @throws SQLException	if no page is defined for the id, or if it is not an article.
	 */
	public Category(WikipediaDatabase database, int id) throws SQLException{
		super(database, id) ;

		if (type != CATEGORY)
			throw new SQLException("The page given by id: " + id + " is not a category") ;
	}

	/**
	 * Initialises a newly created Category so that it represents the category given by <em>title</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param title	the (case dependent) title of the article
	 * @throws SQLException	if no article is defined for the title.
	 */
	public Category(WikipediaDatabase database, String title) throws SQLException {
		super(database, title, CATEGORY) ;
	}

	/**
	 * Returns the Article that relates to the same concept as this category. Note that many categories 
	 * do not have equivalent articles; they to not relate to a single concept, and exist only to organize the 
	 * articles and categories it contains. 
	 * i.e <em>Rugby Teams</em> may have an equivalent article, but <em>Rugby Teams by region</em> is unlikely to.
	 * In this case <em>null</em> will be returned.
	 * 
	 * @return	the equivalent Article, or null
	 * @throws	SQLException if there is a problem with the database
	 */ 
	public Article getEquivalentArticle() throws SQLException {
		Article equivalentArticle = null ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title FROM equivalence, page WHERE page_id=eq_art AND eq_cat=" + id) ;

		if (rs.first()) {
			try {
				equivalentArticle = new Article(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;	

		return equivalentArticle ;
	}

	/**
	 * Returns a SortedVector of Categories that this category belongs to. These are the categories 
	 * that are linked to at the bottom of any Wikipedia category. 
	 * 
	 * @return	a SortedVector of Categories
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public SortedVector<Category> getParentCategories() throws SQLException {
		SortedVector<Category> parentCategories = new SortedVector<Category>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id, page_title, page_type FROM categorylink, page WHERE cl_parent=page_id AND cl_child=" + id + " ORDER BY page_id") ;

		while(rs.next()) {
			try {
				Category wc = new Category(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
				parentCategories.add(wc, true) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;
		return parentCategories ;
	}
	
	/**
	 * @return a sorted array of category ids that this category belongs to. 
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public int[] getParentCategoryIds() throws SQLException {
		
		if (database.areParentIdsCached())
			return database.cachedParentIds.get(id) ;
		
		
		Vector<Integer> parentCategories = new Vector<Integer>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id FROM categorylink, page WHERE cl_parent=page_id AND cl_child=" + id + " ORDER BY page_id") ;

		while(rs.next()) 
			parentCategories.add(rs.getInt(1)) ; 

		rs.close() ;
		stmt.close() ;
		
		int[] ids = new int[parentCategories.size()] ;
		
		int c=0 ;
		for (Integer catId:parentCategories) {
			ids[c] = catId ;
			c++ ;
		}
		
		return ids ;
	}

	/**
	 * Returns a SortedVector of Categories that this category contains. These are the categories 
	 * that are presented in alphabetical lists in any Wikipedia category. 
	 * 
	 * @return	a SortedVector of Categories
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public SortedVector<Category> getChildCategories() throws SQLException{
		SortedVector<Category> childCategories = new SortedVector<Category>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id, page_title, page_type FROM categorylink, page WHERE cl_child=page_id AND page_type=" + CATEGORY + " AND cl_parent=" + id) ;

		while(rs.next()) {
			try {
				Category wc = new Category(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
				childCategories.add(wc, true) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;

		return childCategories ;	
	}
	
	
	/**
	 * @return a sorted array of category ids that this category belongs to. 
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public int[] getChildCategoryIds() throws SQLException {
		Vector<Integer> childCategories = new Vector<Integer>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id FROM categorylink, page WHERE cl_child=page_id AND page_type=" + CATEGORY + " AND cl_parent=" + id + " ORDER BY cl_child") ;

		while(rs.next()) 
			childCategories.add(rs.getInt(1)) ; 

		rs.close() ;
		stmt.close() ;
		
		int[] ids = new int[childCategories.size()] ;
		
		int c=0 ;
		for (Integer catId:childCategories) {
			ids[c] = catId ;
			c++ ;
		}
		
		return ids ;
	}

	/**
	 * Returns true if the argument article is a child of this category, otherwise false
	 * 
	 * @param article the article of interest
	 * @return	true if the argument article is a child of this category, otherwise false
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public boolean contains(Article article) throws SQLException {
		boolean isChild = false ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT * FROM categorylink WHERE cl_parent=" + id + " AND cl_child=" + article.getId()) ;

		if (rs.first()) 
			isChild = true ;

		rs.close() ;
		stmt.close() ;
		return isChild ;
	}

	/**
	 * Returns an ordered Vector of Articles that belong to this category.  
	 * 
	 * @return	a Vector of Articles
	 * @throws SQLException if there is a problem with the wikipedia database
	 */
	public SortedVector<Article> getChildArticles() throws SQLException {

		SortedVector<Article> childArticles = new SortedVector<Article>() ;
		Vector<Redirect> redirects = new Vector<Redirect>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type FROM categorylink, page WHERE cl_child=page_id AND page_type=" + ARTICLE + " AND cl_parent=" + id + " ORDER BY page_id") ;

		while(rs.next()) {
			try {
				childArticles.add(new Article(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")), true) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;

		for(Redirect r: redirects) {
			Page target = r.getTarget() ;
			if (target != null && target.getType() == ARTICLE)
				childArticles.add((Article)target, false) ;
		}

		return childArticles ;		
	}
	
	/**
	 * @return a sorted array of article ids that belong to this category. 
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public int[] getChildArticleIds() throws SQLException {
		Vector<Integer> childArticles = new Vector<Integer>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id FROM categorylink, page WHERE cl_child=page_id AND page_type=" + ARTICLE + " AND cl_parent=" + id + " ORDER BY page_id") ;

		while(rs.next()) 
			childArticles.add(rs.getInt(1)) ; 

		rs.close() ;
		stmt.close() ;	
		
		int[] ids = new int[childArticles.size()] ;
		
		int c=0 ;
		for (Integer catId:childArticles) {
			ids[c] = catId ;
			c++ ;
		}
		
		return ids ;
	}
	
	/**
	 * Provides a demo of functionality available to Categories
	 * 
	 * @param args an array of arguments for connecting to a wikipedia database: server and database names at a minimum, and optionally a username and password
	 * @throws Exception if there is a problem with the wikipedia database.
	 */
	public static void main(String[] args) throws Exception{

		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));	

		while (true) {
			System.out.println("Enter category title (or press ENTER to quit): ") ;
			String title = in.readLine() ;

			if (title == null || title.equals(""))
				break ;

			Category category = wikipedia.getCategoryByTitle(title) ; 

			if (category == null) {
				System.out.println("Could not find category. Try again") ; 
			}else {

				System.out.println("Category: " + category) ; 
				
				if (wikipedia.getDatabase().isContentImported()) {
					
					System.out.println(" - first sentence:") ;
					System.out.println("    - " + category.getFirstSentence(null, null)) ;
					
					System.out.println(" - first paragraph:") ;
					System.out.println("    - " + category.getFirstParagraph()) ;
				}

				Article eqArticle = category.getEquivalentArticle() ;
				if (eqArticle != null) {
					System.out.println("\n - equivalent article") ;
					System.out.println("    - " + eqArticle) ;
				}
				
				System.out.println("\n - parent categories (broader topics): ") ;
				for (Category c: category.getParentCategories()) 
					System.out.println("    - " + c) ; 
				
				System.out.println("\n - child categories (narrower topics): ") ;
				for (Category c: category.getChildCategories()) 
					System.out.println("    - " + c) ; 

				System.out.println("\n - child articles (narrower topics): ") ;
				for (Article a: category.getChildArticles()) 
					System.out.println("    - " + a) ; 
			}
			System.out.println("") ;
		}
	}
}
