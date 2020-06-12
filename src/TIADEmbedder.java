import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class TIADEmbedder {
	public static void main(String argv[]) throws Exception {
		System.err.print("synopsis: TIADEmbedder embeddings.tsv.gz lang DICT1[..n]\n"+
			"\tembeddings.tsv.gz original *word* embeddings (GZPPED!) for one particular language, say, English\n"+
			"\t                  for the format, see https://nlp.stanford.edu/projects/glove/\n"+
			"\tlang              BCP47 language code for the language of embeddings.tsv, say, en for English\n"+
			"\tDICTi             TSV file with the following columns:\n"+
			"\t\twritten_rep_a\n"+
			"\t\tlex_entry_a\n"+
			"\t\tsense_a\n"+
			"\t\ttrans\n"+
			"\t\tsense_b\n"+
			"\t\tlex_entry_b\n"+
			"\t\twritten_rep_b\n"+
			"\t\tPOS\n"+
			"\tNOTE: DICTi arguments *MUST* follow the following naming conventions:\n"+
			"\t\t\"trans_\"$SRC\"-\"$TGT\".tsv\", with $SRC and $TGT BCP47 language codes (case-insensitive)\n"+
			"infer embeddings for translations. At the moment, this is ignorant against senses.\n"+
			"NOTE: we expect embeddings to have values that do not exceed the range ]-10:+10[, values beyond that range are considered to be mis-parsed\n"+
			"NOTE: we consider only those words from the reference language which occur in the dictionaries\n"+
			"TODO: (1) enable and test POS support,\n"+
			"      (2) sense-level embeddings,\n"+
			"      (3) alternative paths/k-means,\n"+
			//"      (4) support lcase embeddings\n"+ // DONE
			//"      (5) limit embeddings to dictionary words of reference language,\n"+ // DONE
			//"      (6) aggregate embeddings for multi-word expressions from reference language\n"+ // DONE
			"      (7) close vocabulary gaps\n"+ // by re-reading all dictionaries
			"      (8) adjust space topology\n"+ // rebuild all embeddings based on the direct links (actually solves 7)
			"");
		// BTW: translations can be easily used to predict sense embeddings, just aggregate all embeddings of sense-level translations
			
		// for reference words
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<double[]> embeddings = new ArrayList<double[]>();

		// for words only appearing in multi-word expressions
		ArrayList<String> auxwords = new ArrayList<String>();
		ArrayList<double[]> auxembeddings = new ArrayList<double[]>();
		
		// for vocabulary gaps: store the entire graph
		Hashtable<String,HashSet<String>> src2tgt = new Hashtable<String,HashSet<String>>();
		
		String lang = argv[1].toLowerCase();

		ArrayList<String> dicts = new ArrayList<String>();
		Hashtable<String,String> dict2src = new Hashtable<String,String>();
		Hashtable<String,String> dict2tgt = new Hashtable<String,String>();
		for(int i=2; i<argv.length; i++) {
			dicts.add(argv[i]);
			String[] x = argv[i].split("[_\\-\\.]");
			dict2src.put(argv[i], x[1].toLowerCase());
			dict2tgt.put(argv[i], x[2].toLowerCase());
			System.err.println("dictionary "+argv[i]+": "+x[1].toLowerCase()+ " > "+x[2].toLowerCase());
		}
		
		System.err.print("initializing core vocabulary");
		TreeSet<String> referenceWords = new TreeSet<String>();
		TreeSet<String> auxWords = new TreeSet<String>(); // words in multi-word expressions
		TreeMap<String,String> lcase2truecase = new TreeMap<String,String>(); // we assume one capitalization strategy per word

		for(String dict : dicts)
			if(dict2src.get(dict).equals(lang) || dict2tgt.get(dict).equals(lang)) {
				BufferedReader in = new BufferedReader(new FileReader(dict));
				in.readLine(); // skip first line
				for(String line = in.readLine(); line!=null; line=in.readLine()) {
					String[] fields = line.split("\t");
					if(fields.length>7) {						
						if(dict2src.get(dict).equals(lang)) {
							String src = fields[0].replaceAll("\"", "").replaceFirst("@.*","").trim();
							referenceWords.add(src);
							if(src.contains(" ")) 
								for(String s: src.split(" +"))
									if(s.trim().length()>0)
										auxWords.add(s);
						}
						if(dict2tgt.get(dict).equals(lang)) {
							String tgt = fields[6].replaceAll("\"", "").replaceFirst("@.*","").trim();
							referenceWords.add(tgt);
							if(tgt.contains(" ")) 
								for(String s: tgt.split(" +"))
									if(s.trim().length()>0)
										auxWords.add(s);
						}
						System.err.print("\rinitializing core vocabulary: "+referenceWords.size()+" words");
					}
				}
				in.close();
			}
		auxWords.removeAll(referenceWords);
		System.err.println();

		System.err.println("enabling support for lowercased embeddings");
		for(String w : referenceWords) {
			String l = w.toLowerCase();
			if(!l.equals(w))
				lcase2truecase.put(l, w);
		}
				
		System.err.print("reading gzipped TSV data from "+argv[0]);
		long entries = 0;
		BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(argv[0]))));
		for(String line = in.readLine(); line!=null; line=in.readLine()) {
			String[] fields = line.split("[ \t]+");
			if(fields.length>1) {
				double[] embedding = new double[fields.length-1];
				boolean inRange = true;
				for(int i = 1; i<fields.length; i++) {
					embedding[i-1]=Double.parseDouble(fields[i]);
					if(embedding[i-1]>=10.0 || embedding[i-1]<=-10.0) {
						inRange=false;
						System.err.println("warning: value \""+fields[i]+" ("+embedding[i-1]+") exceeds the permitted range ]-10:+10[, skipping \""+fields[0]+"\"");
					}
				}
				if(inRange) {
					if(referenceWords.contains(fields[0])) {
						words.add("\""+fields[0]+"\"@"+lang);
						embeddings.add(embedding);
						System.err.print("\rreading gzipped TSV data from "+argv[0]+": "+(++entries)+" reference words");
					} else if(auxWords.contains(fields[0])) {
						auxembeddings.add(embedding);
					} else
					if(lcase2truecase.get(fields[0])!=null) {
						words.add("\""+lcase2truecase.get(fields[0])+"\"@"+lang);
						embeddings.add(embedding);
						System.err.print("\rreading gzipped TSV data from "+argv[0]+": "+(++entries)+" reference words");
					}
				}
			}
		}
		in.close();
		System.err.println();
		
		System.err.print("add multi-word expressions (using addition): 0");
		for(String w : referenceWords)
			if(!words.contains(w) && w.contains(" ")) {
				double[] embedding = new double[embeddings.get(0).length];
				boolean skip = false;
				for(String s : w.split("\\s+")) 
					if(!skip && !s.trim().equals("")) {
						int i = words.indexOf(s);
						if(i==-1)
							skip=true;
						else {
							double[] embedding2 = embeddings.get(i);
							for(int n = 0;n<embedding.length;n++)
								embedding[n]=embedding[n]+embedding2[n];
						}
					}
				if(!skip) {
					words.add(w);
					embeddings.add(embedding);
					System.err.print("add multi-word expressions (using addition): "+words.size()+" entries (total)");					
				}
			}
		System.err.println();

		System.err.println("building translation graph");
		for(String dict : dicts) {
			Map<String,HashSet<String>> mysrc2tgt = buildTranslationGraph(dict, dict2src.get(dict), dict2tgt.get(dict));
			if(src2tgt.size()==0)
				src2tgt.putAll(mysrc2tgt);
			else 
				for(String s : mysrc2tgt.keySet()) {
					if(src2tgt.get(s)==null)
						src2tgt.put(s,mysrc2tgt.get(s));
					else
						src2tgt.get(s).addAll(mysrc2tgt.get(s));
				}
		}
		
		HashSet<String> coveredLanguages = new HashSet<String>();
		coveredLanguages.add(lang);
		
		while(!dicts.isEmpty()) {
			System.err.println("covered languages: "+coveredLanguages);
			System.err.println("processing dicts: "+dicts);
			HashSet<String> newlyCoveredLanguages = new HashSet<String>(); // use all dictionaries *per generation*
			for(int i = 0; i<dicts.size(); i++) {
				String dict=dicts.get(i);
				if(	!coveredLanguages.contains(dict2tgt.get(dict)) && 
					!dict2src.contains(dict2tgt.get(dict))) {
						System.err.println("skipping "+dict+": target language "+dict2tgt.get(dict)+" not supported");
						dicts.remove(i--);
				} else if(coveredLanguages.contains(dict2src.get(dict))) {
					System.err.println("skipping "+dict+": found embeddings for source language "+dict2src.get(dict));
					dicts.remove(i--);
				} else if(coveredLanguages.contains(dict2tgt.get(dict))) {
					System.err.print("processing "+dict);
					in = new BufferedReader(new FileReader(dict));
					in.readLine(); // skip first line
					int lines = 0;
					for(String line = in.readLine(); line!=null; line=in.readLine()) {
						String[] fields = line.split("\t");
						if(fields.length>7) {
							System.err.print("\rprocessing "+dict+": "+(++lines)+" lines");
							
							String src = fields[0].replaceAll("\"", "").replaceFirst("@.*","").trim();
							String tgt = fields[6].replaceAll("\"", "").replaceFirst("@.*","").trim();
							String pos = fields[7].replaceAll("\"", "").trim();
							
							// no pos mode
							src = "\""+src+"\"@"+dict2src.get(dict);
							tgt = "\""+tgt+"\"@"+dict2tgt.get(dict);
						
							int t = words.indexOf(tgt);
							if(t!=-1) {

								double[] tembedding = Arrays.copyOf(embeddings.get(t), embeddings.get(t).length);
								int s = words.indexOf(src);
								if(s==-1) {
									embeddings.add(tembedding);
									words.add(src);
								} else {
									double[] sembedding = Arrays.copyOf(embeddings.get(s), embeddings.get(s).length);
									for(int n = 0; n<tembedding.length; n++)
										sembedding[n]=sembedding[n]+tembedding[n];
									embeddings.set(s, sembedding);
								}
							}
							
							 
//							// pos mode
//							src = "\""+src+"/"+pos+"\"@"+dict2src.get(dict);
//							tgt = "\""+tgt+"/"+pos+"\"@"+dict2tgt.get(dict);
//
//							t = words.indexOf(tgt);
//							if(t!=-1) {
//								double[] tembedding = = Arrays.copyOf(embeddings.get(t), embeddings.get(t).length);
//								int s = words.indexOf(src);
//								if(s==-1) {
//									embeddings.add(tembedding);
//									words.add(src);
//								} else {
//									double[] sembedding = Arrays.copyOf(embeddings.get(s), embeddings.get(s).length);
//									for(int n = 0; n<tembedding.length; n++)
//										sembedding[n]=sembedding[n]+tembedding[n];
//									embeddings.set(s, sembedding);
//								}
//							} 
//							
						}
						
					}
					in.close();

					newlyCoveredLanguages.add(dict2src.get(dict));
					System.err.println();
					dicts.remove(i--);
				}
			}
			if(newlyCoveredLanguages.size()==0) {
				for(String dict : dicts)
					System.err.println("skipping "+dict+": no path from "+dict2tgt.get(dict)+" to "+lang);
				dicts.clear();
			} else {
				//System.err.print("languages: "+coveredLanguages);
				coveredLanguages.addAll(newlyCoveredLanguages);
				// System.err.println(" => "+coveredLanguages);
				newlyCoveredLanguages.clear();
			}
		}
		
		// fix vocabulary gaps (where a word was not found on the shortest path used above for extraction)
		System.err.print("add words from indirect paths: 0");
		for(String s : src2tgt.keySet()) 
			if(words.indexOf(s)==-1) {
				double[] embedding = null;
				for(String tgt : src2tgt.get(s)) {
					int idx = words.indexOf(tgt);
					if(idx>-1)
						if(embeddings.get(idx)!=null) {
							if(embedding==null) embedding=Arrays.copyOf(embeddings.get(idx),embeddings.get(idx).length);
							else 
								for(int n = 0; n < embedding.length; n++)
								embedding[n]=embedding[n]+embeddings.get(idx)[n];
						}
				}
				if(embedding!=null) {
					words.add(s);
					embeddings.add(embedding);
					System.err.print("\radd words from indirect paths: "+words.size()+ " words in total");
				}
			}
		System.err.println();		
		
		// spell out new embeddings
		for(int i = 0; i<words.size();i++) {
			System.out.print(words.get(i));
			for(double d : embeddings.get(i))
				System.out.print(" "+cleanFormat(d));
			System.out.println();
		}
	}
	
	/** aux. routine, replaces String.format() as this produces errors (locale-related???), 
	 *  slow, using float precision, only<br/>
	 *  however, note that the approach produces rounding errors */
	protected static String cleanFormat(Double d) {
		if(d<0) return "-"+cleanFormat(d*-1.0);
		String result = String.format(Locale.US, "%f", d).replaceAll("([\\.])?0+$","");
		if(Math.abs(Double.parseDouble(result)-d) < 0.000001) return result;
		System.err.print("warning: String.format("+d+")=\""+result+"\"");
		result="";
		int dimension = (int)Math.log10(d);
		if(dimension<0) result="0.";
		while(d>(double)Float.MIN_VALUE) {
			// System.err.print("cleanFormat("+d+")=");
			double base = Math.pow(10.0, (double)dimension);
			result=result+(int)(d/base);
			if(d % base>(double)Float.MIN_VALUE) {
				d=d % base;
				if(dimension==0 && d>(double)Float.MIN_VALUE)
					result=result+".";
				dimension--;
			} else d=(double)Float.MIN_VALUE;
			// System.err.println(result);
		}
		System.err.println(" => \""+result+"\"");
		return result;
	}
	
	/** store all translation pairs in a single graph, note that we do not include POS information, but only word@lang */
	protected static Map<String, HashSet<String>> buildTranslationGraph(String dictFile, String srcLang, String tgtLang) throws IOException {
		System.err.print("proccessing "+dictFile);
		Hashtable<String,HashSet<String>> result = new Hashtable<String,HashSet<String>>();
		BufferedReader in = new BufferedReader(new FileReader(dictFile));
			in.readLine(); // skip first line
			int lines = 0;
			for(String line = in.readLine(); line!=null; line=in.readLine()) {
				String[] fields = line.split("\t");
					if(fields.length>7) {
						System.err.print("\rprocessing "+dictFile+": "+(++lines)+" lines");
							String src = fields[0].replaceAll("\"", "").replaceFirst("@.*","").trim();
							String tgt = fields[6].replaceAll("\"", "").replaceFirst("@.*","").trim();
							// String pos = fields[7].replaceAll("\"", "").trim();
							
							// no pos mode
							src = "\""+src+"\"@"+srcLang; 
							tgt = "\""+tgt+"\"@"+tgtLang;
							
							if(result.get(src)==null) result.put(src,new HashSet<String>());
							result.get(src).add(tgt);
						}
					System.err.print("\rproccessing "+dictFile+": "+result.size()+" source words");
						
					}
		in.close();
		System.err.println();
		return result;
	}
}