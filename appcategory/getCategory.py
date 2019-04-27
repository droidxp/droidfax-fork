######## Fetch App names and genre of apps from playstore url, using pakage names #############
"""
Reuirements for running this script:
1. requests library
Note: Run this command to avoid insecureplatform warning pip install --upgrade ndg-httpsclient
2. bs4

pip install requests
pip install bs4
"""

import requests
import csv
import os
import sys
from bs4 import BeautifulSoup

# url to be used for package
APP_LINK = "https://play.google.com/store/apps/details?id="
output_list = []; input_list = []

# get input file path
print "Need input CSV file (absolute) path \nEnsure csv is of format: <package_name>, <id>\n\nEnter Path:"

if len(sys.argv) > 1:
    input_file_path = sys.argv[1]
else:
    input_file_path = str(raw_input())

# store package names and ids in list of tuples
with open(input_file_path, 'rb') as csvfile:
    for line in csvfile.readlines():
        #(p, i) = line.strip().split(',')
        p = line.strip()
        input_list.append((p, 1))


print "\n\nSit back and relax, this might take a while!\n\n"

for package in input_list:

    # generate url, get html
    url = APP_LINK + package[0]
    r = requests.get(url)

    if not (r.status_code==404):
        data = r.text
        soup = BeautifulSoup(data, 'html.parser')

        # parse result
        x = ""; y = "";
        try:
            #x = soup.find('div', {'class': 'id-app-title'})
            x = soup.find('h1', {'itemprop': 'name'})
            x = x.text
        except:
            #print "Package name not found for: %s" %package[0]
            print "App name not found for: %s" %package[0]

        try:
            #y = soup.find('span', {'itemprop': 'genre'})
            y = soup.find('a', {'itemprop': 'genre'})
            y = y.text
        except:
            print "category not found for: %s" %package[0]

        output_list.append([package[0],x,y])

    else:
        print "App not found: %s" %package[0]

# write to csv file
with open('results.csv', 'w') as fp:
    a = csv.writer(fp, delimiter=",")
    a.writerows(output_list)
