package org.gauner.jSpellCorrect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gauner.utils.StringTools;

/**
 * This spelling corrector is based on Peter Norvigs Essay "How to Write a
 * Spelling Corrector".
 * 
 * @link http://norvig.com/spell-correct.html
 * @author Dominik
 * 
 * <pre>
 *  Copyright (c) 2007 Dominik Schulz
 *  This file is part of jSpellCorrect.
 *  jSpellCorrect is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  jSpellCorrect is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with jSpellCorrect; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * </pre>
 * 
 */
public class ToySpellingCorrector {

	public static final String		ALPHABET	= "abcdefghijklmnopqrstuvwxyz";

	private static final boolean	DEBUG		= false;

	/**
	 * Sample usage. First create a new object. Then train with some data. Last
	 * but not least try to correct some words.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ToySpellingCorrector sc = new ToySpellingCorrector();
		sc.clockStart();
		sc.trainFile("big.txt");
		sc.clockStop();
		//sc.trainSingle("Test");
		//System.out.println(sc.correct("Tezt"));
		System.out.println("Tests1:");
		sc.tests1(false);
		System.out.println("Tests2:");
		sc.tests2(false);
		// System.out.println("Tests3:");
		// sc.tests3(true);
	}

	private long	startTime	= 0;

	public void clockStart() {
		this.startTime = System.currentTimeMillis();
	}

	public void clockStop() {
		long duration = System.currentTimeMillis() - this.startTime;
		int durSec = (int) duration / 1000;
		System.out.println("Duration: " + durSec + " (" + duration + " ms)");
	}

	/**
	 * Store the known words and their probability.
	 */
	private Map<String, Integer>	nwords	= new HashMap<String, Integer>(new Integer(1));

	public ToySpellingCorrector() {
	}

	/**
	 * Try to find the correct spelling of the given word.
	 * 
	 * @param word
	 *            the word to correct
	 * @return the best match for the corrected word
	 */
	public String correct(String word) {
		//Set<String> wordSet = new HashSet<String>();
		//wordSet.add(word);
		if (this.nwords.containsKey(word))
			return word;
		else
			/*return max2(known(wordSet), known(edits1(word)),
					known_edits2(word), word);*/
			return max3(word);
	}

	/**
	 * Feed some training data from a file to the statistical engine.
	 * 
	 * @param filename
	 *            a file with training data.
	 */
	@SuppressWarnings("unused")
	private void trainFile1(String filename) {
		log("Reading Training Data ...");
		String[] trainingData = StringTools.readFileIntoStringArray(filename);
		log("Done reading training data. Number of Lines: " + trainingData.length);
		log("Splitting to words ...");
		String[] words = this.words(trainingData);
		log("Done splitting to words. Number of Lines(should equal num of words): " + words.length);
		log("Training ...");
		Map<String, Integer> tmpNwords = this.train(words);
		log("Done training the filter. Number of entrys (should equals num of words): " + tmpNwords.size());
		log("Now merging.");
		int oldNwordsSize = this.nwords.size();
		this.nwords = mergeMaps(this.nwords, tmpNwords);
		log("Merging done. Estimated size: " + (oldNwordsSize + tmpNwords.size()) + ", Actual size: "
				+ this.nwords.size());
	}

	public void trainFile(String filename) {
		//this.trainFile1(filename);
		this.trainFile2(filename);
	}

