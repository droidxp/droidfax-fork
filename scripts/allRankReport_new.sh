#!/bin/bash

(test $# -lt 1) && (echo "too few arguments") && exit 0

rep=${1:-"benign-new-firstrep"}
#rep=${1:-"singleAppLogs_benign_set2_highcov_10m"}
#rep=${1:-"singleAppLogs_benign_set1_highcov_10m"}
cat=${2:-"implicit"}

#APKDIR=/home/hcai/testbed/input/pairs.firstset/$cat/
APKDIR=/home/hcai/testbed/input/pairs.secondset/$cat/

#TRACEDIR=/home/hcai/testbed/$rep/singleAppLogs_$cat
#TRACEDIR=/home/hcai/testbed/$rep/singleAppLogs_10m_$cat
TRACEDIR=/home/hcai/testbed/$rep/singleAppLogsNew_$cat
#TRACEDIR=/home/hcai/testbed/$rep/singleAppLogsNew_10m_$cat

resultdir=/home/hcai/testbed/results.benignNew/rankReport/$rep
#resultdir=/home/hcai/testbed/results.benignNew.highcov/rankReport/$rep
#resultdir=/home/hcai/testbed/results.highcov/rankReport/$rep
#resultdir=/home/hcai/testbed/results.benignNew.highcov.10m/rankReport/
#resultdir=/home/hcai/testbed/results.highcov.10m/rankReport/

mkdir -p $resultdir $resultdir/$cat
resultlog=$resultdir/log.rankReport.all.$cat
> $resultlog
for ((i=1;i<=250;i++))
do
	if [ ! -s $APKDIR/$i/s.apk ];then continue; fi
	if [ ! -s $APKDIR/$i/t.apk ];then continue; fi
	
	#srt=`cat lowcov_benign_set2.${cat}.final | awk '{print $1}' | grep -a -c "^${i}-s.logcat$"`
	srt=`cat lowcov_benign_set1.${cat}.final | awk '{print $1}' | grep -a -c "^${i}-s.logcat$"`
	#if [ $srt -lt 1 ];then 
		#x=`cat addendum.${cat} | awk '{print $1}' | grep -a -c "^${i}-s.logcat$"`
		#if [ $x -ge 1 ];then 
		echo "result for $cat $i/s.apk" >> $resultlog 2>&1
		/home/hcai/bin/getpackage.sh $APKDIR/$i/s.apk >> $resultlog 2>&1
		sh /home/hcai/testbed/rankReport.sh \
			$APKDIR/$i/s.apk \
			$TRACEDIR/$i-s.logcat >> $resultlog 2>&1
		#fi
	#fi

	#trt=`cat lowcov_benign_set2.${cat}.final | awk '{print $1}' | grep -a -c "^${i}-t.logcat$"`
	trt=`cat lowcov_benign_set1.${cat}.final | awk '{print $1}' | grep -a -c "^${i}-t.logcat$"`
	#if [ $trt -lt 1 ];then
		#x=`cat addendum.${cat} | awk '{print $1}' | grep -a -c "^${i}-t.logcat$"`
		#if [ $x -ge 1 ];then 
		echo "result for $cat $i/t.apk" >> $resultlog 2>&1
		/home/hcai/bin/getpackage.sh $APKDIR/$i/t.apk >> $resultlog 2>&1
		sh /home/hcai/testbed/rankReport.sh \
			$APKDIR/$i/t.apk \
			$TRACEDIR/$i-t.logcat >> $resultlog 2>&1
		#fi
	#fi
done
mv /home/hcai/testbed/{srcsink.txt,src.txt,sink.txt,callback.txt,lifecycleMethod.txt,eventHandler.txt} $resultdir/$cat
mv /home/hcai/testbed/securityfeatures.txt $resultdir/$cat/

exit 0
