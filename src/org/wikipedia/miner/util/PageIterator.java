/*
 *    PageIterator.java
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

import java.util.* ;
import java.sql.* ;
import org.wikipedia.miner.model.* ;

/**
 * @author David Milne
 * 
 * Provides efficient iteration over the pages in Wikipedia
 */
public class PageIterator implements Iterator<Page> {

	WikipediaDatabase database ;

	Vector<Page> pageBuffer ;
	int pageType = -1 ;

	int lastId = 0 ;
	int bufferSize = 100000 ;

	/**
	 * Creates an iterator that will loop through all pages in Wikipedia.
	 * 
	 * @param database an active (connected) Wikipedia database.
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public PageIterator(WikipediaDatabase database) throws SQLException {
		this.database = database ;
		pageBuffer = new Vector<Page>() ;

		fillBuffer() ;
	}

	/**
	 * Creates an iterator that will loop through all pages of the given type in Wikipedia.
	 * 
	 * @param database an active (connected) Wikipedia database.
	 * @param pageType the type of page to restrict the iterator to (ARTICLE, CATEGORY, REDIRECT or DISAMBIGUATION_PAGE)
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public PageIterator(WikipediaDatabase database, int pageType) throws SQLException {
		this.database = database ;
		this.pageType = pageType ;
		pageBuffer = new Vector<Page>() ;

		fillBuffer() ;
	}

	public boolean hasNext() {
		return !pageBuffer.isEmpty() ;
	}

	public void remove() {
		throw new UnsupportedOperationException() ;
	}

	public Page next() {

		if (pageBuffer.isEmpty())
			throw new NoSuchElementException() ;

		Page p = pageBuffer.firstElement() ;
		pageBuffer.remove(0) ;

		try {
			while (pageBuffer.isEmpty()) 
				fillBuffer() ;
		} catch (SQLException e) {} ;

		return p ;
	}

	private void fillBuffer() throws SQLException{

		Statement stmt = database.createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title, page_type FROM page WHERE page_id>" + lastId + " ORDER BY page_id LIMIT " + bufferSize) ;

		if (!rs.first()) {
			// there is nothing left to retrieve
			rs.close() ;
			stmt.close() ;
			
			throw new SQLException("No pages left to retrieve") ;
		}

		while (rs.next()) {

			try {
				int id = rs.getInt(1) ;
				String title = new String(rs.getBytes(2), "UTF-8") ;
				int type = rs.getInt(3) ;

				Page p = Page.createPage(database, id, title, type) ; ;
				
				//oddly, it is faster to cut out unwanted page types here, rather than with the database call
				if (p != null && (pageType<0 || type == pageType)) {
					pageBuffer.add(p) ;
				}

				lastId = id ;

			} catch (Exception e) {
				e.printStackTrace() ;
			} ;	
		}
		
		rs.close() ;
		stmt.close() ;
	}
}

