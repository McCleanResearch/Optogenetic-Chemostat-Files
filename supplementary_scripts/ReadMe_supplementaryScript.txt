August 01, 2016
Cameron Stewart, cstewart7@wisc.edu
Research Technician at UW-Madison
Principal Investigator: Megan McClean, mmcclean@wisc.edu
Lab website: http://mccleanlab.bme.wisc.edu
--------------------------------------------------------------------------------------
SUMMARY: The "supplementary_scripts" folder contains Beanshell (a java-like language) 
scripts. The "experimentScipt.bsh" script can be run in Micro-Manager's Beanshell editor.
The downside of using this script is that it may not be synchronized with the plugin code. 
The benefit of using it is that they can be easily customized in the beanshell script editor.
---------------------------------------------------------------------------------------

-The "experimentScript.bsh" script can be used in place of the "bioreactorController.jar"
plugin. It was the prototype for the plugin.

-The "supplementaryScript1_introToBeanshell.bsh" script performs some simple arithmatic
and print calls which demonstrate how the language wroks.

-The "supplementaryScript2_troubleshooting.bsh" script performs many simple actions,
and is useful for troubleshooting. It enables a user to incrementally verify that Micro-
Manager can perform all of the necessary actions.

-The "bioreactorParameters.csv" file contains paramerters for running the experiment.
These include: the filepath where results should be saved to, the port number of
the connected microcontroller, the fraction of time for which a pump should be on...
It is not used by the plugin, but it is used by the "experimentScript.bsh" script. This
was designed to make the script more user friendly. However, now that the plugin exists, 
it will likely be simpler for a user that is comfortable programming to hard-code
these parameters into the script.
