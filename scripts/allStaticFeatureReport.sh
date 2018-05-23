#!/bin/bash

(test $# -lt 3) && (echo "too few arguments") && exit 0

APKDIR=$1
TRACEDIR=$2
RESULTDIR=$3

mkdir -p $RESULTDIR/staticFeatureReport
resultlog=$RESULTDIR/staticFeatureReport/log.staticFeatureReport.all
> $resultlog
for orgapk in $APKDIR/*.apk
do
    packname=${orgapk##*/}
	if [ ! -s $TRACEDIR/$packname.logcat ];
	then
		continue
	fi

    echo "result for $orgapk" >> $resultlog 2>&1
    /home/hcai/bin/getpackage.sh $orgapk >> $resultlog 2>&1
    sh /home/hcai/testbed/staticFeatureReport.sh \
        $orgapk >> $resultlog 2>&1
done

mv /home/hcai/testbed/staticfeatures.txt $RESULTDIR/staticFeatureReport/

exit 0




