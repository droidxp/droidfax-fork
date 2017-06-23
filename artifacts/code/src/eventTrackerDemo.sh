#!/bin/bash

apk=${1:-"adhoc/com.rudicedita.com.kimbailey.apk"}
tmv=${2:-"60"}

echo " Instrument for event tracking ... "
echo
bash eventInstr.sh $apk >/dev/null 2>&1

echo " Tracing events in $apk .... "
runOneApk() {
	fnapk=$1
	
	if [ ! -s $fnapk ];then return; fi

	/home/hcai/testbed/setupEmu.sh Nexus-One-10 2>/dev/null
	apkinstall $fnapk
	adb logcat -v raw -s "event-monitor" &>/tmp/eventtracker.logcat 2>&1 &
	tgtp=`~/bin/getpackage.sh $fnapk | awk '{print $2}'`
	timeout $tmv "adb shell monkey -p $tgtp --ignore-crashes --ignore-timeouts --ignore-security-exceptions --throttle 200 10000000 " 
	killall -9 adb
	killall -9 emulator 
	rm -f /tmp/eventtracker.logcat
}
runOneApkManual() {
	fnapk=$1
	
	if [ ! -s $fnapk ];then return; fi

	/home/hcai/testbed/setupEmu.sh Nexus-One-10
	apkinstall $fnapk
	echo "Now start the app (default: $apk) and manipulate it, then observe the event sequence on screen..."
	adb logcat -v raw -s "event-monitor" 
	killall -9 adb
	killall -9 emulator 
}

suffix=${apk##*/}
suffix=${suffix%.*}
#runOneApk event.instrumented/$suffix.apk
runOneApkManual event.instrumented/$suffix.apk

