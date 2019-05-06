#!/usr/bin/python
import os
import sys
import string

gamecats=[]
familycats=[]

for line in file('gamecats.txt','r').readlines():
    line = line.lstrip().rstrip()
    gamecats.append(line)

for line in file('familycats.txt','r').readlines():
    line = line.lstrip().rstrip()
    familycats.append(line)

fh=file(sys.argv[1], 'r')
cat2num=dict()

total=0
for line in fh.readlines():
    line = line.lstrip().rstrip()

    items = string.split(line, ',')
    #print items
    if len(items) < 2:
        continue

    category = items[-1]

    if category in gamecats:
        category += ' (Games)'

    if category in familycats:
        category += ' (Family)'

    if category not in cat2num.keys():
        cat2num[category] = 1
    else:
        cat2num[category] = cat2num[category]+1

    total+=1

cats = cat2num.keys()
cats.sort()
for cat in cats:
    print "%s\t%d\t%.2f%%" % (cat, cat2num[cat], cat2num[cat]*1.0/total*100)


