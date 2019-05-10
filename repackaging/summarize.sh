
for ((i=1;i<=55;i++));do echo "for result_$i..."; cat result_$i | awk -F, '{print $(NF-1)}'  | sort | uniq -c > stat_result_$i; done

for ((i=1;i<=55;i++));do echo "for result_$i..."; cat result_$i | awk -F, '{print $(NF-1)}'  >> result_all; done

cat result_all | awk '{print substr($1,0,4)}' | sort | uniq -c > stat_result_4digit

