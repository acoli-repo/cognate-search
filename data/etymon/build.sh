#!/bin/bash

for i in {1..10000}; do
	for c in {a..z}; do
		if [ ! -d $c ]; then mkdir $c >&/dev/null; fi;
		TGT=$c/etymon-$i-$c.html;
		if [ ! -e $TGT ]; then
			wget "https://www.etymonline.com/search?page="$i"&q="$c -O $TGT
		fi;
	done
#https://www.etymonline.com/search?page=1&q=a
done;
# 
