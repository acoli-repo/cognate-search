#!/bin/bash
# compile cognate search command-line tool and prep data

# compile
if [ -e bin ]; then
	echo found compiled java classes, skipping compilation 1>&2;
else
	echo compile java classes 1>&2;
	cd src;
	if javac `find |grep 'java$'`; then
		cd ..;
		cp -r src bin
		rm `find src | grep 'class$'`;
		rm `find bin | grep 'java$'`;
	else
		cd ..
	fi;
fi;

# prep multilingual embeddings
if [ -e glove6B.50d.plusTIAD.txt.gz ] ; then
	echo using existing glove6B.50d.plusTIAD.txt.gz 1>&2;
else

	# prep dictionaries
	if [ -e dicts ]; then
		if [ -e dicts/inverted ]; then
			echo found dictionaries: 1>&2;
			find dicts | sed s/'^'/'\t'/g 1>&2;
		else
			echo create inverted dictionaries 1>&2;
			cd dicts;
			mkdir inverted;
			for file in *.tsv; do
				echo "  "$file 1>&2;
				cat $file | bash -e ./invert-dict.sh > inverted/$file.inverted
			done;
		fi;
	else
		echo warning: did not find folder dicts/ 1>&2;
	fi;

	# prep multilingual embeddings
	if [ ! -e glove.6B.50d.txt.gz ]; then
		if [ ! -e glove6B.50d.plusTIAD.txt ] ; then
			wget -nc http://nlp.stanford.edu/data/wordvecs/glove.6B.zip
			unzip -n glove.6B.zip glove.6B.50d.txt
			gzip glove.6B.50d.txt
			java -classpath bin TIADEmbedder glove.6B.50d.txt en `find dict | egrep '\.tsv$|\.inverted$'` > glove6B.50d.plusTIAD.txt
		fi;
		gzip glove6B.50d.plusTIAD.txt
	fi;
fi;

