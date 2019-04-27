#!/bin/bash 

for p1 in ~/testbed/cg.instrumented/AndroZoo/benign-201*;
do
    for p2 in ~/testbed/cg.instrumented/AndroZoo/benign-201*;
    do
        if [ $p1 == $p2 ];then
            continue
        fi
        #echo "working on $p1 and $p2......"
    done
done


checkAcross()
{
    i=0
    for p1 in ~/testbed/cg.instrumented/AndroZoo/benign-201*;
    do
        paths[$i]=$p1
        ((i=i+1))
    done

    sz=${#paths[@]}
    k=1
    >alllog
    for ((i=0;i< sz-1;i++))
    do
        p1=${paths[$i]}
        for ((j=i+1;j<sz;j++))
        do
            p2=${paths[$j]}
            echo "working on $p1 and $p2......"
            echo "## between $p1 and $p2......" 1> result_$k
            echo "## between $p1 and $p2......" 1>> alllog 

            java -jar fsquadra.jar $p1 $p2 -o=result_$k 1>>alllog 2>&1
            ((k=k+1))
        done
    done
}

for p1 in ~/testbed/cg.instrumented/AndroZoo/benign-201*;
do
    java -jar fsquadra.jar $p1 $p1 -o=result_within_${p1##*/}
done


exit 0
