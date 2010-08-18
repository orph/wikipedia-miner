/*
 *    WikipediaDatabase.java
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

import java.io.* ;
import java.sql.*;
import java.util.* ;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gnu.trove.* ;

import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;

/**
 * This class loads, provides access to and maintains the Wikipedia database. It most cases it 
 * should not be used directly - use a Wikipedia instance instead.  
 * <p>
 * <b>NOTE:</b> Each wikipedia database is only intended to store one instance of Wikipedia. If you require more,
 * (for different languages, or from different points in time) then you must create separate databases.
 * 
 * @author David Milne
 */
public class WikipediaDatabase extends MySqlDatabase {
	
	private HashMap<String,String> createStatements  ;
	
	private boolean contentImported = true ;
	private boolean anchorOccurancesSummarized = true ;
	private boolean definitionsSummarized = true ;
	
	private int article_count = 0 ;
	private int category_count = 0 ;
	private int redirect_count = 0 ;
	private int disambig_count = 0 ;
	private int maxPageDepth = 0 ;
	
	protected THashMap<String,CachedAnchor> cachedAnchors = null ;
	protected TIntObjectHashMap<CachedPage> cachedPages = null ;
	protected TIntObjectHashMap<int[]> cachedInLinks = null ;
	protected TIntObjectHashMap<int[][]> cachedOutLinks = null ;
	protected TIntIntHashMap cachedGenerality = null ; 
	protected TIntObjectHashMap<int[]> cachedParentIds = null ;
	
	private TextProcessor cachedProcessor = null ;
		
