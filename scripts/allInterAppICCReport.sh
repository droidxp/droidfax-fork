#!/bin/bash

(test $# -lt 3) && (echo "too few arguments") && exit 0

indir=$1
linkage=$2
resdir=$3
APKDIR=/home/hcai/testbed/$indir/pairs/$linkage
TRACEDIR=/home/hcai/testbed/pairTrace_$linkage

resultdir=/home/hcai/testbed/$resdir/interAppICCReport/$linkage
mkdir -p $resultdir
resultlog=$resultdir/log.interAppICCReport.all.$linkage
> $resultlog
for ((i=1;i<=54;i++))
do
	if [ ! -d $APKDIR/$i ];then continue; fi
	echo "result for $linkage pair $i" >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/s.apk >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/t.apk >> $resultlog 2>&1
	bash /home/hcai/testbed/interAppICCReport.sh \
		$APKDIR/$i \
		$TRACEDIR/$i.logcat >> $resultlog 2>&1
done
mv /home/hcai/testbed/{gicc.txt,dataicc.txt,extraicc.txt,icclink.txt,bothdataicc.txt,pairicc.txt} \
	$resultdir	

exit 0
