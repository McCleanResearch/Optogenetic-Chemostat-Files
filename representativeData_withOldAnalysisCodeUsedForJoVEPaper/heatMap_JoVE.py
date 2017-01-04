#Cameron Stewart and Taylor Scott
#UW-Madison, Biomedical Engineering
#PI: Megan McClean
#Dec 1, 2016

#This code can be used to convert recorded data from a chemostat experiment into
#a surface plot.

import numpy as np
import matplotlib.pyplot as plt
import os

print ("the current directory is:  ")
print (os.getcwd())
print ("Either move times.csv and the results files to this directory, or edit the rooDir variable")

# Save the current directory
curr_dir = os.getcwd()
numBins=30

# Change directory to rootDir. "." stays in the current directory
rootDir="."
os.chdir(rootDir)

times = np.genfromtxt ('times.csv')

logFluor=[]
time=[]
weights=[]

# Iterate through times and return k (the iteration number starting at k=0) and t (the time)
for k, t in enumerate(times):
    # Read fluorescence values from the results file for time t
    fluor=np.genfromtxt ('Results{}.csv'.format(k+1), delimiter=",")[:,4]

    # The number of fluorescence values at time t
    num_values = len(fluor)
    
    # Symmetric weighting; append a list of length num_values to weights
    weights = np.append(weights,np.ones_like(fluor)/num_values)

    # Append the a list of length num_values to the time array
    time = np.append(time, [t]*num_values)
    
    # Replace all fluorescence value less than 10 with 10
    np.place(fluor, fluor < 10, 10)
    
    # Calculate the log of each fluorescence value and append to logFluor
    logFluor=np.append(logFluor,np.log10(fluor))

print (len(time))
print (len(logFluor))
print (len(weights))

# Plot the histogram
plt.hist2d(time, logFluor,bins=[len(times),numBins],weights=weights,cmax=0.4,cmap='CMRmap')
plt.axis([0,4200,1,3.3])
# set the limits of the plot to the limits of the data
plt.colorbar()

plt.show()

# Return to original directory
os.chdir(curr_dir)
