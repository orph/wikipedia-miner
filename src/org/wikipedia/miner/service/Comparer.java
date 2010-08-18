/*
 *    Comparer.java
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

import java.util.*;

import org.w3c.dom.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;


/**
 * This service measures the semantic relatedness between terms.
 * 
 *  @author David Milne
 */
public class Comparer {

	private WikipediaMinerServlet wms ;
	
	private boolean defaultShowDetails = false ;
	private int defaultMaxLinkCount = 250 ;

	/**
	 * Initializes a new Comparer
	 * @param wms the servlet that hosts this service
	 */
	public Comparer(WikipediaMinerServlet wms) {
		this.wms = wms;
	}
	
	/**
	 * @return false: the default behavior is to not show details of how terms are compared. 
	 */
	public boolean getDefaultShowDetails() {
		return defaultShowDetails ;
	}
	
	/**
	 * @return the default maximum number of links that are shown when providing details of how terms are compared.
	 */
	public int getDefaultMaxLinkCount() {
		return defaultMaxLinkCount ;
	}
	
	/**
	 * @return an Element description of this service; what it does, and what parameters it takes.
	 */
	public Element getDescription() {
		
		Element description = wms.doc.createElement("Description") ;
		description.setAttribute("task", "compare") ;
		
		description.appendChild(wms.createElement("Details", "<p>This service measures the semantic relatedness between two terms or a set of page ids. From this you can tell, for example, that New Zealand has more to do with <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=New Zealand&term2=Rugby\">Rugby</a> than <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=New Zealand&term2=Soccer\">Soccer</a>, or that Geeks are more into <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=Geek&term2=Computer Games\">Computer Games</a> than the <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=Geek&term2=Olympic Games\">Olympic Games</a> </p>"
				+ "<p>The relatedness measures are calculated from the links going into and out of each page. Links that are common to both pages are used as evidence that they are related, while links that are unique to one or the other indicate the opposite. The relatedness measure is symmetric, so comparing <i>a</i> to <i>b</i> is the same as comparing <i>b</i> to <i>a</i>. </p>" )) ;
		
		Element group1 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group1) ;
		
		Element paramTerm1 = wms.doc.createElement("Parameter") ;
		paramTerm1.setAttribute("name", "term1") ;
		paramTerm1.appendChild(wms.doc.createTextNode( "The first of two terms (or phrases) to compare.")) ;
		group1.appendChild(paramTerm1) ;
		
		Element paramTerm2 = wms.doc.createElement("Parameter") ;
		paramTerm2.setAttribute("name", "term2") ;
		paramTerm2.appendChild(wms.doc.createTextNode( "The second of two terms (or phrases) to compare.")) ;
		group1.appendChild(paramTerm2) ;
		
