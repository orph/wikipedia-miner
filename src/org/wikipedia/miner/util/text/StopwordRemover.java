/*
 *    StopwordRemover.java
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

package org.wikipedia.miner.util.text;

import java.io.*;
import java.util.*;


/**
 * This class provides moderate morphology. This involves cleaning the text using a TextCleaner then
 * removing all stopwords.
 */	
public class StopwordRemover extends TextProcessor {
	
	HashSet<String> stopwords ;
	Cleaner cleaner = new Cleaner() ;
    File stopwordFile;
	
	/**
	 * Initializes a newly created StopwordRemover with a list of stopwords contained within the given file. 
	 * The file must be in a format where each word is found on its own line.
     * Comments are also supported using the Snowball project syntax: every character between a '|' symbol
     * and the end of the line is ignored. Spaces between the last non-space character and the '|' symbol
     * are also ignored.
	 * 
	 * @param	stopwordFile	the file of stopwords
	 * @throws	IOException		if there is a problem reading from the file of stopwords
	 */	
	public StopwordRemover(File stopwordFile) throws IOException {
		this.stopwordFile = stopwordFile;
		stopwords = new HashSet<String>() ;
		
		BufferedReader input = new BufferedReader(new FileReader(stopwordFile)) ;
		
		String line ;
		while ((line = input.readLine()) != null) {
			String word = line.trim().toLowerCase() ;
            word = word.replaceAll("\\s*[|].*", "");
			stopwords.add(word) ;
		}
	}
	
	/**
	 * Initializes a newly created StopwordRemover with a list of stopwords contained within the HashSet. 
	 * 
	 * @param	stopwords	a HashSet of stopwords
	 */	
	public StopwordRemover(HashSet<String> stopwords){
		this.stopwords = stopwords ;
	}
	
	/**
	 * Returns the processed version of the argument string. This involves 
	 * removing all stopwords, then cleaning each remaining term.
	 * 
	 * @param	text	the string to be processed
	 * @return the processed string
	 */	
	public String processText(String text) {
		StringBuffer processedText = new StringBuffer() ;
		
		String[] terms = text.split(" ") ;
		for(int i=0;i<terms.length; i++) {
			if (!stopwords.contains(terms[i])) {
				processedText.append(cleaner.processText(terms[i])) ;
				processedText.append(" ") ;
			}
		}

		return processedText.toString().trim() ;	
	}

	/**
	 * Returns a string that provides complete information to setup the current configuration of the
     * text processor.
	 *
	 * @return	a textual description of the TextProcessor.
	 */
    @Override
    public String toString(){
        return super.toString() + "<file<" + TextProcessor.encodeParam(this.stopwordFile.toString());
    }
}


