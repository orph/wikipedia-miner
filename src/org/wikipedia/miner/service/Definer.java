/*
 *    Definer.java
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

import org.w3c.dom.*;
import org.wikipedia.miner.model.* ;
import org.wikipedia.miner.util.* ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

/**
 * This services provides definitions for articles and categories from their first sentences or paragraphs, in either plain text, wiki format, or html.
 * It can also optionally obtain URLs for relevant images from <a href="http://www.freebase.org">FreeBase</a> 
 * <p>
 * You can only obtain definitions from unique page ids. If you want definitions for terms, then use the search service to identify the unique id first. 
 * 
 *  @author David Milne
 */
public class Definer {

	private WikipediaMinerServlet wms ;
	
	/**
	 * Only first sentences are used for definitions.
	 */
	public static int LENGTH_SHORT = 0 ;
	
	/**
	 * First paragraphs are used for definitions.
	 */
	public static int LENGTH_LONG = 1 ;
	
	/**
	 * Definitions are provided in plain text
	 */
	public static int FORMAT_PLAIN = 0 ;
	/**
	 * Definitions are provided in MediaWiki markup. Only links and basic formatting (bold and italic) will be shown.
	 */
	public static int FORMAT_WIKI = 1 ;
	/**
	 * Definitions are provided in HTML markup. Only links and basic formatting (bold and italic) will be shown.
	 */
	public static int FORMAT_HTML = 2 ;
	
	/**
	 * If using FORMAT_HTML, links will be reverted to plain text
	 */
	public static int LINK_NONE = 0 ;
	/**
	 * If using FORMAT_HTML, links will refer directly to the relevant wikipedia article.
	 */
	public static int LINK_DIRECT = 1 ;
	/**
	 * If using FORMAT_HTML, links will refer to the result of calling the WikipediaMiner searcher service.
	 */
	public static int LINK_TOOLKIT = 2 ;
	
	int defaultLength = LENGTH_SHORT ;
	int defaultFormat = FORMAT_HTML ;
	int defaultLinkDestination = LINK_DIRECT ;
	
	Pattern fb_imagePattern = Pattern.compile("\"image\"\\:\\[(.*?)\\]") ;
	Pattern fb_idPattern = Pattern.compile("\"id\"\\:\"(.*?)\"") ;
	
	/**
	 * @return the default length of definitions (LENGTH_SHORT).
	 */
	public int getDefaultLength() {
		return defaultLength;
	}

	/**
	 * @return the default format of definitions (FORMAT_HTML).
	 */
	public int getDefaultFormat() {
		return defaultFormat;
	}

	/**
	 * @return the default destination of links (LINK_DIRECT)
	 */
	public int getDefaultLinkDestination() {
		return defaultLinkDestination;
	}
	
	/**
	 * @return the default width (in pixels) for images
	 */
	public int getDefaultMaxImageWidth() {
		return 150 ;
	}
	
	/**
	 * @return the default height (in pixels) for images
	 */
	public int getDefaultMaxImageHeight() {
		return 150 ;
	}
	
