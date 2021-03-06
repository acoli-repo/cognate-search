
# Cognate search

Detection of cognate candidates based on semantic and phonological (graphemic) similarity.

Currently a command-line tool for Unix/Linux systems. Developed with OpenJDK 13.0.2+8.

1. **Setup**
    Requires a Linux/Unix environment. Not tested for Bash 3.2 (MacOS users might want to [update to Bash 4.0](https://itnext.io/upgrading-bash-on-macos-7138bd1066ba) first). 

    > $> bash -e ./build.sh
    
	to compile java and dictionary embeddings

2.  **Cognate candidates from one target language**
	For the interactive prediction of cognate candidates from one particular target language, e.g., to predict cognate candidates in French (`fr`):
	
	> $> java -Dfile.encoding=UTF-8 -classpath bin Cognator glove6B.50d.plusTIAD.txt.gz fr

	This will open an interactive dialogue, enter string with designation of source language (e.g., `en` for English):
 
	  > "dangerous"@en

		"dangerous"@en  danger         0.670120 0.673609 0.666667
		                dangerosité    0.625332 0.673846 0.583333
		                dangereux      0.594574 0.536553 0.666667
		                dangereusement 0.558444 0.632358 0.500000
		                étranger       0.523891 0.637922 0.444444
		                arranger       0.518495 0.622155 0.444444
		                ...

	* **First column**: the requested word
	* **Second column**: target language word (here, French)
	* **Third column**: cognate score (*baseline*: harmonic mean between semantic and phonological similarity)
	* **Fourth column**: semantic similarity (*baseline*: cosine distance)
	* **Fifth column**: phonological (graphemic) similarity (*baseline*: bigram overlap)

	Note that a detailed linguistic, language-specific evaluation is required to adjust metrics and thresholds.

3.  **Cognate candidates from multiple languages**

	Given text (with annotations, in any CoNLL TSV format), add columns that contain the most likely (highest-scored) source language, the most likely cognate candidate from that language, and its similarity scores. A disadvantage in comparison to the bilingual method is that only one candidate for one language is returned.

	synopsis: 
	`CoNLLCognator COL srclang [-threshold BIAS] embeddings.tsv tgtlang[1..n]`

	Example for the prediction of English, Spanish or French cognate candidates for a sample of the [English Web Treebank](https://github.com/UniversalDependencies/UD_English-EWT) provided in this repo:
	
	  > $> cat EWT.test.conll | java -Dfile.encoding=UTF-8 -classpath bin CoNLLCognator 1 en glove6B.50d.plusTIAD.txt.gz en es fr 2>/dev/null

		where   	where   	en      where   1.00 1.00 1.00
		can     	can     	en      can     1.00 1.00 1.00
		I       	I       	en      IP      0.00 0.00 0.50
		get     	get     	en      get     1.00 1.00 1.00
		morcillas       morcilla        es      arcilla 0.00 0.00 0.63
		in      	in      	en      in      1.00 1.00 1.00
		tampa   	tampa   	en      tamp    0.00 0.00 0.80
		bay     	bay     	en      bay     1.00 1.00 1.00

	In the output of this example, the first two columns are from the original file. Predictions are attached, with similarity scores as above. Note that only potential *loan words* are recognized (i.e., words contained in the source language dictionary), not *foreign words*. Words that are not in the source language dictionary will not be compared for semantic similarity, but only for phonological similarity (hence 0 scores for *morcilla* -- *arcilla*). The prediction of Spanish (`es`) origin for *morcilla* is nevertheless correct (even though the word is not in the Spanish dictionaries used here, and there is no semantic relation between *morcilla* and *arcilla*, they share the same suffix, and this is recognized here).
