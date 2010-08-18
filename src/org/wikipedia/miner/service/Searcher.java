/*
 *    Searcher.java
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


package org.wikipedia.miner.service;

import java.sql.SQLException;
import java.util.*;

import org.w3c.dom.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.model.Article.AnchorText;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;

/**
 * @author David Milne
 * 
 * <p>This service provides searching facilities over wms.wikipedia via terms or page ids. </p>"
 * <p> Searching via terms (or phrases) is done through article titles, redirects, and anchors (the terms used to link to each page). This method of searching encodes synonymy: you can find the article about <b>fixed-wing aircraft</b> by searching for <i>airplanes</i>, <i>aeroplanes</i> or <i>propeller aircraft</i>. It also encodes polysemy: you can tell that <b>plane</b> could also refer to a <i>theoretical surface of infinite area and zero depth</i>, or <i>a tool for shaping wooden surfaces</i>. </p> 
 * <p> Searching via terms will return either a list of candidate articles (if the term is ambiguous), or the details of a single article (if it is not). Searching via ids will return details of the appropriate page, which may be an Article, Category, Disambiguation, or Redirect</p>
 */
public class Searcher {

	private WikipediaMinerServlet wms ;
		
	private int defaultMaxLinkCount = 250 ;
	private int defaultMaxSenseCount = 25 ;

	/**
	 * @param wms the servlet that hosts this service
	 */
	public Searcher(WikipediaMinerServlet wms) {
		this.wms = wms ;
	}
	
	/**
	 * @return the default number of links that are shown extending out from and into Wikipedia pages. 
	 */
	public int getDefaultMaxLinkCount() {
		return defaultMaxLinkCount ;
	}
	
	/**
	 * @return the default number of senses that will be shown for an ambiguous term. 
	 */
	public int getDefaultMaxSenseCount() {
		return defaultMaxSenseCount ;
	}
	
	/**
	 * @return an Element description of this service; what it does, and what parameters it takes.
	 */
	public Element getDescription() {
		
		Element description = wms.doc.createElement("Description") ;
		description.setAttribute("task", "search") ;
		
		description.appendChild(wms.createElement("Details", "<p>This service provides searching facilities over wms.wikipedia via terms or page ids. </p>"
				+ "<p> Searching via terms (or phrases) is done through article titles, redirects, and anchors (the terms used to link to each page). This method of searching encodes synonymy: you can find the article about <b>fixed-wing aircraft</b> by searching for <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&term=airplanes\">airplanes</a>, <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&term=aeroplanes\">aeroplanes</a> or <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&term=planes\">planes</a>. It also encodes polysemy: you can tell that <em>plane</em> could also refer to a <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&id=84029\">theoretical surface of infinite area and zero depth</a>, or <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&id=452991\">a tool for shaping wooden surfaces</a>. </p>" 
				+ "<p> Searching via terms will return either a list of candidate articles (if the term is ambiguous), or the details of a single article (if it is not). Searching via ids will return details of the appropriate page, which may be an Article, Category, Disambiguation, or Redirect</p>")) ;
		
		Element group1 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group1) ;
		
		Element paramTerm = wms.doc.createElement("Parameter") ;
		paramTerm.setAttribute("name", "term") ;
		paramTerm.appendChild(wms.doc.createTextNode("The term (or phrase) to search for.")) ;
		group1.appendChild(paramTerm) ;
		
