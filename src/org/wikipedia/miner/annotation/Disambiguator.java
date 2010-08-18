/*
 *    Disambiguator.java
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

import gnu.trove.TIntHashSet;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;
import org.wikipedia.miner.model.Anchor.Sense;

import weka.classifiers.*;
import weka.classifiers.meta.* ;
import weka.core.* ;

/**
 *	A machine-learned disambiguator. Given a term and a sense, it can identify how valid that sense is.
 *  See the following paper for a more detailed explanation:
 *  <p>
 *  Milne, D. and Witten, I.H. (2008) Learning to link with Wikipedia. In Proceedings of the ACM Conference on Information and Knowledge Management (CIKM'2008), Napa Valley, California.
 *  
 *  @author David Milne
 */
public class Disambiguator {

	private Wikipedia wikipedia ;
	private ArticleCleaner cleaner ;
	//private SentenceSplitter ss; 
	private TextProcessor tp ;

	private FastVector attributes ;
	private Instances trainingData ;
	private Instances header ;
	private Classifier classifier ;

	private double minSenseProbability ; 
	private int maxAnchorLength = 20 ;
	private double minLinkProbability ;
	private int maxContextSize ;

	/**
	 * Initializes the Disambiguator with default parameters.
	 * 
	 * @param wikipedia an initialized Wikipedia instance, preferably with relatedness measures cached.
	 * @param textProcessor an optional text processor (may be null) that will be used to alter terms before they are searched for.
	 */
	public Disambiguator(Wikipedia wikipedia, TextProcessor textProcessor) {

		init(wikipedia, textProcessor, 0.02, 0.03, 25) ;
		
		if (!wikipedia.getDatabase().arePagesCached())
			System.err.println("Disambiguator | Warning: pages have not been cached, so this will run significantly slower than it needs to.") ;
		
		if (tp != null && !wikipedia.getDatabase().areAnchorsCached(tp))
			System.err.println("Disambiguator | Warning: anchors have not been cached, so this will run significantly slower than it needs to.") ;
		
		if (!wikipedia.getDatabase().areInLinksCached())
			System.err.println("Disambiguator | Warning: links into pages have not been cached, so this will run significantly slower than it needs to.") ;
	}

	/**
	 * Initializes the Disambiguator with custom parameters. You should train and build a new classifier for
	 * each configuration.
	 * 
	 * @param wikipedia an initialized Wikipedia instance, preferably with relatedness measures cached.
	 * @param textProcessor an optional text processor (may be null) that will be used to alter terms before they are searched for.
	 * @param minSenseProbability the lowest probability (as a destination for the ambiguous anchor term) for which senses will be considered. 
	 * @param minLinkProbability the lowest probability (as a link in Wikipedia) for which terms will be mined from surrounding text
	 * @param maxContextSize the maximum number of concepts that are used as context.
	 */
	public Disambiguator(Wikipedia wikipedia,  TextProcessor textProcessor, double minSenseProbability, double minLinkProbability, int maxContextSize) {
		init(wikipedia, textProcessor, minSenseProbability, minLinkProbability, maxContextSize) ;
	}


	private void init(Wikipedia wikipedia, TextProcessor textProcessor, double minSenseProbability, double minLinkProbability, int maxContextSize) {
		this.wikipedia = wikipedia ;
		this.cleaner = new ArticleCleaner() ;
		this.tp = textProcessor ;
		//this.ss = new SentenceSplitter() ;

		this.minSenseProbability = minSenseProbability ;
		this.minLinkProbability = minLinkProbability ;
		this.maxContextSize = maxContextSize ; 

		attributes = new FastVector() ;

		attributes.addElement(new Attribute("commoness")) ;
		attributes.addElement(new Attribute("relatedness")) ;
		attributes.addElement(new Attribute("context_quality")) ;

		FastVector bool = new FastVector();
		bool.addElement("TRUE") ;
		bool.addElement("FALSE") ;
		attributes.addElement(new Attribute("isValidSense", bool)) ;

		this.header = new Instances("disambiguation_headerOnly", attributes, 0) ;
		header.setClassIndex(header.numAttributes() -1) ;
	}

