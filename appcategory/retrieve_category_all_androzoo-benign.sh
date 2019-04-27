#!/bin/bash 

trial()
{
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
}


getPackage()
{
    i=0
    for p1 in ~/testbed/cg.instrumented/AndroZoo/benign-201*;
    do
        paths[$i]=$p1
        ((i=i+1))
    done

    sz=${#paths[@]}
    k=1
    for ((i=0;i< sz;i++))
    do
        p1=${paths[$i]}
        echo $p1
        echo "collecting package names from ${p1##*/} ......"
        datatag=list.app_packages.${p1##*/}
        >$datatag
        for fnapk in $p1/*.apk;
        do
            tgtp=`~/bin/getpackage.sh $fnapk | awk '{print $2}'`
            echo "$tgtp" >> $datatag
        done

        echo "collecting app categories from ${p1##*/} ......"
        python getCategory.py $datatag 
        restag=app.categories.${p1##*/}
        mv result.csv $restag
    done
}

getPackage

exit 0
