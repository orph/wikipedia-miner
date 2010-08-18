/*
 *    MySqlDatabase.java
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * This convenience class provides access to a MySql database via the MySqlConnector-J toolkit. 
 * 
 * @author David Milne
 */
public class MySqlDatabase {
	
	private Connection connection ;
	
	private String server ;
	private String databaseName ;
	private String userName ;
	private String password ;
	private String encoding ;
	
	private int statementsIssued ;
	
	/**
	 * Initializes a newly created MySqlDatabase and attempts to make a connection to the 
	 * database defined by the arguments given.
	 *
	 * @param server the connection string for the server (e.g 130.232.231.053:8080 or bob:8080)
	 * @param databaseName the name of the database (e.g <em>enwiki</em>)
	 * @param userName the user for the sql database (null if anonymous)
	 * @param password the users password (null if anonymous)
	 * @param encoding the character encoding (e.g. utf8) in which data is stored (null if using database default)
	 * @throws Exception if there is a problem connecting to the database defined by given arguments.
	 */
	public MySqlDatabase(String server, String databaseName, String userName, String password, String encoding) throws Exception{
		
		this.server = server ;
		this.databaseName = databaseName ;
		this.userName = userName ;
		this.password = password ;
		this.encoding = encoding ;
		this.statementsIssued = 0 ;
		connect() ;
	}
	
	/**
	 * @return true if the connection to the database is active, otherwise false.
	 */
	public boolean checkConnection() {
		try {
			//try a simple query
			Statement stmt = createStatement() ;
			ResultSet rs = stmt.executeQuery("SHOW TABLES") ;
			
			rs.close() ;
			stmt.close() ;
			
			return true ;
		} catch (SQLException e) {
			return false ;
		}
	}
	
	/**
	 * attempts to make a connection to the mysql database.
	 * @throws	SQLException	if a connection cannot be made.
	 * @throws InstantiationException	if the mysql driver class cannot be instantiated
	 * @throws IllegalAccessException	if the mysql driver class cannot be instantiated
	 * @throws ClassNotFoundException	if the mysql driver class cannot be found
	 */
	public void connect() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
			
		// load the sql drivers
		Class.forName("org.gjt.mm.mysql.Driver").newInstance(); 
		
		// establish a connection
		String url = "jdbc:mysql://" + server + "/" + databaseName ;
		
		boolean argsStarted = false ;
		
		if (userName != null && !userName.equals("")) { 
			url = url + "?user=" + userName ; 
			argsStarted = true ;
		}
		
		if (password != null && !password.equals("")) {
			if (argsStarted)
				url = url + "&password=" + password ;
			else
				url = url + "?password=" + password ;
			
			argsStarted = true ;
		}
		 
		if (encoding != null && !encoding.equals("")) {
			if (argsStarted)
				url = url + "&characterEncoding=" + encoding ;
			else
				url = url + "?characterEncoding=" + encoding ;	
		}
		