	/**
	 * returns the probability (between 0 and 1) of a sense with the given commonness and relatedness being valid
	 * given the available context.
	 * 
	 * @param commonness the commonness of the sense (it's prior probability, irrespective of context)
	 * @param relatedness the relatedness of the sense to the given context (the result of calling context.getRelatednessTo()
	 * @param context the available context.
	 * @return the probability that the sense implied here is valid.
	 * @throws Exception if we cannot classify this sense.
	 */
	public double getProbabilityOfSense(double commonness, double relatedness, Context context) throws Exception {

		double[] values = new double[attributes.size()];

		values[0] = commonness ;
		values[1] = relatedness ;
		values[2] = context.getQuality() ;
		//values[2] = context.getSize() ;
		//values[3] = context.getTotalRelatedness() ;
		//values[4] = context.getTotalLinkLikelyhood() ;

		values[3] = Instance.missingValue() ;

		Instance i = new Instance(1.0, values) ;
		i.setDataset(header) ;

		return classifier.distributionForInstance(i)[0] ;		
	}

	/**
	 * Trains the disambiguator on a set of Wikipedia articles. This only builds up the training data. 
	 * You will still need to build a classifier in order to use the trained disambiguator. 
	 * 
	 * @param articles the set of articles to use for training. You should make sure these are reasonably tidy, and roughly representative (in size, link distribution, etc) as the documents you intend to process automatically.
	 * @param snippetLength the portion of each article that should be considered for training (see ArticleCleaner).  
	 * @param datasetName a name that will help explain the set of articles and resulting model later.
	 * @param rc a cache in which relatedness measures will be saved so they aren't repeatedly calculated. Make this null if using extremely large training sets, so that caches will be reset from document to document, and won't grow too large.   
	 * @throws Exception 
	 */
	public void train(ArticleSet articles, int snippetLength, String datasetName, RelatednessCache rc) throws Exception{

		initializeTrainingData(datasetName) ;

		ProgressNotifier pn = new ProgressNotifier(articles.getArticleIds().size(), "Disambiguator| training") ;
		for (int id: articles.getArticleIds()) {
			
			Article art = null;
			
			try{ 
				art = new Article(wikipedia.getDatabase(), id) ;
			} catch (Exception e) {
				System.err.println("Warning: " + id + " is not a valid article") ;
			}
			
			if (art != null)
				train(art, snippetLength, rc) ;
			
			pn.update() ;
		}
	}

	/**
	 * Saves the training data to an arff file, so that it can be used by Weka.
	 * Don't bother using this unless you intend to use the file directly. 
	 * You can save and load classifiers instead, which are much much smaller 
	 * and contain all the information needed to run the disambiguator. 
	 * 
	 * @param file the file to save 
	 * @throws IOException if the file cannot be written to.
	 * @throws Exception if the disambiguator has not been trained yet.
	 */
	public void saveTrainingData(File file) throws IOException, Exception {
		System.out.println("Disambiguator: saving training data...") ;
		
		if (trainingData == null)
			throw new Exception("You need to train the disambiguator first!") ;

		BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ;
		writer.write(trainingData.toString()) ;
		writer.close();
	}
	
	/**
	 * Loads the training data from an arff file saved previously. 
	 * You will still need to build a classifier in order to use the trained disambiguator. 
	 * 
	 * @param file the file to load 
	 * @throws IOException if the file cannot be read
	 */
	public void loadTrainingData(File file) throws IOException{
		System.out.println("Disambiguator: loading training data...") ;

		trainingData = new Instances(new BufferedReader(new FileReader(file)));
		trainingData.setClassIndex(trainingData.numAttributes() - 1);
	}

	private void initializeTrainingData(String datasetName) {
		trainingData = new Instances(datasetName, attributes, 0) ;
		trainingData.setClassIndex(trainingData.numAttributes() -1) ;
	}

	/**
	 * Saves the classifier to file, so that it can be reused.
	 * 
	 * @param file the file to save to. 
	 * @throws IOException if the file cannot be written to
	 * @throws Exception unless the disambiguator has been trained and a classifier has been built.
	 */
	public void saveClassifier(File file) throws IOException, Exception {
		System.out.println("Disambiguator: saving classifier...") ;
		
		if (classifier == null)
			throw new Exception("You must train the disambiguator and build a classifier first!") ;

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(classifier);
		oos.flush();
		oos.close();
	}

	/**
	 * Loads a classifier that was previously saved. 
	 * 
	 * @param file the file in which 
	 * @throws IOException if there is a problem reading the file
	 * @throws Exception if the file does not contain a valid classifier. 
	 */
	public void loadClassifier(File file) throws IOException, Exception {
		System.out.println("Disambiguator: loading classifier...") ;

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		classifier = (Classifier) ois.readObject();
		ois.close();
	}

	
	
