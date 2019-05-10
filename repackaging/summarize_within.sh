
#for ((i=2010;i<=2017;i++));do echo "for result_within_benign-$i*..."; cat result_within_benign-$i* | awk -F, '{print $(NF-1)}'  | sort | uniq -c > within_result_$i; done

for ((i=2010;i<=2017;i++));do echo "for result_within_benign-$i*..."; cat result_within_benign-$i* | awk -F, '{print $(NF-1)}'  >> within_result_all; done

cat within_result_all | awk '{print substr($1,0,4)}' | sort | uniq -c > within_result_4digit