		connection = DriverManager.getConnection(url) ; 
	}
	
	/**
	 * Specifies whether each update statement is to be automatically executed immediately, or left until commit() is called. 
	 * It is often more efficent set this to false and wait for several update statements to be issued before calling commit().
	 * This is false by default. 
	 *  
	 * @param autocommit	true if each update statement is to be automatically executed immediately, otherwise false.
	 * @throws SQLException if there is a problem with the database connection
	 */
	public void setAutoCommit(boolean autocommit) throws SQLException {
		this.connection.setAutoCommit(autocommit) ;
	}
	
	/**
	 * Commits any pending update statements. This is only neccessary if autocommit has been set to false. 
	 *  
	 * @throws SQLException if there is a problem with the database connection
	 */
	public void commit() throws SQLException {
		this.connection.commit() ;
	}
		
	/**
	 * Creates a Statement object for sending SQL statements to the database.
	 *  
	 * @return the statement object
	 * @throws SQLException if there is a problem with the database connection
	 */
	public Statement createStatement() throws SQLException {
		statementsIssued ++ ;
		
		try {
			return connection.createStatement() ;
		} catch (SQLException e) {
			try {
				this.connect() ;
				return connection.createStatement() ;
			} catch (Exception e2) {
				throw new SQLException() ;
			}
		}
	}
	
	/**
	 * Returns the number of statements (queries) that have been issued to the database since this
	 * connection was initialized.
	 *  
	 * @return as above
	 */
	public int getStatementsIssuedSinceStartup() {
		return statementsIssued ;
	}
	
	
	/**
	 * returns true if this database contains a table with the given table name, otherwise false.
	 * 
	 * @param tableName the name of the table to check
	 * @return true if this database contains a table with the given table name, otherwise false.
	 * @throws SQLException if there is a problem with the database
	 */
	public boolean tableExists(String tableName) throws SQLException {
		
		boolean exists = true ;
		Statement stmt = createStatement() ;
		
		try {
			stmt.executeQuery("SELECT 1 FROM `" + tableName + "` LIMIT 0") ;
		} catch (SQLException e) {
			exists = false ;
		} 
		
		stmt.close() ;
		return exists ;
	}
	
	/**
	 * returns true if this database contains an index for the given table and index name, otherwise false.
	 * 
	 * @param tableName the name of the table to check
	 * @param indexName the name of the index to check
	 * @return true if this database contains an index for the given table and index name, otherwise false.
	 * @throws SQLException if there is a problem with the database
	 */
	public boolean indexExists(String tableName, String indexName) throws SQLException {
		boolean exists = true ;
		Statement stmt = createStatement() ;
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM information_schema.statistics " +
				"WHERE TABLE_SCHEMA='" + databaseName + "' " +
				"AND TABLE_NAME='" + tableName + "' " +
				"AND INDEX_NAME='" + indexName + "' ;") ;
		
		if (rs.first())
			exists = true ;
		else
			exists = false ;
		
		rs.close() ;
		stmt.close() ;
			
		return exists ;
	}
	
	
	
	/**
	 * Returns an estimated row count for a given table. This may not be accurate (particularly if the table is
	 * in a state of flux) but will always be quickly and efficiently calculated.
	 * 
	 * @param tableName the name of the table to check
	 * @return the number of rows in the given table
	 * @throws SQLException if there is a problem with the database
	 */
	public int getRowCountEstimate(String tableName) throws SQLException {
		int count = 0 ;
		
		Statement stmt = createStatement() ;
		
		ResultSet rs = stmt.executeQuery("SHOW TABLE STATUS " +
				"WHERE name='" + tableName + "';") ;
		
		if (rs.first())
			count = rs.getInt("Rows") ;
		
		rs.close() ;
		stmt.close() ;
		
		return count ;
	}
	
	/**
	 * Returns an exact row count for a given table. This will always be accurate but may take some time 
	 * to calculate for larger tables.
	 * 
	 * @param tableName the name of the table to check
	 * @return the number of rows in the given table
	 * @throws SQLException if there is a problem with the database
	 */
	public int getRowCountExact(String tableName) throws SQLException {
		int count = 0 ;
		
		Statement stmt = createStatement() ;
		
		ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName) ;
				
		if (rs.first())
			count = rs.getInt(1) ;
		
		rs.close() ;
		stmt.close() ;
		
		return count ;
	}
	
	
	
	
	/**
	 * Modifies a string so that it can be safely inserted into a database; escapes all special characters such
	 * as quotes and slashes
	 * 
	 * @param s the string to modify
	 * @return the modified string
	 */
	public String addEscapes( String s )
	 {
	  StringBuffer sb = new StringBuffer();
	  int index;
	  int length = s.length();
	  char ch;

	  for( index = 0; index < length; ++index )
	   if(( ch = s.charAt( index )) == '\"' )
	    sb.append( "\\\"" );
	   else if( ch == '\'' )
		sb.append( "\\'" );
	   else if( ch == '\\' )
	    sb.append( "\\\\" );
	   else
	    sb.append( ch );

	  return( sb.toString());
	 }
}

