#Cameron Stewart
#Lab Technician at UW-Madison, Biomedical Engineering
# PI: Megan McClean
#March 7, 2016

#This code can be used to convert recorded data from a chemostat experiment into
#a surface plot.

import numpy as np
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
from matplotlib import cm
import os
print "the current directory is:  "
print os.getcwd()
print "Either move times.csv and the results files to this directory, or edit the rooDir variable"

numBins=30
rootDir=""
times = np.genfromtxt (rootDir+'times.csv', delimiter=",")

logFluor=[]
time=[]
sumTime=0
weights=[]
for num in range(1,times.size+1):
    csv=np.genfromtxt (rootDir+'Results'+str(num)+'.csv', delimiter=",")
    csv=np.transpose(csv[:,4])
    weights = np.append(weights,np.ones_like(csv)/len(csv))
    for i in np.arange(len(csv)):
        time=np.append(time,times[num-1])
        sumTime=sumTime+1
        if csv[i]<10:
            csv[i]=10
    logFluor=np.append(logFluor,np.log10(csv))

print len(time)
print len(logFluor)
print sumTime
print len(weights)


plt.hist2d(time, logFluor,bins=[len(times),numBins],weights=weights,cmax=0.4,cmap='CMRmap')
plt.axis([0,4200,1,3.3])
# set the limits of the plot to the limits of the data
plt.colorbar()

plt.show()
