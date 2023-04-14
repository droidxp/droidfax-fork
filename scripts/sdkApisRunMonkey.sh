#!/bin/bash

(test $# -lt 3) && (echo "too few arguments") && exit 0

indir=$1
linkage=$2
tmv=${3:-"3600"}
srcdir=/home/hcai/testbed/cg.instrumented/$indir/pairs/${linkage}
OUTDIR=/home/hcai/testbed/sdkApiTrace_$linkage
mkdir -p $OUTDIR

timeout() {

    time=$1

    # start the command in a subshell to avoid problem with pipes
    # (spawn accepts one command)
    command="/bin/sh -c \"$2\""

    expect -c "set echo \"-noecho\"; set timeout $time; spawn -noecho $command; expect timeout { exit 1 } eof { exit 0 }"    

    if [ $? = 1 ] ; then
        echo "Timeout after ${time} seconds"
    fi

}

for ((i=1;i<=55;i++))
do
	if [ ! -s $srcdir/${i}/s.apk ];then continue; fi
	if [ ! -s $srcdir/${i}/t.apk ];then continue; fi

	echo "================ RUN APP PAIR $i ==========================="
	# echo "Starting android emulator ......"
	/home/hcai/testbed/setupEmu.sh Nexus-One-10 2>/dev/null 1>&2

	/home/hcai/testbed/singlePairInstall.sh $indir $linkage $i
  #apkinstall $srcdir/$i/s.apk
	adb logcat -v raw -s "apicall-monitor" &>$OUTDIR/${i}.logcat &

	timeout $tmv "/home/hcai/testbed/runPair.sh $indir $linkage $i >$OUTDIR/${i}.monkey"

	/home/hcai/testbed/singlePairUninstall.sh $indir $linkage $i
  #apkuninstall $srcdir/$i/s.apk

	killall -9 adb
	killall -9 adb
done

exit 0