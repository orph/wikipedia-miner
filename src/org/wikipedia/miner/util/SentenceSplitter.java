/*
 *    SentenceSplitter.java
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

import java.util.HashSet ;
import java.util.Vector ;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rule-based sentence splitter originally developed in PERL by Marcia Munoz.
 * 
 * Modified by Ramya Nagarajan 6/19/01
 * Translated to Java by David Milne 2/26/09 
 */
public class SentenceSplitter {

	/**
	 * All newlines will be used to split the text into paragraphs
	 */
	public static final int ALL_NEWLINES = 1;
	
	/**
	 * Only blocks of two or more consecutive newlines will be used to split the text into paragraphs.
	 */
	public static final int MULTIPLE_NEWLINES = 2;
	
	/**
	 * The text will not be split into paragraphs before splitting into sentences. 
	 */
	public static final int IGNORE_NEWLINES = 3 ;

	private HashSet<String> terminals ;
	private HashSet<String> honorifics ;

	/**
	 * 
	 */
	public SentenceSplitter() {

		terminals = new HashSet<String>() ;
		terminals.add("Esq") ;
		terminals.add("Jr" ) ;
		terminals.add("Sr") ;
		terminals.add("M.D") ;

		honorifics = new HashSet<String>() ;
		honorifics.add("A.");
		honorifics.add("Adj.");
		honorifics.add("Adm.");
		honorifics.add("Adv.");
		honorifics.add("Asst.");
		honorifics.add("B.");
		honorifics.add("Bart.");
		honorifics.add("Bldg.");
		honorifics.add("Brig.");
		honorifics.add("Bros.");
		honorifics.add("C.");
		honorifics.add("Capt.");
		honorifics.add("Cmdr.");
		honorifics.add("Col.");
		honorifics.add("Comdr.");
		honorifics.add("Con.");
		honorifics.add("Cpl.");
		honorifics.add("D.");
		honorifics.add("DR.");
		honorifics.add("Dr.");
		honorifics.add("E.");
		honorifics.add("Ens.");
		honorifics.add("F.");
		honorifics.add("G.");
		honorifics.add("Gen.");
		honorifics.add("Gov.");
		honorifics.add("H.");
		honorifics.add("Hon.");
		honorifics.add("Hosp.");
		honorifics.add("I.");
		honorifics.add("Insp.");
		honorifics.add("J.");
		honorifics.add("K.");
		honorifics.add("L.");
		honorifics.add("Lt.");
		honorifics.add("M.");
		honorifics.add("M.");
		honorifics.add("MM.");
		honorifics.add("MR.");
		honorifics.add("MRS.");
		honorifics.add("MS.");
		honorifics.add("Maj.");
		honorifics.add("Messrs.");
		honorifics.add("Mlle.");
		honorifics.add("Mme.");
		honorifics.add("Mr.");
		honorifics.add("Mrs.");
		honorifics.add("Ms.");
		honorifics.add("Msgr.");
		honorifics.add("N.");
		honorifics.add("O.");
		honorifics.add("Op.");
		honorifics.add("Ord.");
		honorifics.add("P.");
		honorifics.add("Pfc.");
		honorifics.add("Ph.");
		honorifics.add("Prof.");
		honorifics.add("Pvt.");
		honorifics.add("Q.");
		honorifics.add("R.");
		honorifics.add("Rep.");
		honorifics.add("Reps.");
		honorifics.add("Res.");
		honorifics.add("Rev.");
		honorifics.add("Rt.");
		honorifics.add("S.");
		honorifics.add("Sen.");
		honorifics.add("Sens.");
		honorifics.add("Sfc.");
		honorifics.add("Sgt.");
		honorifics.add("Sr.");
		honorifics.add("St.");
		honorifics.add("Supt.");
		honorifics.add("Surg.");
		honorifics.add("T.");
		honorifics.add("U.");
		honorifics.add("V.");
		honorifics.add("W.");
		honorifics.add("X.");
		honorifics.add("Y.");
		honorifics.add("Z.");
		honorifics.add("v.");
		honorifics.add("vs.");

	}
	