	/**
	 * Initializes a new instance of Definer
	 * 
	 * @param wms the servlet that hosts this service
	 * @throws ServletException 
	 */
	public Definer(final WikipediaMinerServlet wms) throws  ServletException{
		this.wms = wms ;
		
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
			throw new ServletException ("WikipediaMiner | Definer: could not set up proxy authentication. Image urls will not be available.") ;
		}
	}
	
	/**
	 * @return an Element description of this service; what it does, and what parameters it takes.
	 */
	public Element getDescription() {
		
		Element description = wms.doc.createElement("Description") ;
		description.setAttribute("task", "define") ;
		
		description.appendChild(wms.createElement("Details", "<p>This services provides definitions for articles and categories from their first sentences or paragraphs, in either plain text, wiki format, or html. It can also optionally obtain URLs for relevant images from <a href=\"http://www.freebase.org\">FreeBase</a> </p>"
				+ "<p>You can only obtain definitions from unique page ids. If you want definitions for terms, then use the <a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&help\">search service</a> to identify the unique id first. </p>")) ; 			
				
		Element paramId = wms.doc.createElement("Parameter") ;
		paramId.setAttribute("name", "id") ;
		paramId.appendChild(wms.doc.createTextNode("The unique identifier of the page to define.")) ;
		description.appendChild(paramId) ;
		
		Element paramLength = wms.doc.createElement("Parameter") ;
		paramLength.setAttribute("name", "length") ;
		paramLength.appendChild(wms.doc.createTextNode("The required length of the definition; either <em>" + LENGTH_SHORT + "</em> (short, one sentence), or <em>" + LENGTH_LONG + "</em> (long, one paragraph).")) ;
		paramLength.setAttribute("optional", "true") ;
		paramLength.setAttribute("default", String.valueOf(getDefaultLength())) ; 
		description.appendChild(paramLength) ;
		
		Element paramFormat = wms.doc.createElement("Parameter") ;
		paramFormat.setAttribute("name", "format") ;
		paramFormat.appendChild(wms.doc.createTextNode("The desired format: <em>" + FORMAT_PLAIN + "</em> (plain text), <em>" + FORMAT_WIKI + "</em> (mediawiki markup), or <em>" + FORMAT_HTML + "</em> (html)")) ;
		paramFormat.setAttribute("optional", "true") ;
		paramFormat.setAttribute("default", String.valueOf(getDefaultFormat())) ; 
		description.appendChild(paramFormat) ;
		
		Element paramLink = wms.doc.createElement("Parameter") ;
		paramLink.setAttribute("name", "linkDestination") ;
		paramLink.appendChild(wms.doc.createTextNode("The destination of links when using html format, <em>" + LINK_NONE + "</em> (none, links revert to plain text), <em>" + LINK_DIRECT + "</em> (to wikipedia), or <em>" + LINK_TOOLKIT + "</em> (to the Wikipedia Miner search service)")) ;
		paramLink.setAttribute("optional", "true") ;
		paramLink.setAttribute("default", String.valueOf(getDefaultLinkDestination())) ; 
		description.appendChild(paramLink) ;
		
		Element paramGetImages = wms.doc.createElement("Parameter") ;
		paramGetImages.setAttribute("name", "getImages") ;
		paramGetImages.appendChild(wms.doc.createTextNode("Whether or not to retrieve relevant image urls from freebase")) ;
		paramGetImages.setAttribute("optional", "true") ;
		paramGetImages.setAttribute("default", String.valueOf(false)) ; 
		description.appendChild(paramGetImages) ;
		
		Element paramMaxImageWidth = wms.doc.createElement("Parameter") ;
		paramMaxImageWidth.setAttribute("name", "maxImageWidth") ;
		paramMaxImageWidth.appendChild(wms.doc.createTextNode("Images can be scaled. This defines their maximum width, in pixels. ")) ;
		paramMaxImageWidth.setAttribute("optional", "true") ;
		paramMaxImageWidth.setAttribute("default", String.valueOf(getDefaultMaxImageWidth())) ; 
		description.appendChild(paramMaxImageWidth) ;
		
		Element paramMaxImageHeight = wms.doc.createElement("Parameter") ;
		paramMaxImageHeight.setAttribute("name", "maxImageHeight") ;
		paramMaxImageHeight.appendChild(wms.doc.createTextNode("Images can be scaled. This defines their maximum height, in pixels. ")) ;
		paramMaxImageHeight.setAttribute("optional", "true") ;
		paramMaxImageHeight.setAttribute("default", String.valueOf(getDefaultMaxImageHeight())) ; 
		description.appendChild(paramMaxImageHeight) ;
		
		return description ;
	}
	
	/**
	 * Returns a definition of the wikipedia page defined by the given id. 
	 * 
	 * @param pageId the id of the wikipedia page to define.
	 * @param length the desired length of the definition (either LENGTH_SHORT or LENGTH_LONG)
	 * @param format the desired format of the definition (FORMAT_PLAIN, FORMAT_WIKI, FORMAT_HTML)
	 * @param linkDestination the desired destination of links if using FORMAT_HTML (LINK_NONE, LINK_DIRECT, LINK_TOOLKIT)
	 * @param getImages true if you want to retrieve URLs for relevant images from freebase, otherwise false
	 * @param maxImageWidth the maximum width of the images, in pixels. 
	 * @param maxImageHeight the maximum width of the images, in pixels. 
	 * @return the definition.
	 * @throws Exception
	 */
	public Element getDefinition(int pageId, int length, int format, int linkDestination, boolean getImages, int maxImageWidth, int maxImageHeight) throws Exception {
		
		Element response = wms.doc.createElement("DefinitionResponse") ;
		
		if (pageId < 0) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
	
		response.setAttribute("id", String.valueOf(pageId)) ;
		
		Page page = wms.wikipedia.getPageById(pageId) ;
		if (page==null) {
			response.setAttribute("unknownId", "true") ;
			return response ;
		}	
		
		response.setAttribute("title", page.getTitle()) ;
				
		if (length==LENGTH_SHORT) 
			response.appendChild(wms.createElement("Definition", formatDefinition(page.getFirstSentence(null, null), format, linkDestination))) ;
				
		if (length==LENGTH_LONG) 
			response.appendChild(wms.createElement("Definition", formatDefinition(page.getFirstParagraph(), format, linkDestination))) ;
		
		
		if (getImages) {
			//get image urls from freebase
			try {
				String freebaseResponse = getHtml("http://www.freebase.com/api/service/mqlread?query={\"query\":{\"key\":[{\"namespace\":\"/wikipedia/en_id\",\"value\":\"" + pageId + "\"}], \"type\":\"/common/topic\", \"article\":[{\"id\":null}], \"image\":[{\"id\":null}]}}") ;
				
				freebaseResponse = freebaseResponse.replaceAll("\\s", "") ;		
				Matcher m = fb_imagePattern.matcher(freebaseResponse) ;
				
				if (m.find()) {
					Matcher n = fb_idPattern.matcher(m.group(1)) ;
					while (n.find()) {
						
						//take all of the images
						Element xmlImage = wms.doc.createElement("Image") ;
						xmlImage.setAttribute("url", "http://www.freebase.com/api/trans/image_thumb" + n.group(1).replace("\\/", "/") + "?maxwidth=" + maxImageWidth + "&maxheight=" + maxImageHeight) ;
						response.appendChild(xmlImage) ;
						
					}
				}
			} catch (Exception e) {
				Element warning = wms.doc.createElement("Warning") ;
				warning.appendChild(wms.doc.createTextNode("Images are not available, because the Wikipedia Miner service has not been configured correctly.")) ;
				response.appendChild(warning) ;				
			}
		}
		
		return response ;
	}
	
	
	private String resolveFormatTags(String markup) {
		
		//replace bold tags
		Pattern p = Pattern.compile("'''([^']*?)'''", Pattern.DOTALL) ;
		Matcher m = p.matcher(markup) ;
		
		int lastPos = 0 ;
		StringBuffer sb = new StringBuffer() ;
		
		while(m.find()) {
			sb.append(markup.substring(lastPos, m.start())) ;
			sb.append("<b>") ;
			sb.append(m.group(1)) ;
			sb.append("</b>") ;			
			lastPos = m.end() ;		
		}
		
		sb.append(markup.substring(lastPos)) ;
		markup = sb.toString() ;
		
		//replace italic tags
		p = Pattern.compile("''([^']*?)''", Pattern.DOTALL) ;
		m = p.matcher(markup) ;
		
		lastPos = 0 ;
		sb = new StringBuffer() ;
		
		while(m.find()) {
			sb.append(markup.substring(lastPos, m.start())) ;
			sb.append("<i>") ;
			sb.append(m.group(1)) ;
			sb.append("</i>") ;			
			lastPos = m.end() ;		
		}
		
		sb.append(markup.substring(lastPos)) ;
		markup = sb.toString() ;
		
		return markup ;
	}
	
	
	protected String formatDefinition(String definition, int format, int linkDestination){
				
		if (format == FORMAT_PLAIN) {
			definition = MarkupStripper.stripLinks(definition) ;
			definition = MarkupStripper.stripFormatting(definition) ;
		}
		
		if (format == FORMAT_HTML) {
			definition = resolveFormatTags(definition) ; 
			definition = resolveFormatTags(definition) ; 
			
			//replace links
			Pattern p = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.DOTALL) ;
			Matcher m = p.matcher(definition) ;
			
			int lastPos = 0 ;
			StringBuffer sb = new StringBuffer() ;
			
			while(m.find()) {
				sb.append(definition.substring(lastPos, m.start())) ;
				
				String link = m.group(1) ;
				String anchor ;
				String dest ;
				
				int pos = link.lastIndexOf("|") ;
				
				if (pos >1) {
					dest = link.substring(0,pos) ;
					anchor = link.substring(pos+1) ;
				} else {
					dest = link ;
					anchor = link ;
				}
				
				Article art = wms.wikipedia.getArticleByTitle(dest) ;
				
				if (art == null || linkDestination == LINK_NONE) {
					sb.append(anchor) ;
				} else {
							
					if (linkDestination == LINK_DIRECT) 
						sb.append("<a href=\"http://www.en.wikipedia.org/wiki/" + art.getTitle() + "\">") ;
					else
						sb.append("<a href=\"" + wms.context.getInitParameter("service_name") + "?task=search&id=" + art.getId() + "&term=" + art.getTitle() + "\">") ;
					
					sb.append(anchor) ;
					sb.append("</a>") ;
				}
				lastPos = m.end() ;		
			}
			
			sb.append(definition.substring(lastPos)) ;
			definition = sb.toString() ;
		}
		
		definition = definition.replaceAll("\\[\\[", "") ;
		definition = definition.replaceAll("\\]\\]", "") ;
		
		return definition ;
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
}
