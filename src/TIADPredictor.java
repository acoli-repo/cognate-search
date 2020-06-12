import java.io.*;
import java.util.*;

public class TIADPredictor {

	final protected Hashtable<String,TreeMap<String,TreeSet<String>>> lang2word2pos;
	final protected Hashtable<String,TreeMap<String,double[]>> lang2word2embedding;

	/** expect plain word without language encoding */
	public double[] embedding(String word, String lang) {
		if(lang2word2embedding.get(lang)==null) return null;
		return lang2word2embedding.get(lang).get(word);
		//if(lang2word2embedding.get(lang).get(word)==null)
		//	return lang2word2embedding.get(lang).get(word.replaceFirst("\"@[^\"]*$","").replaceAll("\"",""));
	}
	
	/** uses "word"@lang encoding */
	public double[] embedding(String wordWLang) {
		String lang = wordWLang.replaceAll(".*@","");
		String word = wordWLang.replaceFirst("\"@[^\"]*$","").replaceAll("\"","");
		return embedding(word,lang);
	}
	
	public TIADPredictor(String embeddings, String[] dicts) throws IOException {
		lang2word2pos = new Hashtable<String,TreeMap<String,TreeSet<String>>>();
		// in principle, this is working, but for some strange reason, the POS lookup fails, e.g.
		// king    principe        0.8424361256934882      2
		// but best match with pos is
		// king    pop     0.6710244150132357      1       "http://www.lexinfo.net/ontology/2.0/lexinfo#noun"
		// although "príncep"       "http://linguistic.linkeddata.es/id/apertium/lexiconCA/pr%C3%ADncep-n-ca"       "http://linguistic.linkeddata.es/id/apertium/tranSetCA-IT/pr%C3%ADncep_principe-n-ca-sense"     "http://linguistic.linkeddata.es/id/apertium/tranSetCA-IT/pr%C3%ADncep_principe-n-ca-sense-principe_pr%C3%ADncep-n-it-sense-trans"   "http://linguistic.linkeddata.es/id/apertium/tranSetCA-IT/principe_pr%C3%ADncep-n-it-sense"     "http://linguistic.linkeddata.es/id/apertium/lexiconIT/principe-n-it"        "principe"      "http://www.lexinfo.net/ontology/2.0/lexinfo#noun"
		  
		/* for(String dict : dicts) {
			String[] x = argv[i].split("[_\\-\\.]");
			String srcLang = x[1].toLowerCase();
			String tgtLang = x[2].toLowerCase();
			System.err.println("read dictionary "+dict+": "+srcLang+ " > "+tgtLang);
			BufferedReader in = new BufferedReader(new FileReader(dict));
			in.readLine(); // skip header
			for(String line = in.readLine(); line!=null; line=in.readLine()) {
				String[] fields = line.split("\t");
				if(fields.length>7) {
					String src = fields[0].replaceFirst("@[^\"@]*$","").replaceAll("\"","").trim().intern();
					String tgt = fields[6].replaceFirst("@[^\"@]*$","").replaceAll("\"","").trim().intern();
					String pos = fields[7].trim().replaceAll("\"","").intern();
					
					if(lang2word2pos.get(srcLang)==null) lang2word2pos.put(srcLang,new TreeMap<String,TreeSet<String>>());
					if(lang2word2pos.get(srcLang).get(src)==null) lang2word2pos.get(srcLang).put(src,new TreeSet<String>());
					// lang2word2pos.get(srcLang).get(src).add(pos);
					TreeSet<String> s = lang2word2pos.get(srcLang).get(src);
					s.add(pos);
					lang2word2pos.get(srcLang).put(src,s);
					
					if(lang2word2pos.get(tgtLang)==null) lang2word2pos.put(tgtLang,new TreeMap<String,TreeSet<String>>());
					if(lang2word2pos.get(tgtLang).get(tgt)==null) lang2word2pos.get(tgtLang).put(tgt,new TreeSet<String>());
					// lang2word2pos.get(tgtLang).get(tgt).add(pos);
					s = lang2word2pos.get(tgtLang).get(tgt);
					s.add(pos);
					lang2word2pos.get(tgtLang).put(tgt,s);
					
				}
			}
		} */ 
		
		System.err.print("read embeddings");
		lang2word2embedding = new Hashtable<String,TreeMap<String,double[]>>();
		BufferedReader in = new BufferedReader(new FileReader(embeddings));
		int entries = 0;
		for(String line = ""; line!=null; line=in.readLine()) {
			String word = line.replaceFirst("^(\"[^\"]*\"@[^\\s]+)\\s.*","$1");
			line=line.substring(word.length()).trim();
			String lang = word.replaceAll(".*@","").toLowerCase();
			word = word.replaceAll("\"","").replaceFirst("@[^@]*$","");
			String[] fields = line.trim().split("\\s");
			if(!line.equals("")) {
				double[] embedding = new double[fields.length];
				for(int i = 0; i<embedding.length; i++)
					embedding[i]=Double.parseDouble(fields[i]);
				add(word, lang, embedding);
				System.err.print("\rread embeddings: "+(++entries)+" entries for "+lang2word2embedding.size()+" languages");
			}
		}
		in.close();
		System.err.println();

		
	}
	