	/**
	 * @param text the text to be split into sentences
	 * @param paragraphMode specifies how newlines are used to split the text into paragraphs before these are split into sentences.
	 * @return a Vector of sentences, in the order that they appear in the source text. 
	 */
	public Vector<String> getSentences(String text, int paragraphMode) {

		Vector<String> sentences = new Vector<String>() ;

		if (paragraphMode == ALL_NEWLINES) {
			String[] paragraphs = text.split("\n") ;

			for (String paragraph:paragraphs) 
				sentences = getSentences(paragraph, sentences) ;

			return sentences ;
		}

		if (paragraphMode == MULTIPLE_NEWLINES) {
			String[] paragraphs = text.split("(\\n\\s*){2,}") ;

			for (String paragraph:paragraphs) 
				sentences = getSentences(paragraph, sentences) ;

			return sentences ;
		}

		return getSentences(text, sentences) ;
	}

	private Vector<String> getSentences(String paragraph, Vector<String> sentences) {
		
		String orig = paragraph.replaceAll("\\s+", " ") ;
		String linksMasked = maskLinks(orig) ;
		
		////System.out.println(orig) ;
		////System.out.println(linksMasked) ;

		String[] words = linksMasked.split(" ") ;

		StringBuffer sentence = new StringBuffer() ;
		int wordPos = 0 ;
		
		for (int i=0 ; i<words.length ; i++) {

			// Check the existence of a candidate
			int period_pos = words[i].lastIndexOf(".") ;
			int question_pos = words[i].lastIndexOf(".") ;
			int exclam_pos = words[i].lastIndexOf("!") ;

			// Determine the position of the rightmost candidate in the word
			int pos = period_pos ;
			char candidate = '.' ;

			if (question_pos > pos) {
				pos = question_pos ;
				candidate = '?' ;
			}
			if (exclam_pos > pos) {
				pos = exclam_pos;
				candidate = '!';
			}

			// Do the following only if the word has a candidate
			if (pos != -1) {

				String wm1 = null ;
				String wm2 = null ;
				boolean wm1C = false ;
				boolean wm2C = false ;

				// Check the previous word
				if (i>0) {
					wm1 = words[i-1];
					wm1C = capital(wm1);

					// Check the word before the previous one 
					if (i>1) {
						wm2 = words[i-2];
						wm2C = capital(wm2);
					}
				}

				String wp1 = null ;
				String wp2 = null;
				boolean wp1C = false ;
				boolean wp2C = false ;

				// Check the next words
				if (i<words.length-1) {
					wp1 = words[i + 1];
					wp1C = capital(wp1);

					if (i<words.length-2) {
						wp2 = words[i+2] ;
						wp2C = capital(wp2) ;
					}
				}

				String prefix = null ;
				boolean prefixC = false ;
				if (pos > 0) {
					prefix = words[i].substring(0, pos) ;
					prefixC = capital(prefix) ;
				}

				String suffix = null ;
				boolean suffixC = false ;
				if (pos < words[i].length()-1) {
					suffix = words[i].substring(pos+1) ;
					suffixC = capital(suffix) ;
				}

				//System.out.println(words[i]) ;
				boolean prediction = isBoundary(candidate, wm2, wm1, removeLinkDelimeter(prefix), removeLinkDelimeter(suffix), wp1, wp2, wm2C, wm1C, prefixC, suffixC, wp1C, wp2C);
				//System.out.println(" - " + prediction) ;
				
				// Append the word to the sentence
				sentence.append(orig.substring(wordPos, wordPos + words[i].length())) ;
				sentence.append(" ") ;
				
				

				if (prediction) {
					//remove the extra space we added
					if (sentence.length() > 0)
						sentences.add(sentence.substring(0, sentence.length()-1)) ;
					else
						sentences.add("") ;

					sentence = new StringBuffer() ;
				}
			}
			else
			{ 
				// If the word doesn't have a candidate, then append the word to the sentence
				sentence.append(orig.substring(wordPos, wordPos + words[i].length())) ;
				sentence.append(" ") ;
			}
			
			wordPos = wordPos + words[i].length() + 1 ;
		}

		if (sentence.length() > 0) 
			sentences.add(sentence.substring(0, sentence.length()-1)) ;

		return sentences ;
	}
	
