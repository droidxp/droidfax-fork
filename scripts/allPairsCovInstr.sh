#!/bin/bash

#(test $# -lt 1) && (echo "too few arguments") && exit 0

srcdir=/home/hcai/testbed/input/pairs/
destdir=/home/hcai/testbed/cov.instrumented/pairs/
mkdir -p $destdir
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

instr()
{
	#for subdir in "explicit" "implicit" "implicit_2ndset"
	#for subdir in "implicit" "implicit_2ndset"
	#for subdir in "implicit_3rdset"
	#for subdir in "implicit"
	#for subdir in "implicit_2ndset"
	#for subdir in "explicit_2ndset"
	for subdir in "explicit" "implicit"
	do
		#for ((i=1;i<51;i++))
		for i in $(seq 1 250)
		do
			if [ ! -d $srcdir/$subdir/$i ];then continue; fi

			srcp=`~/bin/getpackage.sh $srcdir/$subdir/$i/s.apk | awk '{print $2}'`
			tgtp=`~/bin/getpackage.sh $srcdir/$subdir/$i/t.apk | awk '{print $2}'`
			sc=`grep -a -c $srcp $srcdir/installed_apks.txt`
			tc=`grep -a -c $tgtp $srcdir/installed_apks.txt`
			if [ $sc -lt 1 -o $tc -lt 1 ];then continue; fi

			echo "instrument coverage monitoring for pair $i ..."
			for orgapk in $srcdir/$subdir/$i/*.apk
			do
				#echo "/home/hcai/testbed/covInstr.sh $orgapk "$destdir"/$subdir/$((i+0))/"
				timeout 1200 "/home/hcai/testbed/covInstr.sh $orgapk $destdir/$subdir/$((i+0))/ 1>/dev/null 2>&1"
			done

			for instredapk in $destdir/$subdir/$((i+0))/*.apk
			do
				echo "chapple" | /home/hcai/testbed/signandalign.sh $instredapk
			done
		done
	done
}

instr

exit 0
