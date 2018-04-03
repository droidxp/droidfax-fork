#!/bin/bash

for subdir in "benign-2010" "benign-2011" "benign-2012" "benign-2013" "benign-2014" "benign-2015" "benign-2016" 
do
    cd generalReport
    echo "interlayCalls"
    python ~/gitrepo/droidfax/scripts/interlayerCalls.py  gfeatures.txt

    echo "interlayer interactions"
    python  ~/gitrepo/droidfax/scripts/edgefreqRanking.py edgefreq.txt

    echo "edgefreq scatter plot"
    Rscript  ~/gitrepo/droidfax/scripts/edgefreqRanking-scatter.R edgefreq.txt

    echo "component distribution"
    Rscript  ~/gitrepo/droidfax/scripts/compdist.R compdist.txt 
    #Rscript  ~/gitrepo/droidfax/scripts/compdist-insview.R compdist.txt

    echo "call distribution"
    Rscript  ~/gitrepo/droidfax/scripts/gdistcov-combine.R gdistcov.txt 
    #Rscript  ~/gitrepo/droidfax/scripts/gdistcovIns-combine-insview.R gdistcovIns.txt 
    Rscript  ~/gitrepo/droidfax/scripts/gdistcovIns-combine.R gdistcovIns.txt

    cd ../ICCReport
    echo "gicc" 
    Rscript  ~/gitrepo/droidfax/scripts/gicc-single.R gicc.txt

    echo "data icc"
    Rscript  ~/gitrepo/droidfax/scripts/iccdataextras.R dataicc.txt
    mv deicc.pdf dataicc.pdf

    echo "extras icc"
    Rscript  ~/gitrepo/droidfax/scripts/iccdataextras.R extraicc.txt
    mv deicc.pdf extraicc.pdf

    echo "bothdata icc"
    Rscript  ~/gitrepo/droidfax/scripts/iccdataextras.R bothdataicc.txt
    mv deicc.pdf bothdataicc.pdf


    #cd ../securityReport
    cd ../rankReport
    echo "srcsink use extent"
    Rscript  ~/gitrepo/droidfax/scripts/srcsink.R srcsink.txt

    echo "src categorization"
    Rscript  ~/gitrepo/droidfax/scripts/src-tab.R src.txt

    echo "sink categorization"
    Rscript  ~/gitrepo/droidfax/scripts/sink-tab.R sink.txt

    echo "risky source categorization"
    Rscript  ~/gitrepo/droidfax/scripts/risk-src-tab.R src.txt

    echo "risk sink categorization"
    Rscript  ~/gitrepo/droidfax/scripts/risk-sink-tab.R sink.txt


    echo "all callback"
    Rscript  ~/gitrepo/droidfax/scripts/callback.R callback.txt

    echo "callback tab"
    Rscript  ~/gitrepo/droidfax/scripts/callback-tab.R callback.txt

    echo "lifecycle method"
    Rscript  ~/gitrepo/droidfax/scripts/lifecycleMethod-tab.R lifecycleMethod.txt

    echo "event handler"
    Rscript  ~/gitrepo/droidfax/scripts/eventHandler-tab.R eventHandler.txt 


    cd ..
done
exit 0


