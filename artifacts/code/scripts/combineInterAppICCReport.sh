#!/bin/bash

(test $# -lt 1) && (echo "too few arguments") && exit 0

outdir=$1
mkdir -p $outdir
for file in gicc dataicc extraicc icclink bothdataicc pairicc
do
	> $outdir/${file}.txt
	for ct in explicit implicit
	do
		cat $outdir/$ct/${file}.txt >> $outdir/${file}.txt
	done
done
exit 0