	void add(String word, String lang, double[] embedding) {				
				if(lang2word2embedding.get(lang)==null)
					lang2word2embedding.put(lang,new TreeMap<String,double[]>());
				lang2word2embedding.get(lang).put(word,embedding);
	}
	
	public static void main(String argv[]) throws Exception {
		
		int matches = 10;
		
		System.err.println("synopsis: TIADPredictor embeddings.tsv lang [DICT1.tsv .. DICTn.tsv]\n"+
			"\tembeddings.tsv uncompressed TSV file containing the embeddings\n"+
			"\tlang           target language, BCP47 code\n"+
			"\tDICTi.tsv      dictionary in TSV format, using the same columns as TIADEmbedder\n"+
			"Note that we expect words \"$WORD\"@lang as input and as first column in embeddings.tsv.\n"+
			"We read source words from stdin\n"+
			"Return the "+matches+" best matches for the input word.\n"+
			"If dictionaries are provided, we use the POS information to filter the results. (currently disabled)");
			// as a next step, we could actually use the sense information from the dictionaries and use their embeddings rather than the aggregate embeddings
		
		TIADPredictor me;
		if(argv.length > 2)
			me = new TIADPredictor(argv[0], Arrays.copyOfRange(argv,2,argv.length));
		else me= new TIADPredictor(argv[0], new String[0]);

		System.err.println("supported languages: "+me.lang2word2embedding.keySet());
		String tgtLang = argv[1].toLowerCase();
		if(!me.supportsLanguage(tgtLang)) {
			System.err.println("error: no support for language "+argv[1]+". Use one of "+me.languages());
		} else {
			System.err.println("reading \"word\"@lang pairs from stdin, skip with <ENTER>");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				for(String line = in.readLine(); line!=null && !line.equals(""); line=in.readLine()) {
					String word = line.replaceFirst("^(\"[^\"]*\"@[^\\s]+)\\s.*","$1");
					String lang = word.replaceAll(".*@","");
					word = word.replaceAll("\"","").replaceFirst("@[^@]*$","");
					System.out.print(line);
					List<String> result = me.analyze(word,lang,tgtLang,matches, true);
					if(result==null || result.size()==0)
						System.out.println("\t_");
					else 
						for(String r : result)
							System.out.println("\t"+r);
				}
		}
	}
					
