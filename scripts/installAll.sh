#!/bin/bash

destdir=/home/hcai/testbed/cg.instrumented/adhoc

install()
{
	for apk in $destdir/*.apk
	do
		echo install $apk ...
		adb install $apk
	done
}

install

exit 0