		Element group2 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group2) ;
		
		Element paramIds = wms.doc.createElement("Parameter") ;
		paramIds.setAttribute("name", "ids") ;
		paramIds.appendChild(wms.doc.createTextNode("A set of page ids to compare, delimited by commas. For efficiency, the results will be returned in comma delimited form rather than xml, with one line for each comparison.")) ;
		group2.appendChild(paramIds) ;
		
		Element paramShowDetails = wms.doc.createElement("Parameter") ;
		paramShowDetails.setAttribute("name", "showDetails") ;
		paramShowDetails.setAttribute("optional", "true") ;
		paramShowDetails.appendChild(wms.doc.createTextNode("Specifies whether the details of a relatedness comparison (all of the senses and links that were considered) will be shown. This is much more expensive than merely showing the result of the comparison, so please only obtain the details if you will use them.")) ;
		paramShowDetails.setAttribute("default", String.valueOf(getDefaultShowDetails())) ; 
		description.appendChild(paramShowDetails) ;
		
		Element paramLinkCount = wms.doc.createElement("Parameter") ;
		paramLinkCount.setAttribute("name", "maxLinkCount") ;
		paramLinkCount.setAttribute("optional", "true") ;
		paramLinkCount.appendChild(wms.doc.createTextNode("The maximum number of page links to return when presenting the details of a relatedness comparison.")) ;
		paramLinkCount.setAttribute("default", String.valueOf(getDefaultMaxLinkCount())) ; 
		description.appendChild(paramLinkCount) ;
		
		return description ;
	}

	//TODO: add a method for comparing a set of article ids.
	
	/**
	 * Measures the relatedness between two terms, and 
	 * 
	 * @param term1 the first term to compare
	 * @param term2 the second term to compare
	 * @param details true if the details of a relatedness comparison (all of the senses and links that were considered) are needed, otherwise false.
	 * @param linkLimit the maximum number of page links to return when presenting the details of a relatedness comparison.
	 * @return an Element message of how the two terms relate to each other
	 * @throws Exception
	 */
	public Element getRelatedness(String term1, String term2, boolean details, int linkLimit) throws Exception {

		Element response = wms.doc.createElement("RelatednessResponse") ;
		
		if (term1 == null || term2 == null) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
				
		TextProcessor tp = new CaseFolder() ;

		Anchor anchor1 = new Anchor(term1, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses1 = anchor1.getSenses() ; 

		if (senses1.size() == 0) {
			response.setAttribute("unknownTerm", term1) ; 
			return response ;
		}

		Anchor anchor2 = new Anchor(term2, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses2 = anchor2.getSenses() ; 

		if (senses2.size() == 0) {
			response.setAttribute("unknownTerm", term2) ; 
			return response ;
		}
		
		response.setAttribute("term1", term1) ;
		response.setAttribute("term2", term2) ;

		double sr = anchor1.getRelatednessTo(anchor2) ;

		response.setAttribute("relatedness", wms.df.format(sr)) ;

		if (!details)
			return response ;

		

		//now we get the details of how this was calculated

		double minProb = 0.01 ;
		double benchmark_relatedness = 0 ;
		double benchmark_distance = 0.40 ;

		SortedVector<CandidatePair> candidates = new SortedVector<CandidatePair>() ;

		int sensesA = 0 ;
		int sensesB = 0 ;

		for (Anchor.Sense sense1: anchor1.getSenses()) {

			if (sense1.getProbability() < minProb) break ;
			sensesA++ ;
			sensesB = 0 ;

			for (Anchor.Sense sense2: anchor2.getSenses()) {

				if (sense2.getProbability() < minProb) break ;
				sensesB++ ;

				double relatedness = sense1.getRelatednessTo(sense2) ;
				double obviousness = (sense1.getProbability() + sense2.getProbability()) / 2 ;

				if (relatedness > (benchmark_relatedness - benchmark_distance)) {

					//System.out.println(" - - likely candidate " + candidate + ", r=" + relatedness + ", o=" + sense.getProbability()) ;
					// candidate a likely sense
					if (relatedness > benchmark_relatedness + benchmark_distance) {
						//this has set a new benchmark of what we consider likely
						//System.out.println(" - - new benchmark") ;
						benchmark_relatedness = relatedness ;

						candidates.clear() ;
					}
					candidates.add(new CandidatePair(sense1, sense2, relatedness, obviousness), false) ;
				}
			}
		}

		CandidatePair bestSenses = candidates.first() ;

		
		Article art1 = bestSenses.senseA ;
		
		Element xmlSense1 = wms.doc.createElement("Sense1");
		
		
		xmlSense1.setAttribute("title", art1.getTitle()) ;
		xmlSense1.setAttribute("id", String.valueOf(art1.getId())) ;		
		xmlSense1.setAttribute("candidates", String.valueOf(senses1.size())) ;
		
		String firstSentence = null;
		try { 
			firstSentence = art1.getFirstSentence(null, null) ;
			firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {} ;
		
		if (firstSentence != null) 
			xmlSense1.appendChild(wms.createElement("FirstSentence", firstSentence)) ;

		response.appendChild(xmlSense1) ;
		
		
		Article art2 = bestSenses.senseB ;
		
		Element xmlSense2 = wms.doc.createElement("Sense2");
		xmlSense2.setAttribute("title", art2.getTitle()) ;
		xmlSense2.setAttribute("id", String.valueOf(art2.getId())) ;
		xmlSense2.setAttribute("candidates", String.valueOf(senses2.size())) ;
		
		firstSentence = null;
		try { 
			firstSentence = art2.getFirstSentence(null, null) ;
			firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {} ;
		
		if (firstSentence != null) 
			xmlSense2.appendChild(wms.createElement("FirstSentence", firstSentence)) ;
		
		response.appendChild(xmlSense2) ;
		
		
		//details of links coming in to these articles
		
		TreeSet<Integer> linksIn1 = new TreeSet<Integer>() ;
		for (Integer link:art1.getLinksInIds()) 
			linksIn1.add(link) ;
		
		TreeSet<Integer> linksIn2 = new TreeSet<Integer>() ;
		for (Integer link:art2.getLinksInIds()) 
			linksIn2.add(link) ;
		
		TreeSet<Integer> linksInShared = new TreeSet<Integer>() ;
		for (Integer id: linksIn1) {
			if (linksIn2.contains(id))
				linksInShared.add(id) ;
		}
		
		for (Integer id: linksInShared) {
			linksIn1.remove(id) ;
			linksIn2.remove(id) ;
		}
		
		Element xmlLinksIn = wms.doc.createElement("LinksIn") ; 
		xmlLinksIn.appendChild(getLinkListElement(linksInShared, "SharedLink", linkLimit)) ;
		xmlLinksIn.appendChild(getLinkListElement(linksIn1, "Link1", linkLimit)) ;
		xmlLinksIn.appendChild(getLinkListElement(linksIn2, "Link2", linkLimit)) ;
		response.appendChild(xmlLinksIn) ;
		
		//details of links going out from these articles
		
		TreeSet<Integer> linksOut1 = new TreeSet<Integer>() ;
		for (Integer link:art1.getLinksOutIds()) 
			linksOut1.add(link) ;
		
		TreeSet<Integer> linksOut2 = new TreeSet<Integer>() ;
		for (Integer link:art2.getLinksOutIds()) 
			linksOut2.add(link) ;
		
		TreeSet<Integer> linksOutShared = new TreeSet<Integer>() ;
		for (Integer id: linksOut1) {
			if (linksOut2.contains(id))
				linksOutShared.add(id) ;
		}
		
		for (Integer id: linksOutShared) {
			linksOut1.remove(id) ;
			linksOut2.remove(id) ;
		}
		
		Element xmlLinksOut = wms.doc.createElement("LinksOut") ; 
		xmlLinksOut.appendChild(getLinkListElement(linksOutShared, "SharedLink", linkLimit)) ;
		xmlLinksOut.appendChild(getLinkListElement(linksOut1, "Link1", linkLimit)) ;
		xmlLinksOut.appendChild(getLinkListElement(linksOut2, "Link2", linkLimit)) ;
		response.appendChild(xmlLinksOut) ;
		
		
		
		return response ;
	}
	
	private Element getLinkListElement(Collection<Integer> links, String tag, int linkLimit) {
		
		Element xmlLinks = wms.doc.createElement(tag + "List") ;
		xmlLinks.setAttribute("size", String.valueOf(links.size())) ;
		
		int count = 0 ;
		for (Integer link: links) {
			
			if (count++ >= linkLimit) break ;
			
			try {
				Article art = new Article(wms.wikipedia.getDatabase(), link) ;
				
				Element xmlLink = wms.doc.createElement(tag) ;
				xmlLink.setAttribute("id", String.valueOf(art.getId())) ;
				xmlLink.setAttribute("title", art.getTitle()) ;
				
				xmlLinks.appendChild(xmlLink) ;
			} catch (Exception e) {} ;
		}
		return xmlLinks ;
	}
	

	private class CandidatePair implements Comparable<CandidatePair> {

		Anchor.Sense senseA ;
		Anchor.Sense senseB ;
		double relatedness ;
		double obviousness ;

		public CandidatePair(Anchor.Sense senseA, Anchor.Sense senseB, double relatedness, double obviousness) {
			this.senseA = senseA ;
			this.senseB = senseB ;
			this.relatedness = relatedness ;
			this.obviousness = obviousness ;			
		}

		public int compareTo(CandidatePair cp) {
			return new Double(cp.obviousness).compareTo(obviousness) ;
		}

		public String toString() {
			return senseA + "," + senseB + ",r=" + relatedness + ",o=" + obviousness ;
		}
	}
}
