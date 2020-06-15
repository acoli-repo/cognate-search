#!/bin/bash

# retrieve etymological data for English
if [ ! -e etymon.txt ]; then
	for i in {1..10000}; do
		for c in {a..z}; do
			if [ ! -d $c ]; then mkdir $c >&/dev/null; fi;
			TGT=$c/etymon-$i-$c.html;
			if [ ! -e $TGT ]; then
				wget "https://www.etymonline.com/search?page="$i"&q="$c -O $TGT
			fi;
		done
	#https://www.etymonline.com/search?page=1&q=a
	done > etymon.txt;
fi;
# Note that the extraction is imperfect and sometimes loan relations are indirect, especially
# for modern internationalisms.
# As an example, Zionism is historically a loan from Germany (and thus classified a West Germanic),
# but linguistically, it is the English rendering of a Latin derivation of a Semitic place 
# name transmitted through Greek (i.e., basically *everything* but German)

# normalize language identifiers and split gold data
if javac *.java ; then 
	iconv -f utf-8 -t utf-8 etymon.txt | \
	java -Dfile.encoding=UTF-8 NormalizeLanguages langs.txt 1 3 | \
	iconv -f utf-8 -t ascii -c |\
	sort -u | \
	java Split etymon txt 80 0 20
fi;