	private String maskLinks(String text) {
		
		Vector<Integer> linkStack = new Vector<Integer>() ; 
		
		Pattern p = Pattern.compile("(\\[\\[|\\]\\])") ;
		Matcher m = p.matcher(text) ;
		
		StringBuffer sb = new StringBuffer() ;
		int lastIndex = 0 ;
		
		while (m.find()) {
			String tag = text.substring(m.start(), m.end()) ;
			
			if (tag.equals("[["))
				linkStack.add(m.start()) ;
			else {
				if (!linkStack.isEmpty()) {
					int linkStart = linkStack.lastElement() ;
					linkStack.remove(linkStack.size()-1) ;
					
					if (linkStack.isEmpty()) {
						sb.append(text.substring(lastIndex, linkStart)) ;
						
						//we have the whole link, with other links nested inside if it's an image
						for (int i=linkStart; i<m.end() ; i++)
							sb.append("A") ;
						
						lastIndex = m.end() ;
					}
				}
			}
		}
		
		
		sb.append(text.substring(lastIndex)) ;
		return sb.toString() ;
	}




	// This subroutine does all the boundary determination stuff
	// It returns "Y" if it determines the candidate to be a sentence boundary,
	// "N" otherwise
	@SuppressWarnings("unused")
	private boolean isBoundary(char candidate,  String wm2, String wm1, String prefix, String suffix, String wp1, String wp2, boolean wm2C, boolean wm1C, boolean prefixC, boolean suffixC, boolean wp1C, boolean wp2C) {
		// Declare local variables
		
		//System.out.println(" - c:'" + candidate + "' p:'" + prefix + "' s:'" + suffix + "'") ;

		// Check if the candidate was a question mark or an exclamation mark
		if (candidate=='?' || candidate=='!') {
			// Check for the end of the file
			if (wp1==null && wp2==null)	
				return true ;

			// Check for the case of a question mark followed by a capitalized word
			if (suffix==null && wp1C)               
				return true ;

			if (suffix==null && startsWithQuote(wp1))
				return true ;

			if (suffix==null && wp1.equals("--") && wp2C) 
				return true ;

			if (suffix==null && wp1.equals("-RBR-") && wp2C)
				return true ;

			// This rule takes into account vertical ellipses, as shown in the
			// training corpus. We are assuming that horizontal ellipses are
			// represented by a continuous series of periods. If this is not a
			// vertical ellipsis, then it's a mistake in how the sentences were
			// separated.
			if (suffix == null && wp1.equals("."))
				return true ;

			if (isRightEnd(suffix) && isLeftStart(wp1))
				return true ; 
			else
				return false ;
		} else {
			// Check for the end of the file
			if (wp1==null && wp2==null){
				////System.out.print("a") ;
				return true ;
			}
			
			if (suffix==null && startsWithQuote(wp1)) {
				////System.out.print("b") ;
				return true ;
			}

			if (suffix==null && startsWithLeftParen(wp1)) {
				////System.out.print("c") ;
				return true ;
			}
				

			if (suffix==null && wp1.equals("-RBR-") && wp2.equals("--")) {
				////System.out.print("d") ;
				return false ; 
			}
				

			if (suffix==null && isRightParen(wp1)){
				////System.out.print("e") ;
				return true ;
			}

			// Added by Ramya Nagarajan 6/19/01
			// This takes account of the numbered lists seen in the TREC/TIPSTER_V3
			// data files.
			if (candidate=='.' && suffix==null && endsWithRightParen(wp1) && wp2C) {
				////System.out.print("f") ;
				return true ;
			}

			// This rule takes into account vertical ellipses, as shown in the
			// training corpus. We are assuming that horizontal ellipses are
			// represented by a continuous series of periods. If this is not a
			// vertical ellipsis, then it's a mistake in how the sentences were
			// separated.
			if (prefix==null && suffix==null && wp1.equals(".")) {
				////System.out.print("g") ;
				return false ;
			}

			if (suffix==null && wp1.equals(".")) {
				////System.out.print("h") ;
				return true ;
			}

			if (suffix==null && wp1.equals("--") && wp2C && endsInQuote(prefix)){
				////System.out.print("i") ;
				return false ; 
			}

			if (suffix==null && wp1.equals("--") && (wp2C || startsWithQuote(wp2))){
				//System.out.print("j") ;
				return true ;
			}
			
			if (suffix==null && wp1C && prefix!=null && (prefix.equals("p.m") || prefix.equals("a.m")) && isTimeZone(wp1)){
				//System.out.print("k") ;
				return false ;
			}

			// Check for the case when a capitalized word follows a period,
			// and the prefix is a honorific
			if (suffix==null && wp1C && isHonorific(prefix +".")) {
				//System.out.print("l") ;
				return false ; 
			}

			if (suffix==null && wp1C && (startsWithQuote(prefix) && !endsWithQuote(prefix))) {
				//System.out.print("m") ;
				return false ;
			}
			
			// This rule checks for prefixes that are terminal abbreviations
			if (suffix==null && wp1C && isTerminal(prefix)) {
				//System.out.print("n") ;
				return true ;
			}

			// Check for the case when a capitalized word follows a period and the
			// prefix is a single capital letter
			
			if (suffix==null && wp1C && prefix != null && prefix.matches("([A-Z]\\.)*[A-Z]")){
				//System.out.print("o") ;
				return false ;
			}

			// Check for the case when a capitalized word follows a period
			if (suffix==null && wp1C){
				//System.out.print("p") ;
				return true ;
			}

			if (isRightEnd(suffix) && isLeftStart(wp1)){
				//System.out.print("q") ;
				return true ;	
			}
		}
		
		//System.out.print("z") ;
		return false ;
	}