		Element group2 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group2) ;
		
		Element paramId = wms.doc.createElement("Parameter") ;
		paramId.setAttribute("name", "id") ;
		paramId.appendChild(wms.doc.createTextNode("The unique identifier of the page to search for.")) ;
		group2.appendChild(paramId) ;
		
		Element paramLinkCount = wms.doc.createElement("Parameter") ;
		paramLinkCount.setAttribute("name", "maxLinkCount") ;
		paramLinkCount.appendChild(wms.doc.createTextNode("The maximum number of page links or child categories and articles to return when presenting the details of an article or category.")) ;
		paramLinkCount.setAttribute("optional", "true") ;
		paramLinkCount.setAttribute("default", String.valueOf(getDefaultMaxLinkCount())) ; 
		description.appendChild(paramLinkCount) ;
		
		Element paramSenseCount = wms.doc.createElement("Parameter") ;
		paramSenseCount.setAttribute("name", "maxSenseCount") ;
		paramSenseCount.appendChild(wms.doc.createTextNode("The maximum number of senses to return when given an ambiguous term.")) ;
		paramSenseCount.setAttribute("optional", "true") ;
		paramSenseCount.setAttribute("default", String.valueOf(getDefaultMaxSenseCount())) ; 
		description.appendChild(paramSenseCount) ;
		
		return description ;
	}
	
	
	/**
	 * @param term the term to search for
	 * @param linkLimit the number of links to be shown extending out from and into the article, if the term is unambiguous
	 * @param senseLimit the number of senses to show, if the term is ambiguous
	 * @return xml results of the search, which will either be a list of senses (if ambiguous), or the details of the appropriate wms.wikipedia page (if not).  
	 * @throws Exception
	 */
	public Element doSearch(String term, int linkLimit, int senseLimit) throws Exception {
		
		Element response = wms.doc.createElement("SearchResponse") ;
		
		if (term == null) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
		
		TextProcessor tp = new CaseFolder() ;			
		Anchor anchor = new Anchor(term, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses = anchor.getSenses() ; 
		
		if (senses.size() == 0) {
			response.setAttribute("unknownTerm", term) ; 
			return response ;
		}
		
		response.setAttribute("term", term) ;
		
		if (senses.size() == 1) {
			return doSearch(senses.first().getId(), linkLimit) ;
		}
		
		Element xmlSenses = wms.doc.createElement("SenseList") ;
		
		int x = 0 ;
		
		for (Anchor.Sense sense:senses) {
			
			if (sense.getType() == Page.DISAMBIGUATION)
				continue ;
			
			if (x++ >= senseLimit) break ;
			
			
			
			Element xmlSense = wms.doc.createElement("Sense") ;
			xmlSense.setAttribute("id", String.valueOf(sense.getId())) ;
			xmlSense.setAttribute("title", sense.getTitle()) ;
			xmlSense.setAttribute("commonness", wms.df.format(sense.getProbability())) ;
			
			String firstSentence = null;
			try { 
				firstSentence = sense.getFirstSentence(null, null) ;
				firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
			} catch (Exception e) {} ;
			
			if (firstSentence != null) 
				xmlSense.appendChild(wms.createElement("FirstSentence", firstSentence)) ;

			
			xmlSenses.appendChild(xmlSense) ;
		}
		response.appendChild(xmlSenses) ;
		
		return response ;
	}
	
	/**
	 * @param id the id of the page to search for
	 * @param linkLimit the maximum number of links to show extending out from and into the target page.
	 * @return Element details of the page defined by id.
	 * @throws Exception
	 */
	public Element doSearch(int id, int linkLimit) throws Exception {
		
		Element response = wms.doc.createElement("SearchResponse") ;
		
		Page page = wms.wikipedia.getPageById(id) ;
		if (page != null) {
			
			response.setAttribute("term", page.getTitle()) ;
			
			switch (page.getType()) {
			
			case Page.ARTICLE :
				response.appendChild(getArticleDetails(page, linkLimit)) ;
				break ;
			case Page.CATEGORY :
				response.appendChild(getCategoryDetails(page, linkLimit)) ;
				break ;
			case Page.DISAMBIGUATION :
				response.appendChild(getDisambiguationDetails(page)) ;
				break ;
			case Page.REDIRECT :
				response.appendChild(getRedirectDetails(page)) ;
				break ;
			}
		}
		
		return response ;
	}
	
	private Element getArticleDetails(Page page, int linkLimit) throws SQLException{
		
		Article article = (Article)page ;
		
		Element xmlArt = wms.doc.createElement("Article") ;
		
		xmlArt.setAttribute("id", String.valueOf(article.getId())) ;
		xmlArt.setAttribute("title", article.getTitle()) ;
		//xmlArt.setAttribute("description", getDescription(article)) ;
		
		String firstParagraph = null;
		try { 
			firstParagraph = article.getFirstParagraph() ;
			firstParagraph =  wms.definer.formatDefinition(firstParagraph, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {};
		
		if (firstParagraph != null) 
			xmlArt.appendChild(wms.createElement("FirstParagraph", firstParagraph)) ;
		
		
		SortedVector<Redirect> redirects = article.getRedirects() ;
		if (!redirects.isEmpty()) {
			Element xmlRedirects = wms.doc.createElement("RedirectList") ;
			
			for (Redirect r: redirects) {
				Element xmlRedirect = wms.doc.createElement("Redirect") ;
				xmlRedirect.setAttribute("id", String.valueOf(r.getId())) ;
				xmlRedirect.setAttribute("title", r.getTitle()) ;
				xmlRedirects.appendChild(xmlRedirect) ;
			}
			xmlArt.appendChild(xmlRedirects) ;
		}
		
		SortedVector<AnchorText> anchorTexts = article.getAnchorTexts() ;
		if (!anchorTexts.isEmpty()) {
			Element xmlAnchors = wms.doc.createElement("AnchorList") ;
			
			int total = 0 ;
			for (AnchorText at:anchorTexts) 
				total += at.getCount() ;
					
			for (AnchorText at:anchorTexts) {
				int c = at.getCount() ;
				
				if (c > 0) {
					Element xmlAnchor = wms.doc.createElement("Anchor") ;
					xmlAnchor.setAttribute("text", at.getText()) ;
					xmlAnchor.setAttribute("occurances", String.valueOf(c)) ;
					xmlAnchor.setAttribute("proportion", wms.df.format((double)c/total)) ;
				
					xmlAnchors.appendChild(xmlAnchor) ;
				}
			}
			xmlAnchors.setAttribute("totalOccurances", String.valueOf(total)) ;
			xmlArt.appendChild(xmlAnchors) ;
		}
		
		
		HashMap<String,String> translations = article.getTranslations() ;
		if (!translations.isEmpty()) {
			
			Element xmlLangs = wms.doc.createElement("LanguageLinkList") ;
			
			for (String lang:translations.keySet()) {
				Element xmlLang = wms.doc.createElement("LanguageLink") ;
				xmlLang.setAttribute("lang", lang) ;
				xmlLang.setAttribute("text", translations.get(lang)) ;
				xmlLangs.appendChild(xmlLang) ;
			}
			xmlArt.appendChild(xmlLangs) ;
		}
		

		SortedVector<Category> cats = article.getParentCategories() ;
		if (!cats.isEmpty()) {
			Category eq = article.getEquivalentCategory() ;
			
			Element xmlCats = wms.doc.createElement("CategoryList") ;
			
			for (Category c: cats) {
				Element xmlCat ;
				
				if (eq != null && c.equals(eq))
					xmlCat = wms.doc.createElement("EquivalentCategory") ;
				else
					xmlCat = wms.doc.createElement("Category") ;
				
				xmlCat.setAttribute("id", String.valueOf(c.getId())) ;
				xmlCat.setAttribute("title", c.getTitle()) ;
					
				xmlCats.appendChild(xmlCat) ;
			}
			xmlArt.appendChild(xmlCats) ;
		}
		
		int[] linksOut = article.getLinksOutIds() ;
		
		if (linksOut.length > 0) {
			
			Element xmlLinks = wms.doc.createElement("LinkOutList") ;
			xmlLinks.setAttribute("size", String.valueOf(linksOut.length)) ;
			int count = 0 ;
			
			for (int id: linksOut) {
				if (count++ == linkLimit) break ;
				
				try {
					Article link = new Article(wms.wikipedia.getDatabase(), id) ;	
				
					Element xmlLink = wms.doc.createElement("LinkOut") ;
				
					xmlLink.setAttribute("id", String.valueOf(link.getId())) ;
					xmlLink.setAttribute("title", link.getTitle()) ;
					xmlLink.setAttribute("relatedness", wms.df.format(link.getRelatednessTo(article))) ;
										
					xmlLinks.appendChild(xmlLink) ;
				} catch (Exception e) {} ;
			}
			xmlArt.appendChild(xmlLinks) ;
		}
		
		int[] linksIn = article.getLinksInIds() ;
		
		if (linksIn.length > 0) {
			
			Element xmlLinks = wms.doc.createElement("LinkInList") ;
			xmlLinks.setAttribute("size", String.valueOf(linksIn.length)) ;
			int count = 0 ;
			
			for (int id: linksIn) {
				if (count++ == linkLimit) break ;
				
				try {
					Article link = new Article(wms.wikipedia.getDatabase(), id) ;	
				
					Element xmlLink = wms.doc.createElement("LinkIn") ;
				
					xmlLink.setAttribute("id", String.valueOf(link.getId())) ;
					xmlLink.setAttribute("title", link.getTitle()) ;
					xmlLink.setAttribute("relatedness", wms.df.format(link.getRelatednessTo(article))) ;
										
					xmlLinks.appendChild(xmlLink) ;
				} catch (Exception e) {} ;
			}
			xmlArt.appendChild(xmlLinks) ;
		}
			
		return xmlArt ;
	}
	
	
	private Element getCategoryDetails(Page page, int linkLimit) throws SQLException{
		
		Category category = (Category)page ;
		
		Element xmlCat = wms.doc.createElement("Category") ;
		
		xmlCat.setAttribute("id", String.valueOf(category.getId())) ;
		xmlCat.setAttribute("title", category.getTitle()) ;

		String firstParagraph = null;
		try { 
			firstParagraph = category.getFirstParagraph() ;
			firstParagraph = wms.definer.formatDefinition(firstParagraph, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {};
		
		if (firstParagraph != null) 
			xmlCat.appendChild(wms.createElement("FirstParagraph", firstParagraph)) ;
		
		
		Article eq = category.getEquivalentArticle() ;
		if (eq != null) {
			
			Element xmlEq = wms.doc.createElement("EquivalentArticle") ;
			
			xmlEq.setAttribute("id", String.valueOf(eq.getId())) ;
			xmlEq.setAttribute("title", eq.getTitle()) ;
			
			xmlCat.appendChild(xmlEq) ;
		}
		
		xmlCat.appendChild(getCategoryListXml(category.getParentCategoryIds(), "ParentCategory", linkLimit)) ;
		
		xmlCat.appendChild(getCategoryListXml(category.getChildCategoryIds(), "ChildCategory", linkLimit)) ;		
		
		xmlCat.appendChild(getArticleListXml(category.getChildArticleIds(), "ChildArticle", linkLimit)) ;
		
		return xmlCat ;
	}
	
	private Element getDisambiguationDetails(Page page) throws SQLException{
		
		Disambiguation disambig = (Disambiguation)page ;
		
		Element xmlDmb = wms.doc.createElement("Disambiguation") ;
		
		xmlDmb.setAttribute("id", String.valueOf(disambig.getId())) ;
		xmlDmb.setAttribute("title", disambig.getTitle()) ;
		
		String firstParagraph = null;
		try { 
			firstParagraph = disambig.getFirstParagraph() ;
			firstParagraph = wms.definer.formatDefinition(firstParagraph, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {};
		
		if (firstParagraph != null) 
			xmlDmb.appendChild(wms.createElement("FirstParagraph", firstParagraph)) ;
		
		xmlDmb.setAttribute("description", getDescription(disambig)) ;
	
		
		Element xmlSenses = wms.doc.createElement("SenseList") ;
		
		for(SensePage sp:disambig.getSenses()) {
			Element xmlSense = wms.doc.createElement("Sense") ;
			xmlSense.setAttribute("id", String.valueOf(sp.getId())) ;
			xmlSense.setAttribute("title", sp.getTitle()) ;
			
			String firstSentence = null;
			try { 
				firstSentence = sp.getFirstSentence(null, null) ;
				firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
			} catch (Exception e) {} ;
			
			if (firstSentence != null) 
				xmlSense.appendChild(wms.createElement("FirstSentence", firstSentence)) ;
	
			xmlSenses.appendChild(xmlSense) ;
		}
		xmlDmb.appendChild(xmlSenses) ;
		
		return xmlDmb ;
	}
	
	private Element getRedirectDetails(Page page) throws SQLException{
		
		Redirect redirect = (Redirect)page ;
		
		Element xmlRedirect = wms.doc.createElement("Redirect") ;
		
		xmlRedirect.setAttribute("id", String.valueOf(redirect.getId())) ;
		xmlRedirect.setAttribute("title", redirect.getTitle()) ;
		
		Article target = redirect.getTarget() ;
		
		if (target == null) {
			xmlRedirect.appendChild(wms.doc.createTextNode("unresolved")) ;
		} else {
			Element xmlTarget = wms.doc.createElement("Target") ;
			xmlTarget.setAttribute("id", String.valueOf(target.getId())) ;
			xmlTarget.setAttribute("title", target.getTitle()) ;
						
			xmlRedirect.appendChild(xmlTarget) ;
		}
		
		return xmlRedirect ;
	}
	
	
	private Element getCategoryListXml(int[] categories, String tag, int linkLimit) {

		Element xmlPages = wms.doc.createElement(tag + "List") ;
		
		int count = 0 ;
		for (Integer id: categories) {
			
			if (count++ > linkLimit) break ;
			
			try {
				Category c = (Category) wms.wikipedia.getPageById(id) ;
			
				Element xmlPage = wms.doc.createElement(tag) ;

				xmlPage.setAttribute("id", String.valueOf(c.getId())) ;
				xmlPage.setAttribute("title", c.getTitle()) ;

				xmlPages.appendChild(xmlPage) ;
			} catch (Exception e) {}
		}
		return xmlPages ;
	}
	
	private Element getArticleListXml(int[] articles, String tag, int linkLimit) {

		Element xmlPages = wms.doc.createElement(tag + "List") ;
		
		int count = 0 ;
		for (Integer id: articles) {
			
			if (count++ > linkLimit) break ;
			
			try {
				Article a = (Article) wms.wikipedia.getPageById(id) ;
			
				Element xmlPage = wms.doc.createElement(tag) ;

				xmlPage.setAttribute("id", String.valueOf(a.getId())) ;
				xmlPage.setAttribute("title", a.getTitle()) ;

				xmlPages.appendChild(xmlPage) ;
				
			} catch (Exception e) {}
		}
		return xmlPages ;
	}
		
	
	private String getDescription(Page page) {
		if (page.getType() == Page.DISAMBIGUATION) {
			return "This is a disambiguation page, created to list the possible senses of the page's title\n" ;
		} else {
			try {			
				return wms.definer.formatDefinition(page.getFirstSentence(null, null), Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
			} catch (Exception e) {
				return "" ;
			} 
		}
	}		
}
