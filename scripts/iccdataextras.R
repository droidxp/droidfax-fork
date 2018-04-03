#!/bin/Rscript

require(graphics)

args=commandArgs(trailingOnly=TRUE)
if (length(args)<1) {
	stop("too few arguments")
	exit
}

fndata=args[1]
tdata=read.table(file=fndata)

deicc<- matrix(NA, nrow=nrow(tdata), ncol=ncol(tdata)/2)
#print(paste("row=",nrow(tdata),"col=",ncol(tdata)))

f.per <- function (x,y) {
	if (y<1e-10) return (0)
	return (x/y*100)
}

r=1
inv=1
for(i in seq(1,nrow(tdata),1)) {
	dicc=sum(tdata[i,1:8])
	if (dicc<1e-10) {
		inv<-inv+1
		next
	}

	curdeicc<- c(f.per(tdata[i,1]+tdata[i,2],dicc), f.per(tdata[i,3]+tdata[i,4],dicc), f.per(tdata[i,5]+tdata[i,6],dicc), f.per(tdata[i,7]+tdata[i,8],dicc))
	deicc[r,] <- curdeicc

	r <- r+1
}

print(paste(inv," invalid data points ignored."))

colors<-c("red","green","blue","darkorange") #,"black","yellow","darkorange","darkorchid","gold4","darkgrey")

pdf("./deicc.pdf")
icctypenames=c("int_ex","int_im","ext_ex","ex_im")
boxplot(deicc, names=icctypenames,col=colors,ylab="percentage")
meandeicc <- (colMeans(deicc, na.rm=TRUE))
points(meandeicc, col="gold", pch=18, cex=1.5)

stdicc<- apply( t(deicc), 2, sd, na.rm=TRUE)
for (k in 1:ncol(t(icctypenames))) {
	#print( paste(snames[k], meanalls[k], "% (", stdalls[k], "%)") )
	cat(sprintf("%s\t%.2f%%\t%.2f%%\n", icctypenames[k], as.numeric(meandeicc[k]), as.numeric(stdicc[k])))
}
cat("\n")

#dev.off


