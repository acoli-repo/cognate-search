
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import experimental.NGramComparator;

import java.util.*;

public class Cognator {

	public static void main(String argv[]) throws Exception {
		
		int matches = 10;
		
		System.err.println("synopsis: Cognator embeddings.tsv lang [DICT1.tsv .. DICTn.tsv]\n"+
			"\tembeddings.tsv uncompressed TSV file containing the embeddings\n"+
			"\tlang           target language, BCP47 code\n"+
			"\tDICTi.tsv      dictionary in TSV format, using the same columns as TIADEmbedder\n"+
			"Note that we expect words \"$WORD\"@lang as input and as first column in embeddings.tsv.\n"+
			"We read source words from stdin\n"+
			"Return the "+matches+" best matches for the input word, ranked for semantic and orthographic similarity.");
		
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
		
		System.err.println("supported languages: "+sem.lang2word2embedding.keySet());
		String tgtLang = argv[1].toLowerCase();
		if(!sem.supportsLanguage(tgtLang)) {
			System.err.println("error: no support for language "+argv[1]+". Use one of "+sem.languages());
		} else {
			System.err.println("reading \"word\"@lang pairs from stdin, skip with <ENTER>");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				for(String line = in.readLine(); line!=null && !line.equals(""); line=in.readLine()) {
					String word = line.replaceFirst("^(\"[^\"]*\"@[^\\s]+)\\s.*","$1");
					String lang = word.replaceAll(".*@","");
					word = word.replaceAll("\"","").replaceFirst("@[^@]*$","");
					System.out.print(line);
					List<String> result = analyze(sem,phons.get(tgtLang), word, lang, tgtLang, matches);
					if(result==null || result.size()==0)
						System.out.println("\t_");
					else 
						for(String r : result)
							System.out.println("\t"+r);
					
				}
		}
	}

	/** combines semantic and phonological search <br/>
	 *  note that we assume that both semantic and phonological search include language codes 
	 *  note that we only re-order the matches^2 (but at least 25) best semantic and phonological results <br/>
	 *	for phonological search, set sem = null */
	@SuppressWarnings("static-access")
	protected static List<String> analyze(TIADPredictor sem, NGramComparator phon, String word, String srcLang,
			String tgtLang, int matches) {
		
		List<String> candidates = null;
		if(sem!=null) 
			sem.analyze(word,srcLang,tgtLang,(int)Math.pow(Math.max(5, matches),2), false);
		if(candidates==null || candidates.size()==0)
			candidates = phon.analyze(word, matches, false);
		else {
			candidates.addAll(phon.analyze(word,(int)Math.pow(Math.max(5, matches),2), false));
		}
		
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<Double> pScores = new ArrayList<Double>();
		ArrayList<Double> sScores = new ArrayList<Double>();
		ArrayList<Double> scores = new ArrayList<Double>();
		
		for(String c : new TreeSet<String>(candidates)) {
			Double p = phon.compare(word, c);
			Double s = 0.0;
			try {
				s = sem.cosinusSimilarity(sem.lang2word2embedding.get(srcLang).get(word), sem.lang2word2embedding.get(tgtLang).get(c));
			} catch (NullPointerException e) {};
			if(!word.equals(word.toLowerCase())) {
				p = Math.max(p, phon.compare(word.toLowerCase(), c));
				try {
					s = Math.max(
							s,
							sem.cosinusSimilarity(sem.lang2word2embedding.get(srcLang).get(word.toLowerCase()), sem.lang2word2embedding.get(tgtLang).get(c))
							);
				} catch (NullPointerException e) {};
			}
			
			/** harmonic mean of both scores plus one (to make sure one doesn't neutralize the other) */
			Double score = 2.0*(p+1)*(s+1) / (p+s+2);
			
			for(int i = 0; i<words.size() && i<matches && !words.contains(c); i++) 
				if(score>scores.get(i)) {
					scores.add(i,score);
					words.add(i,c);
					pScores.add(i,p);
					sScores.add(i,s);
				}
			
			if(!words.contains(c) && words.size()<matches) {
				scores.add(score);
				words.add(c);
				pScores.add(p);
				sScores.add(s);
			}
			
			while(words.size()>matches) {
				words.remove(matches);
				scores.remove(matches);
				pScores.remove(matches);
				sScores.remove(matches);
			}
		}
		
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i<matches && i<words.size(); i++) {
			double s = sScores.get(i);
			double p = pScores.get(i);
			double score = 0.0;
			if(p+s>0.0) 
				score = 2.0*p*s/(p+s);
			result.add(words.get(i)+"\t"+String.format(Locale.US, "%f\t%f\t%f", score, s, p));
		}

		return result;
	}
}
