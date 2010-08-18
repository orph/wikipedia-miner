/*
 *    Wikifier.java
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

import java.io.* ;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.w3c.dom.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;
import org.wikipedia.miner.annotation.*;
import org.wikipedia.miner.annotation.preprocessing.*;
import org.wikipedia.miner.annotation.tagging.*;
import org.wikipedia.miner.annotation.weighting.*;

/**
 * @author David Milne
 * 
 * <p> This service automatically detects the topics mentioned in the given document, and provides links to the appropriate Wikipedia articles. </p>" 
 *	<p> It doesn't just use Wikipedia as a source of information to link to, but also as training data for how best to do it. In other words, it has been trained to make the same decisions as the people who edit Wikipedia. </p>
 *	<p> It may not work very well if the document does not fit the model of what it has been trained on. Documents should not be too short, and should be dedicated to a particular topic.</p>
 */
public class Wikifier {

	private WikipediaMinerServlet wms ;
	
	private Disambiguator disambiguator ;
	private TopicDetector topicDetector ;
	private LinkDetector linkDetector ;

	//private String homePage  ;
	private String errorPage ;
	private String lostPage  ;

	private double defaultMinProbability = 0.5 ;
	private int defaultRepeatMode = DocumentTagger.FIRST_IN_REGION ;
	private boolean defaultShowTooltips = false ;
	
	private DecimalFormat df = new DecimalFormat("#0.000000") ;
	
	/**
	 * The type of the source document will be detected automatically
	 */
	public static final int SOURCE_AUTODETECT = 0 ;
	
	/**
	 * The source document is a url
	 */
	public static final int SOURCE_URL = 1 ;
	
	
	/**
	 * The source document is a snippet of html
	 */
	public static final int SOURCE_HTML = 2 ;
	
	
	/**
	 * The source document is a snippet of mediawiki markup
	 */
	public static final int SOURCE_WIKI = 3 ;
	
