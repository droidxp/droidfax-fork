#!/bin/bash

(test $# -lt 0) && (echo "too few arguments") && exit 0

rep=${1:-"ContagioLogs"}
TRACEDIR=/home/hcai/testbed/$rep/

resultdir=/home/hcai/testbed/contagioResults/generalReport/
mkdir -p $resultdir 
resultlog=$resultdir/log.generalReport.all
> $resultlog
resultidx=$resultdir/idx.generalReport
> $resultidx
for apkname in /home/hcai/testbed/input/Contagio/*.apk
do
    j=${apkname##*/}
    i=${j%.*}
	if [ ! -s $TRACEDIR/${i}.apk.logcat ];
	then
		continue
	fi
	#rt=`cat lowcov_malware | awk '{print $1}' | grep -a -c "^${i}.apk.logcat$"`
	#if [ $rt -lt 1 ];then
		echo "result for ${i}.apk" >> $resultlog 2>&1
		/home/hcai/bin/getpackage.sh /home/hcai/testbed/cg.instrumented/Contagio/org/${i}-org.apk >> $resultlog 2>&1
		sh /home/hcai/testbed/generalReport.sh \
			/home/hcai/testbed/cg.instrumented/Contagio/org/${i}-org.apk \
			$TRACEDIR/${i}.apk.logcat >> $resultlog 2>&1
		echo "$i" >> $resultidx
	#fi
done
mv /home/hcai/testbed/{calleerank.txt,callerrank.txt,calleerankIns.txt,callerrankIns.txt,compdist.txt,edgefreq.txt,gdistcov.txt,gdistcovIns.txt} $resultdir

mv /home/hcai/testbed/gfeatures.txt $resultdir/

exit 0
