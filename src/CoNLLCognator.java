
import java.io.*;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;

import experimental.NGramComparator;

import java.util.*;

public class CoNLLCognator extends Cognator {

	protected final TIADPredictor sem;
	protected final Map<String,NGramComparator> phons;
	protected final List<String> langs;
	protected final String srcLang;

	public CoNLLCognator(TIADPredictor sem, Map<String,NGramComparator> phons, String srcLang, List<String> langs) {
		this.srcLang=srcLang;
		this.sem= sem;
		this.phons = phons;
		this.langs = langs;
	}

	public static void main(String argv[]) throws Exception {
		
		double BIAS = 0.1;
		boolean closestMatchOnly = true;
		
		System.err.println("synopsis: CoNLLCognator COL srclang [-threshold BIAS] embeddings.tsv tgtlang[1..n] \n"+
			"\tCOL			  column containing word forms or lemmas in the CoNLL data (read from stdin)\n"+
			"\tsrclang        source language, BCP47 code\n"+
			"\tBIAS           minimum threshold for at least one score (cognate, semantic, phonological), should be in ]0:1[, defaults to "+BIAS+"\n"+
			"\tembeddings.tsv uncompressed TSV file containing the embeddings\n"+
			"\ttgtlang		  target language(s), BCP47 codes\n"+
			"We read CoNLL data from stdin and perform all oüerations on column COL.\n"+
			"Return the best matches for the input word for every target language, ranked for harmonic mean of semantic and orthographic similarity.");
		
		int SRC_COL = Integer.parseInt(argv[0]);
		String srcLang = argv[1].toLowerCase();
		argv = Arrays.copyOfRange(argv,2,argv.length);
		
		if(argv[0].equalsIgnoreCase("-threshold")) {
			BIAS = Double.parseDouble(argv[1]);
			argv = Arrays.copyOfRange(argv,2,argv.length);
		}
		
		TIADPredictor sem;
		if(argv.length > 2)
			sem = new TIADPredictor(argv[0], Arrays.copyOfRange(argv,2,argv.length));
		else sem= new TIADPredictor(argv[0], new String[0]);

		TreeMap<String,NGramComparator> phons = new TreeMap<String,NGramComparator>();
		for(String lang : sem.languages()) {
			NGramComparator p = new NGramComparator(2);
			for(String w : sem.words(lang))
				p.add(w);
			phons.put(lang,p);
		}

		if(!sem.supportsLanguage(srcLang))
			System.err.println("Unsupported source source languages. Use of of "+sem.lang2word2embedding.keySet());
		ArrayList<String> tlangs = new ArrayList<String>();
		for(int i = 1; i<argv.length; i++) {
			String tgtLang = argv[i].toLowerCase();
			if(!sem.supportsLanguage(tgtLang))
				System.err.println("error: no support for language "+argv[i]+". Use one of "+sem.languages());
			tlangs.add(tgtLang);
		}

		CoNLLCognator me = new CoNLLCognator(sem,phons,srcLang,tlangs);
		
		System.err.println("reading CoNLL from stdin, analysing colum "+SRC_COL);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String sentence = "";
		for(String line = in.readLine(); line!=null; line=in.readLine()) {
			if(line.trim().equals("")) {
				me.analyze(sentence, SRC_COL, BIAS, closestMatchOnly, System.out);
				sentence="";
			} else
				sentence=sentence+line+"\n";
		}
		me.analyze(sentence, SRC_COL, BIAS,  closestMatchOnly, System.out);
	}
	
