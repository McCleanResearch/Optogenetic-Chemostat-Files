December 01, 2016
Cameron Stewart, cstewart7@wisc.edu
Research Technician at UW-Madison
Principal Investigator: Megan McClean, mmcclean@wisc.edu
Lab website: http://mccleanlab.bme.wisc.edu

-----------------------------------------------------------------------
RELATED ARTICLE:
Stewart, C., McClean, M. N. Design and Implementation of an Automated 
Illuminating, Culturing, and Sampling System for Microbial Optogenetic 
Applications. J. Vis. Exp. (), e54894, doi:10.3791/54894 (2016).

--------------------------------------------------------------------------------------
SUMMARY: This repository contains files for creating a continuous culturing apparatus
for the purpose of studing the effects of light exposure on cultured microbes
by microscopy. At a high level: The user will use "Micro-Manager" to control a
microscope, FIJI to analyze the images, an arduino microcontroller to control
peristaltic pumps and a heating pad, and the "bioreactorController" plugin of MicroManager
 to coordinate these activities.
---------------------------------------------------------------------------------------

-The "extensive_lab_manual....docx" document in this repository is intended to describe
all of the steps needed to build this, and useful figures along the way. It is a verbose
version of the paper:
Stewart, C., McClean, M. N. Design and Implementation of an Automated 
Illuminating, Culturing, and Sampling System for Microbial Optogenetic 
Applications. J. Vis. Exp. (), e54894, doi:10.3791/54894 (2016).
Improvements and troubleshooting tips, especially with regard to
the software, will be listed here.

-The ".fzz" file contains the design for a printed circuit board that can be stacked 
on an Arduino microcontroller and be used to regulate power to external devices, read
inputs from an external thermometer, and control an LED matrix. To build the apparatus
described in the manual, the reader will need to order a circuit board with this
design. It can be opened with the Fritzing software which is free and open-source.
An additional use for this, besides ordering the board, is that the user can inspect
the connections in the PCB and/or modify it for another purpose.

-The "bioreactorControllerMMPlugin_v01.jar" file is the heart of this kit. It should be
copied into the "mmplugins" folder of micromanager. This controls the microscope,
performs the image analysis (or calls an optional Beanshell script to perform the
image analysis), sends parameters to the microcontroller which regulates power to
the peristaltic pumps and heating pad of the continuous culturing vessel, and it can call
external scripts for supplementary analysis or feedback control.

-The "microcontroller_code" folder contains code that should be uploaded to the
microcontroller and its own "readme"

-The "McCleanLab_BioreactorController" folder contrains the folder that was in my eclipse
workspace when I generated the jar. It contains all of the source code to edit the plugin.
You should be able to import it into eclipse and edit it there. Note that when this folder is
important into eclipse, the file paths that were used on my computer won't work, and the 
import statements will fail. Fix this by deleting the imports, and then configuring the 
build path to import all of the jars in Fiji/plugins, Fiji/jars, and Fiji/mmplugins. Except don't
import the "Bioreactor_Controller.jar" into itself. For more troubleshooting advice, see the
"InstructionsToEditPluginYourself.docx" file.

-The "supplementary_scripts" folder contains Beanshell (a java-like language) scripts
that can be used in place of the plugin. The downside of using these scripts is that they
may not be synchronized with the plugin code. The benefit of using them is that they can
be easily customized in the beanshell script editor.
