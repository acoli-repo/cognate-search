package experimental;
import java.io.*;
import java.util.*;

public class NGramComparator extends TreeMap<String, TreeMap<String,Integer>> {

	final protected int ngramsize;
	
		public NGramComparator(int ngramsize) {
			this.ngramsize=ngramsize;
	}
		public static void main(String[] argv) throws Exception {
		System.err.println("reads whitespace-tokenized strings from args, performs n-gram character match against stdin");
		NGramComparator me = new NGramComparator(2);
		for(String a : argv) {
			System.err.print("read "+a+" ..");
			BufferedReader in = new BufferedReader(new FileReader(a));
			for(String line = in.readLine(); line!=null; line=in.readLine())
				for(String w : line.replaceFirst("([^\\\\])#.*","").replaceFirst("^#.*", "").trim().split("\\s+"))
					me.add(w);
			System.err.println(". "+me.size()+" words");
		}
		
		System.err.println("read one token per line from stdin, quit with empty line");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		for(String line = in.readLine(); line!=null && !line.equals(""); line=in.readLine()) {
			System.out.print(line);
			for(String result : me.analyze(line,10, true, false))
				System.out.println("\t"+result);
		}
			
	}
		
		/** if keepCase = false, merge lcase and regular results, by default true */
		public List<String> analyze(String word, int matches, boolean withScores, boolean keepCase) {
			if(keepCase || word.equals(word.toLowerCase())) return analyze(word, matches, withScores);

			TreeMap<String,Integer> wgrams = split(word,ngramsize);

			TreeSet<String> candidates = new TreeSet<String>(analyze(word,matches,false));
			candidates.addAll(analyze(word.toLowerCase(),matches,false));
			
			ArrayList<String> words = new ArrayList<String>();
			ArrayList<Double> scores = new ArrayList<Double>();
			
			TreeMap<String,Integer> xgrams = split(word,ngramsize);
			TreeMap<String,Integer> xgramsLcase = split(word.toLowerCase(),ngramsize);

			for(String c : candidates) {
				double score = Math.max(compare(xgrams, this.get(c)), compare(xgramsLcase, this.get(c)));
				for(int i = 0; i<words.size() && i<matches && !words.contains(c); i++)
					if(score>scores.get(i)) {
						words.add(i,c);
						scores.add(i,score);
					}
				if(words.size()<matches) {
					words.add(c);
					scores.add(score);
				}
				while(words.size()>matches) {
					words.remove(matches);
					scores.remove(matches);
				}
			}
			
			if(!withScores) return words;
			
			ArrayList<String> result = new ArrayList<String>();
			for(int i = 0; i<matches && i<words.size(); i++)
				result.add(words.get(i)+"\t"+String.format(Locale.US, "%f", scores.get(i)));
			return result;
		}
		
		
		public double compare(String x, String y) {
			TreeMap<String,Integer> xgrams = this.get(x);
			TreeMap<String,Integer> ygrams = this.get(y);
			if(xgrams==null)
				xgrams = split(x,ngramsize);
			if(ygrams==null)
				ygrams = split(y,ngramsize);
			double result = compare(xgrams, ygrams);
					
			// System.err.println("compare("+x+","+y+")="+result);

			return result;
		}

		
		/** returns a string representation of the first i matches, comprising word<TAB>score, write scores only if withScores is true */
		public List<String> analyze(String word, int matches, boolean withScores) {
			TreeMap<String,Integer> wgrams = split(word,ngramsize);
			ArrayList<String> words = new ArrayList<String>();
			ArrayList<Double> scores = new ArrayList<Double>(); 
			for(String w : this.keySet()) {
				double score = compare(this.get(w),wgrams);
				for(int i = 0; i<scores.size() && i<matches && !words.contains(w); i++)
					if(score>scores.get(i)) {
						scores.add(i,score);
						words.add(i,w);
					}
				if(words.size()<matches) {
					words.add(w);
					scores.add(score);
				}
				while(words.size()>matches) {
					words.remove(matches);
					scores.remove(matches);
				}
			}
			if(!withScores) return words;

			ArrayList<String> result = new ArrayList<String>();
			for(int i = 0; i<matches && i<words.size(); i++)
				result.add(words.get(i)+"\t"+String.format(Locale.US, "%f", scores.get(i)));
			return result;
		}
		
		/** score: number of shared ngrams, divided by the number of ngrams of the longer one*/
		protected double compare(TreeMap<String,Integer> x, TreeMap<String,Integer> y) {
			if(x.size()<y.size())
				return compare(y,x);
				// x should be the longer one
			    // note that this is a hack, as we count ngrams types rather than ngram tokens
			
			int overlap = 0;
			int xgrams = 0; 
			for(String ngram : x.keySet()) {
				if(y.get(ngram)!=null)
					overlap+=Math.min(x.get(ngram), y.get(ngram));
				xgrams=xgrams+x.get(ngram);
			}
			
			return ((double)overlap)/xgrams;
		}
		
		public void add(String w) {
			if(!w.trim().equals("") && !this.containsKey(w))
				this.put(w,split(w,ngramsize));
		}

		/** split a given word into character ngrams */
		protected static TreeMap<String, Integer> split(String string, int ngramsize) {
			string = "^"+string.trim()+"$";
			TreeMap<String,Integer> result = new TreeMap<String,Integer>();
			for(int i=0; i<string.length()-ngramsize;i++) {
				String ngram = string.substring(i, i+ngramsize);
				if(result.get(ngram)==null) result.put(ngram, 1); else result.put(ngram, result.get(ngram)+1);
			}
			return result;
		}
}
