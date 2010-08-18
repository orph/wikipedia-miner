/*
 *    WikipediaMinerServlet.java
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

import gnu.trove.TIntHashSet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.* ;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * @author David Milne
 *
 * This class provides a hub for WikipediaMiner services. All services are routed through this servlet, so that they can all share the same cached data. 
 */
public class WikipediaMinerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected ServletContext context ;

	protected Wikipedia wikipedia ;
	private CacherThread cachingThread ;

	protected Comparer comparer ;
	protected Searcher searcher ;
	protected Definer definer ;
	protected Wikifier wikifier ;

	private HashMap<String,Transformer> transformersByName ;
	DOMParser parser = new DOMParser() ;
	protected Document doc = new DocumentImpl();
	protected DecimalFormat df = new DecimalFormat("#0.000000") ;


	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		context = config.getServletContext() ;

		TextProcessor tp = new CaseFolder() ; 

		try {
			wikipedia = new Wikipedia(context.getInitParameter("mysql_server"), context.getInitParameter("mysql_database"), context.getInitParameter("mysql_user"), context.getInitParameter("mysql_password")) ;
		} catch (Exception e) {
			throw new ServletException("Could not connect to wikipedia database.") ;
		}

		//Escaper escaper = new Escaper() ;

		definer = new Definer(this) ;
		comparer = new Comparer(this) ;
		searcher = new Searcher(this) ;
		
		try {
			wikifier = new Wikifier(this, tp) ;
			
		} catch (Exception e) {
			System.err.println("Could not initialize wikifier") ;			
		}
		
		try {
			File dataDirectory = new File(context.getInitParameter("data_directory")) ;

			if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
				throw new Exception() ;
			}

			cachingThread = new CacherThread(dataDirectory, tp) ;
			cachingThread.start() ;
		} catch (Exception e) {
			throw new ServletException("Could not locate wikipedia data directory.") ;
		}

		try {
			TransformerFactory tf = TransformerFactory.newInstance();

			transformersByName = new HashMap<String,Transformer>() ;
			transformersByName.put("help", buildTransformer("help", new File("/research/wikipediaminer/web/xsl"), tf)) ;
			transformersByName.put("loading", buildTransformer("loading", new File("/research/wikipediaminer/web/xsl"), tf)) ;
			transformersByName.put("search", buildTransformer("search", new File("/research/wikipediaminer/web/xsl"), tf)) ;
			transformersByName.put("compare", buildTransformer("compare", new File("/research/wikipediaminer/web/xsl"), tf)) ;
			transformersByName.put("wikify", buildTransformer("wikify", new File("/research/wikipediaminer/web/xsl"), tf)) ;
			
			Transformer serializer = TransformerFactory.newInstance().newTransformer();
			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			serializer.setOutputProperty(OutputKeys.METHOD,"xml");
			serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			transformersByName.put("serializer", serializer) ;


		} catch (Exception e) {
			throw new ServletException("Could not load xslt library.") ;
		}
	}


	private Transformer buildTransformer(String name, File xslDir, TransformerFactory tf) throws Exception {

		Transformer tr = tf.newTransformer(new StreamSource(new FileReader(xslDir.getAbsolutePath() + File.separatorChar + name + ".xsl"))) ;
		tr.setOutputProperty(OutputKeys.INDENT, "yes");
		tr.setOutputProperty(OutputKeys.METHOD,"html");
		tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

		return tr ;
	}



	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		doGet(request, response) ;

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		try {


			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF-8") ;

			String task = request.getParameter("task") ;

			


			Element data = null ;
			

			//process help request
			if (request.getParameter("help") != null) 
				data = getDescription(task) ;

			//redirect to home page if there is no task
			if (data==null && task==null) {
				response.setContentType("text/html");
				response.getWriter().append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html><head><meta http-equiv=\"REFRESH\" content=\"0;url=" + context.getInitParameter("server_path") + "></head><body></body></html>") ;
				return ;
			}

			//process definition request
			if (data==null && task.equals("define")) {
				int id = resolveIntegerArg(request.getParameter("id"), -1) ; 
				int length = resolveIntegerArg(request.getParameter("length"), definer.getDefaultLength()) ;
				int format = resolveIntegerArg(request.getParameter("format"), definer.getDefaultFormat()) ; 
				int maxImageWidth = resolveIntegerArg(request.getParameter("maxImageWidth"), definer.getDefaultMaxImageWidth()) ; 
				int maxImageHeight = resolveIntegerArg(request.getParameter("maxImageHeight"), definer.getDefaultMaxImageHeight()) ;
				int linkDestination = resolveIntegerArg(request.getParameter("linkDestination"), definer.getDefaultLinkDestination()) ;
				boolean getImages = resolveBooleanArg(request.getParameter("getImages"), false) ;

				data = definer.getDefinition(id, length, format, linkDestination, getImages, maxImageWidth, maxImageHeight) ;				
			}

			
			//all of the remaining tasks require data to be cached, so lets make sure that is finished before continuing.
			if (!cachingThread.isOk())
				throw new ServletException("Could not cache wikipedia data") ;

			double progress = cachingThread.getProgress() ;
			if (data==null && (progress < 1 || task.equals("progress"))) {
				//still caching up data, not ready to return a response yet.

				data = doc.createElement("loading") ;
				data.setAttribute("progress", df.format(progress)) ;
				task = "loading" ;
			}
			
			//process search request
			if (data==null && task.equals("search")) {
				String term = request.getParameter("term") ;
				String id = request.getParameter("id") ;
				int linkLimit = resolveIntegerArg(request.getParameter("linkLimit"), searcher.getDefaultMaxLinkCount()) ;
				int senseLimit = resolveIntegerArg(request.getParameter("senseLimit"), searcher.getDefaultMaxSenseCount()) ;

				if (id == null) 
					data = searcher.doSearch(term, linkLimit, senseLimit) ;
				else
					data = searcher.doSearch(Integer.parseInt(id), linkLimit) ;
			}
			
			//process compare request
			if (data==null && task.equals("compare")) {
				String term1 = request.getParameter("term1");
				String term2 = request.getParameter("term2") ;
				int linkLimit = resolveIntegerArg(request.getParameter("linkLimit"), comparer.getDefaultMaxLinkCount()) ;	
				boolean details = resolveBooleanArg(request.getParameter("details"), comparer.getDefaultShowDetails()) ;

				data = comparer.getRelatedness(term1, term2, details, linkLimit) ;
			}
			
			//process wikify request
			if (data==null && task.equals("wikify")) {
				
				if (this.wikifier == null) 
					throw new ServletException("Wikifier is not available. You must configure the servlet so that it has access to link detection and disambiguation models.") ;

				String source = request.getParameter("source") ;
				int sourceMode = resolveIntegerArg(request.getParameter("sourceMode"), Wikifier.SOURCE_AUTODETECT) ;
				String linkColor = request.getParameter("linkColor") ;
				String baseColor = request.getParameter("baseColor") ;
				double minProb = resolveDoubleArg(request.getParameter("minProbability"), wikifier.getDefaultMinProbability()) ;
				int repeatMode = resolveIntegerArg(request.getParameter("repeatMode"), wikifier.getDefaultRepeatMode()) ;
				boolean showTooltips = resolveBooleanArg(request.getParameter("showTooltips"), wikifier.getDefaultShowTooltips()) ;
				String bannedTopics = request.getParameter("bannedTopics") ;

				boolean wrapInXml = resolveBooleanArg(request.getParameter("wrapInXml"), true) ;

				if (wrapInXml) {				
					data = wikifier.wikifyAndWrapInXML(source, sourceMode, minProb, repeatMode, bannedTopics, baseColor, linkColor, showTooltips) ;
				} else {
					response.setContentType("text/html");
					response.getWriter().append(wikifier.wikify(source, sourceMode, minProb, repeatMode, bannedTopics, baseColor, linkColor, showTooltips)) ;
					return ;
				}
			}
			
			if (data==null)
				throw new Exception("Unknown Task") ;


			//wrap data
			Element wrapper = doc.createElement("WikipediaMinerResponse") ;
			wrapper.setAttribute("server_path", context.getInitParameter("server_path")) ;
			wrapper.setAttribute("service_name", context.getInitParameter("service_name")) ;
			wrapper.appendChild(data) ;

			data = wrapper ;


			//Transform or serialize xml data as appropriate

			Transformer tf = null ;

			if (request.getParameter("xml") == null) {
				// we need to transform the data into html
				tf = transformersByName.get(task) ;

				if (request.getParameter("help") != null) 
					tf = transformersByName.get("help") ;
			}

			if (tf == null) {
				//we need to serialize the data as xml
				tf = transformersByName.get("serializer") ;
				response.setContentType("application/xml");
			} else {
				// output will be transformed to html				
				response.setContentType("text/html");
				response.getWriter().append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n") ;
			}

			tf.transform(new DOMSource(data), new StreamResult(response.getWriter()));

		} catch (Exception error) {
			response.reset() ;
			response.setContentType("application/xml");
			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF8") ;

			Element xmlError =doc.createElement("Error") ;
			if (error.getMessage() != null)
				xmlError.setAttribute("message", error.getMessage()) ;

			Element xmlStackTrace = doc.createElement("StackTrace") ;
			xmlError.appendChild(xmlStackTrace) ;

			for (StackTraceElement ste: error.getStackTrace()) {

				Element xmlSte = doc.createElement("StackTraceElement") ;
				xmlSte.setAttribute("message", ste.toString()) ;
				xmlStackTrace.appendChild(xmlSte) ;
			}
			try {
				transformersByName.get("serializer").transform(new DOMSource(xmlError), new StreamResult(response.getWriter()));
			} catch (Exception e) {
				//TODO: something for when an error is thrown processing an error????

			} ;
		}
	}

	private Element getDescription(String task) {


		if (task != null) {

			if (task.equals("define")) 
				return definer.getDescription() ;
			
			if (task.equals("compare")) 
				return comparer.getDescription() ;

			if (task.equals("search")) 
				return searcher.getDescription() ;

			if (task.equals("wikify")) 
				return wikifier.getDescription() ;
			 
		}

		Element description = doc.createElement("Description") ;

		description.appendChild(createElement("Details", "<p>This servlet provides a range of services for mining information from Wikipedia. Further details depend on what you want to do.</p>"
				+ "<p>You can <a href=\"" + context.getInitParameter("service_name") + "?task=search&help\">search for pages</a>, <a href=\"" + context.getInitParameter("service_name") + "?task=compare&help\">measure how terms or articles related to each other</a>, <a href=\"" + context.getInitParameter("service_name") + "?task=define&help\">obtain short definitions from articles</a>, and <a href=\"" + context.getInitParameter("service_name") + "?task=wikify&help\">detect topics in web pages</a>.</p>")) ; 		
	
		Element paramTask = createElement("Parameter", "Specifies what you want to do: can be <em>search</em>, <em>compare</em>, <em>define</em>, or <em>wikify</em>") ;
		paramTask.setAttribute("name", "task") ;
		description.appendChild(paramTask) ;

		Element paramId = doc.createElement("Parameter") ;
		paramId.setAttribute("name", "help") ;
		paramId.appendChild(doc.createTextNode("Specifies that you want help about the service.")) ;
		description.appendChild(paramId) ;

		return description ;
	}

	private int resolveIntegerArg(String arg, int defaultValue) {
		try {
			return Integer.parseInt(arg) ;			
		} catch (Exception e){
			return defaultValue ;
		}
	}

	private double resolveDoubleArg(String arg, double defaultValue) {
		try {
			return Double.parseDouble(arg) ;			
		} catch (Exception e){
			return defaultValue ;
		}
	}

	private boolean resolveBooleanArg(String arg, boolean defaultValue) {
		if (arg == null)
			return defaultValue ;

		try {
			return Boolean.parseBoolean(arg) ;			
		} catch (Exception e){
			return defaultValue ;
		}
	}

	private class CacherThread extends Thread {
		private ProgressNotifier pn ;
		private TextProcessor tp ;
		private boolean completed ;
		File dataDirectory ;
		boolean ok = true ;

		CacherThread(File dataDirectory, TextProcessor tp) {
			this.pn = null ;
			this.tp = tp ;
			this.completed = false ;
			this.dataDirectory = dataDirectory ;
		}

		public boolean isOk() {
			return ok ;
		}

		public double getProgress() {

			if (completed)
				return 1 ;

			if (pn == null) 
				return 0 ;

			return pn.getGlobalProgress() ;
		}

		public void run() {
			pn = new ProgressNotifier(5) ;

			try {
				TIntHashSet ids = wikipedia.getDatabase().getValidPageIds(dataDirectory, 3, pn) ;
				wikipedia.getDatabase().cacheParentIds(dataDirectory, pn) ;
				wikipedia.getDatabase().cacheGenerality(dataDirectory, ids, null) ;
				wikipedia.getDatabase().cachePages(dataDirectory, ids, pn) ;
				wikipedia.getDatabase().cacheAnchors(dataDirectory, tp, ids, 3, pn) ;
				wikipedia.getDatabase().cacheInLinks(dataDirectory, ids, pn) ;

				ids = null ;
			} catch (Exception e) {
				ok = false ;

			} ;

			completed = true ;
		}
	}

	/**
	 * @return true if the connection to wikipedia is active (or was able to be activated), otherwise false. 
	 */
	public boolean checkConnection() {

		if (!wikipedia.getDatabase().checkConnection()) {	
			try {
				wikipedia.getDatabase().connect() ;
			} catch (Exception e) {
				return false ;
			}
		}
		return true ;
	}


	protected Element createElement(String tagName, String xmlContent)  {

		try {
			//try to parse the xml content
			parser.parse(new InputSource(new StringReader("<" + tagName + ">" + xmlContent.replaceAll("&", "&amp;") + "</" + tagName + ">"))) ;	

			Element e = parser.getDocument().getDocumentElement() ;		
			return (Element) doc.importNode(e, true) ;
		} catch (Exception exception) {
			//if that fails, just dump the xml content as a text node within the element. All special characters will be escaped.

			Element e = doc.createElement(tagName) ;
			e.appendChild(doc.createTextNode(xmlContent)) ;
			return e ;			
		}
	}
}

