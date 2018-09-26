#!/bin/bash 

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
getgraph()
{
    mkdir -p $2
    > $2/log.getAppGraph
    for apk in $1/*.apk
    do
        if [ -s $2/${apk##*/}.txt ];then
            echo "$apk already processed, skipping it"
            continue;
        fi

        timeout 1800 "bash getAppGraph.sh $apk >> $2/log.getAppGraph"
        mv $apk.txt $2/
    done
}

#getgraph /home/hcai/testbed/input/pairs.thirdset /home/hcai/Downloads/Mamadroid/mamadroid_code/graphs/benign-2015


cats=""
while read cate;
do
    cats="$cats""$cate""    "
done < /home/hcai/testbed/cat-final.txt

for cate in $cats;
do
    getgraph /home/hcai/bin/apks2017/$cate /home/hcai/mama/graphs/benign-2017
done


