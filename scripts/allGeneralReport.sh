#!/bin/bash

(test $# -lt 3) && (echo "too few arguments") && exit 0

indir=$1
linkage=$2
resdir=$3
APKDIR=/home/hcai/testbed/$indir/pairs/$linkage
TRACEDIR=/home/hcai/testbed/singleappTrace_$linkage

resultdir=/home/hcai/testbed/$resdir/generalReport/$linkage
mkdir -p $resultdir
resultlog=$resultdir/log.generalReport.all.$linkage
> $resultlog
for ((i=1;i<=54;i++))
do
	if [ ! -d $APKDIR/$i ];then continue; fi
	echo "result for $linkage $i/s.apk" >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/s.apk >> $resultlog 2>&1
	bash /home/hcai/testbed/generalReport.sh \
		$APKDIR/$i/s.apk \
		$TRACEDIR/$i-s.logcat >> $resultlog 2>&1

	echo "result for $linkage $i/t.apk" >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/t.apk >> $resultlog 2>&1
	bash /home/hcai/testbed/generalReport.sh \
		$APKDIR/$i/t.apk \
		$TRACEDIR/$i-t.logcat >> $resultlog 2>&1
done
mv /home/hcai/testbed/{calleerank.txt,callerrank.txt,calleerankIns.txt,callerrankIns.txt,compdist.txt,edgefreq.txt,gdistcov.txt,gdistcovIns.txt,gfeatures.txt} $resultdir

exit 0
