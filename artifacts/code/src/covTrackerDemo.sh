#!/bin/bash

apk=${1:-"adhoc/com.rudicedita.com.kimbailey.apk"}
tmv=${2:-"60"}

echo " Instrument for statement coverage tracking ... "
echo
bash covInstr.sh $apk >/dev/null 2>&1

echo " Tracking statement coverage in $apk .... "
runOneApk() {
	fnapk=$1
	
	if [ ! -s $fnapk ];then return; fi

	/home/hcai/testbed/setupEmu.sh Nexus-One-10 2>/dev/null
	apkinstall $fnapk
	adb logcat -v raw -s "hcai-cov-monitor" &>/tmp/covtracker.logcat 2>&1 &
	tgtp=`~/bin/getpackage.sh $fnapk | awk '{print $2}'`
	timeout $tmv "adb shell monkey -p $tgtp --ignore-crashes --ignore-timeouts --ignore-security-exceptions --throttle 200 10000000 " 
	killall -9 adb
	killall -9 emulator 
	rm -f /tmp/covtracker.logcat
}
runOneApkManual() {
	fnapk=$1
	
	if [ ! -s $fnapk ];then return; fi

	/home/hcai/testbed/setupEmu.sh Nexus-One-10
	apkinstall $fnapk
	echo "Now start the app (default: $apk) and manipulate it, then observe the coverage statistics on screen..."
	adb logcat -v raw -s "hcai-cov-monitor" 
	killall -9 adb
	killall -9 emulator 
}

suffix=${apk##*/}
suffix=${suffix%.*}
#runOneApk event.instrumented/$suffix.apk
runOneApkManual cov.instrumented/$suffix.apk