	public List<String> analyze(String word, String srcLang, String tgtLang, int matches, boolean includeScores) {
		ArrayList<String> result = new ArrayList<String>();
		
		srcLang=srcLang.trim().toLowerCase();
		tgtLang=tgtLang.trim().toLowerCase();

		if(lang2word2embedding.get(srcLang)==null)
			System.err.println("error: source language "+srcLang+" not supported, use one of "+lang2word2embedding.keySet());
		else if(lang2word2embedding.get(tgtLang)==null)
			System.err.println("error: target language "+tgtLang+" not supported, use one of "+lang2word2embedding.keySet());
		else if(lang2word2embedding.get(srcLang).get(word)==null)
			System.err.println("error: did not find word \""+word+"\"@"+srcLang+" in the embedding store");
		else {
			double[] embedding = lang2word2embedding.get(srcLang).get(word);

				// base version, nice, fast and elegant
				Vector<String> tgts = new Vector<String>();
				Vector<Double> sims = new Vector<Double>();
				for(String tgt : lang2word2embedding.get(tgtLang).keySet()) {
					double sim = cosinusSimilarity(embedding,lang2word2embedding.get(tgtLang).get(tgt));
					if(sim>0.0) { // otherwise, they're independent
						if(sims.size()==0) {
							sims.add(sim);
							tgts.add(tgt);
						} else if(sim <= sims.get(sims.size()-1)) {
							if(sims.size()<matches) {
								sims.add(sim);
								tgts.add(tgt);
							}
						} else if(sims.size()>=matches && sims.get(matches-1)>=sim) { // skip
						} else {
							for(int i = 0; i<sims.size() && i<matches && !tgts.contains(tgt); i++)
								if(sims.get(i)<sim) {
									sims.insertElementAt(sim,i);
									tgts.insertElementAt(tgt,i);
								}
							if(!tgts.contains(tgt) && sims.size()<matches) {
								sims.add(sim);
								tgts.add(tgt);
							}
						}
					}
				}
				
				while(tgts.size()>matches)
					tgts.remove(matches);
				
				if(!includeScores) 
					return tgts;
				else
					for(int i = 0; i<matches && i<sims.size(); i++)
						result.add(tgts.get(i)+"\t"+sims.get(i)+"\t"+(1+i));
				

		}
		return result;
				
			/* POS implementation
			if(lang2word2pos.get(srcLang)!=null && lang2word2pos.get(srcLang).get(word)!=null && lang2word2pos.get(srcLang).get(word).size()>0 
			   && lang2word2pos.get(tgtLang)!=null)   // we use POS information
				for(String pos : lang2word2pos.get(srcLang).get(word)) {
					tgts = new Vector<String>();
					sims = new Vector<Double>();
					for(String tgt : lang2word2embedding.get(tgtLang).keySet()) 
					  if(lang2word2pos.get(srcLang)!=null && lang2word2pos.get(srcLang).get(tgt)!=null) { // && lang2word2pos.get(lang).get(tgt).contains(pos)) {
						boolean posOverlap = lang2word2pos.get(srcLang).get(tgt).contains(pos);	// for some strange reason, this fails!
						for(String tpos : lang2word2pos.get(srcLang).get(tgt))
							posOverlap=posOverlap||tpos.trim().equals(pos.trim());
						if(posOverlap) {
							double sim = cosinusSimilarity(embedding,lang2word2embedding.get(tgtLang).get(tgt));
							if(sim>0.0) { // otherwise, they're independent
								if(sims.size()==0) {
									sims.add(sim);
									tgts.add(tgt);
								} else if(sim <= sims.get(sims.size()-1)) {
									if(sims.size()<matches) {
										sims.add(sim);
										tgts.add(tgt);
									}
								} else if(sims.size()>=matches && sims.get(matches-1)>=sim) { // skip
								} else {
									for(int i = 0; i<sims.size() && i<matches && !tgts.contains(tgt); i++)
										if(sims.get(i)<sim) {
											sims.insertElementAt(sim,i);
											tgts.insertElementAt(tgt,i);
										}
									if(!tgts.contains(tgt) && sims.size()<matches) {
										sims.add(sim);
										tgts.add(tgt);
									}
								}
							}
						}
					  }
					 for(int i = 0; i<matches && i<sims.size(); i++)
						System.out.println(word+"\t"+tgts.get(i)+"\t"+sims.get(i)+"\t"+(1+i)+"\t"+pos);

				}
				*/
	}

	/** return the supported languages */
	public Set<String> languages() {
		return lang2word2embedding.keySet();
	}

	/** check whether lang is a supported language */
	public boolean supportsLanguage(String lang) {
		return lang2word2embedding.get(lang)!=null;
	}

	protected static double cosinusSimilarity(double[] embedding1, double[] embedding2) {
		double e12 = sumOfSquares(embedding1);
		double e22 = sumOfSquares(embedding2);
		double result = 0.0;
		for(int i = 0; i<embedding1.length; i++)
			result=result+embedding1[i]*embedding2[i];
		return result/(Math.sqrt(e12)*Math.sqrt(e22));
	}
	
	protected static double sumOfSquares(double[] vector) {
		double result = 0.0;
		for(double d : vector)
			result=result+(d*d);
		return result;
	}

	/** return entries for one language */
	public Set<String> words(String lang) {
		if(this.lang2word2embedding.get(lang)==null) return new HashSet<String>();
		return this.lang2word2embedding.get(lang).keySet();
	}
}