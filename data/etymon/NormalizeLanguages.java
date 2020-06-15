import java.io.*;
import java.util.*;

public class NormalizeLanguages {

	public static void main(String[] argv) throws Exception {
		System.err.println("synopsis: NormalizeLanguages LANGS.TAB COL_WORD COL_LANG\n"+
			"\tLANGS.TAB TSV file with:\n"+
			"\t          column 1: language identifier as used in the input,\n"+
			"\t          column 2: language identifier as used in the output,\n"+
			"\t          column 3-n: additional columns, optional (ignored)\n"+
			"\tCOL_WORD  column in the input data that contains the word whose source language is to be classified\n"+
			"\t          Note: column numbering starts with 1\n"+
			"\tCOL_LANG  column in the input data that contains the original language identifier\n"+
			"Reads input file (TSV format) from stdin, should contain words being marked for the language that\n"+
			"they original from (e.g., as loan words, inherited words or substrate words). NormalizeLanguages will normalize\n"+
			"these language identifiers according to the mapping file provided as argument. If a language identifier\n"+
			"is not recognized, partial matches with target identifiers will be returned. If this does fail, partial\n"+
			"matches with source language identifiers will be returned.");

		int word = Integer.parseInt(argv[1])-1;
		int lang = Integer.parseInt(argv[2])-1;

		int srcLang = 0;
		int tgtLang = 1;
		Hashtable<String,String> src2tgt = new Hashtable<String,String>();
		BufferedReader in = new BufferedReader(new FileReader(argv[0]));
		for(String line = ""; line!=null; line=in.readLine()) {
			line=line.replaceFirst("#.*","");
			String[] fields = line.split("\t");
			if(!line.trim().equals("") && fields.length > srcLang && fields.length > tgtLang) {
				String src = fields[srcLang];
				String tgt = fields[tgtLang];
				if(!src.trim().equals("") && !tgt.trim().equals("")) {
					if(src2tgt.get(src)!=null && !src2tgt.get(src).equals(tgt)) 
						System.err.println("warning: trying to map language \""+src+"\" to both \""+tgt+"\" and \""+src2tgt.get(src)+"\"");
					else
						src2tgt.put(src,tgt);
				}
			}
		}
		in.close();
		
		in = new BufferedReader(new InputStreamReader(System.in));
		for(String line=in.readLine(); line!=null; line=in.readLine()) {
			String[] fields = line.split("\t");
			if(fields.length>word && fields.length>lang && !fields[word].trim().equals("") && !fields[lang].trim().equals(""))
				System.out.println(fields[word].trim()+"\t"+getLang(fields[lang],src2tgt));
		}
		in.close();
	}
	
	public static String getLang(String lang, Map<String,String> src2tgt) {
		if(src2tgt.get(lang)!=null) return src2tgt.get(lang);
		if(src2tgt.values().contains(lang)) return lang;
		for(String cand : new TreeSet<String>(src2tgt.values())) // for replicability
			if(lang.contains(cand)) return cand;
		for(String cand : new TreeSet<String>(src2tgt.keySet())) // for replicability
			if(cand.contains(lang)) return cand;
		for(String cand : new TreeSet<String>(src2tgt.keySet())) // for replicability
			if(lang.contains(cand)) return cand;
		if(!lang.equals(lang.toLowerCase()))
			return getLang(lang.toLowerCase(),src2tgt);
		return "other";
	}
	
}