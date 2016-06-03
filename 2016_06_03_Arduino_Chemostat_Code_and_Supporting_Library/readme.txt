Feb 17, 2016.
Cameron Stewart
Research Technician at UW-Madison
cstewart7@wisc.edu
PI: Megan McClean

The chemostatController.ino file contains the code that will be used to make the Arduino-controlled chemostat work.

Inside the library:
-The "Intensity" folder contains code to control the number of LEDs of an 8x8 Matrix that are ON as well as their individual
intensities by PWM.
-The "br3ttb-Arduino-PID-Library-fb095d8" was written by Bret (see his readME). I use this library for PID control of the heating.
PWM to the heating pad is the output, digital thermometer reading is the input.
-The "OneWire" code and the "dallas-temperature-control" were not written by Cameron Stewart (see their respective readMe.txt 
files). They are used to interprete the messages from a DS18B20 digital thermometer. A nice perk is that the output from
multiple thermometers can be multiplexed across a single wire.
  
For information on installing libraries, see: http://www.arduino.cc/en/Guide/Libraries
