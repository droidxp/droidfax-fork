#!/bin/bash

samplelist=$1
tmv=${2:-"300"}
port=${3:-"5554"}
avd=${4:-"Nexus-One-10"}
year=${5:-"2017"}

did="emulator-$port"

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

profiling()
{
    cate=$1

    srcdir=/home/hcai/testbed/cov.instrumented/AndroZoo/$cate
    finaldir=$srcdir

    OUTDIR=/home/hcai/testbed/sapienzCovAndroZooLogs/$cate
    mkdir -p $OUTDIR

	k=1

    setupEmu.sh ${avd} $port
    sleep 3
    pidemu=`ps axf | grep -v grep | grep -a -E "$avd -scale .3 -no-boot-anim -no-window -wipe-data -port $port" | awk '{print $1}'`

    for fnapk in $finaldir/*.apk;
	do
        echo "================ RUN INDIVIDUAL APP: ${fnapk##*/} ==========================="
        if [ -s $OUTDIR/${fnapk##*/}.logcat ];
        then
            echo "$fnapk has been processed already, skipped."
            continue
        fi

        #if [ `grep -a -c ${fnapk##*/} $samplelist` -lt 1 ];then
        #    echo "$fnapk is not in the sample list, skipped."
        #    continue
        #fi

		ret=`/home/hcai/bin/apkinstall $fnapk $did`
		n1=`echo $ret | grep -a -c "Success"`
		if [ $n1 -lt 1 ];then 
            #echo "killing pid $pidemu, the process of emulator at port $port, from runAndroZooApks_monkey.sh... because app cannot be installed successfully"
            #kill -9 $pidemu
            echo "$fnapk cannot be installed successfully, why would we waste time with sapienz then? skipped it."
            continue
        fi

        adb -s $did shell mount -o rw,remount /system
        adb -s $did push lib/motifcore.jar /system/framework
        adb -s $did push resources/motifcore /system/bin

        adb -s $did logcat -v raw -s "hcai-cov-monitor" &>$OUTDIR/${fnapk##*/}.logcat &
        pidadb=$!

        timeout $tmv "python main_hcai.py $fnapk $did"

        #killall -9 adb qemu-system-i386

        #rm -rf /tmp/android-hcai/*

        timeout 30 "/home/hcai/bin/apkuninstall $fnapk $did"
        kill -9 $pidadb

        ((k=k+1))
	done

    kill -9 $pidemu

	echo "totally $k apps in category $cate successfully traced."
}


s=0

#for cate in 2016 2015 2014
#for cate in 2013 2011 2010
#for cate in "benign-$year"
for cate in "$year"
do
    c=0
    echo "================================="
    echo "try installing category $cate ..."
    echo "================================="
    echo
    echo

    profiling $cate
    rm -rf /tmp/android-hcai/*
done

exit 0
