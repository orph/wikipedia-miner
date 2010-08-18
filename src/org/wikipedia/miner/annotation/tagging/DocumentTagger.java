/*
 *    DocumentTagger.java
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


package org.wikipedia.miner.annotation.tagging;

import java.util.*;
import org.wikipedia.miner.annotation.* ;
import org.wikipedia.miner.annotation.preprocessing.* ;
import org.wikipedia.miner.util.*;

/**
 * Tags documents by adding markup to to the topics it mentions. 
 * <p>
 * You can use this to tag all topics by giving it the output of the TopicDetector, or 
 * just those that should be linked to (from LinkDetector) or those that the document 
 * is about (from Indexers). If using the latter two, then you should first modify the list 
 * of topics to include only those that are most likely to be links or key topics 
 * (i.e those above a certain weight). 
 * 
 * @author David Milne
 */
public abstract class DocumentTagger {
	
	/**
	 * all mentions of a topic will be tagged
	 */
	public static final int ALL = 0 ;
	
	/**
	 * only the first mention of a topic in the document will be tagged
	 */
	public static final int FIRST = 1 ;
	
	/**
	 * only the first mention of a topic within each region of the document will be tagged
	 */
	public static final int FIRST_IN_REGION = 2 ;
	
	
	/**
	 * Specifies how terms in the document will be replaced by tags. A tagger for html, for example, might return a link to the relevant Wikipedia article.
	 * 
	 * @param term the text in the original document that will be tagged.
	 * @param topic the relevant topic that the term was disambiguated to.
	 * @return the tag that will replace the given term.
	 */
	public abstract String getTag(String term, Topic topic) ;
	
	/**
	 * Tags the given text with occurrences of the given topics. 
	 * 
	 * @param doc the document to be tagged
	 * @param topics a set of automatically detected topics, i.e. from TopicDetector or LinkDetector
	 * @param tagMode ALL, FIRST, or FIRST_IN_REGION 
	 * @return the tagged text
	 */
	public String tag(PreprocessedDocument doc, Collection<Topic> topics, int tagMode) {
		
		doc.resetRegionTracking() ;
		
		HashMap<Integer,Topic> topicsById = new HashMap<Integer, Topic>() ;
		for (Topic topic: topics) 
			topicsById.put(topic.getId(), topic) ;
		
		Vector<TopicReference> references = resolveCollisions(topics) ;
		
		String originalText = doc.getOriginalText() ;
		StringBuffer wikifiedText = new StringBuffer() ;
		int lastIndex = 0 ;
		
		HashSet<Integer> doneIds = new HashSet<Integer>() ;
				
		for (TopicReference reference:references) {
			int start = reference.getPosition().getStart() ; 
			int end = reference.getPosition().getEnd() ;
			int id = reference.getTopicId() ;
			
			Topic topic = topicsById.get(id) ;	
			
			//System.out.println("considering tagging " + topic + " at " + reference.getPosition()) ;
			
			if (tagMode == DocumentTagger.FIRST_IN_REGION)
				doneIds = doc.getDoneIdsInCurrentRegion(start) ;
						
			if (topic != null && (tagMode == DocumentTagger.ALL || !doneIds.contains(id))) {
				
				doneIds.add(id) ;
				wikifiedText.append(originalText.substring(lastIndex, start)) ;
				wikifiedText.append(getTag(originalText.substring(start, end), topic)) ;
				
				lastIndex = end ;
				
				//System.out.println(" - tagged") ;
			}
		}
		
		wikifiedText.append(originalText.substring(lastIndex)) ;
		return wikifiedText.toString() ;
	}
	
	private Vector<TopicReference> resolveCollisions(Collection<Topic> topics) {
		
		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>() ;
		
		TreeSet<TopicReference> temp = new TreeSet<TopicReference>() ;
		
		for(Topic topic: topics) {	
			for (Position pos: topic.getPositions()) {
				topicWeights.put(topic.getId(), topic.getWeight()) ;
				
				TopicReference tr = new TopicReference(null, topic.getId(), pos) ;
				temp.add(tr) ;
			}
		}
		
		Vector<TopicReference> references = new Vector<TopicReference>() ;
		references.addAll(temp) ;
		
		for (int i=0 ; i<references.size(); i++) {
			TopicReference reference = references.elementAt(i) ;
			
			Vector<TopicReference> overlappedTopics = new Vector<TopicReference>() ;
			
			for (int j=i+1 ; j<references.size(); j++){
				TopicReference reference2 = references.elementAt(j) ;
				
				if (reference.overlaps(reference2)) 
					overlappedTopics.add(reference2) ;
			}
			
			for (int j=0 ; j<overlappedTopics.size() ; j++) {
				references.removeElementAt(i+1) ;
			}
			
			//double refWeight = 0 ;
			//Integer refId = reference.getTopicId() ;
			
			//if (topicWeights.containsKey(refId))
			//	refWeight = topicWeights.get(refId) ;
			/*
			double overlapWeight = 0 ;
			
			for (int j=i+1 ; j<references.size(); j++){
				TopicReference reference2 = references.elementAt(j) ;
				
				if (reference.overlaps(reference2)) {
					//System.out.println("--" + getNGram(words, c.getStartIndex(), c.getEndIndex()) + " overlaps " + getNGram(words, c1.getStartIndex(), c1.getEndIndex()));
					overlappingTopics.add(reference2) ;
					
					double ref2Weight = 0 ;
					Integer ref2Id = reference2.getTopicId() ;
					if (topicWeights.containsKey(ref2Id))
						ref2Weight = topicWeights.get(ref2Id) ;
					
					overlapWeight = overlapWeight + ref2Weight ; 
				} else {
					break ;
				}
			}
			
			if (overlappingTopics.size() > 0)
				overlapWeight = overlapWeight / overlappingTopics.size() ;
			
			//if (overlapWeight > refWeight) {
				// want to keep the overlapped items
			//	references.removeElementAt(i) ;
			//	i = i-1 ;				
			//} else {
				//want to keep the overlapping item
				for (int j=0 ; j<overlappingTopics.size() ; j++) {
					references.removeElementAt(i+1) ;
				}
			//}
			 * 
			 */
		}
		return references ;
	}
}
