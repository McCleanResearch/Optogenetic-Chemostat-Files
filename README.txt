June 03, 2016
Cameron Stewart, cstewart7@wisc.edu
Research Technician at UW-Madison
Principal Investigator: Megan McClean, mmcclean@wisc.edu
Lab website: http://mccleanlab.bme.wisc.edu
--------------------------------------------------------------------------------------
SUMMARY: This repository contains files for creating a continuous culturing apparatus
for the purpose of studing the effects of light exposure on cultured microbes
by microscopy. At a high level: The user will use "Micro-Manager" to control a
microscope, FIJI to analyze the images, an arduino microcontroller to control
peristaltic pumps and a heating pad, and a beanshell script to coordinate these
activities.
---------------------------------------------------------------------------------------

-The "extensive_lab_manual....docx" document in this repository is intended to describe
all of the steps needed to build this, and useful figures along the way.

-The ".fzz" file contains the design for a printed circuit board that can be stacked 
on an Arduino microcontroller and be used to regulate power to external devices, read
inputs from an external thermometer, and control an LED matrix. To build the apparatus
described in the manual, the reader will need to order a circuit board with this
design. It can be opened with the Fritzing software which is free and open-source.
An additional use for this, besides ordering the board, is that the usser can inspect
the connections in the PCB and/or modify it for another purpose.

-The "bioreactorParameters.csv" file contains paramerters for running the experiment.
These include: the filepath where results should be saved to, the port number of
the connected microcontroller, the fraction of time for which a pump should be on...

-The "bioreactorTimecourse.csv" file contains a 3 columns of values. The first
is a column of time values. The second is a column of the number of LEDs of the 
LED matrix that should be on (0-64), and the third is the pulse-width-modulated
intensity of those LEDs (0-255). Together, they dictate how the microcontroller
control the LED matrix over time.

-The ".bsh" files are beanshell scripts. Beanshell is similar to java. The best way
to learn about it would be to open the "supplementaryScript1_Intro_to_beanshell.bsh"
in the script editor in Micro-Manager. This file performs some simple arithmatic
and print calls which demonstrate how the language wroks. The most important
of these files is the "experimentScript.bsh." This is the file that is actually used
to run the optogenetic chemostat. Importantly, its first action is to pull values
from the ".csv" files. This is the results of an (amateur) attempt to generalize the
code. In reality, the user will benifit from looking at the code itself and modifying
it for their on purpose.

-The "microcontroller_code" folder contains code that should be uploaded to the
microcontroller and its own "readme"
