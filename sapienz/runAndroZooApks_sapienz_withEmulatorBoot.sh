#!/bin/bash

#tmv=${1:-"300"}
#did=${2:-"emulator-5554"}
tmv=${1:-"300"}
port=${2:-"5554"}
avd=${3:-"Nexus-One-10"}
year=${4:-"2010"}
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

    srcdir=/home/hcai/testbed/cg.instrumented/AndroZoo/$cate
    finaldir=$srcdir

    OUTDIR=/home/hcai/testbed/sapienzAndroZooLogs/$cate
    mkdir -p $OUTDIR

	k=1

    for fnapk in $finaldir/*.apk;
	do
        echo "================ RUN INDIVIDUAL APP: ${fnapk##*/} ==========================="
        if [ -s $OUTDIR/${fnapk##*/}.logcat ];
        then
            echo "$fnapk has been processed already, skipped."
            continue
        fi

        timeout $tmv "python main.py $OUTDIR $fnapk"

        killall -9 adb qemu-system-i386

        rm -rf /tmp/android-hcai/*

        ((k=k+1))
	done

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
