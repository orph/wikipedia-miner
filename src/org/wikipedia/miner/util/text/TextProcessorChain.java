/*
 *    TextProcessorChain.java
 *    Copyright (C) 2009 Giulio Paci, g.paci@cineca.it
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;


/**
 * This class provides a wrapper to be able to combine more than one textprocessor.
 *
 * @author Giulio Paci
 */
public class TextProcessorChain extends TextProcessor {
    private Vector<TextProcessor> text_processor_list;
    String name = null;
    boolean name_from_outside = false;

    /**
     * Create an empty TextProcessorChain.
     */
    public TextProcessorChain() {
        this.text_processor_list = new Vector<TextProcessor>();
    }

    /**
     * Create a TextProcessorChain and add a TextProcessor to it.
     */
    public TextProcessorChain(TextProcessor tp) {
        this.text_processor_list = new Vector<TextProcessor>();
        this.text_processor_list.add(tp);
    }

    /**
     * Create a TextProcessorChain from a list of TextProcessors.
     */
    public TextProcessorChain(Vector<TextProcessor> vtp) {
        this.text_processor_list = vtp;
    }

    /**
     * Create a TextProcessorChain from a textual description.
     *
     * @param base_path relative paths in the description are assumed to be relative to this directory.
     * @param description a description of the TextProcessorChain: a list of encoded TextProcessors descriptions, separated by '>' symbol.
     *
     * @throws java.lang.InstantiationException
     */
    public TextProcessorChain(File base_path, String description) throws InstantiationException{
        String tokens[] = description.split(">");
        this.text_processor_list = new Vector<TextProcessor>();
        for (String tp_string : tokens) {
            this.text_processor_list.add(TextProcessor.fromString(base_path, tp_string));
        }
    }


	/**
	 * Set the name of this chain.
     * 
	 * @return	the name of this TextProcessor.
	 */
    @Deprecated
    public void setName(String name){
        this.name = name;
        this.name_from_outside = true;
    }


	/**
	 * Returns the name of the current TextProcessorChain.
	 *
	 * @return	the name of this TextProcessor.
	 */
    @Override
    public String getName() {
        if (this.name != null) {
            return this.name;
        } else {
            StringBuffer ret_name = new StringBuffer(this.getClass().getSimpleName());
            for(TextProcessor tp : this.text_processor_list){
                ret_name.append("_");
                ret_name.append(tp.getName());
            }
            if(ret_name.length() > TextProcessor.MAX_ID_LENGTH){
                int char_to_keep;
                int char_to_remove;
                //remove exceeding characters
            }
            this.name = ret_name.toString();
            return this.name;
        }
    }

    /**
     * Appends a TextProcessor to the chain.
     *
     * @param tp	the TextProcessor to be appended.
     * @return  true (as per the general contract of Collection.add).
     */
    public boolean addTextProcessor(TextProcessor tp) {
        if (!this.name_from_outside) {
            this.name = null;
        }
        return this.text_processor_list.add(tp);
    }


    /**
     * Returns a processed copy of the argument text.
     *
     * @param text	the text to be processed.
     * @return	the processed version of this text.
     */
    public String processText(String text) {
        Iterator itr = text_processor_list.iterator();

        String t = text;
        while (itr.hasNext()) {
            text = ((TextProcessor) itr.next()).processText(text);
        }
		return text;
    }

    /**
     * Returns a string that provides complete information to setup the current configuration of the
     * text processor.
     *
     * @return	a textual description of the TextProcessor.
     */
    @Override
    public String toString() {
        StringBuffer my_list_string = new StringBuffer();
        for (TextProcessor tp : this.text_processor_list) {
            my_list_string.append(tp.toString()).append(">");
        }

        return super.toString() + "<string<" + TextProcessor.encodeParam(my_list_string.substring(0, my_list_string.length()-1));
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException{
        TextProcessorChain tpc = new TextProcessorChain();

        TextProcessor stem = new SnowballStemmer();
        tpc.addTextProcessor(stem);
        stem = new SnowballStemmer("italian");
        tpc.addTextProcessor(stem);
        stem = new CaseFolder();
        tpc.addTextProcessor(stem);
        stem = new StopwordRemover(new File("../../../../../../mnt/extern/wikipedia_miner/data/endump/snowball-english-stopwords.txt"));
        tpc.addTextProcessor(stem);
        System.err.println(tpc.toString());


        try{
        System.err.println(TextProcessor.fromString(new File("."), tpc.toString()).toString());
        }
        catch(Exception ex){
            System.err.println("No :-(");
        }
        try{
            TextProcessor tp = TextProcessor.fromString(new File("."), "org.wikipedia.miner.util.text.SnowballStemmerWrapper<string<italian");
            System.err.println(tp.getName());
            System.err.println(tp.toString());
        }
        catch(Exception ex){
            System.err.println("Noooo");
        }

//        tpc.addTextProcessor(new StopwordRemover(new File(stopword_filename)));
 //       File tmp = new File(input_filename);
//        FileInputStream fstream = new FileInputStream(tmp);
//        DataInputStream in = new DataInputStream(fstream);
 //       BufferedReader br = new BufferedReader(new InputStreamReader(in));
//        String strLine;
//        String tokens[];
//
//        while ((strLine = br.readLine()) != null) {
//            tokens = strLine.split("\t");
//            strLine = tokens[0];
//            System.out.println(strLine + "\t" + tpc.processText(strLine) + "\t" + stem.processText(strLine));
//        }
        System.out.println("name: " + tpc.getName() + "\n" );
    }
}
