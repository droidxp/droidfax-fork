#!/bin/bash
if [ $# -lt 1 ];then
	echo "Usage: $0 apk-file"
	exit 1
fi

apkfile=$1

ROOT=/home/hcai/
subjectloc=`pwd`

OUTDIR=${2:-"$subjectloc/cg.instrumented/"}

MAINCP="$ROOT/libs/rt.jar:$ROOT/libs/polyglot.jar:$ROOT/libs/soot-trunk.jar:$ROOT/libs/duafdroid.jar:$ROOT/workspace/droidfax/bin:$ROOT/libs/java_cup.jar"

SOOTCP="$ROOT/workspace/droidfax/bin:/home/hcai/Android/Sdk/platforms/android-19/android.jar"

for i in $ROOT/libs/*.jar;
do
	#SOOTCP=$SOOTCP:$i
    MAINCP=$MAINCP:$i
done

# get the apk file name without prefixing path and suffixing extension
suffix=${apkfile##*/}
suffix=${suffix%.*}

LOGDIR=out-sdkApi
mkdir -p $LOGDIR
logout=$LOGDIR/instr-$suffix.out
logerr=$LOGDIR/instr-$suffix.err

mkdir -p $OUTDIR

pkgname=`$ROOT/bin/getpackage.sh $apkfile | awk '{print $2}'`
echo -e "\n\n\tsdkApiInstrumenting $pkgname ..."

starttime=`date +%s%N | cut -b1-13`

	#-allowphantom \
   	#-duaverbose \
	#-dumpFunctionList \
	#-statUncaught \
    #-f c \
    #--nostatic --aplength 1 --aliasflowins --nocallbacks --layoutmode none --noarraysize --nopaths --pathalgo sourcesonly \
    #-android-jars $ROOT/libs/backup/android.jar \
    #-src-prec apk \
    #-f J \
    #-debug \
    #-force-android-jar $ROOT/libs/backup/android.jar \
	#-slicectxinsens \
	#-nophantom \
	#-p wjtp enabled:true -p wjpp enabled:false \
    #
	#-instr3rdparty \
	#-noMonitorICC \
	#-dumpJimple \
    #-noMonitorCalls \
    #-monitorEvents \
	#-catcallback /home/hcai/libs/catCallbacks.txt \
    #-instrlifecycle \
cmd="java -Xmx14g -ea -cp ${MAINCP} apiTracker.sceneInstr \
	-w -cp $SOOTCP -p cg verbose:false,implicit-entry:true \
	-p cg.spark verbose:false,on-fly-cg:true,rta:false \
	-d $OUTDIR \
	-instr3rdparty \
	-catsink /home/hcai/libs/catsinks.txt.final \
	-process-dir $apkfile"

#($cmd | tee $logout) 3>&1 1>&2 2>&3 | tee $logerr
#${cmd} 2>&1 | tee $logout
${cmd} 2>$logerr 1>$logout

stoptime=`date +%s%N | cut -b1-13`
echo "StaticAnalysisTime for $suffix elapsed: " `expr $stoptime - $starttime` milliseconds
echo "static analysis finished."

echo "now signing $suffix ..."
echo "chapple" | ./signandalign.sh $OUTDIR/${suffix}.apk >/dev/null
echo
exit 0


# hcai vim :set ts=4 tw=4 sws=4

