/*
 *    RelatednessCache.java
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

import org.wikipedia.miner.model.Article;
import gnu.trove.* ;
import java.sql.*;

/**
 * @author David Milne
 *
 * This class caches the results of calculating relatedness measures; 
 * If all relatedness comparisons are performed via this class, then no calculations will be repeated.
 */
public class RelatednessCache {

	TLongDoubleHashMap cachedRelatedness ;
	
	/**
	 * Initializes the relatedness cache.
	 */
	public RelatednessCache() {
		cachedRelatedness = new TLongDoubleHashMap() ;
	}
	
	/**
	 * Calculates (or retrieves) the semantic relatedness of two articles. 
	 * The result will be identical to that returned by art1.getRelatednessTo(art2) or art2.getRelatednessTo(art1)
	 * 
	 * @param art1 
	 * @param art2
	 * @return the semantic relatedness of art1 and art2
	 * @throws SQLException
	 */
	public double getRelatedness(Article art1, Article art2) throws SQLException {
		
		//generate unique key for this pair
		long min = Math.min(art1.getId(), art2.getId()) ;
		long max = Math.max(art1.getId(), art2.getId()) ;
		long key = min + (max << 30) ;
				
		if (!cachedRelatedness.containsKey(key)) {		
			Double rel = art1.getRelatednessTo(art2) ;		
			cachedRelatedness.put(key, rel) ;
			return rel ;
		} else {			
			return cachedRelatedness.get(key) ;
		}
	}	
}
