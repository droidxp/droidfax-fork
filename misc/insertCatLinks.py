#!/usr/bin/python

import os
import sys
import string

def readACMLinks(fnlinks):
    ARTICLE="<!-- ACM DL Article: "
    BIBLIO="<!-- ACM DL Bibliometrics: "
    TAIL="-->"
    lines = file(fnlinks,'r').readlines()
    curarticle=None
    curbiblio=None
    linkinfo = dict()
    articlelines=""
    bibliolines=""
    for line in lines:
        line=line.lstrip().rstrip()
        line=line.rstrip('\r').rstrip('\n')


        if line.startswith(ARTICLE) and line.endswith(TAIL):
            # a new entry starts
            if curarticle != None:
                if curbiblio==None:
                    raise Exception("bibliometrics is none at article=" + curarticle)
                if curarticle.lower()!=curbiblio.lower():
                    raise Exception("curarticle="+curarticle+"; but curbiblio is for " + curbiblio)
                linkinfo[curarticle] = (articlelines, bibliolines)
                curbiblio = None
                articlelines=""
                bibliolines=""
            curarticle = line.replace(ARTICLE,'').replace(TAIL,'').lstrip().rstrip()
            continue
        if curarticle==None:
            continue
        if line.startswith(BIBLIO) and line.endswith(TAIL):
            curbiblio = line.replace(BIBLIO,'').replace(TAIL,'').lstrip().rstrip()
            #print "got curbiblio = " + curbiblio
            continue
        if curbiblio == None:
            articlelines += line
        else:
            bibliolines += line
    # the last entry
    if curarticle != None:
        assert curarticle.lower()==curbiblio.lower()
        linkinfo[curarticle] = (articlelines, bibliolines)

    return linkinfo

def findLinkInfoFuzzy(linkinfo, tstr):
    ret = None
    art = None
    for article in linkinfo.keys():
        if tstr.lower().find(article.lower()) != -1:
            if art == None or len(art)<len(article):
                art = article
                ret = linkinfo[art]
    return ret 

usedACMarticles=set()

def findLinkInfo(linkinfo, tstr):
    global usedACMarticles
    import re
    titles = re.findall(r"\"(.*?)\"", tstr, re.DOTALL)
    if len(titles)<1:
        raise Exception("no quoted title found in " + tstr)
    title = titles[0]
    for article in linkinfo.keys():
        if title.lower()==article.lower():
            usedACMarticles.add (article)
            return linkinfo[article]
    return None

if __name__ == "__main__":
    linkinfo = readACMLinks( sys.argv[1] )
    #print linkinfo
    #print str(len(linkinfo)) + " paper entries found."
    print string.join(file('header.txt','r').readlines(),'')

    from bs4 import BeautifulSoup
    soup = BeautifulSoup(string.join( file(sys.argv[2],'r').readlines(), '\n' ),'lxml')
    for li in soup.find_all('li'):
        ps = li.find_all('p')
        bs = li.find_all('b') 
        tstr = li.next_element
        #print tstr.lower()
        acminfo = findLinkInfo(linkinfo, tstr)
        if acminfo == None:
            #raise Exception("no acm info found for paper " + tstr)
            print li
            print "\r\n"
            continue
        print "<LI>"
        print acminfo[0]
        print acminfo[1]
        for p in ps[1:]:
            if str(p).lower().find('local') != -1:
                continue
            print p
        print "\r\n"

    print string.join(file('tail.txt','r').readlines(),'')

    sys.exit(0)

    print "never used acm articles"
    global usedACMarticles
    for a in set(linkinfo.keys()) - usedACMarticles:
        print a


