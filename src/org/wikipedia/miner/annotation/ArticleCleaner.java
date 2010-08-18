/*
 *    ArticleCleaner.java
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

package org.wikipedia.miner.annotation;

import org.wikipedia.miner.util.MarkupStripper;
import org.wikipedia.miner.util.SentenceSplitter;
import org.wikipedia.miner.model.Article;

/**
 *	A utility class for cleaning up Wikipedia articles so that they can be used to train and test disambiguation, link detection, etc.
 *
 * @author David Milne
 */
public class ArticleCleaner {

	/**
	 * all of the article will be extracted and cleaned
	 */
	public static final int ALL = 0 ;
	/**
	 * only the first sentence of the article will be extracted and cleaned
	 */
	public static final int FIRST_SENTENCE = 1;
	/**
	 * only the first paragraph of the article will be extracted and cleaned
	 */
	public static final int FIRST_PARAGRAPH = 2 ;
	
	private SentenceSplitter sentenceSplitter ;
	
	/**
	 * Initializes a new ArticleCleaner
	 */
	public ArticleCleaner() {
		this.sentenceSplitter = new SentenceSplitter() ;
	}
	
	
	/**
	 * @param article the article to clean
	 * @param snippetLength the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content (or snippet) of the given article, with all markup removed except links to other articles.  
	 * @throws Exception
	 */
	public String getMarkupLinksOnly(Article article, int snippetLength) throws Exception {
		
		if (snippetLength == FIRST_SENTENCE || snippetLength == FIRST_PARAGRAPH) {
			
			String content ;
			
			if (snippetLength == FIRST_SENTENCE) 
				content = article.getFirstSentence(null, sentenceSplitter) ;
			else
				content = article.getFirstParagraph() ;
			
			content = MarkupStripper.stripFormatting(content) ;
			return content ;
			
		} else {
			String content = article.getContent() ;
			
			content = MarkupStripper.stripTemplates(content) ;
			content = MarkupStripper.stripSection(content, "see also") ;
			content = MarkupStripper.stripSection(content, "references") ;
			content = MarkupStripper.stripSection(content, "external links") ;
			content = MarkupStripper.stripSection(content, "further reading") ;
			content = MarkupStripper.stripHeadings(content) ; 
			content = MarkupStripper.stripNonArticleLinks(content) ;
			content = MarkupStripper.stripExternalLinks(content) ;
			content = MarkupStripper.stripIsolatedLinks(content) ;
			content = MarkupStripper.stripTables(content) ;
			content = MarkupStripper.stripHTML(content) ;
			content = MarkupStripper.stripMagicWords(content) ;
			content = MarkupStripper.stripFormatting(content) ;
			content = MarkupStripper.stripExternalLinks(content) ;		
			content = MarkupStripper.stripExcessNewlines(content) ;
			
			return content ;
		}
		
		
	}
	
	/**
	 * @param article the article to clean
	 * @param snippetLength the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content of the given article, with all markup removed.  
	 * @throws Exception
	 */
	public String getCleanedContent(Article article,  int snippetLength) throws Exception{
		
		if (snippetLength == FIRST_SENTENCE || snippetLength == FIRST_PARAGRAPH) {
			
			String content ;
			
			if (snippetLength == FIRST_SENTENCE) 
				content = article.getFirstSentence(null, sentenceSplitter) ;
			else
				content = article.getFirstParagraph() ;
			
			content = MarkupStripper.stripFormatting(content) ;
			content = MarkupStripper.stripLinks(content) ;
			return content ;
	
		} else {
		
			String content = article.getContent() ;
			content = MarkupStripper.stripTemplates(content) ;
			content = MarkupStripper.stripSection(content, "see also") ;
			content = MarkupStripper.stripSection(content, "references") ;
			content = MarkupStripper.stripSection(content, "external links") ;
			content = MarkupStripper.stripSection(content, "further reading") ;
			content = MarkupStripper.stripHeadings(content) ; 
			content = MarkupStripper.stripExternalLinks(content) ;
			content = MarkupStripper.stripIsolatedLinks(content) ;
			content = MarkupStripper.stripLinks(content) ;
			content = MarkupStripper.stripTables(content) ;
			content = MarkupStripper.stripHTML(content) ;
			content = MarkupStripper.stripMagicWords(content) ;
			content = MarkupStripper.stripFormatting(content) ;
			content = MarkupStripper.stripExternalLinks(content) ;		
			content = MarkupStripper.stripExcessNewlines(content) ;
		
			return content ;
		}
	}
}