	/** reads a CoNLL sentence, returns the same CoNLL sentence with additional columns for the target languages and the resp. confidence <br/>
	if closestMatchOnly, return only results for the language with the highest aggregate score */
	public void analyze(String conllSentence, int col, double bias, boolean closestMatchOnly, PrintStream out) throws IOException {
		
		// build a sentence embedding
		double[] s = null;
		double[] sNormalized = null; // using the phonologically closest known word

		for(String line : conllSentence.split("\n")) {
			line = line.replaceFirst("([^\\\\])#.*","$1").replaceFirst("^#.*","");
			String[] fields = line.split("\t");
			if(fields.length>col&&!line.trim().equals("")) {
				String word = fields[col];
				double[] e = sem.embedding(word);
				if(e==null && !word.equals(word.toLowerCase()))
					e = sem.embedding(word.toLowerCase());
				if(e!=null) {
					if(s==null ) s=Arrays.copyOf(e,e.length);
					else 
						for(int i = 0; i<s.length; i++)
							s[i]=s[i]+e[i];
				} else 
					if(s==null) { // update sNormalized only if s is empty
						if(phons.get(srcLang)!=null) {
							try {
								e = sem.embedding(phons.get(srcLang).analyze(word,1,false,false).get(0));
								if(sNormalized==null) sNormalized=Arrays.copyOf(e,e.length);
								else 
									for(int i = 0; i<s.length; i++)
										sNormalized[i]=sNormalized[i]+e[i];

							} catch (NullPointerException ex) {}
						}
					}
			}
		}
		
		String sEmbeddingWarning = "\n";
		if(s==null && sNormalized!=null) {
			sEmbeddingWarning="warning: did not recognize any source word, using orthographical closest known words for sentence embeddings";
			s=sNormalized;
		}
		
		for(String line : conllSentence.split("\n")) {
			out.print(line);
			line = line.replaceFirst("([^\\\\])#.*","$1").replaceFirst("^#.*","");
			String[] fields = line.split("\t");
			if(fields.length>col && !line.trim().equals("")) {
				String word = fields[col];
				word = word.replaceFirst("\"@[^\\s]+$","").replaceAll("\"","").trim();
				if(sem.embedding(word, srcLang)==null && sem.embedding(word.toLowerCase(),srcLang)!=null) 
					word=word.toLowerCase();
				if(sem.embedding(word,srcLang)==null && s!=null) { 
						sem.add(word,srcLang, s);
						System.err.print("warning: use (first) sentence embedding as word embedding for \""+word+"\"\n"+sEmbeddingWarning);
						sEmbeddingWarning="\n";
					}

				String closestMatch = "_\t_\t_\t_\t";
				Double closestMatchScore = null;
				Double closestMatchAvg = null;
				
				for(String tgtLang : langs) {
					
					List<String> result = super.analyze(sem,phons.get(tgtLang), word, srcLang, tgtLang, 1);
					if(result!=null && result.size()>0) {
						Double max = null;
						for(String d : result.get(0).split("\t")) {
							try {
								if(max==null) max = Double.parseDouble(d);
								else max = Math.max(Double.parseDouble(d), max);
							} catch (NumberFormatException e) {}
						}
						if(max!=null && max<bias) result=null;
					}
					if(result!=null && result.size()>0) {
						if(closestMatchOnly) {
							try {
								double score = Double.parseDouble(result.get(0).split("\t")[1]);
								if(closestMatchScore==null || score>closestMatchScore) {
									closestMatchScore=score;
									closestMatch=tgtLang+"\t"+result.get(0);
								} else if(score==closestMatchScore && score==0.0) {
									// average over all scores rather than just the first
									double sum = 0.0;
									int freq = 0;
									for(String d : result.get(0).split("\t")) {
										try {
											sum=sum+Double.parseDouble(d);
											freq++;
										} catch(NumberFormatException e) {}
									}
									if(closestMatchAvg==null) {
										double closestSum = 0.0;
										int closestFreq = 0;
										for(String d: closestMatch.split("\t")) {
											try {
												closestSum=closestSum+Double.parseDouble(d);
												closestFreq++;
											} catch(NumberFormatException e) {}
										}
										closestMatchAvg=closestSum/(double)closestFreq;
									}
									double myAvg = sum/(double)freq;
									if(myAvg>closestMatchAvg) {
										closestMatchAvg=myAvg;
										closestMatch = tgtLang+"\t"+result.get(0);
										closestMatchScore = 0.0;
									}
								}
								
								
							} catch(NumberFormatException e) {}
						} else { // all languages
							if(result==null || result.size()==0) 
								out.print("\t_\t_\t_\t_");
							else 
								out.print("\t"+result.get(0));
						}
					}
				}
				if(closestMatchOnly) out.print("\t"+closestMatch);
			}
			out.println();
		}
	}
}