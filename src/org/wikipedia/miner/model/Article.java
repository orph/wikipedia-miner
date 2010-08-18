/*
 *    Article.java
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
import java.sql.ResultSet ;
import java.sql.Statement ;
import java.sql.SQLException ;
import java.text.DecimalFormat;
import java.util.* ; 

import org.wikipedia.miner.util.* ;

/**
 * This class represents articles in Wikipedia; the pages that contain descriptive text regarding a particular topic. 
 * It is intended to contain all properties and methods that are relevant for an article, such as its pertinent statistics,
 * the categories it belongs to, and the articles that link to it.  
 * 
 * @author David Milne
 */
public class Article extends Page {

	/**
	 * ids of incoming links - needed every time we calculate sr, so lets cache it
	 */
	private int inLinkIds[] ;

	/**
	 * ids and counts of outgoing links - needed every time we calculate sr, so lets cache it
	 */
	private int outLinkIdsAndCounts[][] ;


	/**
	 * Initialises a newly created Article so that it represents the page given by <em>id</em> and <em>title</em>.
	 * 
	 * This is the most efficient constructor as no database lookup is required.
	 * 
	 * @param database	an active WikipediaDatabase 
	 * @param id	the unique identifier of the article
	 * @param title	the (case dependent) title of the article
	 */
	public Article(WikipediaDatabase database, int id, String title) {
		super(database, id, title, ARTICLE) ;
	}

	/**
	 * Initializes a newly created Article so that it represents the article given by <em>id</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param id	the unique identifier of the article
	 * @throws SQLException	if no page is defined for the id, or if it is not an article.
	 */
	public Article(WikipediaDatabase database, int id) throws SQLException{
		super(database, id) ;

		if (type != ARTICLE && type != DISAMBIGUATION)
			throw new SQLException("The page given by id: " + id + " is not an article") ;
	}

	/**
	 * Initialises a newly created Article so that it represents the article given by <em>title</em>.
	 * 
	 * @param database	an active WikipediaDatabase
	 * @param title	the (case dependent) title of the article
	 * @throws SQLException	if no article is defined for the title.
	 */
	public Article(WikipediaDatabase database, String title) throws SQLException {
		super(database, title, ARTICLE) ;
	}

	protected Article(WikipediaDatabase database, String title, int type) throws SQLException{
		super(database, title, type) ;
	}
	
	/**
	 * Returns a SortedVector of Redirects that point to this article.
	 * 
	 * @return	a SortedVector of Redirects
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public SortedVector<Redirect> getRedirects() throws SQLException{
		SortedVector<Redirect> redirects = new SortedVector<Redirect>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id, page_title, page_type FROM redirect, page WHERE page_id=rd_from AND rd_to=" + id) ;

		while(rs.next()) {
			try {
				Redirect wr = new Redirect(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
				redirects.add(wr, true) ;
			} catch (Exception e) {} ;	
		}

		rs.close() ;
		stmt.close() ;

		return redirects ;	
	}
	
	/**
	 * @return an array of category ids that this article belongs to. 
	 */
	public int[] getParentCategoryIds() {
		
		int[] parentIds = null;
		
		if (database.areParentIdsCached())
			parentIds = database.cachedParentIds.get(id) ;
		
		if (parentIds == null)
			parentIds = new int[0] ;
		
		return parentIds ;
	}

