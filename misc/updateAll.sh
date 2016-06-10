#!/bin/bash

for name in sec.php OO.php fdns.php optOO.php ccanal.php perf.php incr.php paradfa.php expl.php
do
	rm $name
	python insertCatLinks.py links2.txt trimmedRefs/$name > ${name}
done
scp sec.php OO.php fdns.php optOO.php ccanal.php perf.php incr.php paradfa.php expl.php ap1.cs.vt.edu://web/research/prolangs/refs