	private void trainFile2(String filename) {
		File file = new File(filename);
		BufferedReader bw = null;
		try {
			bw = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		String pat = "[^\\w+]";
		String rep = " ";
		try {
			while ((line = bw.readLine()) != null) {
				line = line.trim().toLowerCase();
				line = replace(line, pat, rep);
				StringTokenizer tok = new StringTokenizer(line);
				while (tok.hasMoreTokens()) {
					this.trainSingle(tok.nextToken());
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void trainMany(String[] text) {
		//this.trainMany1(text);
		this.trainMany2(text);
	}

	public void trainMany(String text) {
		//this.trainMany1(text);
		this.trainMany2(text);
	}

	private void trainMany2(String[] text) {
		//String pat = "[^\\w+]";
		//String rep = " ";
		String line;
		for (int i = 0; i < text.length; i++) {
			line = text[i].trim().toLowerCase();
			StringTokenizer tok = new StringTokenizer(line);
			while (tok.hasMoreTokens()) {
				this.trainSingle(tok.nextToken());
			}
		}
	}

	private void trainMany2(String text) {
		String pat = "[^\\w+]";
		String rep = " ";
		String line;
		StringTokenizer tok1 = new StringTokenizer(text);
		StringTokenizer tok2;
		while (tok1.hasMoreTokens()) {
			line = tok1.nextToken().toLowerCase().trim();
			line = replace(line, pat, rep);
			tok2 = new StringTokenizer(line);
			while (tok2.hasMoreTokens()) {
				this.trainSingle(tok2.nextToken().trim());
			}
		}
	}

	/**
	 * Feed some training data direct to the statistical engine.
	 * 
	 * @param text
	 *            an array of some lines of text.
	 */
	private void trainMany1(String[] text) {
		String[] words = this.words(text);
		Map<String, Integer> tmpNwords = this.train(words);
		this.nwords = mergeMaps(this.nwords, tmpNwords);
	}

	/**
	 * Feed some training data direct to the statistical engine.
	 * 
	 * @param text
	 *            just some text.
	 */
	@SuppressWarnings("unused")
	private void trainMany1(String text) {
		String[] trainingData = StringTools.stringToArray(text);
		this.trainMany1(trainingData);
	}

	/**
	 * Feed a single word to the training engine.
	 * 
	 * @param key
	 */
	public void trainSingle(String key) {
		this.trainSingle(key, 1);
	}

	/**
	 * Feed a single word to the training engine and specify a bias value.
	 * 
	 * @param key
	 *            the word to add to the known words
	 * @param bias
	 *            the bias of this word. can be positive or negative
	 */
	public void trainSingle(String word, int bias) {
		String key = word.trim().toLowerCase();
		if (this.nwords.containsKey(key)) {
			this.nwords.put(key, new Integer(this.nwords.get(key).intValue() + bias));
		} else {
			this.nwords.put(key, new Integer(1));
		}
	}

	/**
	 * Generate a set of words with edit distance one from the original word.
	 * 
	 * @param word
	 * @return
	 */
	private Set<String> edits1(String word) {
		int n = word.length();
		Set<String> result = new HashSet<String>();
		// deletion
		for (int i = 0; i < n; i++) {
			String str = word.substring(0, i) + word.substring(i + 1);
			// log("Deletion, adding: " + str);
			result.add(str);
		}
		// transposition
		for (int i = 0; i < (n - 1); i++) {
			String str = word.substring(0, i) + word.charAt(i + 1) + word.charAt(i) + word.substring(i + 2);
			// log("Transposition, adding: " + str);
			result.add(str);
		}
		// alteration
		for (int i = 0; i < ALPHABET.length(); i++) {
			String c = String.valueOf(ALPHABET.charAt(i));
			for (int j = 0; j < n; j++) {
				String str = word.substring(0, j) + c + word.substring(j + 1);
				// log("Alteration, adding: " + str);
				result.add(str);
			}
		}
		// insertion
		for (int i = 0; i < ALPHABET.length(); i++) {
			String c = String.valueOf(ALPHABET.charAt(i));
			for (int j = 0; j < n + 1; j++) {
				String str = word.substring(0, j) + c + word.substring(j);
				// log("Insertion, adding: " + str);
				result.add(str);
			}
		}
		// log("The number of words with edit distance 1 from " + word + " is "
		// + result.size());
		return result;
	}

	@SuppressWarnings("unused")
	private Set<String> edits2(String word) {
		Set<String> result = new HashSet<String>();
		Set<String> edit1 = edits1(word);
		for (Iterator<String> iter = edit1.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			result.addAll(edits1(element));
		}
		// log("The number of words with edit distance 2 from " + word + " is "
		// + result.size());
		return result;
	}

	/**
	 * Check which words from the input were already learned.
	 * 
	 * @param words
	 * @return
	 */
	private Set<String> known(Set<String> words) {
		Set<String> result = new HashSet<String>();
		for (Iterator<String> iter = words.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (this.nwords.containsKey(element))
				result.add(element);
		}
		return result;
	}

	/**
	 * Create all words with a edit distance of two from the given input but
	 * return only those which were already learned. This should speed things up
	 * about 10% related to edits2.
	 * 
	 * @param word
	 *            a word
	 * @return all mutations with edit distance two which are known
	 */
	private Set<String> known_edits2(String word) {
		Set<String> result = new HashSet<String>();
		Set<String> edit1 = edits1(word);
		for (Iterator<String> iter = edit1.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			Set<String> edit1Inner = edits1(element);
			for (Iterator<String> iterator = edit1Inner.iterator(); iterator.hasNext();) {
				String element2 = (String) iterator.next();
				if (this.nwords.containsKey(element2))
					result.add(element2);
			}
		}
		// log("The number of known words with edit distance 2 from " + word + "
		// is " + result.size());
		return result;
	}

	private void log(String msg) {
		if (DEBUG)
			System.out.println(msg);
	}

	/**
	 * Get the best match from the set.
	 * 
	 * @param set
	 * @param word
	 * @return
	 */
	private String max(Set<String> set, String word) {
		int maxPc = 0;
		String bestMatch = word;
		log("max - num of candidates: " + set.size());
		for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			// log("Checking " + element);
			for (Iterator<String> iterator = this.nwords.keySet().iterator(); iterator.hasNext();) {
				String elem2 = (String) iterator.next();
				if (element.equalsIgnoreCase(elem2)) {
					int newPc = this.nwords.get(elem2);
					// log("newPc for " + elem2 + " is " + newPc);
					if (newPc > maxPc) {
						bestMatch = element;
						maxPc = newPc;
						log("New Best Match so far - Word: " + bestMatch + ", P(c): " + maxPc);
					}
				}
			}
		}
		return bestMatch;
	}

	@SuppressWarnings("unused")
	private String max2(Set<String> knownWordSet, Set<String> knownEdits1, Set<String> knownEdits2, String word) {
		String bestMatch = word;
		// any word in a set is infinitely more probable than
		// any word from the next set
		bestMatch = max(knownWordSet, word);
		if (knownWordSet.contains(bestMatch)) {
			log("max2: Word in known Word Set.");
			return bestMatch;
		}
		bestMatch = max(knownEdits1, word);
		if (knownEdits1.contains(bestMatch)) {
			log("max2: Word in Known Edits 1 Set.");
			return bestMatch;
		}
		bestMatch = max(knownEdits2, word);
		if (knownEdits2.contains(bestMatch)) {
			log("max2: Word in Known Edits 2 Set.");
			return bestMatch;
		} else {
			log("max2: Word not found. Using word itself.");
			Set<String> wordSet = new HashSet<String>();
			wordSet.add(word);
			return max(wordSet, word);
		}
	}

	private String max3(String word) {
		// known(wordSet), known(edits1(word)),known_edits2(word), word
		Set<String> wordSet = new HashSet<String>();
		wordSet.add(word);
		String bestMatch = word;
		// any word in a set is infinitely more probable than
		// any word from the next set
		Set<String> knownWordSet = known(wordSet);
		bestMatch = max(knownWordSet, word);
		if (knownWordSet.contains(bestMatch)) {
			log("max3: Word in known Word Set.");
			return bestMatch;
		}
		Set<String> knownEdits1 = known(edits1(word));
		bestMatch = max(knownEdits1, word);
		if (knownEdits1.contains(bestMatch)) {
			log("max3: Word in Known Edits 1 Set.");
			return bestMatch;
		}
		Set<String> knownEdits2 = known_edits2(word);
		bestMatch = max(knownEdits2, word);
		if (knownEdits2.contains(bestMatch)) {
			log("max3: Word in Known Edits 2 Set.");
			return bestMatch;
		} else {
			log("max3: Word not found. Using word itself.");
			wordSet = new HashSet<String>();
			wordSet.add(word);
			return max(wordSet, word);
		}
	}

	/**
	 * Merge to maps by adding the values of the entrys (if present)
	 * 
	 * @param map1
	 * @param map2
	 * @return
	 */
	private Map<String, Integer> mergeMaps(Map<String, Integer> map1, Map<String, Integer> map2) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (Iterator<String> iter = map1.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			if (map2.containsKey(key)) {
				result.put(key, map1.get(key) + map2.get(key));
			} else {
				result.put(key, map1.get(key));
			}
		}
		// add the remaining entrys from map2
		for (Iterator<String> iter = map2.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			if (!map1.containsKey(key)) {
				result.put(key, map2.get(key));
			}
		}
		return result;
	}

	@SuppressWarnings("unused")
	private int[] range(int end) {
		return range(0, end, 1);
	}

	@SuppressWarnings("unused")
	private int[] range(int begin, int end) {
		return range(begin, end, 1);
	}

	/**
	 * @link http://www.network-theory.co.uk/docs/pytut/rangeFunction.html
	 * @param begin
	 * @param end
	 * @return
	 */
	private int[] range(int begin, int end, int step) {
		int[] result = new int[end - begin];
		for (int i = 0; i < end; i = i + step) {
			result[i] = begin + i;
		}
		return result;
	}

	/**
	 * Train the features
	 * @param features
	 * @return
	 */
	private Map<String, Integer> train(String[] features) {
		Map<String, Integer> model = new HashMap<String, Integer>(new Integer(1));
		for (int i = 0; i < features.length; i++) {
			String key = features[i].toLowerCase();
			// the default value is one
			if (model.containsKey(key)) {
				model.put(key, new Integer(model.get(key).intValue() + 1));
			} else {
				model.put(key, new Integer(1));
			}
		}
		return model;
	}

	/**
	 * Extract all words from a given input text.
	 * 
	 * @param text
	 *            any text
	 * @return a array containing of all words TODO mit REGEXP \w+ überarbeiten
	 */
	private String[] words1(String[] text) {
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < text.length; i++) {
			StringTokenizer token = new StringTokenizer(text[i]);
			while (token.hasMoreTokens()) {
				String curToken = token.nextToken().trim();
				// only add this word if it has
				// at least two characters.
				if (curToken.length() > 1) {
					// remove trailing {.,!?}
					if (curToken.endsWith(".") || curToken.endsWith(",") || curToken.endsWith("!")
							|| curToken.endsWith("?")) {
						words.add(curToken.substring(0, curToken.length() - 1).toLowerCase().trim());
					} else {
						words.add(curToken.toLowerCase().trim());
					}
				}
			}
		}
		return words.toArray(new String[1]);
	}

	private String[] words(String[] text) {
		return this.words1(text);
		//return this.words2(text);
	}

	/**
	 * This will tokenize the text by splitting it up by whitespace, newline or
	 * tabs and remove any non-alpha-numerical characters.
	 * 
	 * @param text
	 * @return
	 */
	@SuppressWarnings("unused")
	private String[] words2(String[] text) {
		String pat = "[^\\w+]";
		String rep = " ";
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < text.length; i++) {
			StringTokenizer token = new StringTokenizer(replace(text[i], pat, rep));
			while (token.hasMoreTokens()) {
				String curToken = token.nextToken().trim();
				// only add this word if it has
				// at least two characters.
				if (curToken.length() > 1) {
					// remove trailing {.,!?}
					// words.add(WikiMarkup.replace(curToken, pat, rep).trim());
					words.add(curToken);
				}
			}
		}
		return words.toArray(new String[1]);
	}

	public static String replace(String in, String pat, String rep) {
		Pattern pattern = Pattern.compile(pat);
		Matcher matcher = pattern.matcher(in);
		return matcher.replaceAll(rep);
	}

	public void tests3(boolean verbose) {
		String[][] tests = new String[6][2];
		int i = 0;
		tests[i][0] = "forbidden";
		tests[i++][1] = "forbiden";
		tests[i][0] = "comments";
		tests[i++][1] = "coments";
		tests[i][0] = "decisions";
		tests[i++][1] = "deciscions";
		tests[i][0] = "decisions";
		tests[i++][1] = "descisions";
		tests[i][0] = "supposedly";
		tests[i++][1] = "supposidly";
		tests[i][0] = "embellishing";
		tests[i++][1] = "embelishing";
		System.out.println(this.spelltest(tests, 0, verbose));
	}

	public void tests2(boolean verbose) {
		String[][] tests = new String[400][2];
		int i = 0;
		tests[i][0] = "forbidden";
		tests[i++][1] = "forbiden";
		tests[i][0] = "comments";
		tests[i++][1] = "coments";
		tests[i][0] = "decisions";
		tests[i++][1] = "deciscions";
		tests[i][0] = "decisions";
		tests[i++][1] = "descisions";
		tests[i][0] = "supposedly";
		tests[i++][1] = "supposidly";
		tests[i][0] = "embellishing";
		tests[i++][1] = "embelishing";
		tests[i][0] = "technique";
		tests[i++][1] = "tecnique";
		tests[i][0] = "permanently";
		tests[i++][1] = "perminantly";
		tests[i][0] = "confirmation";
		tests[i++][1] = "confermation";
		tests[i][0] = "appointment";
		tests[i++][1] = "appoitment";
		tests[i][0] = "continued";
		tests[i++][1] = "contuned";
		tests[i][0] = "progression";
		tests[i++][1] = "progresion";
		tests[i][0] = "accompanying";
		tests[i++][1] = "acompaning";
		tests[i][0] = "applicable";
		tests[i++][1] = "aplicable";
		tests[i][0] = "regained";
		tests[i++][1] = "regined";
		tests[i][0] = "guidelines";
		tests[i++][1] = "guidlines";
		tests[i][0] = "surrounding";
		tests[i++][1] = "serounding";
		tests[i][0] = "titles";
		tests[i++][1] = "tittles";
		tests[i][0] = "unavailable";
		tests[i++][1] = "unavailble";
		tests[i][0] = "advantageous";
		tests[i++][1] = "advantageos";
		tests[i][0] = "brief";
		tests[i++][1] = "brif";
		tests[i][0] = "appeal";
		tests[i++][1] = "apeal";
		tests[i][0] = "consisting";
		tests[i++][1] = "consisiting";
		tests[i][0] = "clerk";
		tests[i++][1] = "cleark";
		tests[i][0] = "clerk";
		tests[i++][1] = "clerck";
		tests[i][0] = "commercial";
		tests[i++][1] = "comersial";
		tests[i][0] = "favourable";
		tests[i++][1] = "faverable";
		tests[i][0] = "separation";
		tests[i++][1] = "seperation";
		tests[i][0] = "search";
		tests[i++][1] = "serch";
		tests[i][0] = "receive";
		tests[i++][1] = "recieve";
		tests[i][0] = "employees";
		tests[i++][1] = "emploies";
		tests[i][0] = "prior";
		tests[i++][1] = "piror";
		tests[i][0] = "resulting";
		tests[i++][1] = "reulting";
		tests[i][0] = "suggestion";
		tests[i++][1] = "sugestion";
		tests[i][0] = "opinion";
		tests[i++][1] = "oppinion";
		tests[i][0] = "cancellation";
		tests[i++][1] = "cancelation";
		tests[i][0] = "composed";
		tests[i++][1] = "compossed";
		tests[i][0] = "useful";
		tests[i++][1] = "usful";
		tests[i][0] = "humour";
		tests[i++][1] = "humor";
		tests[i][0] = "anomalies";
		tests[i++][1] = "anomolies";
		tests[i][0] = "would";
		tests[i++][1] = "whould";
		tests[i][0] = "doubt";
		tests[i++][1] = "doupt";
		tests[i][0] = "examination";
		tests[i++][1] = "eximination";
		tests[i][0] = "therefore";
		tests[i++][1] = "therefoe";
		tests[i][0] = "recommend";
		tests[i++][1] = "recomend";
		tests[i][0] = "separated";
		tests[i++][1] = "seperated";
		tests[i][0] = "successful";
		tests[i++][1] = "sucssuful";
		tests[i][0] = "successful";
		tests[i++][1] = "succesful";
		tests[i][0] = "apparent";
		tests[i++][1] = "apparant";
		tests[i][0] = "occurred";
		tests[i++][1] = "occureed";
		tests[i][0] = "particular";
		tests[i++][1] = "paerticulaur";
		tests[i][0] = "pivoting";
		tests[i++][1] = "pivting";
		tests[i][0] = "announcing";
		tests[i++][1] = "anouncing";
		tests[i][0] = "challenge";
		tests[i++][1] = "chalange";
		tests[i][0] = "arrangements";
		tests[i++][1] = "araingements";
		tests[i][0] = "proportions";
		tests[i++][1] = "proprtions";
		tests[i][0] = "organized";
		tests[i++][1] = "oranised";
		tests[i][0] = "accept";
		tests[i++][1] = "acept";
		tests[i][0] = "dependence";
		tests[i++][1] = "dependance";
		tests[i][0] = "unequalled";
		tests[i++][1] = "unequaled";
		tests[i][0] = "numbers";
		tests[i++][1] = "numbuers";
		tests[i][0] = "sense";
		tests[i++][1] = "sence";
		tests[i][0] = "conversely";
		tests[i++][1] = "conversly";
		tests[i][0] = "provide";
		tests[i++][1] = "provid";
		tests[i][0] = "arrangement";
		tests[i++][1] = "arrangment";
		tests[i][0] = "responsibilities";
		tests[i++][1] = "responsiblities";
		tests[i][0] = "fourth";
		tests[i++][1] = "forth";
		tests[i][0] = "ordinary";
		tests[i++][1] = "ordenary";
		tests[i][0] = "description";
		tests[i++][1] = "desription";
		tests[i][0] = "description";
		tests[i++][1] = "descvription";
		tests[i][0] = "description";
		tests[i++][1] = "desacription";
		tests[i][0] = "inconceivable";
		tests[i++][1] = "inconcievable";
		tests[i][0] = "data";
		tests[i++][1] = "dsata";
		tests[i][0] = "register";
		tests[i++][1] = "rgister";
		tests[i][0] = "supervision";
		tests[i++][1] = "supervison";
		tests[i][0] = "encompassing";
		tests[i++][1] = "encompasing";
		tests[i][0] = "negligible";
		tests[i++][1] = "negligable";
		tests[i][0] = "allow";
		tests[i++][1] = "alow";
		tests[i][0] = "operations";
		tests[i++][1] = "operatins";
		tests[i][0] = "executed";
		tests[i++][1] = "executted";
		tests[i][0] = "interpretation";
		tests[i++][1] = "interpritation";
		tests[i][0] = "hierarchy";
		tests[i++][1] = "heiarky";
		tests[i][0] = "indeed";
		tests[i++][1] = "indead";
		tests[i][0] = "years";
		tests[i++][1] = "yesars";
		tests[i][0] = "through";
		tests[i++][1] = "throut";
		tests[i][0] = "committee";
		tests[i++][1] = "committe";
		tests[i][0] = "inquiries";
		tests[i++][1] = "equiries";
		tests[i][0] = "before";
		tests[i++][1] = "befor";
		tests[i][0] = "interesting";
		tests[i++][1] = "intresting";
		tests[i][0] = "permanent";
		tests[i++][1] = "perminant";
		tests[i][0] = "choose";
		tests[i++][1] = "chose";
		tests[i][0] = "virtually";
		tests[i++][1] = "vertually";
		tests[i][0] = "correspondence";
		tests[i++][1] = "correspondance";
		tests[i][0] = "eventually";
		tests[i++][1] = "eventully";
		tests[i][0] = "lonely";
		tests[i++][1] = "lonley";
		tests[i][0] = "profession";
		tests[i++][1] = "preffeson";
		tests[i][0] = "they";
		tests[i++][1] = "thay";
		tests[i][0] = "now";
		tests[i++][1] = "noe";
		tests[i][0] = "desperately";
		tests[i++][1] = "despratly";
		tests[i][0] = "university";
		tests[i++][1] = "unversity";
		tests[i][0] = "adjournment";
		tests[i++][1] = "adjurnment";
		tests[i][0] = "possibilities";
		tests[i++][1] = "possablities";
		tests[i][0] = "stopped";
		tests[i++][1] = "stoped";
		tests[i][0] = "mean";
		tests[i++][1] = "meen";
		tests[i][0] = "weighted";
		tests[i++][1] = "wagted";
		tests[i][0] = "adequately";
		tests[i++][1] = "adequattly";
		tests[i][0] = "shown";
		tests[i++][1] = "hown";
		tests[i][0] = "matrix";
		tests[i++][1] = "matriiix";
		tests[i][0] = "profit";
		tests[i++][1] = "proffit";
		tests[i][0] = "encourage";
		tests[i++][1] = "encorage";
		tests[i][0] = "collate";
		tests[i++][1] = "colate";
		tests[i][0] = "disaggregate";
		tests[i++][1] = "disaggreagte";
		tests[i][0] = "disaggregate";
		tests[i++][1] = "disaggreaget";
		tests[i][0] = "receiving";
		tests[i++][1] = "recieving";
		tests[i][0] = "receiving";
		tests[i++][1] = "reciving";
		tests[i][0] = "proviso";
		tests[i++][1] = "provisoe";
		tests[i][0] = "umbrella";
		tests[i++][1] = "umberalla";
		tests[i][0] = "approached";
		tests[i++][1] = "aproached";
		tests[i][0] = "pleasant";
		tests[i++][1] = "plesent";
		tests[i][0] = "difficulty";
		tests[i++][1] = "dificulty";
		tests[i][0] = "appointments";
		tests[i++][1] = "apointments";
		tests[i][0] = "base";
		tests[i++][1] = "basse";
		tests[i][0] = "conditioning";
		tests[i++][1] = "conditining";
		tests[i][0] = "earliest";
		tests[i++][1] = "earlyest";
		tests[i][0] = "beginning";
		tests[i++][1] = "begining";
		tests[i][0] = "universally";
		tests[i++][1] = "universaly";
		tests[i][0] = "unresolved";
		tests[i++][1] = "unresloved";
		tests[i][0] = "length";
		tests[i++][1] = "lengh";
		tests[i][0] = "exponentially";
		tests[i++][1] = "exponentualy";
		tests[i][0] = "utilized";
		tests[i++][1] = "utalised";
		tests[i][0] = "set";
		tests[i++][1] = "et";
		tests[i][0] = "surveys";
		tests[i++][1] = "servays";
		tests[i][0] = "system";
		tests[i++][1] = "sysem";
		tests[i][0] = "approximately";
		tests[i++][1] = "aproximatly";
		tests[i][0] = "their";
		tests[i++][1] = "ther";
		tests[i][0] = "scheme";
		tests[i++][1] = "scheem";
		tests[i][0] = "speaking";
		tests[i++][1] = "speeking";
		tests[i][0] = "repetitive";
		tests[i++][1] = "repetative";
		tests[i][0] = "inefficient";
		tests[i++][1] = "ineffiect";
		tests[i][0] = "geneva";
		tests[i++][1] = "geniva";
		tests[i][0] = "exactly";
		tests[i++][1] = "exsactly";
		tests[i][0] = "immediate";
		tests[i++][1] = "imediate";
		tests[i][0] = "appreciation";
		tests[i++][1] = "apreciation";
		tests[i][0] = "luckily";
		tests[i++][1] = "luckeley";
		tests[i][0] = "eliminated";
		tests[i++][1] = "elimiated";
		tests[i][0] = "believe";
		tests[i++][1] = "belive";
		tests[i][0] = "appreciated";
		tests[i++][1] = "apreciated";
		tests[i][0] = "readjusted";
		tests[i++][1] = "reajusted";
		tests[i][0] = "were";
		tests[i++][1] = "wer";
		tests[i][0] = "were";
		tests[i++][1] = "where";
		tests[i][0] = "feeling";
		tests[i++][1] = "fealing";
		tests[i][0] = "and";
		tests[i++][1] = "anf";
		tests[i][0] = "false";
		tests[i++][1] = "faulse";
		tests[i][0] = "seen";
		tests[i++][1] = "seeen";
		tests[i][0] = "interrogating";
		tests[i++][1] = "interogationg";
		tests[i][0] = "academically";
		tests[i++][1] = "academicly";
		tests[i][0] = "relatively";
		tests[i++][1] = "relativly";
		tests[i][0] = "relatively";
		tests[i++][1] = "relitivly";
		tests[i][0] = "traditionally";
		tests[i++][1] = "traditionaly";
		tests[i][0] = "studying";
		tests[i++][1] = "studing";
		tests[i][0] = "majority";
		tests[i++][1] = "majorty";
		tests[i][0] = "build";
		tests[i++][1] = "biuld";
		tests[i][0] = "aggravating";
		tests[i++][1] = "agravating";
		tests[i][0] = "transactions";
		tests[i++][1] = "trasactions";
		tests[i][0] = "arguing";
		tests[i++][1] = "aurguing";
		tests[i][0] = "sheets";
		tests[i++][1] = "sheertes";
		tests[i][0] = "successive";
		tests[i++][1] = "sucsesive";
		tests[i][0] = "successive";
		tests[i++][1] = "sucessive";
		tests[i][0] = "extremely";
		tests[i++][1] = "extreemly";
		tests[i][0] = "especially";
		tests[i++][1] = "especaily";
		tests[i][0] = "later";
		tests[i++][1] = "latter";
		tests[i][0] = "senior";
		tests[i++][1] = "sienior";
		tests[i][0] = "dragged";
		tests[i++][1] = "draged";
		tests[i][0] = "atmosphere";
		tests[i++][1] = "atmospher";
		tests[i][0] = "drastically";
		tests[i++][1] = "drasticaly";
		tests[i][0] = "particularly";
		tests[i++][1] = "particulary";
		tests[i][0] = "visitor";
		tests[i++][1] = "vistor";
		tests[i][0] = "session";
		tests[i++][1] = "sesion";
		tests[i][0] = "continually";
		tests[i++][1] = "contually";
		tests[i][0] = "availability";
		tests[i++][1] = "avaiblity";
		tests[i][0] = "busy";
		tests[i++][1] = "buisy";
		tests[i][0] = "parameters";
		tests[i++][1] = "perametres";
		tests[i][0] = "surroundings";
		tests[i++][1] = "suroundings";
		tests[i][0] = "surroundings";
		tests[i++][1] = "seroundings";
		tests[i][0] = "employed";
		tests[i++][1] = "emploied";
		tests[i][0] = "adequate";
		tests[i++][1] = "adiquate";
		tests[i][0] = "handle";
		tests[i++][1] = "handel";
		tests[i][0] = "means";
		tests[i++][1] = "meens";
		tests[i][0] = "familiar";
		tests[i++][1] = "familer";
		tests[i][0] = "between";
		tests[i++][1] = "beeteen";
		tests[i][0] = "overall";
		tests[i++][1] = "overal";
		tests[i][0] = "timing";
		tests[i++][1] = "timeing";
		tests[i][0] = "committees";
		tests[i++][1] = "comittees";
		tests[i][0] = "committees";
		tests[i++][1] = "commitees";
		tests[i][0] = "queries";
		tests[i++][1] = "quies";
		tests[i][0] = "econometric";
		tests[i++][1] = "economtric";
		tests[i][0] = "erroneous";
		tests[i++][1] = "errounous";
		tests[i][0] = "decides";
		tests[i++][1] = "descides";
		tests[i][0] = "reference";
		tests[i++][1] = "refereence";
		tests[i][0] = "reference";
		tests[i++][1] = "refference";
		tests[i][0] = "intelligence";
		tests[i++][1] = "inteligence";
		tests[i][0] = "edition";
		tests[i++][1] = "ediion";
		tests[i][0] = "edition";
		tests[i++][1] = "ediition";
		tests[i][0] = "are";
		tests[i++][1] = "arte";
		tests[i][0] = "apologies";
		tests[i++][1] = "appologies";
		tests[i][0] = "thermawear";
		tests[i++][1] = "thermawere";
		tests[i][0] = "thermawear";
		tests[i++][1] = "thermawhere";
		tests[i][0] = "techniques";
		tests[i++][1] = "tecniques";
		tests[i][0] = "voluntary";
		tests[i++][1] = "volantary";
		tests[i][0] = "subsequent";
		tests[i++][1] = "subsequant";
		tests[i][0] = "subsequent";
		tests[i++][1] = "subsiquent";
		tests[i][0] = "currently";
		tests[i++][1] = "curruntly";
		tests[i][0] = "forecast";
		tests[i++][1] = "forcast";
		tests[i][0] = "weapons";
		tests[i++][1] = "wepons";
		tests[i][0] = "routine";
		tests[i++][1] = "rouint";
		tests[i][0] = "neither";
		tests[i++][1] = "niether";
		tests[i][0] = "approach";
		tests[i++][1] = "aproach";
		tests[i][0] = "available";
		tests[i++][1] = "availble";
		tests[i][0] = "recently";
		tests[i++][1] = "reciently";
		tests[i][0] = "ability";
		tests[i++][1] = "ablity";
		tests[i][0] = "nature";
		tests[i++][1] = "natior";
		tests[i][0] = "component";
		tests[i++][1] = "componant";
		tests[i][0] = "agencies";
		tests[i++][1] = "agences";
		tests[i][0] = "however";
		tests[i++][1] = "howeverr";
		tests[i][0] = "suggested";
		tests[i++][1] = "sugested";
		tests[i][0] = "career";
		tests[i++][1] = "carear";
		tests[i][0] = "many";
		tests[i++][1] = "mony";
		tests[i][0] = "annual";
		tests[i++][1] = "anual";
		tests[i][0] = "according";
		tests[i++][1] = "acording";
		tests[i][0] = "receives";
		tests[i++][1] = "recives";
		tests[i][0] = "receives";
		tests[i++][1] = "recieves";
		tests[i][0] = "expense";
		tests[i++][1] = "expence";
		tests[i][0] = "relevant";
		tests[i++][1] = "relavent";
		tests[i][0] = "relevant";
		tests[i++][1] = "relevaant";
		tests[i][0] = "table";
		tests[i++][1] = "tasble";
		tests[i][0] = "throughout";
		tests[i++][1] = "throuout";
		tests[i][0] = "conference";
		tests[i++][1] = "conferance";
		tests[i][0] = "sensible";
		tests[i++][1] = "sensable";
		tests[i][0] = "described";
		tests[i++][1] = "discribed";
		tests[i][0] = "described";
		tests[i++][1] = "describd";
		tests[i][0] = "union";
		tests[i++][1] = "unioun";
		tests[i][0] = "interest";
		tests[i++][1] = "intrest";
		tests[i][0] = "flexible";
		tests[i++][1] = "flexable";
		tests[i][0] = "refered";
		tests[i++][1] = "reffered";
		tests[i][0] = "families";
		tests[i++][1] = "familys";
		tests[i][0] = "sufficient";
		tests[i++][1] = "suficient";
		tests[i][0] = "dissension";
		tests[i++][1] = "desention";
		tests[i][0] = "adaptable";
		tests[i++][1] = "adabtable";
		tests[i][0] = "representative";
		tests[i++][1] = "representitive";
		tests[i][0] = "irrelevant";
		tests[i++][1] = "irrelavent";
		tests[i][0] = "unnecessarily";
		tests[i++][1] = "unessasarily";
		tests[i][0] = "applied";
		tests[i++][1] = "upplied";
		tests[i][0] = "apologised";
		tests[i++][1] = "appologised";
		tests[i][0] = "these";
		tests[i++][1] = "thees";
		tests[i][0] = "these";
		tests[i++][1] = "thess";
		tests[i][0] = "choices";
		tests[i++][1] = "choises";
		tests[i][0] = "will";
		tests[i++][1] = "wil";
		tests[i][0] = "procedure";
		tests[i++][1] = "proceduer";
		tests[i][0] = "shortened";
		tests[i++][1] = "shortend";
		tests[i][0] = "manually";
		tests[i++][1] = "manualy";
		tests[i][0] = "disappointing";
		tests[i++][1] = "dissapoiting";
		tests[i][0] = "excessively";
		tests[i++][1] = "exessively";
		tests[i][0] = "containing";
		tests[i++][1] = "containg";
		tests[i][0] = "develop";
		tests[i++][1] = "develope";
		tests[i][0] = "credit";
		tests[i++][1] = "creadit";
		tests[i][0] = "government";
		tests[i++][1] = "goverment";
		tests[i][0] = "acquaintances";
		tests[i++][1] = "aquantences";
		tests[i][0] = "orientated";
		tests[i++][1] = "orentated";
		tests[i][0] = "widely";
		tests[i++][1] = "widly";
		tests[i][0] = "advise";
		tests[i++][1] = "advice";
		tests[i][0] = "difficult";
		tests[i++][1] = "dificult";
		tests[i][0] = "investigated";
		tests[i++][1] = "investegated";
		tests[i][0] = "bonus";
		tests[i++][1] = "bonas";
		tests[i][0] = "conceived";
		tests[i++][1] = "concieved";
		tests[i][0] = "nationally";
		tests[i++][1] = "nationaly";
		tests[i][0] = "compared";
		tests[i++][1] = "comppared";
		tests[i][0] = "compared";
		tests[i++][1] = "compased";
		tests[i][0] = "moving";
		tests[i++][1] = "moveing";
		tests[i][0] = "necessity";
		tests[i++][1] = "nessesity";
		tests[i][0] = "opportunity";
		tests[i++][1] = "oppertunity";
		tests[i][0] = "opportunity";
		tests[i++][1] = "oppotunity";
		tests[i][0] = "opportunity";
		tests[i++][1] = "opperttunity";
		tests[i][0] = "thoughts";
		tests[i++][1] = "thorts";
		tests[i][0] = "equalled";
		tests[i++][1] = "equaled";
		tests[i][0] = "scrutinized";
		tests[i++][1] = "scrutiniesed";
		tests[i][0] = "analysis";
		tests[i++][1] = "analiss";
		tests[i][0] = "analysis";
		tests[i++][1] = "analsis";
		tests[i][0] = "analysis";
		tests[i++][1] = "analisis";
		tests[i][0] = "patterns";
		tests[i++][1] = "pattarns";
		tests[i][0] = "qualities";
		tests[i++][1] = "quaties";
		tests[i][0] = "easily";
		tests[i++][1] = "easyly";
		tests[i][0] = "organization";
		tests[i++][1] = "oranisation";
		tests[i][0] = "organization";
		tests[i++][1] = "oragnisation";
		tests[i][0] = "the";
		tests[i++][1] = "thw";
		tests[i][0] = "the";
		tests[i++][1] = "hte";
		tests[i][0] = "the";
		tests[i++][1] = "thi";
		tests[i][0] = "corporate";
		tests[i++][1] = "corparate";
		tests[i][0] = "criticism";
		tests[i++][1] = "citisum";
		tests[i][0] = "enormously";
		tests[i++][1] = "enomosly";
		tests[i][0] = "financially";
		tests[i++][1] = "financialy";
		tests[i][0] = "functionally";
		tests[i++][1] = "functionaly";
		tests[i][0] = "discipline";
		tests[i++][1] = "disiplin";
		tests[i][0] = "announcement";
		tests[i++][1] = "anouncement";
		tests[i][0] = "progresses";
		tests[i++][1] = "progressess";
		tests[i][0] = "except";
		tests[i++][1] = "excxept";
		tests[i][0] = "recommending";
		tests[i++][1] = "recomending";
		tests[i][0] = "mathematically";
		tests[i++][1] = "mathematicaly";
		tests[i][0] = "source";
		tests[i++][1] = "sorce";
		tests[i][0] = "combine";
		tests[i++][1] = "comibine";
		tests[i][0] = "input";
		tests[i++][1] = "inut";
		tests[i][0] = "careers";
		tests[i++][1] = "currers";
		tests[i][0] = "careers";
		tests[i++][1] = "carrers";
		tests[i][0] = "resolved";
		tests[i++][1] = "resoved";
		tests[i][0] = "demands";
		tests[i++][1] = "diemands";
		tests[i][0] = "unequivocally";
		tests[i++][1] = "unequivocaly";
		tests[i][0] = "suffering";
		tests[i++][1] = "suufering";
		tests[i][0] = "immediately";
		tests[i++][1] = "imidatly";
		tests[i][0] = "immediately";
		tests[i++][1] = "imediatly";
		tests[i][0] = "accepted";
		tests[i++][1] = "acepted";
		tests[i][0] = "projects";
		tests[i++][1] = "projeccts";
		tests[i][0] = "necessary";
		tests[i++][1] = "necasery";
		tests[i][0] = "necessary";
		tests[i++][1] = "nessasary";
		tests[i][0] = "necessary";
		tests[i++][1] = "nessisary";
		tests[i][0] = "necessary";
		tests[i++][1] = "neccassary";
		tests[i][0] = "journalism";
		tests[i++][1] = "journaism";
		tests[i][0] = "unnecessary";
		tests[i++][1] = "unessessay";
		tests[i][0] = "night";
		tests[i++][1] = "nite";
		tests[i][0] = "output";
		tests[i++][1] = "oputput";
		tests[i][0] = "security";
		tests[i++][1] = "seurity";
		tests[i][0] = "essential";
		tests[i++][1] = "esential";
		tests[i][0] = "beneficial";
		tests[i++][1] = "benificial";
		tests[i][0] = "beneficial";
		tests[i++][1] = "benficial";
		tests[i][0] = "requested";
		tests[i++][1] = "rquested";
		tests[i][0] = "supplementary";
		tests[i++][1] = "suplementary";
		tests[i][0] = "questionnaire";
		tests[i++][1] = "questionare";
		tests[i][0] = "employment";
		tests[i++][1] = "empolyment";
		tests[i][0] = "proceeding";
		tests[i++][1] = "proceding";
		tests[i][0] = "decision";
		tests[i++][1] = "descisions";
		tests[i][0] = "decision";
		tests[i++][1] = "descision";
		tests[i][0] = "per";
		tests[i++][1] = "pere";
		tests[i][0] = "discretion";
		tests[i++][1] = "discresion";
		tests[i][0] = "reaching";
		tests[i++][1] = "reching";
		tests[i][0] = "analysed";
		tests[i++][1] = "analised";
		tests[i][0] = "expansion";
		tests[i++][1] = "expanion";
		tests[i][0] = "although";
		tests[i++][1] = "athough";
		tests[i][0] = "subtract";
		tests[i++][1] = "subtrcat";
		tests[i][0] = "analysing";
		tests[i++][1] = "aalysing";
		tests[i][0] = "comparison";
		tests[i++][1] = "comparrison";
		tests[i][0] = "months";
		tests[i++][1] = "monthes";
		tests[i][0] = "hierarchal";
		tests[i++][1] = "hierachial";
		tests[i][0] = "misleading";
		tests[i++][1] = "missleading";
		tests[i][0] = "commit";
		tests[i++][1] = "comit";
		tests[i][0] = "auguments";
		tests[i++][1] = "aurgument";
		tests[i][0] = "within";
		tests[i++][1] = "withing";
		tests[i][0] = "obtaining";
		tests[i++][1] = "optaning";
		tests[i][0] = "accounts";
		tests[i++][1] = "acounts";
		tests[i][0] = "primarily";
		tests[i++][1] = "pimarily";
		tests[i][0] = "operator";
		tests[i++][1] = "opertor";
		tests[i][0] = "accumulated";
		tests[i++][1] = "acumulated";
		tests[i][0] = "segment";
		tests[i++][1] = "segemnt";
		tests[i][0] = "there";
		tests[i++][1] = "thear";
		tests[i][0] = "summarys";
		tests[i++][1] = "sumarys";
		tests[i][0] = "analyse";
		tests[i++][1] = "analiss";
		tests[i][0] = "understandable";
		tests[i++][1] = "understadable";
		tests[i][0] = "safeguard";
		tests[i++][1] = "safegaurd";
		tests[i][0] = "consist";
		tests[i++][1] = "consisit";
		tests[i][0] = "declarations";
		tests[i++][1] = "declaratrions";
		tests[i][0] = "minutes";
		tests[i++][1] = "muinutes";
		tests[i][0] = "minutes";
		tests[i++][1] = "muiuets";
		tests[i][0] = "associated";
		tests[i++][1] = "assosiated";
		tests[i][0] = "accessibility";
		tests[i++][1] = "accessability";
		tests[i][0] = "examine";
		tests[i++][1] = "examin";
		tests[i][0] = "surveying";
		tests[i++][1] = "servaying";
		tests[i][0] = "politics";
		tests[i++][1] = "polatics";
		tests[i][0] = "annoying";
		tests[i++][1] = "anoying";
		tests[i][0] = "again";
		tests[i++][1] = "agiin";
		tests[i][0] = "assessing";
		tests[i++][1] = "accesing";
		tests[i][0] = "ideally";
		tests[i++][1] = "idealy";
		tests[i][0] = "variety";
		tests[i++][1] = "variatry";
		tests[i][0] = "simular";
		tests[i++][1] = "similar";
		tests[i][0] = "personnel";
		tests[i++][1] = "personel";
		tests[i][0] = "whereas";
		tests[i++][1] = "wheras";
		tests[i][0] = "when";
		tests[i++][1] = "whn";
		tests[i][0] = "geographically";
		tests[i++][1] = "goegraphicaly";
		tests[i][0] = "gaining";
		tests[i++][1] = "ganing";
		tests[i][0] = "explaining";
		tests[i++][1] = "explaning";
		tests[i][0] = "separate";
		tests[i++][1] = "seporate";
		tests[i][0] = "students";
		tests[i++][1] = "studens";
		tests[i][0] = "prepared";
		tests[i++][1] = "prepaired";
		tests[i][0] = "generated";
		tests[i++][1] = "generataed";
		tests[i][0] = "graphically";
		tests[i++][1] = "graphicaly";
		tests[i][0] = "suited";
		tests[i++][1] = "suted";
		tests[i][0] = "variable";
		tests[i++][1] = "varible";
		tests[i][0] = "variable";
		tests[i++][1] = "vaiable";
		tests[i][0] = "building";
		tests[i++][1] = "biulding";
		tests[i][0] = "controlled";
		tests[i++][1] = "controled";
		tests[i][0] = "required";
		tests[i++][1] = "reequired";
		tests[i][0] = "necessitates";
		tests[i++][1] = "nessisitates";
		tests[i][0] = "together";
		tests[i++][1] = "togehter";
		tests[i][0] = "profits";
		tests[i++][1] = "proffits";
		System.out.println(spelltest(tests, 0, verbose));
	}

	public void tests1(boolean verbose) {
		String[][] tests = new String[270][2];
		int i = 0;
		tests[i][0] = "consider";
		tests[i++][1] = "concider";
		tests[i][0] = "hierarchy";
		tests[i++][1] = "hierchy";
		tests[i][0] = "valuable";
		tests[i++][1] = "valubale";
		tests[i][0] = "valuable";
		tests[i++][1] = "valuble";
		tests[i][0] = "sources";
		tests[i++][1] = "sorces";
		tests[i][0] = "committee";
		tests[i++][1] = "comittee";
		tests[i][0] = "transportability";
		tests[i++][1] = "transportibility";
		tests[i][0] = "minuscule";
		tests[i++][1] = "miniscule";
		tests[i][0] = "singular";
		tests[i++][1] = "singulaur";
		tests[i][0] = "cemetery";
		tests[i++][1] = "cemetary";
		tests[i][0] = "cemetery";
		tests[i++][1] = "semetary";
		tests[i][0] = "refreshment";
		tests[i++][1] = "reafreshment";
		tests[i][0] = "refreshment";
		tests[i++][1] = "refreshmant";
		tests[i][0] = "refreshment";
		tests[i++][1] = "refresment";
		tests[i][0] = "refreshment";
		tests[i++][1] = "refressmunt";
		tests[i][0] = "totally";
		tests[i++][1] = "totaly";
		tests[i][0] = "centrally";
		tests[i++][1] = "centraly";
		tests[i][0] = "meant";
		tests[i++][1] = "ment";
		tests[i][0] = "someone";
		tests[i++][1] = "somone";
		tests[i][0] = "possible";
		tests[i++][1] = "possable";
		tests[i][0] = "choice";
		tests[i++][1] = "choise";
		tests[i][0] = "decide";
		tests[i++][1] = "descide";
		tests[i][0] = "awful";
		tests[i++][1] = "awfall";
		tests[i][0] = "awful";
		tests[i++][1] = "afful";
		tests[i][0] = "unique";
		tests[i++][1] = "uneque";
		tests[i][0] = "articles";
		tests[i++][1] = "articals";
		tests[i][0] = "february";
		tests[i++][1] = "febuary";
		tests[i][0] = "necessary";
		tests[i++][1] = "neccesary";
		tests[i][0] = "necessary";
		tests[i++][1] = "necesary";
		tests[i][0] = "necessary";
		tests[i++][1] = "neccesary";
		tests[i][0] = "necessary";
		tests[i++][1] = "necassary";
		tests[i][0] = "necessary";
		tests[i++][1] = "necassery";
		tests[i][0] = "necessary";
		tests[i++][1] = "neccasary";
		tests[i][0] = "level";
		tests[i++][1] = "leval";
		tests[i][0] = "establishing";
		tests[i++][1] = "astablishing";
		tests[i][0] = "establishing";
		tests[i++][1] = "establising";
		tests[i][0] = "remind";
		tests[i++][1] = "remine";
		tests[i][0] = "remind";
		tests[i++][1] = "remined";
		tests[i][0] = "benefit";
		tests[i++][1] = "benifit";
		tests[i][0] = "addressable";
		tests[i++][1] = "addresable";
		tests[i][0] = "biscuits";
		tests[i++][1] = "biscits";
		tests[i][0] = "biscuits";
		tests[i++][1] = "biscutes";
		tests[i][0] = "biscuits";
		tests[i++][1] = "biscuts";
		tests[i][0] = "biscuits";
		tests[i++][1] = "bisquits";
		tests[i][0] = "biscuits";
		tests[i++][1] = "buiscits";
		tests[i][0] = "biscuits";
		tests[i++][1] = "buiscuts";
		tests[i][0] = "wrote";
		tests[i++][1] = "rote";
		tests[i][0] = "wrote";
		tests[i++][1] = "wote";
		tests[i][0] = "unexpected";
		tests[i++][1] = "unexpcted";
		tests[i][0] = "unexpected";
		tests[i++][1] = "unexpeted";
		tests[i][0] = "unexpected";
		tests[i++][1] = "unexspected";
		tests[i][0] = "bicycle";
		tests[i++][1] = "bicycal";
		tests[i][0] = "bicycle";
		tests[i++][1] = "bycicle";
		tests[i][0] = "bicycle";
		tests[i++][1] = "bycycle";
		tests[i][0] = "often";
		tests[i++][1] = "ofen";
		tests[i][0] = "often";
		tests[i++][1] = "offen";
		tests[i][0] = "often";
		tests[i++][1] = "offten";
		tests[i][0] = "often";
		tests[i++][1] = "ofton";
		tests[i][0] = "there";
		tests[i++][1] = "ther";
		tests[i][0] = "receipt";
		tests[i++][1] = "receit";
		tests[i][0] = "receipt";
		tests[i++][1] = "receite";
		tests[i][0] = "receipt";
		tests[i++][1] = "reciet";
		tests[i][0] = "receipt";
		tests[i++][1] = "recipt";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnificnet";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magificent";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnifcent";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnifecent";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnifiscant";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnifisent";
		tests[i][0] = "magnificent";
		tests[i++][1] = "magnificant";
		tests[i][0] = "challenges";
		tests[i++][1] = "chalenges";
		tests[i][0] = "challenges";
		tests[i++][1] = "chalenges";
		tests[i][0] = "decided";
		tests[i++][1] = "descided";
		tests[i][0] = "choosing";
		tests[i++][1] = "chosing";
		tests[i][0] = "further";
		tests[i++][1] = "futher";
		tests[i][0] = "questionnaire";
		tests[i++][1] = "questionaire";
		tests[i][0] = "special";
		tests[i++][1] = "speaical";
		tests[i][0] = "special";
		tests[i++][1] = "specail";
		tests[i][0] = "special";
		tests[i++][1] = "specal";
		tests[i][0] = "special";
		tests[i++][1] = "speical";
		tests[i][0] = "really";
		tests[i++][1] = "realy";
		tests[i][0] = "really";
		tests[i++][1] = "relley";
		tests[i][0] = "really";
		tests[i++][1] = "relly";
		tests[i][0] = "fails";
		tests[i++][1] = "failes";
		tests[i][0] = "voluntary";
		tests[i++][1] = "volantry";
		tests[i][0] = "purple";
		tests[i++][1] = "perple";
		tests[i][0] = "purple";
		tests[i++][1] = "perpul";
		tests[i][0] = "purple";
		tests[i++][1] = "poarple";
		tests[i][0] = "separate";
		tests[i++][1] = "seperate";
		tests[i][0] = "access";
		tests[i++][1] = "acess";
		tests[i][0] = "various";
		tests[i++][1] = "vairious";
		tests[i][0] = "between";
		tests[i++][1] = "beetween";
		tests[i][0] = "available";
		tests[i++][1] = "avaible";
		tests[i][0] = "accessing";
		tests[i++][1] = "accesing";
		tests[i][0] = "variant";
		tests[i++][1] = "vairiant";
		tests[i][0] = "completely";
		tests[i++][1] = "completly";
		tests[i][0] = "address";
		tests[i++][1] = "adress";
		tests[i][0] = "address";
		tests[i++][1] = "adres";
		tests[i][0] = "desiccate";
		tests[i++][1] = "desicate";
		tests[i][0] = "desiccate";
		tests[i++][1] = "dessicate";
		tests[i][0] = "desiccate";
		tests[i++][1] = "dessiccate";
		tests[i][0] = "embarrass";
		tests[i++][1] = "embaras";
		tests[i][0] = "embarrass";
		tests[i++][1] = "embarass";
		tests[i][0] = "wanted";
		tests[i++][1] = "wantid";
		tests[i][0] = "wanted";
		tests[i++][1] = "wonted";
		tests[i][0] = "loans";
		tests[i++][1] = "lones";
		tests[i][0] = "accommodation";
		tests[i++][1] = "accomodation";
		tests[i][0] = "accommodation";
		tests[i++][1] = "acommodation";
		tests[i][0] = "accommodation";
		tests[i++][1] = "acomodation";
		tests[i][0] = "chapter";
		tests[i++][1] = "chaper";
		tests[i][0] = "chapter";
		tests[i++][1] = "chaphter";
		tests[i][0] = "chapter";
		tests[i++][1] = "chaptur";
		tests[i][0] = "definition";
		tests[i++][1] = "defenition";
		tests[i][0] = "benefits";
		tests[i++][1] = "benifits";
		tests[i][0] = "career";
		tests[i++][1] = "carrer";
		tests[i][0] = "hierarchal";
		tests[i++][1] = "hierachial";
		tests[i][0] = "experience";
		tests[i++][1] = "experance";
		tests[i][0] = "experience";
		tests[i++][1] = "experiance";
		tests[i][0] = "liaison";
		tests[i++][1] = "liaision";
		tests[i][0] = "liaison";
		tests[i++][1] = "liason";
		tests[i][0] = "planned";
		tests[i++][1] = "planed";
		tests[i][0] = "initials";
		tests[i++][1] = "inetials";
		tests[i][0] = "initials";
		tests[i++][1] = "inistals";
		tests[i][0] = "initials";
		tests[i++][1] = "initails";
		tests[i][0] = "initials";
		tests[i++][1] = "initals";
		tests[i][0] = "initials";
		tests[i++][1] = "intials";
		tests[i][0] = "initial";
		tests[i++][1] = "intial";
		tests[i][0] = "useful";
		tests[i++][1] = "usefull";
		tests[i][0] = "diagrammatically";
		tests[i++][1] = "diagrammaticaally";
		tests[i][0] = "arrangeing";
		tests[i++][1] = "aranging";
		tests[i][0] = "laugh";
		tests[i++][1] = "lagh";
		tests[i][0] = "laugh";
		tests[i++][1] = "lauf";
		tests[i][0] = "laugh";
		tests[i++][1] = "laught";
		tests[i][0] = "laugh";
		tests[i++][1] = "lugh";
		tests[i][0] = "lieu";
		tests[i++][1] = "liew";
		tests[i][0] = "scissors";
		tests[i++][1] = "scisors";
		tests[i][0] = "scissors";
		tests[i++][1] = "sissors";
		tests[i][0] = "occurrence";
		tests[i++][1] = "occurence";
		tests[i][0] = "occurrence";
		tests[i++][1] = "occurence";
		tests[i][0] = "poem";
		tests[i++][1] = "poame";
		tests[i][0] = "management";
		tests[i++][1] = "managment";
		tests[i][0] = "beginning";
		tests[i++][1] = "begining";
		tests[i][0] = "locally";
		tests[i++][1] = "localy";
		tests[i][0] = "parallel";
		tests[i++][1] = "paralel";
		tests[i][0] = "parallel";
		tests[i++][1] = "paralell";
		tests[i][0] = "parallel";
		tests[i++][1] = "parrallel";
		tests[i][0] = "parallel";
		tests[i++][1] = "parralell";
		tests[i][0] = "parallel";
		tests[i++][1] = "parrallell";
		tests[i][0] = "pronunciation";
		tests[i++][1] = "pronounciation";
		tests[i][0] = "inconvenient";
		tests[i++][1] = "inconvienient";
		tests[i][0] = "inconvenient";
		tests[i++][1] = "inconvient";
		tests[i][0] = "inconvenient";
		tests[i++][1] = "inconvinient";
		tests[i][0] = "curtains";
		tests[i++][1] = "cartains";
		tests[i][0] = "curtains";
		tests[i++][1] = "certans";
		tests[i][0] = "curtains";
		tests[i++][1] = "courtens";
		tests[i][0] = "curtains";
		tests[i++][1] = "cuaritains";
		tests[i][0] = "curtains";
		tests[i++][1] = "curtans";
		tests[i][0] = "curtains";
		tests[i++][1] = "curtians";
		tests[i][0] = "curtains";
		tests[i++][1] = "curtions";
		tests[i][0] = "families";
		tests[i++][1] = "familes";
		tests[i][0] = "triangular";
		tests[i++][1] = "triangulaur";
		tests[i][0] = "extended";
		tests[i++][1] = "extented";
		tests[i][0] = "understand";
		tests[i++][1] = "undersand";
		tests[i][0] = "understand";
		tests[i++][1] = "undistand";
		tests[i][0] = "basically";
		tests[i++][1] = "basicaly";
		tests[i][0] = "particular";
		tests[i++][1] = "particulaur";
		tests[i][0] = "considerable";
		tests[i++][1] = "conciderable";
		tests[i][0] = "remember";
		tests[i++][1] = "rember";
		tests[i][0] = "remember";
		tests[i++][1] = "remeber";
		tests[i][0] = "remember";
		tests[i++][1] = "rememmer";
		tests[i][0] = "remember";
		tests[i++][1] = "rermember";
		tests[i][0] = "account";
		tests[i++][1] = "acount";
		tests[i][0] = "arranged";
		tests[i++][1] = "aranged";
		tests[i][0] = "arranged";
		tests[i++][1] = "arrainged";
		tests[i][0] = "unfortunately";
		tests[i++][1] = "unfortunatly";
		tests[i][0] = "ecstasy";
		tests[i++][1] = "exstacy";
		tests[i][0] = "ecstasy";
		tests[i++][1] = "ecstacy";
		tests[i][0] = "whether";
		tests[i++][1] = "wether";
		tests[i][0] = "pretend";
		tests[i++][1] = "pertend";
		tests[i][0] = "pretend";
		tests[i++][1] = "protend";
		tests[i][0] = "pretend";
		tests[i++][1] = "prtend";
		tests[i][0] = "pretend";
		tests[i++][1] = "pritend";
		tests[i][0] = "transferred";
		tests[i++][1] = "transfred";
		tests[i][0] = "receive";
		tests[i++][1] = "recieve";
		tests[i][0] = "cake";
		tests[i++][1] = "cak";
		tests[i][0] = "visited";
		tests[i++][1] = "fisited";
		tests[i][0] = "visited";
		tests[i++][1] = "viseted";
		tests[i][0] = "visited";
		tests[i++][1] = "vistid";
		tests[i][0] = "visited";
		tests[i++][1] = "vistied";
		tests[i][0] = "problem";
		tests[i++][1] = "problam";
		tests[i][0] = "problem";
		tests[i++][1] = "proble";
		tests[i][0] = "problem";
		tests[i++][1] = "promblem";
		tests[i][0] = "problem";
		tests[i++][1] = "proplen";
		tests[i][0] = "minutes";
		tests[i++][1] = "muinets";
		tests[i][0] = "compare";
		tests[i++][1] = "compair";
		tests[i][0] = "certain";
		tests[i++][1] = "cirtain";
		tests[i][0] = "supersede";
		tests[i++][1] = "supercede";
		tests[i][0] = "supersede";
		tests[i++][1] = "superceed";
		tests[i][0] = "contented";
		tests[i++][1] = "contenpted";
		tests[i][0] = "contented";
		tests[i++][1] = "contende";
		tests[i][0] = "contented";
		tests[i++][1] = "contended";
		tests[i][0] = "contented";
		tests[i++][1] = "contentid";
		tests[i][0] = "juice";
		tests[i++][1] = "guic";
		tests[i][0] = "juice";
		tests[i++][1] = "juce";
		tests[i][0] = "juice";
		tests[i++][1] = "jucie";
		tests[i][0] = "juice";
		tests[i++][1] = "juise";
		tests[i][0] = "juice";
		tests[i++][1] = "juse";
		tests[i][0] = "stomach";
		tests[i++][1] = "stomac";
		tests[i][0] = "stomach";
		tests[i++][1] = "stomache";
		tests[i][0] = "stomach";
		tests[i++][1] = "stomec";
		tests[i][0] = "stomach";
		tests[i++][1] = "stumache";
		tests[i][0] = "different";
		tests[i++][1] = "diffrent";
		tests[i][0] = "clerical";
		tests[i++][1] = "clearical";
		tests[i][0] = "monitoring";
		tests[i++][1] = "monitering";
		tests[i][0] = "built";
		tests[i++][1] = "biult";
		tests[i][0] = "perhaps";
		tests[i++][1] = "perhapse";
		tests[i][0] = "personnel";
		tests[i++][1] = "personnell";
		tests[i][0] = "poetry";
		tests[i++][1] = "poartry";
		tests[i][0] = "poetry";
		tests[i++][1] = "poertry";
		tests[i][0] = "poetry";
		tests[i++][1] = "poetre";
		tests[i][0] = "poetry";
		tests[i++][1] = "poety";
		tests[i][0] = "poetry";
		tests[i++][1] = "powetry";
		tests[i][0] = "arrangement";
		tests[i++][1] = "arragment";
		tests[i][0] = "standardizing";
		tests[i++][1] = "stanerdizing";
		tests[i][0] = "independent";
		tests[i++][1] = "independant";
		tests[i][0] = "independent";
		tests[i++][1] = "independant";
		tests[i][0] = "literature";
		tests[i++][1] = "litriture";
		tests[i][0] = "description";
		tests[i++][1] = "discription";
		tests[i][0] = "opposite";
		tests[i++][1] = "opisite";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppasite";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppesite";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppisit";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppisite";
		tests[i][0] = "opposite";
		tests[i++][1] = "opposit";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppossite";
		tests[i][0] = "opposite";
		tests[i++][1] = "oppossitte";
		tests[i][0] = "poems";
		tests[i++][1] = "poims";
		tests[i][0] = "poems";
		tests[i++][1] = "pomes";
		tests[i][0] = "southern";
		tests[i++][1] = "southen";
		tests[i][0] = "driven";
		tests[i++][1] = "dirven";
		tests[i][0] = "visitors";
		tests[i++][1] = "vistors";
		tests[i][0] = "levels";
		tests[i++][1] = "levals";
		tests[i][0] = "experiences";
		tests[i++][1] = "experances";
		tests[i][0] = "position";
		tests[i++][1] = "possition";
		tests[i][0] = "variable";
		tests[i++][1] = "varable";
		tests[i][0] = "extremely";
		tests[i++][1] = "extreamly";
		tests[i][0] = "gallery";
		tests[i++][1] = "galery";
		tests[i][0] = "gallery";
		tests[i++][1] = "gallary";
		tests[i][0] = "gallery";
		tests[i++][1] = "gallerry";
		tests[i][0] = "gallery";
		tests[i++][1] = "gallrey";
		tests[i][0] = "scarcely";
		tests[i++][1] = "scarcly";
		tests[i][0] = "scarcely";
		tests[i++][1] = "scarecly";
		tests[i][0] = "scarcely";
		tests[i++][1] = "scarely";
		tests[i][0] = "scarcely";
		tests[i++][1] = "scarsely";
		tests[i][0] = "auxiliary";
		tests[i++][1] = "auxillary";
		tests[i][0] = "splendid";
		tests[i++][1] = "spledid";
		tests[i][0] = "splendid";
		tests[i++][1] = "splended";
		tests[i][0] = "splendid";
		tests[i++][1] = "splened";
		tests[i][0] = "splendid";
		tests[i++][1] = "splended";
		tests[i][0] = "definitely";
		tests[i++][1] = "definately";
		tests[i][0] = "definitely";
		tests[i++][1] = "difinately";
		tests[i][0] = "aunt";
		tests[i++][1] = "annt";
		tests[i][0] = "aunt";
		tests[i++][1] = "anut";
		tests[i][0] = "aunt";
		tests[i++][1] = "arnt";
		tests[i][0] = "definitions";
		tests[i++][1] = "defenitions";
		tests[i][0] = "voting";
		tests[i++][1] = "voteing";
		tests[i][0] = "latest";
		tests[i++][1] = "lates";
		tests[i][0] = "latest";
		tests[i++][1] = "latets";
		tests[i][0] = "latest";
		tests[i++][1] = "latiest";
		tests[i][0] = "latest";
		tests[i++][1] = "latist";
		System.out.println(spelltest(tests, 0, verbose));
	}

	private String spelltest(String[][] tests, int bias, boolean verbose) {
		int n = 0;
		int bad = 0;
		int unknown = 0;
		long start = System.currentTimeMillis();
		// apply the bias
		if (bias != 0) {
			for (Iterator<String> iter = this.nwords.keySet().iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				this.nwords.put(key, (this.nwords.get(key).intValue() + bias));
			}
		}
		for (int i = 0; i < tests.length; i++) {
			String target = tests[i][0];
			String wrong = tests[i][1];
			n++;
			String w = this.correct(wrong);
			if (!target.equalsIgnoreCase(w)) {
				bad++;
				if (this.nwords.containsKey(target)) {
					unknown++;
				}
				if (verbose)
					System.out.println(wrong + " => " + w + "(" + this.nwords.get(w) + "); expected " + target + "("
							+ this.nwords.get(target) + ")");
			}
		}
		long end = System.currentTimeMillis();
		long duration = end - start;
		long seconds = duration / 1000;
		int pct = (int) (100.0 - ((100.0 * bad) / n));
		return "'bad': " + bad + ", 'bias': " + bias + ", 'unknown': " + unknown + ", 'secs': " + seconds + ", 'pct': "
				+ pct + ", 'n': " + n;

	}
}