	/**
	 * Returns a SortedVector of Categories that this article belongs to. These are the categories 
	 * that are linked to at the bottom of any Wikipedia article. Note that one of these will be the article's
	 * equivalent category, if one exists.
	 * 
	 * @return	a Vector of WikipediaCategories
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public SortedVector<Category> getParentCategories() throws SQLException {
		//TODO: use cached parent ids
		
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
	 * Returns the Category that relates to the same concept as this article. For instance, calling 
	 * this for "6678: Cat" returns the category "799717: Cats"
	 * 
	 * Note that many articles do not have equivalent categories; they are only used when the article 
	 * describes a general topic for which there are other, more specific, articles. Consequently, 
	 * this method will often return null. 
	 * 
	 * @return	the equivalent Category, or null
	 * @throws SQLException if there is a problem with the wikipedia database
	 */
	public Category getEquivalentCategory() throws SQLException{

		Category equivalentCategory = null ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title FROM equivalence, page WHERE page_id=eq_cat AND eq_art=" + id) ;

		if (rs.first()) {
			try {
				equivalentCategory = new Category(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;	

		return equivalentCategory ;
	}

	/**
	 * Returns a SortedVector of Articles that link to this article. These 
	 * are defined by the internal hyperlinks within article text. If these hyperlinks came via 
	 * redirects, then they are resolved.
	 * 
	 * @return	the SortedVector of Articles that this article links to
	 * @throws SQLException if there is a problem with the wikipedia database
	 */
	public SortedVector<Article> getLinksIn() throws SQLException{

		SortedVector<Article> articles = new SortedVector<Article>() ;	

		String query = "" ;

		for (int linkIn: getLinksInIds()) {
			query = query + linkIn + "," ;
		}

		if (query.length() > 0) {
			query = query.substring(0, query.length()-1) ;

			Statement stmt = getWikipediaDatabase().createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id, page_title, page_type FROM page WHERE page_id IN (" + query + ") ORDER BY page_id") ;

			while(rs.next()) {
				int type = rs.getInt(3) ;
				try {
					if (type == Page.ARTICLE) 
						articles.add(new Article(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")), true) ;

					if (type == Page.DISAMBIGUATION) 
						articles.add(new Disambiguation(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")), true) ;
				} catch (Exception e) {} ;	
			}
			rs.close() ;
			stmt.close() ;
		}

		return articles ;
	}

	/**
	 * Returns a Vector of Articles that this article links to. These 
	 * are defined by the internal hyperlinks within article text. If these hyperlinks point
	 * to redirects, then these are resolved. 
	 * 
	 * @return	the Vector of Articles that this article links to
	 * @throws SQLException if there is a problem with the wikipedia database
	 */
	public SortedVector<Article> getLinksOut() throws SQLException{

		SortedVector<Article> articles = new SortedVector<Article>() ;	

		String query = "" ;

		for (int linksOut[]: getLinksOutIdsAndCounts()) {
			query = query + linksOut[0] + "," ;
		}

		if (query.length() > 0) {
			query = query.substring(0, query.length()-1) ;

			Statement stmt = getWikipediaDatabase().createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT DISTINCT page_id, page_title, page_type FROM page WHERE page_id IN (" + query + ") ORDER BY page_id") ;

			while(rs.next()) {
				int type = rs.getInt(3) ;
				try {
					if (type == Page.ARTICLE) 
						articles.add(new Article(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")), true) ;

					if (type == Page.DISAMBIGUATION) 
						articles.add(new Disambiguation(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")), true) ;
				} catch (Exception e) {} ;	
			}
			rs.close() ;
			stmt.close() ;
		}

		return articles ;
	}

	/**
	 * Returns a Vector of language codes for which translations are available (i.e. fn, jp, de, etc). 
	 * 
	 * @return true if there exists a link from this article to the argument one; otherwise false.
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */	
	public Vector<String> getAvaliableLanguages() throws SQLException {
		Vector<String> languages = new Vector<String>() ; 

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT tl_lang FROM translation WHERE tl_id=" + id) ; 

		while(rs.next())
			languages.add(rs.getString(1)) ;

		rs.close() ;
		stmt.close() ;

		return languages ;
	}

	/**
	 * Returns the title of the article translated into the language given by <em>languageCode</em>
	 * (i.e. fn, jp, de, etc) or null if translation is not available. 
	 * 
	 * @param languageCode	the (generally 2 character) language code.
	 * @return the translated title if it is available; otherwise null.
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */	
	public String getTranslation(String languageCode) throws SQLException {
		String translation = null ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT tl_text FROM translation WHERE tl_id=" + id + " AND tl_lang=\"" + languageCode + "\"") ; 

		try {
			if(rs.first())
				translation = new String(rs.getBytes(1), "UTF-8") ;
		} catch (Exception e) {} ;			

		rs.close() ;
		stmt.close() ;

		return translation ;
	}

	/**
	 * Returns a HashMap associating language code with translated title for all available translations 
	 * 
	 * @return a HashMap associating language code with translated title.
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */	
	public HashMap<String,String> getTranslations() throws SQLException{
		HashMap<String,String> translations = new HashMap<String,String>() ;

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT tl_lang, tl_text FROM translation WHERE tl_id=" + id) ; 

		try {
			while(rs.next())
				translations.put(rs.getString(1), new String(rs.getBytes(2), "UTF-8")) ;
		} catch (Exception e) {
			e.printStackTrace() ;			
		}

		rs.close() ;
		stmt.close() ;

		return translations ;
	}

	/**
	 * <p>
	 * Calculates a weight of the semantic relation between this article and the argument one. 
	 * The stronger the semantic relation, the higher the weight returned. 
	 * i.e "6678: Cat" has a higher relatedness to "4269567: Dog" than to "27178: Shoe".
	 * This is based on the links extending out from and in to each of the articles being compared. 
	 * </p>
	 * 
	 * <p>
	 * The details of this measure (and an evaluation) is described in the paper:
	 * <br/>
	 * Milne, D and Witten, I.H. (2008) An effective, low-cost measure of semantic relatedness obtained from Wikipedia links. In Proceedings of WIKIAI'08. 
	 * </p>
	 * 
	 * <p>
	 * If you only cache inLinks, then for efficiency's sake relatedness measures will only be calculated from them.
	 * Measures obtained only from inLinks are only marginally less accurate than those obtained from both anyway.
	 * </p>
	 * 
	 * <p>
	 * The reverse is true if you cache only outLinks, although that isnt reccomended. They take up much more memory, and 
	 * resulting measures are not as accurate. 
	 * </p>
	 * 
	 * @param article the other article of interest
	 * @return the weight of the semantic relation between this article and the argument one.
	 * @throws SQLException if there is a problem with the wikipedia database
	 */
	public double getRelatednessTo(Article article) throws SQLException{
		
		if (database.areOutLinksCached() && database.areInLinksCached()) 
			return (getRelatednessFromInLinks(article) + getRelatednessFromOutLinks(article))/2 ;
			
		if (database.areOutLinksCached()) 
			return getRelatednessFromOutLinks(article) ;
		
		if (database.areInLinksCached()) 
			return getRelatednessFromInLinks(article) ;
		
		return (getRelatednessFromInLinks(article) + getRelatednessFromOutLinks(article))/2 ;
	}

	/**
	 * @return an ordered array of article ids that link to this page (with redirects resolved) 
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public int[] getLinksInIds() throws SQLException{

		if (inLinkIds != null)
			return inLinkIds ;

		if (database.areInLinksCached()){
			//if this stuff is cached then we just want to grab it. Dont save it to this.inLinkIds, otherwise we would have duplicate copies in memory

			if (database.cachedInLinks.containsKey(id))
				return database.cachedInLinks.get(id) ;
			else
				return new int[0] ; 
		}

		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT li_data FROM pagelink_in WHERE li_id=" + id) ;

		inLinkIds = new int[0] ;
		
		

		if (rs.first()) {
			String data = rs.getString(1) ;
			if (!data.equals("")) {
				String[] l = data.split(":") ;
				inLinkIds = new int[l.length] ;
				for (int i=0 ; i<l.length ; i++) 
					inLinkIds[i] = new Integer(l[i]).intValue() ;
			}
		}
		
		

		rs.close() ;
		stmt.close() ;

		return inLinkIds ;
	}
	
	/**
	 * @return the number of articles that link to this one 
	 * @throws SQLException
	 */
	public int getLinksInCount() throws SQLException {
		
		int linkCount = 0 ;
		
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT lc_in FROM linkcount WHERE lc_id=" + id) ;
		
		if (rs.first())
			linkCount = rs.getInt(1) ;
		
		rs.close() ;
		stmt.close() ;
		
		return linkCount ;
	}
	
	/**
	 * @return the number of articles that this one links to 
	 * @throws SQLException
	 */
	public int getLinksOutCount() throws SQLException {
		
		int linkCount = 0 ;
		
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT lc_out FROM linkcount WHERE lc_id=" + id) ;
		
		if (rs.first())
			linkCount = rs.getInt(1) ;
		
		rs.close() ;
		stmt.close() ;
		
		return linkCount ;
	}
	
	/**
	 * @return an ordered array of article ids that this page links to (with redirects resolved) 
	 * @throws SQLException
	 */
	public int[] getLinksOutIds() throws SQLException {
		
		int[][] idsAndCounts = getLinksOutIdsAndCounts() ;
		
		int[] idsOnly = new int[idsAndCounts.length] ;
		
		for (int i=0 ; i<idsAndCounts.length ; i++)
			idsOnly[i] = idsAndCounts[i][0] ;

		return idsOnly ;		
	}

	private int[][] getLinksOutIdsAndCounts() throws SQLException {

		if (outLinkIdsAndCounts != null)
			return outLinkIdsAndCounts ;

		if (database.areOutLinksCached()) {
			//if this stuff is cached then we just want to grab it. Dont save it to this.inLinkIds, otherwise we would have duplicate copies in memory
			if (database.cachedOutLinks.containsKey(id))
				return database.cachedOutLinks.get(id) ;
			else
				return new int[0][2] ; 
		}

		String data = "" ;

		Statement stmt = database.createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT lo_data FROM pagelink_out WHERE lo_id = " + id) ;

		if (rs.first()) 
			data = rs.getString(1) ;

		stmt.close() ;
		rs.close();

		String[] values = data.split(";") ;

		outLinkIdsAndCounts = new int[values.length][2] ;

		int index = 0 ;
		for (String value:values) {
			String values2[] = value.split(":") ;
			
			if (values2.length == 2) {
				outLinkIdsAndCounts[index][0] = Integer.parseInt(values2[0]) ;
				outLinkIdsAndCounts[index][1] = Integer.parseInt(values2[1]) ;
			}
			index ++ ;
		}

		return outLinkIdsAndCounts ;
	}

	private double getRelatednessFromOutLinks(Article art) throws SQLException{

		if (getId() == art.getId()) {
			return 1 ;
		}

		int totalArticles = database.getArticleCount() ;
		int[][] dataA = getLinksOutIdsAndCounts() ;
		int[][] dataB = art.getLinksOutIdsAndCounts() ;

		if (dataA.length == 0 || dataB.length == 0)
			return 0 ;

		int indexA = 0 ;
		int indexB = 0 ;

		Vector<Double> vectA = new Vector<Double>() ;
		Vector<Double> vectB = new Vector<Double>() ;

		while (indexA < dataA.length || indexB < dataB.length) {

			int idA = -1 ;
			int idB = -1 ;

			if (indexA < dataA.length)
				idA = dataA[indexA][0] ;

			if (indexB < dataB.length)
				idB = dataB[indexB][0] ;

			if (idA == idB) {
				double probability = Math.log((double)totalArticles/dataA[indexA][1]) ;
				vectA.add(new Double(probability)) ;
				vectB.add(new Double(probability)) ;

				indexA ++ ;
				indexB ++ ;
			} else if ((idA < idB && idA > 0)|| idB < 0) {
				double probability = Math.log((double)totalArticles/dataA[indexA][1]) ;
				vectA.add(new Double(probability)) ;
				vectB.add(new Double(0)) ;

				indexA ++ ;
			} else {
				double probability = Math.log((double)totalArticles/dataB[indexB][1]) ;
				vectA.add(new Double(0)) ;
				vectB.add(new Double(probability)) ;

				indexB ++ ;
			}
		}

		// calculate angle between vectors
		double dotProduct = 0 ;
		double magnitudeA = 0 ;
		double magnitudeB = 0 ;

		for (int x=0;x<vectA.size();x++) {
			double valA = ((Double)vectA.elementAt(x)).doubleValue() ;
			double valB = ((Double)vectB.elementAt(x)).doubleValue() ;

			dotProduct = dotProduct + (valA * valB) ;
			magnitudeA = magnitudeA + (valA * valA) ;
			magnitudeB = magnitudeB + (valB * valB) ;
		}

		magnitudeA = Math.sqrt(magnitudeA) ;
		magnitudeB = Math.sqrt(magnitudeB) ;

		double sr = Math.acos(dotProduct / (magnitudeA * magnitudeB)) ;		
		sr = (Math.PI/2) - sr ; // reverse, so 0=no relation, PI/2= same
		sr = sr / (Math.PI/2) ; // normalize, so measure is between 0 and 1 ;				

		return sr ;
	}

	
	private double getRelatednessFromInLinks(Article article) throws SQLException{

		int[] linksA = this.getLinksInIds() ; 
		int[] linksB = article.getLinksInIds() ; 

		int linksBoth = 0 ;

		int indexA = 0 ;
		int indexB = 0 ;

		while (indexA < linksA.length && indexB < linksB.length) {

			long idA = linksA[indexA] ;
			long idB = linksB[indexB] ;

			if (idA == idB) {
				linksBoth ++ ;
				indexA ++ ;
				indexB ++ ;
			} else if ((idA < idB && idA > 0)|| idB < 0) {
				indexA ++ ;
			} else {				
				indexB ++ ;
			}
		}

		double a = Math.log(linksA.length) ;
		double b = Math.log(linksB.length) ;
		double ab = Math.log(linksBoth) ;
		double m = Math.log(database.getArticleCount()) ;

		double sr = (Math.max(a, b) -ab) / (m - Math.min(a, b)) ;

		if (Double.isNaN(sr) || Double.isInfinite(sr) || sr > 1)
			sr = 1 ;

		sr = 1-sr ;

		return sr ;
	}

	
	/**
	 * Returns a SortedVector of AnchorTexts used to link to this page, in 
	 * descending order of use
	 * 
	 * @return see above.
	 * @throws SQLException if there is a problem with the sql database
	 */
	public SortedVector<AnchorText> getAnchorTexts() throws SQLException {

		SortedVector<AnchorText> anchors = new SortedVector<AnchorText>() ;

		Statement stmt = database.createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT an_text, an_count FROM anchor WHERE an_to = " + id + " ORDER BY an_count DESC") ;

		while (rs.next()) {
			try {
				AnchorText at = new AnchorText(new String(rs.getBytes(1), "UTF-8"), this, rs.getInt(2)) ;
				anchors.add(at, true) ;
			} catch (Exception e) {} ;	
		}

		return anchors ;
	}

	/**
	 * Represents a term or phrase that is used to link to a particular page.
	 */
	public class AnchorText implements Comparable<AnchorText>{

		private String text ;
		private Page destination ;
		private int count ;

		/**
		 * Initializes the AnchorText
		 * 
		 * @param text the text used within the anchor
		 * @param destination the id of the article that this anchor links to
		 * @param count the number of times the given text is used to link to the given destination.
		 */
		public AnchorText(String text, Page destination, int count) {
			this.text = text;
			this.destination = destination ;
			this.count = count ;
		}

		/**
		 * Returns the text used to make the link
		 * 
		 * @return see above.
		 */
		public String getText() {
			return text ;
		}

		/**
		 * Returns the destination page of the link
		 * 
		 * @return see above.
		 */
		public Page getDestination() {
			return destination ;
		}

		/**
		 * Returns the number of times the link is made
		 * 
		 * @return see above.
		 */
		public int getCount() {
			return count ;
		}

		public String toString() {
			return text ;
		}

		/**
		 * Compares this AnchorText to another, so that the AnchorText with the greatest count will be 
		 * considered smaller, and therefore occur earlier in lists.  
		 * 
		 * @param	at	the AnchorText to be compared
		 * @return	see above.
		 */
		public int compareTo(AnchorText at) {
			
			int cmp = -1 * (new Integer(count)).compareTo(new Integer(at.getCount())) ;
			
			if (cmp == 0) 
				cmp = text.compareTo(at.getText()) ;
			
			return cmp ;
		}
	}

	/**
	 * Provides a demo of functionality available to Articles
	 * 
	 * @param args an array of arguments for connecting to a wikipedia database: server and database names at a minimum, and optionally a username and password
	 * @throws Exception if there is a problem with the wikipedia database.
	 */
	public static void main(String[] args) throws Exception {
		
		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;

		BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );	
		DecimalFormat df = new DecimalFormat("0") ;

		while (true) {
			System.out.println("Enter article title (or enter to quit): ") ;
			String title = in.readLine() ;

			if (title == null || title.equals(""))
				break ;

			Article article = wikipedia.getArticleByTitle(title) ;
			
			if (article == null) {
				System.out.println("Could not find exact match. Searching through anchors instead") ;
				article = wikipedia.getMostLikelyArticle(title, null) ; 
			}
			
			if (article == null) {
				System.out.println("Could not find exact article. Try again") ;
			} else {
				System.out.println("\n" + article + "\n") ;

				if (wikipedia.getDatabase().isContentImported()) {
					
					System.out.println(" - first sentence:") ;
					System.out.println("    - " + article.getFirstSentence(null, null)) ;
					
					System.out.println(" - first paragraph:") ;
					System.out.println("    - " + article.getFirstParagraph()) ;
				}
				
				Category eqCategory = article.getEquivalentCategory() ;
				if (eqCategory != null) {
					System.out.println("\n - equivalent category") ;
					System.out.println("    - " + eqCategory) ;
				}
				
				System.out.println("\n - redirects (synonyms or very small related topics that didn't deserve a seperate article):") ;
				for (Redirect r: article.getRedirects())
					System.out.println("    - " + r);
	
				System.out.println("\n - anchors (synonyms and hypernyms):") ;
				for (AnchorText at:article.getAnchorTexts()) 
					System.out.println("    - \"" + at.getText() + "\" (used " + at.getCount() + " times)") ;
	
				System.out.println("\n - parent categories (hypernyms):") ;
				for (Category c: article.getParentCategories()) 
					System.out.println("    - " + c); 
				
				System.out.println("\n - language links (translations):") ;
				HashMap<String,String> translations = article.getTranslations() ;
				for (String lang:translations.keySet())
					System.out.println("    - \"" + translations.get(lang) + "\" (" + lang + ")") ;
				
				System.out.println("\n - pages that this links to (related concepts):") ;
				for (Article a: article.getLinksOut()) {
					System.out.println("    - " + a + " (" + df.format(article.getRelatednessTo(a)*100) + "% related)"); 
				}
			}
			System.out.println("") ;
		}
	
	}
}