	private boolean capital(String word) {
		if (word==null || word.equals(""))
			return false ;
		return Character.isUpperCase(word.charAt(0)) ;
	}

	private boolean isHonorific(String word) {
		return (honorifics.contains(word)) ;
	}

	//checks to see if the string is a terminal abbreviation.
	private boolean isTerminal(String word) {
		return (terminals.contains(word)) ;
	}

	private boolean isTimeZone(String word) {
		if (word == null)
			return false ;
		
		return (word.startsWith("EDT") || word.startsWith("CST") || word.startsWith("EST")) ;
	}

	private boolean endsInQuote(String word) {  
		if (word==null)
			return false ;
		
		return (word.endsWith("'") || word.endsWith("'") || word.endsWith("\"")) ;
	}

	private boolean startsWithQuote(String word) {
		if (word == null)
			return false ;
		
		return (word.startsWith("'") || word.startsWith("\"") || word.startsWith("`")) ;
	}
	
	private boolean endsWithQuote(String word) {
		if (word == null)
			return false ;
		
		return (word.endsWith("'") || word.endsWith("\"") || word.endsWith("`")) ;
	}


	private boolean startsWithLeftParen(String word) {
		if (word == null)
			return false ;
			
		return (word.startsWith("{") || word.startsWith("(") || word.startsWith("<")) ;
	}

	private boolean endsWithRightParen(String word) {
		if (word==null)
			return false ;
		
		return (word.endsWith("}") || word.endsWith(")") || word.endsWith(">")) ;
	}

	private boolean startsWithLeftQuote(String word) {
		if (word==null)
			return false ;
		
		return (word.startsWith("'") || word.startsWith("`") || word.startsWith("\"")) ; 
	}

	private boolean isRightEnd(String word) {
		return (isRightParen(word) || isRightQuote(word)) ;
	}

	private boolean isLeftStart(String word) {
		return (startsWithLeftQuote(word) || startsWithLeftParen(word) || capital(word)) ;
	}

	private boolean isRightParen(String word) {
		if (word == null)
			return false ;
		
		return (word.equals("}") || word.equals(")") || word.equals(">")) ;
	}
	
	private String removeLinkDelimeter(String word) {
		
		if (word == null)
			return null ;
		
		String w = word.replace("\\[\\[", "") ;
		w = w.replace("\\]\\]", "") ;
		
		if (w.equals(""))
			return null ;
		else
			return w ;
	}
	
	private boolean isRightQuote(String word) {
		if (word == null)
			return false ;
		
		return (word.equals("'") ||  word.equals("''") || word.equals("'''") || word.equals("\"") || word.equals("'\"")) ;
	}

}