	/**
	 * Initializes a new instance of Wikifier.
	 *
	 * @param wms the servlet that hosts this service
	 * @param tp an (optional) text processor to use for modifying how text is compared to anchors in Wikipedia.
	 * @throws ServletException
	 */
	public Wikifier(WikipediaMinerServlet wms, TextProcessor tp) throws ServletException {
		
		this.wms = wms ;
		//homePage = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"> <html><head> <title>MW Wikifier</title> <link rel=\"stylesheet\" href=\"css/style.css\" type=\"text/css\"></head><body><p>Welcome to the Wikipedia Miner wikifier service. This service automatically augments web pages with links to relevant Wikipedia topics. It doesn't just use Wikipedia as a source of information to link to, but also as training data for how best to do it. In other words, it has been trained to make the same decisions as the people who edit Wikipedia. </p> <p>Try it out! Just enter a url above, or drag this <a href=\"javascript:void(document.location='" + wms.context.getInitParameter("server_path") + "wikifier?url='+escape(document.location)')\" target=\"_top\">Wikifier</a> link into your bookmarks and you will be able to wikify pages with the click of a button.</p></body></html>" ;
		errorPage = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"> <html><head> <title>MW Wikifier | error</title> <link rel=\"stylesheet\" href=\"css/style.css\" type=\"text/css\"></head><body><p><em>Ooops!</em></p><p>I've run into a problem while processing this document. Can you try a different one?</p></body></html>" ;
		lostPage = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"> <html><head> <title>MW Wikifier | error</title> <link rel=\"stylesheet\" href=\"css/style.css\" type=\"text/css\"></head><body><p><em>Ooops!</em></p><p>I couldn't find the page you wanted to wikify. Can you please check that the url is correct, or try a different one?</p></body></html>" ;

		
		
		try {	
			disambiguator = new Disambiguator(wms.wikipedia, tp, 0.01, 0, 25) ;
			disambiguator.loadClassifier(new File(wms.context.getInitParameter("wikifier_disambigModel"))) ;
		} catch (Exception e) {
			throw new ServletException("WikipediaMiner | could not retrieve disambiguation model for wikification (" + wms.context.getInitParameter("wikifier_disambigModel") + ")") ;
		}
		
		File stopwordFile = null;
		if ( wms.context.getInitParameter("stopword_file") != null) {
			stopwordFile = new File(wms.context.getInitParameter("stopword_file")) ;
		}

		try {
			topicDetector = new TopicDetector(wms.wikipedia, disambiguator, stopwordFile, true, false) ;
		} catch (IOException e) {
			throw new ServletException("WikipediaMiner | could not retrieve stopwords for wikification (" + wms.context.getInitParameter("stopword_file")  + ")") ;
		}

		try {
			linkDetector = new LinkDetector(wms.wikipedia) ;
			linkDetector.loadClassifier(new File(wms.context.getInitParameter("wikifier_linkModel"))) ;
		} catch (Exception e) {
			throw new ServletException("WikipediaMiner | could not retrieve link detection model for wikification") ;
		}


		try {
			String proxyHost = wms.context.getInitParameter("proxy_host") ;
			String proxyPort = wms.context.getInitParameter("proxy_port") ;
			
			Properties systemSettings = System.getProperties();
			if (proxyHost != null)
				systemSettings.put("http.proxyHost", proxyHost) ;
			
			if (proxyPort != null)
				systemSettings.put("http.proxyPort", proxyPort) ;

			final String proxyUser = wms.context.getInitParameter("proxy_user") ;
			final String proxyPassword = wms.context.getInitParameter("proxy_password") ;
			
			if (proxyUser != null && proxyPassword != null) {
				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
					}
				});
			}

		} catch (Exception e) {
			throw new ServletException ("WikipediaMiner | could not set up proxy authentication.") ;
		}
	}
	
	/**
	 * @return the default predicted link probability below which topics are not considered worth linking to. 
	 */
	public double getDefaultMinProbability() {
		return defaultMinProbability;
	}

	/**
	 * @return the default behavior for how repeat mentions of the same topic are handled.
	 */
	public int getDefaultRepeatMode() {
		return defaultRepeatMode;
	}

	/**
	 * @return the default setting for whether tool-tips are generated for links or not
	 */
	public boolean getDefaultShowTooltips() {
		return defaultShowTooltips;
	}

	/**
	 * @return an Element description of this service; what it does, and what parameters it takes.
	 */
	public Element getDescription() {
		
		Element description = wms.doc.createElement("Description") ;
		description.setAttribute("task", "wikify") ;
		
		description.appendChild(wms.createElement("Details", "<p>This service automatically detects the topics mentioned in the given document, and provides links to the appropriate Wikipedia articles. </p>" 
				+ "<p> It doesn't just use Wikipedia as a source of information to link to, but also as training data for how best to do it. In other words, it has been trained to make the same decisions as the people who edit Wikipedia. </p>"
				+ "<p> It may not work very well if the document does not fit the model of what it has been trained on. Documents should not be too short, and should be dedicated to a particular topic.</p>" )) ; 	
		
		Element paramSource = wms.doc.createElement("Parameter") ;
		paramSource.setAttribute("name", "source") ;
		paramSource.appendChild(wms.doc.createTextNode("The document to be wikified (either it's content or a web-accessible URL)")) ;
		description.appendChild(paramSource) ;
		
		Element paramSourceMode = wms.createElement("Parameter", "the type of the source document: <em>" + SOURCE_AUTODETECT + "</em> (detect automatically), <em>" + SOURCE_URL + "</em> (url), <em>" + SOURCE_HTML + "</em> (HTML markup), or <em>" + SOURCE_WIKI + "</em> (MediaWiki markup)") ;
		paramSourceMode.setAttribute("name", "sourceMode") ;
		paramSourceMode.setAttribute("optional", "true") ;
		paramSourceMode.setAttribute("default", "0") ; 
		description.appendChild(paramSourceMode) ;
		
		Element paramWrap = wms.doc.createElement("Parameter") ;
		paramWrap.setAttribute("name", "wrapInXml") ;
		paramWrap.appendChild(wms.doc.createTextNode("Whether to wrap the result in descriptive xml, or simply return it directly")) ;
		paramWrap.setAttribute("optional", "true") ;
		paramWrap.setAttribute("default", "true") ; 
		description.appendChild(paramWrap) ;
		
		Element paramMinProbability = wms.doc.createElement("Parameter") ;
		paramMinProbability.setAttribute("name", "minProbability") ;
		paramMinProbability.appendChild(wms.doc.createTextNode("The system calculates a probability for each topic of whether a Wikipedian would consider it interesting enough to link to. This parameter specifies the minimum probability a topic must have before it will be linked. ")) ;
		paramMinProbability.setAttribute("optional", "true") ;
		paramMinProbability.setAttribute("default", String.valueOf(getDefaultMinProbability())) ; 
		description.appendChild(paramMinProbability) ;
		
		Element paramRepeatMode = wms.createElement("Parameter", "Specifies whether repeat mentions of the same topic will be linked or ignored: <em>" + DocumentTagger.ALL + "</em> (all mentions are linked), <em>" + DocumentTagger.FIRST + "</em> (only first mention is linked), or <em>" + DocumentTagger.FIRST_IN_REGION + "</em> (only first mention in each region of the page is linked).") ;
		paramRepeatMode.setAttribute("name", "repeatMode") ;
		paramRepeatMode.setAttribute("optional", "true") ;
		paramRepeatMode.setAttribute("default", String.valueOf(getDefaultRepeatMode())) ; 
		description.appendChild(paramRepeatMode) ;
		
		Element paramBannedTopics = wms.doc.createElement("Parameter") ;
		paramBannedTopics.setAttribute("name", "bannedTopics") ;
		paramBannedTopics.appendChild(wms.doc.createTextNode("A list of topics (titles or ids, separated by ';') that are not allowed to be linked to. ")) ;
		paramBannedTopics.setAttribute("optional", "true") ;
		paramBannedTopics.setAttribute("default", "null") ; 
		description.appendChild(paramBannedTopics) ;
		
		Element paramLinkColor = wms.doc.createElement("Parameter") ;
		paramLinkColor.setAttribute("name", "linkColor") ;
		paramLinkColor.appendChild(wms.doc.createTextNode("Specifies color of the added links. This can be specified in any format that would be recognized by css.")) ;
		paramLinkColor.setAttribute("optional", "true") ;
		paramLinkColor.setAttribute("default", "null, - link color dictated by page style") ; 
		description.appendChild(paramLinkColor) ;
		
		Element paramBaseColor = wms.doc.createElement("Parameter") ;
		paramBaseColor.setAttribute("name", "baseColor") ;
		paramBaseColor.appendChild(wms.doc.createTextNode("Allows link color to fade into a specified base color if their probability of interest is low. For this to work, both baseColor and and linkColor must be specified as rgb values")) ;
		paramBaseColor.setAttribute("optional", "true") ;
		paramBaseColor.setAttribute("default", "null, - the same linkColor is applied to all links, regardless of thier probability") ; 
		description.appendChild(paramBaseColor) ;
		
		Element paramTooltips = wms.doc.createElement("Parameter") ;
		paramTooltips.setAttribute("name", "showTooltips") ;
		paramTooltips.appendChild(wms.doc.createTextNode("Specifies whether tooltips will be added to describe each link. This is only valid when processing urls, and javascript must be enabled for the tooltips to work.")) ;
		paramTooltips.setAttribute("optional", "true") ;
		paramTooltips.setAttribute("default", String.valueOf(getDefaultShowTooltips())) ; 
		description.appendChild(paramTooltips) ;
		
		return description ;
	}
	
	/**
	 * Wikifies the source document by augmenting it with links to relevant wms.wikipedia articles. The source can specify the content of the document directly as 
	 * HTML or MediaWiki markup, or as a url from which the content can be obtained (in which case HTML markup is assumed). 
	 * <p>
	 * The result is returned in the original markup, wrapped in an xml message which also describes the source document, paramaters, and detected topics.. 
	 * 
	 * @param source the document to be wikified (either it's content or a web-accessible URL)
	 * @param sourceMode the type of the source document (SOURCE_URL, SOURCE_WIKI, SOURCE_HTML or SOURCE_AUTODETECT)
	 * @param minProbability The system calculates a probability for each topic of whether a Wikipedian would consider it interesting enough to link to. This parameter specifies the minimum probability a topic must have before it will be linked.
	 * @param repeatMode Specifies whether repeat mentions of the same topic will be linked or ignored (see DocumentTagger)
	 * @param bannedTopics A list of topics (titles or ids, seperated by ';') that are not allowed to be linked to. 
	 * @param baseColor allows link color to fade into this specified base color if the probability of interest is low. For this to work, both baseColor and and linkColor must be specified as rgb values
	 * @param linkColor the color of the links to be added, in any format that would be recognized by css. 
	 * @param showTooltips true if tooltips are to be provided for html links, otherwise false (not available in wiki markup). 
	 * @return an xml message containing markup of the given document, augmented with links to the relevant Wikipedia articles.
	 * @throws Exception
	 */
	public Element wikifyAndWrapInXML(String source, int sourceMode, double minProbability, int repeatMode, String bannedTopics, String baseColor, String linkColor, boolean showTooltips) throws Exception {
		
		
		Element xmlResponse = wms.doc.createElement("WikifierResponse") ;
		xmlResponse.setAttribute("minProbability", df.format(minProbability)) ;
		xmlResponse.setAttribute("repeatMode", String.valueOf(repeatMode)) ;
		xmlResponse.setAttribute("sourceMode", String.valueOf(sourceMode)) ;
		if (bannedTopics == null)
			xmlResponse.setAttribute("bannedTopics", "") ;
		else
			xmlResponse.setAttribute("bannedTopics", bannedTopics)  ;
		
		
		if (source==null || source.trim() == "")
			return xmlResponse ;
		
		Element xmlSource = wms.doc.createElement("Source") ;
		xmlSource.appendChild(wms.doc.createTextNode(source)) ;
		xmlResponse.appendChild(xmlSource) ;
		
		if (sourceMode < SOURCE_URL || sourceMode > SOURCE_WIKI)
			sourceMode = resolveSourceMode(source) ;
		
		
		SortedVector<Topic> detectedTopics = new SortedVector<Topic>() ;
		
		Element xmlResult = wms.createElement("Result", wikifyAndGatherTopics(source, sourceMode, minProbability, repeatMode, bannedTopics, baseColor, linkColor, showTooltips, detectedTopics)) ;
		xmlResponse.appendChild(xmlResult) ;
		if (sourceMode == SOURCE_URL) 
			xmlResult.setAttribute("ouputMode", String.valueOf(SOURCE_HTML)) ;
		else
			xmlResult.setAttribute("outputMode", String.valueOf(sourceMode)) ;
		
		double docScore = 0.0 ;
		for (Topic t:detectedTopics)
			docScore = docScore + t.getRelatednessToOtherTopics() ;
		
		xmlResult.setAttribute("documentScore", df.format(docScore)) ;
		
		Element xmlDetectedTopicList = wms.doc.createElement("DetectedTopicList") ;
		xmlResponse.appendChild(xmlDetectedTopicList) ;

		for (Article dt:detectedTopics) {
			if (dt.getWeight() < minProbability) break ;

			Element detectedTopic = wms.doc.createElement("DetectedTopic") ;
			detectedTopic.setAttribute("id", String.valueOf(dt.getId())) ;
			detectedTopic.setAttribute("title", dt.getTitle()) ;
			detectedTopic.setAttribute("weight", df.format(dt.getWeight())) ;

			xmlDetectedTopicList.appendChild(detectedTopic) ;
		}
		
		
		return xmlResponse ;
	}
	
	
	/**
	 * Wikifies the source document by augmenting it with links to relevant wms.wikipedia articles. The source can specify the content of the document directly as 
	 * HTML or MediaWiki markup, or as a url from which the content can be obtained (in which case HTML markup is assumed). 
	 * <p>
	 * The result is returned in the original markup. 
	 * 
	 * @param source the document to be wikified (either it's content or a web-accessible URL)
	 * @param sourceMode the type of the source document (SOURCE_URL, SOURCE_WIKI, SOURCE_HTML or SOURCE_AUTODETECT)
	 * @param minProbability The system calculates a probability for each topic of whether a Wikipedian would consider it interesting enough to link to. This parameter specifies the minimum probability a topic must have before it will be linked.
	 * @param repeatMode Specifies whether repeat mentions of the same topic will be linked or ignored (see DocumentTagger)
	 * @param bannedTopics A list of topics (titles or ids, seperated by ';') that are not allowed to be linked to. 
	 * @param baseColor allows link color to fade into this specified base color if the probability of interest is low. For this to work, both baseColor and and linkColor must be specified as rgb values
	 * @param linkColor the color of the links to be added, in any format that would be recognized by css. 
	 * @param showTooltips true if tooltips are to be provided for html links, otherwise false (not available in wiki markup). 
	 * @return the markup of the given document, augmented with links to the relevant Wikipedia articles.
	 */
	public String wikify(String source, int sourceMode, double minProbability, int repeatMode, String bannedTopics, String baseColor, String linkColor, boolean showTooltips) {
		
		try {
			return wikifyAndGatherTopics(source, sourceMode, minProbability, repeatMode, bannedTopics, baseColor, linkColor, showTooltips, new SortedVector<Topic>()) ;
		
		} catch (IOException e) {
			return lostPage ;
		} catch (Exception e) {
			if (sourceMode == SOURCE_HTML || sourceMode == SOURCE_URL) 
				return errorPage ;
			else
				return "Ooops!</em></p><p>I've run into a problem while processing this document. Can you try a different one?" ;
		}
	}
	
	private Vector<Article> resolveTopicList(String topicList) {
		
		Vector<Article> topics = new Vector<Article>() ;
		
		if (topicList == null || topicList.equals("")) 
			return topics ;
				
		for (String t:topicList.split(";")) {
			try {
				// try it as an id first
				Integer id = Integer.parseInt(t) ;
				topics.add(new Article(wms.wikipedia.getDatabase(), id)) ;
			} catch (Exception e) {
				// if that fails, try as an article title				
				Article art = wms.wikipedia.getArticleByTitle(t.trim()) ;
				if (art != null)
					topics.add(art) ;				
			}
		}
		
		return topics ;
	}
	
	
	private String wikifyAndGatherTopics(String source, int sourceMode, double minProbability, int repeatMode, String bannedTopics, String baseColor, String linkColor, boolean showTooltips, SortedVector<Topic> detectedTopics) throws IOException, Exception {
		
		if (source == null || source.trim().equals(""))
			return "" ;
		
		if (sourceMode < SOURCE_URL || sourceMode > SOURCE_WIKI)
			sourceMode = resolveSourceMode(source) ;
		
		Vector<Article> bannedTopicList = resolveTopicList(bannedTopics) ;
		
		DocumentPreprocessor dp ;
		DocumentTagger dt ;
		
		if (sourceMode == SOURCE_WIKI) {
			dp = new WikiPreprocessor(wms.wikipedia) ;
			dt = new MyWikiTagger(linkColor, baseColor) ;
		} else {
			dp = new HtmlPreprocessor() ;
			dt = new MyHtmlTagger(linkColor, baseColor, showTooltips) ;
		}
		
		String markup = source ;
		if (sourceMode == SOURCE_URL) {
						
			if (!source.startsWith("http://"))
				source = "http://" + source ;

			markup = null ;
			try {
				markup = getHtml(source) ;
			} catch (Exception e) {
				throw new IOException() ;			
			}

			if (markup == null)
				throw new IOException() ;
		}
		
		PreprocessedDocument doc = dp.preprocess(markup) ;
		
		for (Article bt: bannedTopicList) 
			doc.banTopic(bt.getId()) ;
		
		//TODO: find smarter way to resolve this hack, which stops wikifier from detecting "Space (punctuation)" ;
		doc.banTopic(143856) ;
		
		SortedVector<Topic> allTopics = linkDetector.getWeightedTopics(topicDetector.getTopics(doc, null)) ;
		SortedVector<Topic> bestTopics = new SortedVector<Topic>() ;
		for (Topic t:allTopics) {
			if (t.getWeight() >= minProbability)
				bestTopics.add(t, true) ;
			
			detectedTopics.add(t, true) ;
		}
		
		String taggedText = dt.tag(doc, bestTopics, repeatMode) ;
		
		if (sourceMode == SOURCE_URL) {
			taggedText = taggedText.replaceAll("(?i)<html", "<base href=\"" + source  + "\" target=\"_top\"/><html") ;
			
				if (showTooltips) {
					taggedText = taggedText.replaceAll("(?i)</head", "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + wms.context.getInitParameter("server_path") + "wikifier/tooltips/styles.css\"/>\n</head") ;
					taggedText = taggedText.replaceAll("(?i)</head", "<script type=\"text/javascript\" src=\"" + wms.context.getInitParameter("server_path") + "wikifier/tooltips/script.js\"></script>\n</head") ;
			
					taggedText = taggedText.replaceAll("(?i)</body", "<div class=\"wms.wikipediaminerTooltip\" id=\"wmtt\"></div>\n</body") ;		
				}
		}		
		
		return taggedText ;
	}
	
	private int resolveSourceMode(String source) {
		
		String s = source.trim().toLowerCase() ;
		
		if (s.startsWith("www.") || s.startsWith("http://")) 
			return SOURCE_URL ;
		
		int htmlCount = 0 ;
		Pattern htmlTag = Pattern.compile("<(.*?)>") ;
		Matcher m = htmlTag.matcher(source) ;
		while (m.find())
			htmlCount++ ;
		
		int wikiCount = 0 ;
		Pattern wikiTag = Pattern.compile("\\[\\[(.*?)\\]\\]") ;
		m = wikiTag.matcher(source) ;
		while (m.find())
			wikiCount++ ;
		
		if (htmlCount > wikiCount)
			return SOURCE_HTML ;
		else
			return SOURCE_WIKI ;
	}
	
	private String getHtml(String url) throws Exception {
		

		URL u = new URL(url);
		HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.setInstanceFollowRedirects(true) ;
		
		BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8")) ;
		String line ;
		
		StringBuffer content = new StringBuffer() ;
		
		while ((line=input.readLine())!=null) {
			
			content.append(line + "\n") ;
		}
			
		return content.toString() ;
	}
	
	private class MyWikiTagger extends WikiTagger {
		
		String linkColor ;
		String baseColor ;
				
		MyWikiTagger(String linkColor, String baseColor) {		
			this.linkColor = linkColor ;
			this.baseColor = baseColor ;
		}
		
		public String getTag(String anchor, Topic topic) {
			
			StringBuffer sb = new StringBuffer() ;
			
			String style = "border:1px solid black ; " ;
			
			if (linkColor != null) 
				if (baseColor != null) 				
					style = style + "color:" + getColor(linkColor, baseColor, topic.getWeight()) + "; " ;
				else
					style = style + "color:" + linkColor + "; " ;
			
			if (linkColor != null)
				sb.append("<span style=\"" + style + "\">&nbsp;") ;
			
			if (topic.getTitle().compareToIgnoreCase(anchor) == 0)
				sb.append("[[" + anchor + "]]") ;
			else
				sb.append("[[" + topic.getTitle() + "|" + anchor + "]]") ;
			
			if (linkColor != null)
				sb.append("&nbsp;</span>") ;
			
			return sb.toString() ;
		}
	}
	

	private class MyHtmlTagger extends HtmlTagger{

		String linkColor ;
		String baseColor ;
		boolean showTooltips = true ;
		
		int index = 0 ;

		public MyHtmlTagger(String linkColor, String baseColor) {		
			this.linkColor = linkColor ;
			this.baseColor = baseColor ;
		}
		
		public MyHtmlTagger(String linkColor, String baseColor, boolean showTooltips) {		
			this.linkColor = linkColor ;
			this.baseColor = baseColor ;
			this.showTooltips = showTooltips ;
		}
		
		public String getTag(String anchor, Topic topic) {
			index++ ;
			
			String style = "" ;
			
			if (linkColor != null) 
				if (baseColor != null) 				
					style = "style=\"color:" + getColor(linkColor, baseColor, topic.getWeight()) + "\"" ;
				else
					style = "style=\"color:" + linkColor + "\"" ;
			
				
			if (showTooltips)
				return "<a id=\"wmwtt_" + index + "\" " + style + " href=\"http://www.en..wikipedia.org/wiki/" + topic.getTitle() + "\" onmouseover=\"wmw_showTooltip(" + index + ", " + topic.getId() + ", '" + wms.context.getInitParameter("server_path") + "', '" + wms.context.getInitParameter("service_name") + "')\" onmouseout=\"wmw_hideTooltip()\">" + anchor + "</a>" ;
			else
				return "<a " + style + " href=\"http://www.en.wikipedia.org/wiki/" + topic.getTitle() + "\">" + anchor + "</a>" ;
		}	
	}
	
	private String getColor(String baseColor, String linkColor, double saturation) {
		
		try {
			int[] rgbBase = getRgbValues(baseColor) ;
			int[] rgbLink = getRgbValues(linkColor) ;
		
			long r = extrapolateColorVal(rgbBase[0], rgbLink[0], saturation) ;
			long g = extrapolateColorVal(rgbBase[1], rgbLink[1], saturation) ;
			long b = extrapolateColorVal(rgbBase[2], rgbLink[2], saturation) ;
			
			return "rgb(" + r + "," + g + "," + b + ")" ;
		} catch (Exception e) {
			e.printStackTrace() ;
			return linkColor ;
		}
		
	}
	
	private int[] getRgbValues(String color) {
		
		int pos1 = color.indexOf("(") ;
		int pos2 = color.indexOf(")") ;
	
		String values[] = color.substring(pos1+1, pos2).split(",") ;
	
		int[] rgb = new int[3] ;
		rgb[0] = Integer.parseInt(values[0]) ;
		rgb[1] = Integer.parseInt(values[1]) ;
		rgb[2] = Integer.parseInt(values[2]) ;
		
		return rgb ;
	}
	
	private int extrapolateColorVal(int base, int link, double weight) {
		
		if (link > base)
			return (int) Math.round(base + ((link - base) * weight)) ;
		else
			return (int) Math.round(base - ((base - link) * weight)) ;
	}
}
