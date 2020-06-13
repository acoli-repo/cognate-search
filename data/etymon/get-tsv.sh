#!/bin/bash
# parse TSV list with selected source languages from etymon html


# todo: skip Middle English

for file in [a-z]/etym*html; do
	echo $file 1>&2;
	cat $file | \
	perl -pe 's/[\s\r\n]+/ /g; s/(<object)/\n$1/g; s/(<\/object>)/$1\n/g;' | \
	egrep '<object' | \
	perl -e '
		while(<>) {
			s/[\r\n]//g;
			my $word=$_;
			$word=~s/.*<a [^>]+>(.*)<\/a>.*/$1/;
			$word=~s/<\/a>.*//;
			$word=~s/<[^>]*>//g;
			$word=~s/ +/ /g;
			my $tag=$word;
			$word=~s/\([^\)]*\)//g;
			$word=~s/ +/ /g;
			$tag=~s/.*(\([^\)]+\)).*/$1/g;
			if($tag=~m/^\(.*\)$/) {
				$tag=~s/[\(\)]//g;
			} else {
				$tag="";
			}
			my $origin=$_;
			$origin=~s/.*<\/a>//g;
			if($origin=~m/ from /) {
				while($origin=~m/ from .* from /) {
					$origin=~s/( from .*) from .*/$1/;
				}
				$sourceForm=$origin;
				$origin=~s/.* from ([^<]+) *<.*/$1/;
				$sourceForm=~s/.* from ([^<]+) *<span[^>]*>([^<]+)<.*/$2/;
				if($sourceForm=~m/</) {
					$sourceForm="";
				}
				$origin=~s/^\s*([^<]+)<.*/$1/;
				if($origin=~m/</) {
					$origin="";
				}				
			} else { $origin="" }
			if($origin=~m/^[A-Z][a-zA-Z ]+$/) {
				print $word."\t".$tag."\t".$origin."\t".$sourceForm."\n"; #  ".$_."\n";
			}
		}
	';
done
# word
# uri
# source language
# source form