The Intensity files are used to control the intensity of light emitted by an 8x8 LED matrix, driven by a MAX7219 driver, and controlled
by an Arduino. It controls the number of lights ON in the range of 0 to 64, and it controls their individual intensities via pulse-width-
modulation (PWM). For the lights in the range of 1-64, as much as possible, lights are turned on symmetrically from the center outward. 
