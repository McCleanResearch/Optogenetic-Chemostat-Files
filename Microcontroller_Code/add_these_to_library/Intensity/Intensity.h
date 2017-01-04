/*
  Intensity.h - Library for turning on 0-64 LEDs on 8x8 array.
  Created by Cameron J. Stewart, November 25, 2015.
  Released into the public domain.
*/
#ifndef Intensity_h
#define Intensity_h

#include "Arduino.h"

class Intensity
{
  public:
    Intensity(int dataIn,int load, int clock);
    void numLEDs(int num);
    void PWM(byte pwm);
  private:
  	void maxSingle( byte _reg, byte _col);
  	void putByte(byte data);
    int _dataIn;
    int _load;
    int _clock;
};

#endif
