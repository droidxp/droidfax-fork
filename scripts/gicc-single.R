#!/bin/Rscript

require(graphics)

args=commandArgs(trailingOnly=TRUE)
if (length(args)<1) {
	stop("too few arguments")
	exit
}

fndata=args[1]
tdata=read.table(file=fndata)

sdoverall<- matrix(NA, nrow=nrow(tdata), ncol=2)
dataextra<- matrix(NA, nrow=nrow(tdata), ncol=3)
gicc<- matrix(NA, nrow=nrow(tdata), ncol=4)

f.per <- function (x,y) {
	if (y<1e-10) return (0)
	return (x/y*100)
}

r=1
inv=1
for(i in seq(1,nrow(tdata),1)) {
	sicc=sum(tdata[i,4:5])
	dicc=sum(tdata[i,6:7])
	if (dicc<1e-10) {
		inv<-inv+1
		next
	}
	cursd <- c(f.per(sicc,tdata[i,1]), f.per(dicc,tdata[i,3]))
	sdoverall[r,] <- cursd 

	curdataextra<- c(f.per(tdata[i,8],dicc), f.per(tdata[i,9],dicc), f.per(tdata[i,10],dicc))
	dataextra[r,] <- curdataextra 

	curgicc<- c(f.per(tdata[i,11]+tdata[i,12],dicc), f.per(tdata[i,13]+tdata[i,14],dicc), f.per(tdata[i,15]+tdata[i,16],dicc), f.per(tdata[i,17]+tdata[i,18],dicc))
	gicc[r,] <- curgicc 

	r <- r+1
}

print(paste(inv," invalid data points ignored"))

colors2<-c("gray80","gray80")
colors3<-c("#ffff33","gray80","#ffff33","gray80","#ffff33","gray80") 
colors4<-c("#ffff33","gray80","#ffff33","gray80","#ffff33","gray80","#ffff33","gray80") 

pdf("./gicc-sdboth.pdf",width=2.5,height=3.0)
dboth <- cbind (sdoverall[,1], sdoverall[,2] )
sdnames = c("static","dynamic")
boxplot(dboth, names=c("single-app",""),col=colors2,ylab="percentage",range=0,cex.axis=0.4,lwd=0.3,cex.lab=0.5)
meandboth <- (colMeans(dboth, na.rm=TRUE))
points(meandboth, col="red", pch=18, cex=0.5)
stddboth <- apply( dboth, 2, sd, na.rm=TRUE )
#print(meandboth)
#print(stddboth)
for (k in 1:ncol(t(sdnames))) {
	#print( paste(snames[k], meanalls[k], "% (", stdalls[k], "%)") )
	cat(sprintf("%s\t%.2f%%\t%.2f%%\n", sdnames[k], as.numeric(meandboth[k]), as.numeric(stddboth[k])))
}
cat("\n")

pdf("./gicc-databoth.pdf",width=4.1,height=3.0)
datatypenames=c("standard-data only","bundle-data only","both forms of data")
dataextraboth <- cbind ( dataextra[,1], dataextra[,2], dataextra[,3] )
boxplot(dataextraboth, names=datatypenames,col=colors3,ylab="percentage (dynamic view)",range=0,cex.axis=0.4,lwd=0.3,cex.lab=0.5, medcol='black')
meandataextraboth <- (colMeans(dataextraboth, na.rm=TRUE))
points(meandataextraboth, col="red", pch=18, cex=0.5)
legend("top", legend=c("single-app"), cex=.5, col=c("#ffff33"), lwd=4.5, bty="n",horiz=TRUE)

stddataextraboth <- apply( t(dataextraboth), 2, sd )
for (k in 1:ncol(t(datatypenames))) {
	#print( paste(snames[k], meanalls[k], "% (", stdalls[k], "%)") )
	cat(sprintf("%s\t%.2f%%\t%.2f%%\n", datatypenames[k], as.numeric(meandataextraboth[k]), as.numeric(stddataextraboth[k])))
}
cat("\n")

pdf("./gicc-iccboth.pdf",width=4.1,height=3.0)
icctypenames=c("internal explicit","internal implicit","external explicit","external implicit")
giccboth <- cbind ( gicc[,1], gicc[,2], gicc[,3], gicc[,4] )
boxplot(giccboth, names=icctypenames,col=colors4,ylab="percentage (dynamic view)",range=0,cex.axis=0.4,lwd=0.3,cex.lab=0.5,medcol='black')
meangiccboth <- (colMeans(giccboth, na.rm=TRUE))
points(meangiccboth, col="red", pch=18, cex=0.5)
legend("top", legend=c("single-app"), cex=.5, col=c("#ffff33"), lwd=4.5, bty="n",horiz=TRUE)

stdgiccboth <- apply( t(giccboth), 2, sd )
for (k in 1:ncol(t(icctypenames))) {
	#print( paste(snames[k], meanalls[k], "% (", stdalls[k], "%)") )
	cat(sprintf("%s\t%.2f%%\t%.2f%%\n", icctypenames[k], as.numeric(meangiccboth[k]), as.numeric(stdgiccboth[k])))
}
cat("\n")

#dev.off

