#!/bin/bash

(test $# -lt 3) && (echo "too few arguments") && exit 0

indir=$1
linkage=$2
resdir=$3
APKDIR=/home/hcai/testbed/$indir/pairs/$linkage
TRACEDIR=/home/hcai/testbed/singleappTrace_$linkage

resultdir=/home/hcai/testbed/$resdir/securityReport/$linkage
mkdir -p $resultdir
resultlog=$resultdir/log.securityReport.all.$linkage
> $resultlog
for ((i=1;i<=54;i++))
do
	if [ ! -d $APKDIR/$i ];then continue; fi
	echo "result for $linkage $i/s.apk" >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/s.apk >> $resultlog 2>&1
	bash /home/hcai/testbed/securityReport.sh \
		$APKDIR/$i/s.apk \
		$TRACEDIR/$i-s.logcat >> $resultlog 2>&1

	echo "result for $linkage $i/t.apk" >> $resultlog 2>&1
	/home/hcai/bin/getpackage.sh $APKDIR/$i/t.apk >> $resultlog 2>&1
	bash /home/hcai/testbed/securityReport.sh \
		$APKDIR/$i/t.apk \
		$TRACEDIR/$i-t.logcat >> $resultlog 2>&1
done

mv /home/hcai/testbed/{srcsink.txt,src.txt,sink.txt,callback.txt,lifecycleMethod.txt,eventHandler.txt,securityfeatures.txt} \
	$resultdir

exit 0
