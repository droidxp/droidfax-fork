#!/bin/bash

destdir=/home/hcai/testbed/cg.instrumented/adhoc/

instr()
{
	mkdir -p $destdir
	for orgapk in /home/hcai/testbed/adhoc/*.apk
	do
		/home/hcai/testbed/cgInstr.sh $orgapk $destdir
	done
}

sign()
{
	for instredapk in $destdir/*.apk
	do
		echo "chapple" | /home/hcai/testbed/signandalign.sh $instredapk
	done
}

instr

#sign

exit 0