	/**
	 * Initializes a newly created WikipediaDatabase and attempts to make a connection to the mysql
	 * database defined by the arguments given, as defined by MySqlDatabase. In addition, it will check
	 * that the wikipedia database is complete; that all necessary tables and indexes exist.
	 *
	 * @param	server	the connection string for the server (e.g 130.232.231.053:8080 or bob:8080)
	 * @param	databaseName	the name of the database (e.g <em>enwiki</em>)
	 * @param	userName		the user for the sql database (null if anonymous)
	 * @param	password	the users password (null if anonymous)
	 * @throws	Exception	if a connection cannot be made.
	 */
	public WikipediaDatabase(String server, String databaseName, String userName, String password) throws Exception{
		
		super(server, databaseName, userName, password, "utf8") ;
		
		createStatements = new HashMap<String,String>() ;
		
		createStatements.put("page", "CREATE TABLE page (" 
				+ "page_id int(8) unsigned NOT NULL, "
				+ "page_title varchar(255) binary NOT NULL, "
				+ "page_type int(2) NOT NULL default '0', "
				+ "PRIMARY KEY  (page_id),"
				+ "UNIQUE KEY type_title (page_type,page_title)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("redirect", "CREATE TABLE redirect (" 
				+ "rd_from int(8) unsigned NOT NULL, "
				+ "rd_to int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (rd_from, rd_to),"
				+ "KEY rd_to (rd_to)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		/*
		createStatements.put("pagelink", "CREATE TABLE pagelink (" 
				+ "pl_from int(8) unsigned NOT NULL, "
				+ "pl_to int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (pl_from, pl_to), "
				+ "KEY pl_to (pl_to)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		*/
		
		createStatements.put("categorylink", "CREATE TABLE categorylink (" 
				+ "cl_parent int(8) unsigned NOT NULL, "
				+ "cl_child int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (cl_parent, cl_child), "
				+ "KEY cl_child (cl_child)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;		  
				  
		createStatements.put("translation", "CREATE TABLE translation (" 
				+ "tl_id int(8) unsigned NOT NULL, "
				+ "tl_lang varchar(10) binary NOT NULL, "
				+ "tl_text varchar(255) binary NOT NULL, "
				+ "PRIMARY KEY (tl_id, tl_lang)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
	  
		createStatements.put("disambiguation", "CREATE TABLE disambiguation (" 
				+ "da_from int(8) unsigned NOT NULL, "
				+ "da_to int(8) unsigned NOT NULL, "
				+ "da_index int(3) unsigned NOT NULL, "
				+ "da_scope mediumblob NOT NULL, "
				+ "PRIMARY KEY (da_from, da_to), "
				+ "KEY da_to (da_to)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;	 
		
		createStatements.put("linkcount", "CREATE TABLE linkcount (" 
				+ "lc_id int(8) unsigned NOT NULL, "
				+ "lc_in int(8) unsigned NOT NULL, "
				+ "lc_out int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (lc_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("content", "CREATE TABLE content (" 
				+ "co_id int(8) unsigned NOT NULL, "
				+ "co_content mediumblob NOT NULL, "
				+ "PRIMARY KEY (co_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("pagelink_in", "CREATE TABLE pagelink_in (" 
				+ "li_id int(8) unsigned NOT NULL, "
				+ "li_data mediumblob NOT NULL, "
				+ "PRIMARY KEY (li_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("pagelink_out", "CREATE TABLE pagelink_out (" 
				+ "lo_id int(8) unsigned NOT NULL, "
				+ "lo_data mediumblob NOT NULL, "
				+ "PRIMARY KEY (lo_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
				
		createStatements.put("equivalence", "CREATE TABLE equivalence (" 
				+ "eq_cat int(8) unsigned NOT NULL, "
				+ "eq_art int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (eq_cat), " 
				+ "UNIQUE KEY (eq_art)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("anchor", "CREATE TABLE anchor (" 
				+ "an_text varchar(300) binary NOT NULL, "
				+ "an_to int(8) unsigned NOT NULL, "
				+ "an_count int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (an_text, an_to), " 
				+ "KEY (an_to)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("anchor_occurance", "CREATE TABLE anchor_occurance (" 
				+ "ao_text varchar(300) binary NOT NULL, "
				+ "ao_linkCount int(8) unsigned NOT NULL, "
				+ "ao_occCount int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (ao_text)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("stats", "CREATE TABLE stats (" 
						+ "st_articles int(8) unsigned NOT NULL, "
						+ "st_categories int(8) unsigned NOT NULL, "
						+ "st_redirects int(8) unsigned NOT NULL, "
						+ "st_disambigs int(8) unsigned NOT NULL) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
		
		createStatements.put("generality", "CREATE TABLE generality (" 
				+ "gn_id int(8) unsigned NOT NULL, "
				+ "gn_depth int(2) unsigned NOT NULL, "
				+ "PRIMARY KEY (gn_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ; 
		
		try {
			checkDatabase() ;	
			setStats() ;
		} catch (SQLException e) {
			System.out.println("WARNING: wikipedia database is incomplete.") ;
			e.printStackTrace() ;
		}
	}
	
	private void setStats() throws SQLException {
		Statement stmt = createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT * FROM stats") ;
		
		if (rs.first()) {
			this.article_count = rs.getInt(1) ;
			this.category_count = rs.getInt(2) ;
			this.redirect_count = rs.getInt(3) ;
			this.disambig_count = rs.getInt(4) ;
		}
		
		rs.close() ;
		stmt.close() ;		
	}
	
	/**
	 * Prepares the database so that it can be efficiently searched with the given text processor 
	 *
	 * @param tp the text processor to prepare this database for
	 * @throws SQLException if there is a problem with the Wikipedia database
	 */
	public void prepareForTextProcessor(TextProcessor tp) throws SQLException{
		prepareAnchorsForTextProcessor(tp) ;
		
		if (tableExists("anchor_occurance"))
			prepareAnchorOccurancesForTextProcessor(tp) ;
	}
	
	private void prepareAnchorsForTextProcessor(TextProcessor tp) throws SQLException {
		
		System.out.println("Preparing anchors for " + tp.getName()) ;
		String tableName = "anchor_" + tp.getName() ;
		
		ProgressNotifier pn = new ProgressNotifier(2) ;
		int rows = this.getRowCountExact("anchor") ;
		pn.startTask(rows, "Gathering and processing anchors") ;
		
		Statement stmt = createStatement() ;
		stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName) ;
		stmt.close() ;
		
		stmt = createStatement() ;
		stmt.executeUpdate("CREATE TABLE " + tableName + " (" 
						+ "an_text varchar(500) character set latin1 collate latin1_bin NOT NULL, "
						+ "an_to int(8) unsigned NOT NULL, "
						+ "an_count int(8) unsigned NOT NULL, "
						+ "PRIMARY KEY (an_text, an_to), " 
						+ "KEY (an_to)) ; ") ;
		stmt.close() ;
		
		HashMap<String, Integer> anchorCounts = new HashMap<String, Integer>() ;
		
		
			
		int currRow = 0 ;
		
		int chunkIndex = 0 ;
		int chunkSize = 100000 ;
						
		while (chunkIndex * chunkSize < rows) {
			
			//if (chunkIndex > 10) break ;
			stmt = createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT * FROM anchor LIMIT " + chunkIndex * chunkSize + ", " + chunkSize ) ;
			
			while (rs.next()) {
				try {
					
					String an_text = new String(rs.getBytes(1), "UTF-8") ;
					long an_to = rs.getLong(2) ;
					int an_count = rs.getInt(3) ;
					
					an_text = tp.processText(an_text) ;
					
					//System.out.println(an_text + "," + an_to + "," + an_count) ;
					
					Integer count = anchorCounts.get(an_text + ":" + an_to) ;
					
					if (count == null)
						anchorCounts.put(an_text + ":" + an_to, an_count) ;
					else
						anchorCounts.put(an_text + ":" + an_to, count + an_count) ;
				
				} catch (Exception e) {e.printStackTrace() ;} ;
				
				currRow++ ;
			}
			
			rs.close() ;
			stmt.close() ;
			
			chunkIndex++ ;
			pn.update(currRow) ;
		}
		
		rows = anchorCounts.size() ;
		pn.startTask(rows, "Saving processed anchors") ;
		
		chunkIndex = 0 ;
		
		currRow = 0 ;
		
		StringBuffer insertQuery = new StringBuffer() ; ;
		
		for(String key:anchorCounts.keySet()) {
			currRow ++ ;
			
			int pos = key.lastIndexOf(':') ;
			
			String an_text = key.substring(0, pos) ;
			long an_to = new Long(key.substring(pos+1)).longValue() ;
			int an_count = anchorCounts.get(key) ;
			
			if (an_text != "") 
				insertQuery.append(" (\"" + addEscapes(an_text) + "\"," + an_to + "," + an_count + "),") ;
			
			if (currRow%chunkSize == 0) {
				if (insertQuery.length() > 0) {
					insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
					
					stmt = createStatement() ;
					stmt.setEscapeProcessing(false) ;
					stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
					stmt.close() ;
					
					insertQuery = new StringBuffer() ;
				}
				pn.update(currRow) ;
			}
		}
		
		if (insertQuery.length() > 0) {
			insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
						
			stmt = createStatement() ;
			stmt.setEscapeProcessing(false) ;
			stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
			stmt.close() ;
		}
	}
	
	private void prepareAnchorOccurancesForTextProcessor(TextProcessor tp) throws SQLException {
		
		System.out.println("Preparing anchor occurances for " + tp.getName()) ;
		String tableName = "anchor_occurance_" + tp.getName() ;
		
		int rows = this.getRowCountExact("anchor") ;
		ProgressNotifier pn = new ProgressNotifier(2) ;
		pn.startTask(rows, "Gathering and processing anchor occurances") ;
		
		Statement stmt = createStatement() ;
		stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName) ;
		stmt.close() ;
		
		stmt = createStatement() ;
		stmt.executeUpdate("CREATE TABLE " + tableName + " (" 
				+ "ao_text varchar(500) character set latin1 collate latin1_bin NOT NULL, "
				+ "ao_linkCount int(8) unsigned NOT NULL, "				
				+ "ao_occCount int(8) unsigned NOT NULL, "
				+ "PRIMARY KEY (ao_text)) ; ") ;
					
		stmt.close() ;
		
		HashMap<String, Integer[]> occuranceStats = new HashMap<String, Integer[]>() ;
		
		int currRow = 0 ;
		
		int chunkIndex = 0 ;
		int chunkSize = 100000 ;
						
		while (chunkIndex * chunkSize < rows) {
			
			//if (chunkIndex > 10) break ;
			stmt = createStatement() ;
			ResultSet rs = stmt.executeQuery("SELECT * FROM anchor_occurance LIMIT " + chunkIndex * chunkSize + ", " + chunkSize ) ;
			
			while (rs.next()) {
				try {
					
					String ao_text = new String(rs.getBytes(1), "UTF-8") ;
					int ao_linkCount = rs.getInt(2) ;
					int ao_occCount = rs.getInt(3) ;
					
					ao_text = tp.processText(ao_text) ;					
					Integer[] stats = occuranceStats.get(ao_text) ;
					
					if (stats != null) {
						stats[0] = stats[0] + ao_linkCount ;
						stats[1] = stats[1] + ao_occCount ;
					} else {
						stats = new Integer[2] ;
						stats[0] = ao_linkCount ;
						stats[1] = ao_occCount ;
					}
					
					occuranceStats.put(ao_text, stats) ;
				
				} catch (Exception e) {e.printStackTrace() ;} ;
				
				currRow++ ;
			}
			
			rs.close() ;
			stmt.close() ;
			
			chunkIndex++ ;
			pn.update(currRow) ;
		}
		
		rows = occuranceStats.size() ;
		pn.startTask(rows, "Saving processed anchor occurances") ;
		
		chunkIndex = 0 ;
		
		currRow = 0 ;
		
		StringBuffer insertQuery = new StringBuffer() ; ;
		
		for(String anchor:occuranceStats.keySet()) {
			currRow ++ ;
			
			Integer[] stats = occuranceStats.get(anchor) ;
						
			if (anchor != "") 
				insertQuery.append(" (\"" + addEscapes(anchor) + "\"," + stats[0] + "," + stats[1] + "),") ;
			
			if (currRow%chunkSize == 0) {
				if (insertQuery.length() > 0) {
					insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
					
					stmt = createStatement() ;
					stmt.setEscapeProcessing(false) ;
					stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
					stmt.close() ;
					
					insertQuery = new StringBuffer() ;
				}
				
				pn.update(currRow) ;
			}
		}
		
		if (insertQuery.length() > 0) {
			insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
						
			stmt = createStatement() ;
			stmt.setEscapeProcessing(false) ;
			stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
			stmt.close() ;
		}
	}	
	
	/**
	 * Loads a directory of summarized csv tables into the database. If overwrite is set to true, then 
	 * all previously stored data is overwritten. 
	 * 
	 * If any core files ('page', 'pagelink', 'categorylink', 'redirect', 'disambiguation', 'translation', 'pagelink', 'stats') 
	 * are missing or unreadable, then this method will fail without making any modifications to the stored data, regardless of  
	 * <em>overwrite</em>. If any optional files are found (e.g. 'content', or 'ngram') then these will be loaded as well.
	 * 
	 * 
	 * @param directory the directory in which the data files are located
	 * @param overwrite true if existing tables are to be overwritten
	 * @throws SQLException if there is a problem with the database
	 * @throws IOException if there is a problem with the files to be loaded.
	 */
	public void loadData(File directory, boolean overwrite) throws SQLException, IOException {
		
		//check that all manditory files exist
		
		File page = new File(directory.getPath() + File.separatorChar + "page.csv") ;
		checkFile(page) ;
		
		File redirect = new File(directory.getPath() + File.separatorChar + "redirect.csv") ;
		checkFile(redirect) ;
		
		File disambig = new File(directory.getPath() + File.separatorChar + "disambiguation.csv") ;
		checkFile(disambig) ;
		
		File translation = new File(directory.getPath() + File.separatorChar + "translation.csv") ;
		checkFile(translation) ;
		
		File catlink = new File(directory.getPath() + File.separatorChar + "categorylink.csv") ;
		checkFile(catlink) ;
		
		/*
		File pagelinkFile = new File(directory.getPath() + File.separatorChar + "pagelink.csv") ;
		if (!pagelinkFile.canRead())
			throw new IOException(pagelinkFile.getPath() + " cannot be read") ;
		*/
		
		File linkcount = new File(directory.getPath() + File.separatorChar + "linkcount.csv") ;
		checkFile(linkcount) ; 
		
		File anchor = new File(directory.getPath() + File.separatorChar + "anchor.csv") ;
		checkFile(anchor) ;
		
		File stats = new File(directory.getPath() + File.separatorChar + "stats.csv") ;
		checkFile(stats) ;
		
		File generality = new File(directory.getPath() + File.separatorChar + "generality.csv") ;
		checkFile(generality) ;
		
		File equivalence = new File(directory.getPath() + File.separatorChar + "equivalence.csv") ;
		checkFile(equivalence) ;
		
		File pagelink_in = new File(directory.getPath() + File.separatorChar + "pagelink_in.csv") ;
		checkFile(pagelink_in) ;
		
		File pagelink_out = new File(directory.getPath() + File.separatorChar + "pagelink_out.csv") ;
		checkFile(pagelink_out) ;
		
		// load manditory tables 
		
		if (overwrite || !tableExists("page")) {
			initializeTable("page") ;
			loadFile(page, "page") ;
		}
		
		if (overwrite || !tableExists("redirect")) {
			initializeTable("redirect") ;
			loadFile(redirect, "redirect") ;
		}
		
		if (overwrite || !tableExists("disambiguation")) {
			initializeTable("disambiguation") ;
			loadFile(disambig, "disambiguation") ;
		}
		
		if (overwrite || !tableExists("translation")) {
			initializeTable("translation") ;
			loadFile(translation, "translation") ;
		}
		
		if (overwrite || !tableExists("categorylink")) {
			initializeTable("categorylink") ;
			loadFile(catlink, "categorylink") ;
		}
		
		/*
		if (overwrite || !tableExists("pagelink")) {
			initializeTable("pagelink") ;
			loadFile(pagelinkFile, "pagelink") ;
		}
		*/
		 
		if (overwrite || !tableExists("linkcount")) {
			initializeTable("linkcount") ;
			loadFile(linkcount, "linkcount") ;
		}
		
		if (overwrite || !tableExists("anchor")) {
			initializeTable("anchor") ;
			loadFile(anchor, "anchor") ;
		}
		
		if (overwrite || !tableExists("stats")) {
			initializeTable("stats") ;
			loadFile(stats, "stats") ;
		}
		
		if (overwrite || !tableExists("generality")) {
			initializeTable("generality") ;
			loadFile(generality, "generality") ;
		}
		
		if (overwrite || !tableExists("equivalence")) {
			initializeTable("equivalence") ;
			loadFile(equivalence, "equivalence") ;
		}
		
		if (overwrite || !tableExists("pagelink_in")) {
			initializeTable("pagelink_in") ;
			loadFile(pagelink_in, "pagelink_in") ;
		}
		
		if (overwrite || !tableExists("pagelink_out")) {
			initializeTable("pagelink_out") ;
			loadFile(pagelink_out, "pagelink_out") ;
		}
				
		if (overwrite || !tableExists("content")) {
			File contentFile = new File(directory.getPath() + File.separatorChar + "content.csv") ;
			
			if (contentFile.canRead()) {
				initializeTable("content") ;
				loadFile(contentFile, "content") ;
			}
		}
		
		if (overwrite || !tableExists("anchor_occurance")) {
			File occFile = new File(directory.getPath() + File.separatorChar + "anchor_occurance.csv") ;
			
			if (occFile.canRead()) {
				initializeTable("anchor_occurance") ;
				loadFile(occFile, "anchor_occurance") ;
			}
		}	
	}
	
	private void checkFile(File file) throws IOException {
		if (!file.canRead())
			throw new IOException(file.getPath() + " cannot be read") ;
	}
		
	private void loadFile(File file, String tableName) throws IOException, SQLException{
		
		long bytes = file.length() ; 
		ProgressNotifier pn = new ProgressNotifier(bytes, "Loading " + tableName) ;
				
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")) ;
		String line ;
		
		long bytesRead = 0 ;		
		long chunkSize = bytes/100 ;
				
		StringBuffer insertQuery = new StringBuffer() ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
				
			insertQuery.append("(" + line + "),") ; 
			
			if (insertQuery.length() > chunkSize) {
				if (insertQuery.length() > 0) {
					insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
					
					try {
						Statement stmt = createStatement() ;
						stmt.setEscapeProcessing(false) ;
						stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
						stmt.close() ;
					} catch (SQLException e) {
						System.out.println(insertQuery) ;
						throw(e) ;
					}
					
					insertQuery = new StringBuffer() ;
				}
				pn.update(bytesRead) ;
			}
		}
		
		input.close() ;
		
		if (insertQuery.length() > 0) {
			insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
			
			Statement stmt = createStatement() ;
			stmt.setEscapeProcessing(false) ;
			stmt.executeUpdate("INSERT IGNORE INTO " + tableName + " VALUES" + insertQuery.toString() ) ;
			stmt.close() ;
		}
		
		pn.update(bytes) ;
	}
	
	private int getLineCount(File file) throws IOException{
		int count = 0 ;
		
		BufferedReader input = new BufferedReader(new FileReader(file)) ;

		while (input.readLine() != null) 
			count ++ ;
		
		input.close();
		
		return count ;		
	}
	
	private void initializeTable(String tableName) throws SQLException {
		
		Statement stmt ;
		
		if (tableExists(tableName)) {
			stmt = createStatement() ;
			stmt.executeUpdate("TRUNCATE TABLE " + tableName) ;
			stmt.close() ;
			
			stmt = createStatement() ;
			stmt.executeUpdate("DROP TABLE " + tableName) ;
			stmt.close() ;
		}
		
		stmt = createStatement() ;
		stmt.executeUpdate(createStatements.get(tableName)) ;
		stmt.close() ;
	}
	
	
	/**
	 * Checks that this database contains all core tables.
	 * 
	 * @throws SQLException if it doesnt.
	 */
	public void checkDatabase() throws SQLException {
		
		if (!tableExists("page"))
			throw new SQLException("mysql table 'page' does not exist and must be imported.") ;
		
		if (!tableExists("categorylink"))
			throw new SQLException("mysql table 'categorylink' does not exist and must be imported.") ;
				
		if (!tableExists("redirect"))
			throw new SQLException("mysql table 'redirect' does not exist and must be imported.") ;
		
		if (!tableExists("translation"))
			throw new SQLException("mysql table 'translation' does not exist and must be imported.") ;
		
		if (!tableExists("disambiguation"))
			throw new SQLException("mysql table 'disambiguation' does not exist and must be imported.") ;
		
		if (!tableExists("stats"))
			throw new SQLException("mysql table 'stats' does not exist and must be imported.") ;
		
		if (!tableExists("pagelink_in"))
			throw new SQLException("mysql table 'pagelink_in' does not exist and must be imported.") ;
		
		if (!tableExists("pagelink_out"))
			throw new SQLException("mysql table 'pagelink_out' does not exist and must be imported.") ;
		
		if (!tableExists("equivalence"))
			throw new SQLException("mysql table 'equivalence' does not exist and must be imported.") ;
		
		if (!tableExists("generality"))
			throw new SQLException("mysql table 'generality' does not exist and must be imported.") ;
		
		if (!tableExists("content")){
			contentImported = false ;
			System.err.println("WARNING: page content has not been imported. You will only be able to retrieve the structure of wikipedia, not it's content.") ;
		}
		
		if (!tableExists("anchor_occurance")){
			anchorOccurancesSummarized = false ;
			System.err.println("WARNING: anchor occurances have not been imported. You will not be able to calculate how often anchor terms occur as plain text.") ;
		}
		
		if (!tableExists("definition")){
			definitionsSummarized = false ;
			System.err.println("WARNING: definitions have not been summarized. Obtaining the first sentence and first paragraph of pages will be more expensive than it needs to be. ") ;
		}
	}
	
	/**
	 * Checks if the database has been prepared for use with a particular morphological processor
	 * 
	 * @param TextProcessor the TextProcessor to be checked.
	 * @throws SQLException if the data has not been prepared for this TextProcessor.
	 */
	public void checkTextProcessor(TextProcessor TextProcessor) throws SQLException {
			
		if (!tableExists("anchor_" + TextProcessor.getName()))
			throw new SQLException("anchors have not been prepared for the morphological processor \"" + TextProcessor.getName() + "\"") ;
		
		if (!tableExists("ngram")) {
			if (!tableExists("ngram_" + TextProcessor.getName()))
				throw new SQLException("ngrams have not been prepared for the morphological processor \"" + TextProcessor.getName() + "\"") ;
		}
	}

	/**
	 * @return true if the database contains the content markup of pages, otherwise false.
	 */
	public boolean isContentImported() {
		return contentImported;
	}
	
	/**
	 * @return true if the counts of when terms occur as anchors and as plain text have been calculated and imported, otherwise false.
	 */
	public boolean areAnchorOccurancesSummarized() {
		return anchorOccurancesSummarized;
	}
	
	/**
	 * @return true if the first paragraphs and first sections of pages have been summarised, otherwise false.
	 */
	public boolean areDefinitionsSummarized() {
		return definitionsSummarized;
	}
	
	/**
	 * @return the exact number of articles (not redirects or disambiguations) stored in the database.
	 */
	public int getArticleCount() {
		return article_count;
	}

	/**
	 * @return the exact number of categories stored in the database.
	 */
	public int getCategoryCount() {
		return category_count;
	}

	/**
	 * @return the exact number of disambiguations stored in the database.
	 */
	public int getDisambigCount() {
		return disambig_count;
	}

	/**
	 * @return the exact number of redirects stored in the database.
	 */
	public int getRedirectCount() {
		return redirect_count;
	}
	
	/**
	 * @return the total number of pages stored in the database.
	 */
	public int getPageCount() {
		return article_count + category_count + disambig_count + redirect_count ;
	}
	
	/**
	 * @return the maximum path length from any article to the root "Fundamental" category. 
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public int getMaxPageDepth() throws SQLException{
		if (maxPageDepth > 0)
			return maxPageDepth ;
			
		Statement stmt = createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT MAX(gn_depth) FROM generality") ;
		
		if (rs.first()) {
			try {
				maxPageDepth = rs.getInt(1) ; 
			} catch (Exception e) {} ;
		}
		
		rs.close() ;
		stmt.close() ;
		
		return maxPageDepth ;
	}
	
	/**
	 * Caches anchors, destinations, and occurrence counts (if these have been summarized), so that they can 
	 * be searched very quickly without consulting the database.
	 * 
	 * @param dir	the directory containing csv files extracted from a Wikipedia dump.
	 * @param tp	an optional text processor
	 * @param validIds an optional set of ids. Only anchors that point to these ids, and only destinations within this list will be cached.
	 * @param minLinkCount the minimum number of times a destination must occur for a particular anchor before it is cached. 
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cacheAnchors(File dir, TextProcessor tp, TIntHashSet validIds, int minLinkCount, ProgressNotifier pn) throws IOException{
	
		File anchorFile = new File(dir.getPath() + File.separatorChar + "anchor_summary.csv") ;
		File occuranceFile = new File(dir.getPath() + File.separatorChar + "anchor_occurance.csv") ;
		
		boolean cachingOccurances = occuranceFile.canRead() ;
		
		cachedAnchors = new THashMap<String,CachedAnchor>() ;
		
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(anchorFile), "UTF-8")) ;
		
		if (pn == null) pn = new ProgressNotifier(1) ;
		if (cachingOccurances)
			pn.startTask(anchorFile.length() + occuranceFile.length(), "caching anchors") ;
		else
			pn.startTask(anchorFile.length(), "caching anchors") ;
				
		
		long bytesRead = 0 ;
		String line ;
						
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
						
			int sep = line.lastIndexOf(',') ;
			String anchor = line.substring(1, sep-1) ;
			String data = line.substring(sep+2, line.length()-1) ;
			
			//System.out.println(anchor + " -> " + data) ;
			
			String[] temp = data.split(";") ;
			
			Vector<int[]> senses = new Vector<int[]>() ;

			for (String t:temp) {
				String[] values = t.split(":") ;
				int[] sense = new int[2] ;
								
				sense[0] = new Integer(values[0]) ;
				sense[1] = new Integer(values[1]) ;
				
				if ((validIds == null || validIds.contains(sense[0])) && sense[1] > minLinkCount) {
					senses.add(sense) ;
				}
			}
			
			if (senses.size() > 0) {
				if (tp != null) 
					anchor = tp.processText(anchor) ;
				
				CachedAnchor ca = cachedAnchors.get(anchor) ;
				if (ca == null) {
					ca = new CachedAnchor(senses) ;
					cachedAnchors.put(anchor, ca) ;
				} else {			
					ca.addSenses(senses) ;
				}
			}
			pn.update(bytesRead) ;
		}
		
		input.close() ;
		
		if (occuranceFile.canRead()) {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(occuranceFile), "UTF-8")) ;
							
			while ((line=input.readLine()) != null) {
				bytesRead = bytesRead + line.length() ;
				
				int sep2 = line.lastIndexOf(',') ;
				int sep1 = line.lastIndexOf(',', sep2-1) ;
				
				String ngram = line.substring(1, sep1-1) ;
				//int linkCount = new Integer(line.substring(sep1+1, sep2)) ;
				int occCount = new Integer(line.substring(sep2+1)) ;
				
				if (tp != null) 
					ngram = tp.processText(ngram) ;
				
				// if we are doing morphological processing, then we need to resolve collisions
				CachedAnchor ca = cachedAnchors.get(ngram) ;
				if (ca != null) 
					ca.addOccCount(occCount) ;
				
				pn.update(bytesRead) ;
			}
			input.close();
		}
		this.cachedProcessor = tp ;
	}

	/**
	 * Caches pages, so that titles and types can be retrieved 
	 * very quickly without consulting the database.
	 * 
	 * @param dir	the directory containing csv files extracted from a Wikipedia dump.
	 * @param validIds an optional set of ids. Only anchors that point to these ids, and only destinations within this list will be cached. 
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cachePages(File dir, TIntHashSet validIds, ProgressNotifier pn) throws IOException {
		
		File pageFile = new File(dir.getPath() + File.separatorChar + "page.csv") ;
		
		if (validIds == null)
			cachedPages = new TIntObjectHashMap<CachedPage>(getPageCount(), 1) ;
		else
			cachedPages = new TIntObjectHashMap<CachedPage>(validIds.size(), 1) ;
		
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(pageFile), "UTF-8")) ;
		
		if (pn == null) pn = new ProgressNotifier(1) ;
		pn.startTask(pageFile.length(), "caching pages") ;
		
		long bytesRead = 0 ;
		String line ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
			
			int sep1 = line.indexOf(',') ;
			int sep2 = line.lastIndexOf(',') ;
			
			int id = new Integer(line.substring(0, sep1)) ;
			
			if (validIds == null || validIds.contains(id)) {
				String title = line.substring(sep1+2, sep2-1) ;
				int type = new Integer(line.substring(sep2+1)) ;
						
				CachedPage p = new CachedPage(title, type) ;
				cachedPages.put(id, p) ;
			}
			pn.update(bytesRead) ;
		}
		input.close();
	}
	
	/**
	 * Caches links in to pages, so these and relatedness measures can be calculated very quickly,
	 * without consulting the database.
	 * 
	 * @param dir	the directory containing csv files extracted from a Wikipedia dump.
	 * @param validIds an optional set of ids. Only anchors that point to these ids, and only destinations within this list will be cached. 
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cacheInLinks(File dir, TIntHashSet validIds, ProgressNotifier pn) throws IOException {
		
		File file = new File(dir.getPath() + File.separatorChar + "pagelink_in.csv") ;		
		
		if (validIds == null)
			cachedInLinks = new TIntObjectHashMap<int[]>(getLineCount(file), 1) ;
		else
			cachedInLinks = new TIntObjectHashMap<int[]>(validIds.size(), 1) ;
		
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")) ;
		
		if (pn == null) pn = new ProgressNotifier(1) ;
		pn.startTask(file.length(), "caching links into pages") ;
		
		long bytesRead = 0 ;
		String line ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
						
			int pos = line.indexOf(',') ;
			int id = new Integer(line.substring(0, pos)) ;
			
			if (validIds == null || validIds.contains(id)) {
				String data = line.substring(pos+2, line.length()-1) ;
				
				String[] temp = data.split(":") ;
				int[] links = new int[temp.length] ;
				
				int i = 0 ;
				for (String t:temp) {
					if (!t.equals("")) {
						links[i] = new Integer(t) ;
						i++ ;
					}
				}
				
				cachedInLinks.put(id, links) ;
			}

			pn.update(bytesRead) ;
		}
		input.close();
	}
	
	/**
	 * Caches links out from pages, so these and relatedness measures can be calculated very quickly,
	 * without consulting the database.
	 * 
	 * @param dir	the directory containing csv files extracted from a Wikipedia dump.
	 * @param validIds an optional set of ids. Only anchors that point to these ids, and only destinations within this list will be cached. 
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cacheOutLinks(File dir, TIntHashSet validIds, ProgressNotifier pn) throws IOException{
		
		File file = new File(dir.getPath() + File.separatorChar + "pagelink_out.csv") ;
		
		if (validIds == null)
			cachedOutLinks = new TIntObjectHashMap<int[][]>(getLineCount(file), 1) ;
		else
			cachedOutLinks = new TIntObjectHashMap<int[][]>(validIds.size(), 1) ;
			
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")) ;
		
		if (pn == null) pn = new ProgressNotifier(1) ;
		pn.startTask(file.length(), "caching links out from pages") ;
		
		long bytesRead = 0 ;
		String line ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
						
			int pos = line.indexOf(',') ;
			int id = new Integer(line.substring(0, pos)) ;
			
			if (validIds == null || validIds.contains(id)) {
				String data = line.substring(pos+2, line.length()-1) ;
				String[] temp = data.split(";") ;
	
				int[][] links = new int[temp.length][2] ;
	
				int i = 0 ;
				for (String t:temp) {
					String[] values = t.split(":") ;
					links[i][0] = new Integer(values[0]) ;
					links[i][1] = new Integer(values[1]) ;
	
					i++ ;
				}
				cachedOutLinks.put(id, links) ;
			}
			
			pn.update(bytesRead) ;
		}
	}
	
	
	/**
	 * Caches generality, so measures can be retrieved very quickly,
	 * without consulting the database.
	 * 
	 * @param dir	the directory containing csv files extracted from a Wikipedia dump.
	 * @param validIds an optional set of ids. Only anchors that point to these ids, and only destinations within this list will be cached. 
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cacheGenerality(File dir, TIntHashSet validIds, ProgressNotifier pn) throws IOException {
		

		File pageFile = new File(dir.getPath() + File.separatorChar + "generality.csv") ;		

		if (validIds == null)
			cachedGenerality = new TIntIntHashMap(getPageCount() , 1) ;
		else
			cachedGenerality = new TIntIntHashMap(validIds.size(), 1) ;

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(pageFile), "UTF-8")) ;
		
		if (pn == null) pn = new ProgressNotifier(1) ;
		pn.startTask(pageFile.length(), "caching generality") ;
		
		long bytesRead = 0 ;
		String line ;
		
		int maxDepth = 0 ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
						
			int sep = line.indexOf(',') ;			
			int id = new Integer(line.substring(0, sep)) ;
			
			if (validIds==null || validIds.contains(id)){
				int depth = new Integer(line.substring(sep+1).trim()) ;
				cachedGenerality.put(id, depth) ;
				if (depth > maxDepth) maxDepth = depth ;	
			}

			pn.update(bytesRead) ;
		}
		
		this.maxPageDepth = maxDepth ;
		input.close();
	}
	
	/**
	 * Caches parent categories of articles and categories, so that they can be retrieved and 
	 * traversed very quickly without consulting the database.
	 * 
	 * @param dataDirectory the directory containing csv files extracted from a Wikipedia dump.
	 * @param pn an optional progress notifier
	 * @throws IOException if the relevant files cannot be read.
	 */
	public void cacheParentIds(File dataDirectory, ProgressNotifier pn) throws IOException{
		//TODO: make this work for a set of article ids
		cachedParentIds = new TIntObjectHashMap<int[]> () ;
		
		File categoryFile = new File(dataDirectory.getPath() + File.separatorChar + "categorylink.csv") ;	
		
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(categoryFile), "UTF-8")) ;
		
		if (pn== null) pn = new ProgressNotifier(1) ;
		pn.startTask(categoryFile.length(), "caching parents") ;
		
		long bytesRead = 0 ;
		String line ;
		Pattern p = Pattern.compile("^(\\d*),(\\d*)$") ;
		
		int lastChild = -1 ;
		Vector<Integer> parents = new Vector<Integer>() ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
			
			Matcher m = p.matcher(line) ;
			if (m.matches()) {
				int parentId = new Integer(m.group(1)) ;
				int childId = new Integer(m.group(2)) ;
				
				if (childId == lastChild)
					parents.add(parentId) ;
				else {
					if (!parents.isEmpty()) {
						int[] pArray = new int[parents.size()] ;
						
						int i=0 ;
						for (int pId:parents) {
							pArray[i] = pId ;
							i++ ;
						}
						
						cachedParentIds.put(lastChild, pArray) ;
					}

					lastChild = childId ;
					parents = new Vector<Integer>() ;
					parents.add(parentId) ;
				}
			}
			
			pn.update(bytesRead) ;
		}
		
		if (!parents.isEmpty()) {
			int[] pArray = new int[parents.size()] ;
			
			int i=0 ;
			for(int pId :parents) {
				pArray[i] = pId ;
				i++ ;				
			}
			
			cachedParentIds.put(lastChild, pArray) ;
		}
		
		input.close();
	}
	
	
	/**
	 * @return true if parent category ids are cached, otherwise false
	 */
	public boolean areParentIdsCached() {
		return !(cachedParentIds == null) ;
	}
	
	/**
	 * @return true if page titles and types are cached, otherwise false
	 */
	public boolean arePagesCached() {
		return !(cachedPages == null) ;
	}
	
	/**
	 * @param tp an optional textProcessor
	 * @return true if anchors and their destinations are cached according to the given textProcessor, otherwise false
	 */
	public boolean areAnchorsCached(TextProcessor tp) {
		
		if (cachedAnchors == null)
			return false ;
		
		String nameA = "null" ;
		if (cachedProcessor != null) nameA = cachedProcessor.getName() ;
		
		String nameB = "null" ;
		if (tp != null) nameB = tp.getName() ;
		
		if (!nameA.equals(nameB))
			return false ;
		
		return true ;
	}
	
	/**
	 * Identifies the set of valid article ids which fit the given constrains. 
	 * 
	 * @param dir the directory containing csv files extracted from a Wikipedia dump.
	 * @param minLinkCount the minimum number of links that an article must have (both in and out)
	 * @param pn an optional progress notifier
	 * @return the set of valid ids which fit the given constrains. 
	 * @throws IOException if the relevant files cannot be read.
	 */
	public TIntHashSet getValidPageIds(File dir, int minLinkCount, ProgressNotifier pn) throws IOException{
		
		File linkCountFile = new File(dir.getPath() + File.separatorChar + "linkcount.csv") ;		
		File pageFile = new File(dir.getPath() + File.separatorChar + "page.csv") ;
		
		TIntHashSet pageIds = new TIntHashSet() ;
				
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(linkCountFile), "UTF-8")) ;
		if (pn == null) pn = new ProgressNotifier(1) ;
		pn.startTask(linkCountFile.length() + pageFile.length(), "gathering valid page ids") ;
		
		long bytesRead = 0 ;
		String line ;
		
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
			
			String[] vals = line.split(",") ;
						
			int id = new Integer(vals[0]) ;
			int lc_in = new Integer(vals[1]) ;
			int lc_out = new Integer(vals[2]) ;
			
			if (lc_in >= minLinkCount && lc_out >= minLinkCount)
				pageIds.add(id) ;
			
			pn.update(bytesRead) ;
		}
		
		input.close();
		
		
		input = new BufferedReader(new InputStreamReader(new FileInputStream(pageFile), "UTF-8")) ;
		Pattern p = Pattern.compile("(\\d*),.*?,(\\d*)");	
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() ;
			
			Matcher m = p.matcher(line) ;
			if (m.matches()) {
				int id = Integer.parseInt(m.group(1)) ;
				int type = Integer.parseInt(m.group(2)) ;

				if (type == Page.CATEGORY)
					pageIds.add(id) ;
			}	
			pn.update(bytesRead) ;
		}
		input.close();
		
		return pageIds ;
	}
	
	/**
	 * @return true if links out from pages are cached, otherwise false.
	 */
	public boolean areOutLinksCached() {
		return !(cachedOutLinks == null) ;
	}
	
	/**
	 * @return true if links in from pages are cached, otherwise false.
	 */
	public boolean areInLinksCached() {
		return !(cachedInLinks == null) ;
	}
	
	
	/**
	 * @return true if generality measures are cached, otherwise false.
	 */
	public boolean isGeneralityCached() {
		return !(cachedGenerality == null) ;
	}
	
	protected class CachedPage {
		String title ;
		int type ;
		
		public CachedPage(String title, int type) {
			this.title = title ;
			this.type = type ;
		}		
	}
	
	protected class CachedAnchor {
		int linkCount ;
		int occCount ;
		int[][] senses ;
				
		public CachedAnchor(Vector<int[]> senses) {
			this.occCount = -1 ;  //flag this as unavailable for now
			this.linkCount = 0 ;
			
			this.senses = new int[senses.size()][2] ;
			
			int i = 0 ;
			for (int[] sense:senses) {
				this.senses[i] = sense ;
				linkCount = linkCount + sense[1] ;
				i++ ;
			}
		}
		
		public void addOccCount(int occCount) {
			
			if (this.occCount < 0)
				this.occCount = occCount ;
			else
				this.occCount += occCount ;
		}
		
		public void addSenses(Vector<int[]> senses) {
			
			if (this.senses == null) {
				this.linkCount = 0 ;
				this.senses = new int[senses.size()][2] ;
				
				int i = 0 ;
				for (int[] sense:senses) {
					this.senses[i] = sense ;
					linkCount = linkCount + sense[1] ;
					i++ ;
				}
				return ;
			}
						
			// merge the senses
			HashMap<Integer,Integer> senseCounts = new HashMap<Integer,Integer>() ;
			
			for (int[] sense: this.senses) {
				senseCounts.put(sense[0], sense[1]) ;			
			}
			
			for (int[] sense: senses) {
				Integer count = senseCounts.get(sense[0]) ;
				
				if (count == null)
					count = sense[1] ;
				else
					count = count + sense[1] ;
				
				senseCounts.put(sense[0], count) ;	
				
				linkCount = linkCount + sense[1] ;
			}
			
			// sort the merged senses
			TreeSet<Sense> orderedSenses = new TreeSet<Sense>() ;
			for (Integer senseId: senseCounts.keySet()) {
				Integer count = senseCounts.get(senseId) ;
				orderedSenses.add(new Sense(senseId, count)) ;
			}
			
			// store 
			this.senses = new int[orderedSenses.size()][2] ;
			
			int index = 0 ;
			for (Sense sense: orderedSenses) {
				int[] s = {sense.id, sense.count} ;
				this.senses[index] = s ;		
				index++ ;
			}
		}
		
		private class Sense implements Comparable<Sense> {
			Integer id ;
			Integer count ;
			
			public Sense(Integer id, Integer count) {
				this.id = id ;
				this.count = count ;
			}
			
			public int compareTo(Sense s) {
				int cmp = s.count.compareTo(count) ;
				if (cmp == 0) 
					cmp = id.compareTo(s.id) ;
				return cmp ;
			}
		}
	}
	
	/**
	 * Summarizes first paragraphs and first sentences so short definitions can be obtained efficiently. 
	 * 
	 * @throws SQLException 
	 */
	public void summarizeDefinitions() throws SQLException {
		
		if (!isContentImported())
			throw new SQLException("You must import article content first!") ;
		
		int rows = this.getPageCount() ;
		
		ProgressNotifier pn = new ProgressNotifier(1) ;
		pn.startTask(rows, "Summarizing definitions") ;
		
		Statement stmt = createStatement() ;
		stmt.executeUpdate("DROP TABLE IF EXISTS definition") ;
		stmt.close() ;
		
		stmt = createStatement() ;
		stmt.executeUpdate("CREATE TABLE definition (" 
				+ "df_id int(8) unsigned NOT NULL, "
				+ "df_firstSentence mediumblob NOT NULL, "
				+ "df_firstParagraph mediumblob NOT NULL, "
				+ "PRIMARY KEY (df_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8;") ;
					
		stmt.close() ;
		
		int currRow = 0 ;
		
		int chunkSize = 100 ;
			
		StringBuffer insertQuery = new StringBuffer() ;
		SentenceSplitter ss = new SentenceSplitter() ;
		
		PageIterator i = new PageIterator(this) ;
		while (i.hasNext()) {
			Page p = i.next() ;
			currRow ++ ;
			
			String paragraph = "" ;
			String sentence = "" ;
			
			try {
				paragraph = p.getFirstParagraph() ;
				sentence = p.getFirstSentence(paragraph, ss) ;
			} catch (Exception e) {
				System.err.println(p + " " + e.getMessage()) ;
			}
			
			insertQuery.append(" (" + p.getId() + ",\"" + addEscapes(sentence) + "\",\"" + addEscapes(paragraph) + "\"),") ;
			
			if (currRow%chunkSize == 0) {
				if (insertQuery.length() > 0) {
					insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
					
					stmt = createStatement() ;
					stmt.setEscapeProcessing(false) ;
					stmt.executeUpdate("INSERT IGNORE INTO definition VALUES" + insertQuery.toString() ) ;
					stmt.close() ;
					
					insertQuery = new StringBuffer() ;
				}
				
				pn.update(currRow) ;
			}
		}
		
		if (insertQuery.length() > 0) {
			insertQuery.delete(insertQuery.length()-1, insertQuery.length()) ;
						
			stmt = createStatement() ;
			stmt.setEscapeProcessing(false) ;
			stmt.executeUpdate("INSERT IGNORE INTO definition VALUES" + insertQuery.toString() ) ;
			stmt.close() ;
		}
	}	
	
	
	/**
	 * Demo code for importing and initializing a Wikipedia database. 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Wikipedia wikipedia = new Wikipedia("localhost", "enwiki_20080727", "student", "*****") ; 
			//Wikipedia.getInstanceFromArguments(args) ;
			
			//File dataDirectory = new File("/research/wikipediaminer/data/en/20080727") ;
			//wikipedia.getDatabase().loadData(dataDirectory, false) ;
			
			//wikipedia.getDatabase().summarizeDefinitions() ;
			//wikipedia.getDatabase().prepareForTextProcessor(new CaseFolder()) ;
			//wikipedia.getDatabase().prepareForTextProcessor(new Cleaner()) ;
			//wikipedia.getDatabase().prepareForTextProcessor(new PorterStemmer()) ;
			
			//Wikipedia wikipedia = new Wikipedia("nui", "enwiki_20080727", "root", null) ;
			//wikipedia.getDatabase().getValidPageIds(dataDirectory, 5, null) ;	
			
			
			wikipedia.getDatabase().summarizeDefinitions() ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
	
}
