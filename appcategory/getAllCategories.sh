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
    for datatag in list.app_packages.*;
    do
        echo "collecting app categories from ${datatag##*.} ......"
        python getCategory.py $datatag 
        restag=app.categories.${datatag##*.}
        cat results.csv >> $restag
    done
}

getPackage

exit 0
