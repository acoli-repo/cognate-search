#!/bin/bash
# if a reference language is not available as target language, use this function to invert the dictionary
# reads from stdin, writes to stdout
# example call
# cat trans_CA-IT.tsv | bash -e invert-dict.sh > trans_IT-CA.tsv

perl -e '
	while(<>) {
		my @args = split(/\t/,$_);	# includes <newline> in last argument
		print $args[6]."\t".$args[5]."\t".$args[4]."\t".$args[3]."\t".$args[2]."\t".$args[1]."\t".$args[0]."\t".$args[7];
	}'
# 0 "written_rep_a"
# 1 "lex_entry_a"
# 2 "sense_a"
# 3 "trans"
# 4 "sense_b"
# 5 "lex_entry_b"
# 6 "written_rep_b"
# 7 "POS"