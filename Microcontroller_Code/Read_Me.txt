Add the subfolders in the "add_these_to_library" folder to the "libraries" subfolder
of the "Arduino" folder. On windows, the default location for this is "Program Files (x86)"

When using the bioreactor plugin, ensure that the "Microcontroller_Code_Use_With_Bioreactor_Control_Plugin.ino"
software is the most recently uploaded software to the Arduino. The other .ino file is only useful for 
troubleshooting. It turns the pumps on and off, and it iterates through various LED matrix settings.

WARNING! If the hard-coded version is used with the plugin, the microcontroller will not follow the instructions
from the plugin, and it will be useless.