	/**
	 * Builds a classifier of the given type using the previously built (or loaded) training data.
	 * 
	 * @param classifier a configured classifier, that is ready to be built.
	 * @throws Exception if there is no training data
	 */
	public void buildClassifier(Classifier classifier) throws Exception {
		System.out.println("Disambiguator: Building classifier...") ;

		weightTrainingInstances() ;

		if (trainingData == null) {
			throw new WekaException("You must load training data or train on a set of articles before builing classifier.") ;
		} else {
			this.classifier = classifier ;
			classifier.buildClassifier(trainingData) ;
		}
	}

	private void train(Article article, int snippetLength, RelatednessCache rc) throws Exception {

		Vector<Anchor> unambigAnchors = new Vector<Anchor>() ;
		Vector<TopicReference> ambigRefs = new Vector<TopicReference>() ;

		String content = cleaner.getMarkupLinksOnly(article, snippetLength) ;

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]") ; 
		Matcher linkMatcher = linkPattern.matcher(content) ;

		// split references into ambiguous and unambiguous
		while (linkMatcher.find()) {			
			String linkText = content.substring(linkMatcher.start()+2, linkMatcher.end()-2) ;
			
			

			String anchorText = linkText ;
			String destText = linkText ;

			int pos = linkText.lastIndexOf('|') ;
			if (pos>0) {
				destText = linkText.substring(0, pos) ;
				anchorText = linkText.substring(pos+1) ;
			}

			//System.out.println(linkText + ", " + anchorText + ", " + destText) ;
			
			Anchor anchor = new Anchor(anchorText, tp, wikipedia.getDatabase()) ;
			int senseCount = anchor.getSenses().size() ;
			Article dest = wikipedia.getArticleByTitle(destText) ;
			
			//if (dest == null) {
			//	System.err.println("Could not locate article entitled \"" + destText + "\"") ;
			//}

			if (dest != null && senseCount >= 1) {
				TopicReference ref = new TopicReference(anchor, dest.getId(), new Position(0, 0)) ;

				if (senseCount == 1 || anchor.getSenses().first().getProbability() >= (1-minSenseProbability))
					unambigAnchors.add(anchor) ;
				else
					ambigRefs.add(ref) ;
			}
		}

		
		// use all terms as context
		Context context = getContext(article, snippetLength, rc) ;
		
		//only use links
		//Context context = new Context(unambigAnchors, rc, maxContextSize) ;

