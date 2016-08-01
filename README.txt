August 01, 2016
Cameron Stewart, cstewart7@wisc.edu
Research Technician at UW-Madison
Principal Investigator: Megan McClean, mmcclean@wisc.edu
Lab website: http://mccleanlab.bme.wisc.edu
--------------------------------------------------------------------------------------
SUMMARY: This repository contains files for creating a continuous culturing apparatus
for the purpose of studing the effects of light exposure on the cultured microbes
by microscopy. At a high level: The user will use "Micro-Manager" to control a
microscope, FIJI to analyze the images, an arduino microcontroller to control
peristaltic pumps and a heating pad, and the "bioreactorController" plugin to coordinate
these activities.
---------------------------------------------------------------------------------------

-The "extensive_lab_manual....docx" document in this repository is intended to describe
all of the steps needed to build this, and useful figures along the way. It is a verbose
version of the paper "Design and Implementation of an Automated Illuminating, Culturing,
 and Sampling System for Microbial Optogenetic Applications" by Stewart et. al. which
is scheduled to be published in JoVE in late 2016 pending peer review.

-The ".fzz" file contains the design for a printed circuit board that can be stacked 
on an Arduino microcontroller and be used to regulate power to external devices, read
inputs from an external thermometer, and control an LED matrix. To build the apparatus
described in the manual, the reader will need to order a circuit board with this
design. It can be opened with the Fritzing software which is free and open-source.
An additional use for this, besides ordering the board, is that the usser can inspect
the connections in the PCB and/or modify it for another purpose.

-The "bioreactorControllerMMPlugin_v01.jar" file is the heart of this kit. It should be
copied into the "mmplugins" folder of micromanager. This controls the microscope,
performs the image analysis (or calls an optional Beanshell script to perform the
image analysis), sends parameters to the microcontroller which regulates power to
the peristaltic pumps and heating pad of the continuous culturing vessel, and it can call
external scripts for supplementary analysis or feedback control.

-The "microcontroller_code" folder contains code that should be uploaded to the
microcontroller and its own "readme"

-The "bioreactorTimecourse.csv" file contains a 3 columns of values. The first
is a column of time values. The second is a column of the number of LEDs of the 
LED matrix that should be on (0-64), and the third is the pulse-width-modulated
intensity of those LEDs (0-255). Together, they dictate how the microcontroller
control the LED matrix over time. It is not necessary, but it may be useful to
use this file instead of manually entering these values into the "bioreactorController"
plugin.

-The "supplementary_scripts" folder contains Beanshell (a java-like language) scripts
that can be used in place of the plugin. The downside of using these scripts is that they
may not be synchronized with the plugin code. The benefit of using them is that they can
be easily customized in the beanshell script editor.