		//resolve ambiguous links
		for (TopicReference ref: ambigRefs) {
			for (Sense sense:ref.getAnchor().getSenses()) {

				if (sense.getProbability() < minSenseProbability) break ;

				double[] values = new double[attributes.size()];

				values[0] = sense.getProbability() ;
				values[1] = context.getRelatednessTo(sense) ;
				values[2] = context.getQuality() ;

				if (sense.getId() == ref.getTopicId()) 
					values[3] = 0.0 ;
				else
					values[3] = 1.0 ;

				trainingData.add(new Instance(1.0, values));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void weightTrainingInstances() {

		double positiveInstances = 0 ;
		double negativeInstances = 0 ; 

		Enumeration<Instance> e = trainingData.enumerateInstances() ;

		while (e.hasMoreElements()) {
			Instance i = (Instance) e.nextElement() ;

			double isValidSense = i.value(3) ;

			if (isValidSense == 0) 
				positiveInstances ++ ;
			else
				negativeInstances ++ ;
		}

		double p = (double) positiveInstances / (positiveInstances + negativeInstances) ;

		e = trainingData.enumerateInstances() ;

		while (e.hasMoreElements()) {
			Instance i = (Instance) e.nextElement() ;

			double isValidSense = i.value(3) ;

			if (isValidSense == 0) 
				i.setWeight(0.5 * (1.0/p)) ;
			else
				i.setWeight(0.5 * (1.0/(1-p))) ;
		}

	}

	/**
	 * Tests the disambiguator on a set of Wikipedia articles, to see how well it makes the same 
	 * decisions as the original article editors did. You need to train the disambiguator and build 
	 * a classifier before using this.
	 * 
	 * @param testSet the set of articles to use for testing. You should make sure these are reasonably tidy, and roughly representative (in size, link distribution, etc) as the documents you intend to process automatically.
	 * @param snippetLength the portion of each article that should be considered for testing (see ArticleCleaner).  
	 * @param rc a cache in which relatedness measures will be saved so they aren't repeatedly calculated. Make this null if using extremely large testing sets, so that caches will be reset from document to document, and won't grow too large.
	 * @return Result a result (including recall, precision, f-measure) of how well the classifier did.   
	 * @throws SQLException if there is a problem with the WikipediaMiner database.
	 * @throws Exception if there is a problem with the classifier
	 */
	public Result<Integer> test(ArticleSet testSet, int snippetLength, RelatednessCache rc) throws SQLException, Exception{

		if (classifier == null) 
			throw new WekaException("You must build (or load) classifier first.") ;

		Result<Integer> r = new Result<Integer>() ;
		Article art = null ;
		
		ProgressNotifier pn = new ProgressNotifier(testSet.getArticleIds().size(), "Testing") ;
		for (int id: testSet.getArticleIds()) {
			try {
				art = new Article(wikipedia.getDatabase(), id) ;
			} catch (Exception e) {
				System.err.println("Warning: " + id + " is not a valid article") ;
			} ;
			
			if (art != null)
				r.addIntermediateResult(test(art, snippetLength, rc)) ;
			
			pn.update() ;
		}

		return r ;
	}

	private Result<Integer> test(Article article, int snippetLength, RelatednessCache rc) throws Exception {

		System.out.println(" - testing " + article) ;

		Vector<Anchor> unambigAnchors = new Vector<Anchor>() ;
		Vector<TopicReference> ambigRefs = new Vector<TopicReference>() ;

		String content = cleaner.getMarkupLinksOnly(article, snippetLength) ;

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]") ; 
		Matcher linkMatcher = linkPattern.matcher(content) ;

		HashSet<Integer> goldStandard = new HashSet<Integer>() ;
		HashSet<Integer> disambiguatedLinks = new HashSet<Integer>() ;

		while (linkMatcher.find()) {			
			String linkText = content.substring(linkMatcher.start()+2, linkMatcher.end()-2) ;

			String anchorText = linkText ;
			String destText = linkText ;

			int pos = linkText.lastIndexOf('|') ;
			if (pos>0) {
				destText = linkText.substring(0, pos) ;
				anchorText = linkText.substring(pos+1) ;
			}

			destText = Character.toUpperCase(destText.charAt(0)) + destText.substring(1) ;     // Get first char and capitalize

			Anchor anchor = new Anchor(anchorText, tp, wikipedia.getDatabase()) ;
			int senseCount = anchor.getSenses().size() ;
			Article dest = wikipedia.getArticleByTitle(destText) ;

			if (senseCount > 0 && dest != null) {

				goldStandard.add(dest.getId()) ;

				if (senseCount == 1 || anchor.getSenses().first().getProbability() >= (1-minSenseProbability)) { 
					unambigAnchors.add(anchor) ;
					disambiguatedLinks.add(dest.getId()) ;
				} else {
					TopicReference ref = new TopicReference(anchor, dest.getId(), null) ;
					ambigRefs.add(ref) ;
				}
			}
		}

		// use all terms as context
		Context context = getContext(article, snippetLength, rc) ;
		
		//only use links
		//Context context = new Context(unambigAnchors, rc, maxContextSize) ;

		// resolve senses		
		for (TopicReference ref: ambigRefs) {

			TreeSet<Article> validSenses = new TreeSet<Article>() ;

			for (Sense sense:ref.getAnchor().getSenses()) {

				if (sense.getProbability() < minSenseProbability) break ;

				double[] values = new double[attributes.size()];


				values[0] = sense.getProbability() ;
				values[1] = context.getRelatednessTo(sense) ;
				values[2] = context.getQuality() ;
				values[3] = Instance.missingValue() ;

				Instance i = new Instance(1.0, values) ;
				i.setDataset(header) ;

				double prob = classifier.distributionForInstance(i)[0] ;

				if (prob>0.5) {
					Article art = new Article(wikipedia.getDatabase(), sense.getId()) ;
					art.setWeight(prob) ;
					validSenses.add(art) ;					
				}
			}

			//use most valid sense
			if (!validSenses.isEmpty()) 
				disambiguatedLinks.add(validSenses.first().getId()) ;
		}

		Result<Integer> result = new Result<Integer>(disambiguatedLinks, goldStandard) ;

		System.out.println("   " + result) ;

		return result ;
	}

	private Context getContext(Article article, int snippetLength, RelatednessCache rc) throws Exception{

		Vector<Anchor> unambigAnchors = new Vector<Anchor>() ;

		String content = cleaner.getMarkupLinksOnly(article, snippetLength) ;
		String s = "$ " + content + " $" ;

		Pattern p = Pattern.compile("[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_]") ;  //would just match all non-word chars, but we dont want to match utf chars
		Matcher m = p.matcher(s) ;

		Vector<Integer> matchIndexes = new Vector<Integer>() ;

		while (m.find()) 
			matchIndexes.add(m.start()) ;

		for (int i=0 ; i<matchIndexes.size() ; i++) {

			int startIndex = matchIndexes.elementAt(i) + 1 ;

			for (int j=Math.min(i + maxAnchorLength, matchIndexes.size()-1) ; j > i ; j--) {
				int currIndex = matchIndexes.elementAt(j) ;	
				String ngram = s.substring(startIndex, currIndex) ;

				if (! (ngram.length()==1 && s.substring(startIndex-1, startIndex).equals("'"))&& !ngram.trim().equals("")) {
					Anchor anchor = new Anchor(wikipedia.getDatabase().addEscapes(ngram), tp, wikipedia.getDatabase()) ;


					if (anchor.getLinkProbability() > minLinkProbability)
						if (anchor.getSenses().size() == 1 || anchor.getSenses().first().getProbability() >= (1-minSenseProbability)) 
							unambigAnchors.add(anchor) ;
				}	
			}
		}

		return new Context(unambigAnchors, rc, maxContextSize) ;
	}

	/**
	 * @return the maximum length (in words) for ngrams that will be checked against wikipedia's anchor vocabulary.
	 */
	public int getMaxAnchorLength() {
		return maxAnchorLength;
	}

	
	/**
	 * @return the lowest probability (as a link in Wikipedia) for which terms will be mined from surrounding text and used as context.
	 */
	public double getMinLinkProbability() {
		return minLinkProbability;
	}	 

	/**
	 * @return the lowest probability (as a destination for the ambiguous anchor term) for which senses will be considered.
	 */
	public double getMinSenseProbability() {
		return minSenseProbability;
	}
	
	/**
	 * @return the maximum number of concepts that are used as context.
	 */
	public int getMaxContextSize() {
		return maxContextSize ;
	}

	/**
	 * @return the text processor used to modify terms and phrases before they are compared to Wikipedia's anchor vocabulary.
	 */
	public TextProcessor getTextProcessor() {
		return tp ;
	}

	/**
	 * A demo of how to train and test the disambiguator. 
	 * 
	 * @param args	an array of 2 or 4 String arguments; the connection string of the Wikipedia 
	 * database server, the name of the Wikipedia database and (optionally, if anonymous access
	 * is not allowed) a username and password for the database.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{

		//set up an instance of wikipedia
		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;
		
		//use a text processor, so that terms and items in wikipedia will both be case-folded before being compared.
		TextProcessor tp = new CaseFolder() ;

		//cache tables that will be used extensively
		File dataDirectory = new File("/research/wikipediaminer/data/en/20080727") ;
		ProgressNotifier pn = new ProgressNotifier(4) ;
		
		TIntHashSet ids = wikipedia.getDatabase().getValidPageIds(dataDirectory, 2, pn) ;
		wikipedia.getDatabase().cachePages(dataDirectory, ids, pn) ;
		wikipedia.getDatabase().cacheAnchors(dataDirectory, tp, ids, 2, pn) ;
		wikipedia.getDatabase().cacheInLinks(dataDirectory, ids, pn) ;
		
		//gather article sets for training and testing		
		ArticleSet trainSet = new ArticleSet(new File("data/articleSets/trainingIds.csv")) ;
		ArticleSet testSet = new ArticleSet(new File("data/articleSets/testIds_disambig.csv")) ;

		//use relatedness cache, so we won't repeat these calculations unnecessarily
		RelatednessCache rc = new RelatednessCache() ;
		
		//train disambiguator
		Disambiguator disambiguator = new Disambiguator(wikipedia, tp, 0.01, 0.01, 25) ;
		disambiguator.train(trainSet, ArticleCleaner.ALL, "disambig_trainingIds", rc) ;
		
		//build disambiguation classifier
		Classifier classifier = new Bagging() ;
		classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 2")) ;
		disambiguator.buildClassifier(classifier) ;
		disambiguator.saveClassifier(new File("data/models/disambig.model")) ;
		
		//test
		Result<Integer> r = disambiguator.test(testSet, ArticleCleaner.ALL, rc) ;
		System.out.println(r) ; 
	}

}
