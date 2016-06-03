/*
  Intensity.cpp - Library for turning on 0-64 LEDs on 8x8 array.
  Created by Cameron J. Stewart, November 25, 2015.
  Released into the public domain.
*/

#include "Arduino.h"
#include "Intensity.h"
  // define max7219 _registers
#define  MAX7219_REG_NOOP     0x00
#define  MAX7219_REG_DIGIT0   0x01
#define  MAX7219_REG_DIGIT1   0x02
#define  MAX7219_REG_DIGIT2   0x03
#define  MAX7219_REG_DIGIT3   0x04
#define  MAX7219_REG_DIGIT4   0x05
#define  MAX7219_REG_DIGIT5   0x06
#define  MAX7219_REG_DIGIT6   0x07
#define  MAX7219_REG_DIGIT7   0x08
#define  MAX7219_REG_DECODEMODE 0x09
#define  MAX7219_REG_INTENSITY 0x0a
#define  MAX7219_REG_SCANLIMIT 0x0b
#define  MAX7219_REG_SHUTDOWN 0x0c
#define  MAX7219_REG_DISPLAYTEST 0x0f

Intensity::Intensity(int dataIn, int load, int clock){
	_dataIn=dataIn;
	_load=load;
	_clock=clock;
  
  int maxInUse=1;        // code only made to handle 1 LED Array
  int e = 0;           // just a variable
  

    pinMode(_dataIn, OUTPUT);
    pinMode(_clock,  OUTPUT);
    pinMode(_load,   OUTPUT);
  

  //initiation of the max 7219
    maxSingle(MAX7219_REG_SCANLIMIT, 0x07);      
    maxSingle(MAX7219_REG_DECODEMODE, 0x00);  // using an led matrix (not digits)
    maxSingle(MAX7219_REG_SHUTDOWN, 0x01);    // not in shutdown mode
    maxSingle(MAX7219_REG_DISPLAYTEST, 0x00); // no display test
    numLEDs(0);
    maxSingle(MAX7219_REG_INTENSITY, 0x0f);    // the first 0x0f is the value you can set
                                                    // range: 0x00 to 0x0f

}
void Intensity::PWM(byte _pwm){
  maxSingle(MAX7219_REG_INTENSITY, _pwm);    // the first 0x0f is the value you can set
                                            // range: 0x00 to 0x0f
}
void Intensity::numLEDs(int i){
  if(i==0){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT4,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT5,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  } else if(i==1){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT4,16);      //  - - - - + - - -
      maxSingle(MAX7219_REG_DIGIT5,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  } else if(i==2){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,8);       //  - - - + - - - -
      maxSingle(MAX7219_REG_DIGIT4,16);      //  - - - - + - - -
      maxSingle(MAX7219_REG_DIGIT5,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==3){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,16);      //  - - - - + - - -
      maxSingle(MAX7219_REG_DIGIT5,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==4){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==5){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,32);      //  - - - - - + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==6){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,4);       //  - - + - - - - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,32);      //  - - - - - + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==7){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,36);      //  - - + - - + - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,32);      //  - - - - - + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==8){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,36);      //  - - + - - + - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,36);      //  - - + - - + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==9){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,44);      //  - - + + - + - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,36);      //  - - + - - + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==10){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,44);      //  - - + + - + - -
      maxSingle(MAX7219_REG_DIGIT3,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==11){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,44);      //  - - + + - + - -
      maxSingle(MAX7219_REG_DIGIT3,56);      //  - - - + + + - -
      maxSingle(MAX7219_REG_DIGIT4,24);      //  - - - + + - - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==12){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,44);      //  - - + + - + - -
      maxSingle(MAX7219_REG_DIGIT3,56);      //  - - - + + + - -
      maxSingle(MAX7219_REG_DIGIT4,28);      //  - - + + + - - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==13){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,44);      //  - - + + - + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,28);      //  - - + + + - - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==14){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,28);      //  - - + + + - - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==15){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,52);      //  - - + - + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==16){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==17){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,64);      //  - - - - - - + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==18){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,2);       //  - + - - - - - -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,64);      //  - - - - - - + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==19){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,66);      //  - + - - - - + -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,64);      //  - - - - - - + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==20){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,66);      //  - + - - - - + -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,66);      //  - + - - - - + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==21){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,70);      //  - + + - - - + -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,66);      //  - + - - - - + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==22){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,70);      //  - + + - - - + -
      maxSingle(MAX7219_REG_DIGIT2,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,98);      //  - + - - - + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==23){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,70);      //  - + + - - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,98);      //  - + - - - + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==24){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,70);      //  - + + - - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,98);      //  - + - - - + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==25){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,78);      //  - + + + - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,98);      //  - + - - - + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==26){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,78);      //  - + + + - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,98);      //  - + - - - + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==27){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,78);      //  - + + + - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,60);      //  - - + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,114);     //  - + - - + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==28){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,78);      //  - + + + - - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,114);     //  - + - - + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==29){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,94);      //  - + + + + - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,114);     //  - + - - + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==30){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,94);      //  - + + + + - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,114);     //  - + - - + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==31){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,94);      //  - + + + + - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,122);     //  - + - + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==32){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,94);      //  - + + + + - + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,122);     //  - + - + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==33){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,62);      //  - + + + + + - -
      maxSingle(MAX7219_REG_DIGIT6,122);     //  - + - + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==34){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,122);     //  - + - + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==35){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,124);     //  - - + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==36){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,0);       //  - - - - - - - -
  }else if(i==37){
      maxSingle(MAX7219_REG_DIGIT0,0);       //  - - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,128);     //  - - - - - - - +
  }else if(i==38){
      maxSingle(MAX7219_REG_DIGIT0,1);       //  + - - - - - - -
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,128);     //  - - - - - - - +
  }else if(i==39){
      maxSingle(MAX7219_REG_DIGIT0,129);     //  + - - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,128);     //  - - - - - - - +
  }else if(i==40){
      maxSingle(MAX7219_REG_DIGIT0,129);     //  + - - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,129);     //  + - - - - - - +
  }else if(i==41){
      maxSingle(MAX7219_REG_DIGIT0,131);     //  + + - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,129);     //  + - - - - - - +
  }else if(i==42){
      maxSingle(MAX7219_REG_DIGIT0,131);     //  + + - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,193);     //  + - - - - - + +
  }else if(i==43){
      maxSingle(MAX7219_REG_DIGIT0,131);     //  + + - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,193);     //  + - - - - - + +
  }else if(i==44){
      maxSingle(MAX7219_REG_DIGIT0,131);     //  + + - - - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,193);     //  + - - - - - + +
  }else if(i==45){
      maxSingle(MAX7219_REG_DIGIT0,135);     //  + + + - - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,193);     //  + - - - - - + +
  }else if(i==46){
      maxSingle(MAX7219_REG_DIGIT0,135);     //  + + + - - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,225);     //  + - - - - + + +
  }else if(i==47){
      maxSingle(MAX7219_REG_DIGIT0,135);     //  + + + - - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,225);     //  + - - - - + + +
  }else if(i==48){
      maxSingle(MAX7219_REG_DIGIT0,135);     //  + + + - - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,225);     //  + - - - - + + +
  }else if(i==49){
      maxSingle(MAX7219_REG_DIGIT0,143);     //  + + + + - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,225);     //  + - - - - + + +
  }else if(i==50){
      maxSingle(MAX7219_REG_DIGIT0,143);     //  + + + + - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,241);     //  + - - - + + + +
  }else if(i==51){
      maxSingle(MAX7219_REG_DIGIT0,143);     //  + + + + - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,126);     //  - + + + + + + -
      maxSingle(MAX7219_REG_DIGIT4,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,241);     //  + - - - + + + +
  }else if(i==52){
      maxSingle(MAX7219_REG_DIGIT0,143);     //  + + + + - - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,241);     //  + - - - + + + +
  }else if(i==53){
      maxSingle(MAX7219_REG_DIGIT0,159);     //  + + + + + - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,241);     //  + - - - + + + +
  }else if(i==54){
      maxSingle(MAX7219_REG_DIGIT0,159);     //  + + + + + - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,241);     //  + - - - + + + +
  }else if(i==55){
      maxSingle(MAX7219_REG_DIGIT0,159);     //  + + + + + - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,249);     //  + - - + + + + +
  }else if(i==56){
      maxSingle(MAX7219_REG_DIGIT0,159);     //  + + + + + - - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,249);     //  + - - + + + + +
  }else if(i==57){
      maxSingle(MAX7219_REG_DIGIT0,191);     //  + + + + + + - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,249);     //  + - - + + + + +
  }else if(i==58){
      maxSingle(MAX7219_REG_DIGIT0,191);     //  + + + + + + - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,253);     //  + - + + + + + +
  }else if(i==59){
      maxSingle(MAX7219_REG_DIGIT0,191);     //  + + + + + + - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,253);     //  + - + + + + + +
  }else if(i==60){
      maxSingle(MAX7219_REG_DIGIT0,191);     //  + + + + + + - +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,253);     //  + - + + + + + +
  }else if(i==61){
      maxSingle(MAX7219_REG_DIGIT0,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,253);     //  + - + + + + + +
  }else if(i==62){
      maxSingle(MAX7219_REG_DIGIT0,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT1,254);     //  - + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,255);     //  + + + + + + + +
  }else if(i==63){
      maxSingle(MAX7219_REG_DIGIT0,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT1,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT6,127);     //  + + + + + + + -
      maxSingle(MAX7219_REG_DIGIT7,255);     //  + + + + + + + +
  }else if(i==64){
      maxSingle(MAX7219_REG_DIGIT0,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT1,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT2,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT3,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT4,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT5,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT6,255);     //  + + + + + + + +
      maxSingle(MAX7219_REG_DIGIT7,255);     //  + + + + + + + +
  }
}
void Intensity::putByte(byte data){
  byte i = 8;
  byte mask;
  while(i > 0) {
    mask = 0x01 << (i - 1);      // get bitmask
    digitalWrite( _clock, LOW);   // tick
    if (data & mask){            // choose bit
    	digitalWrite(_dataIn, HIGH);// send 1
	}else{
      digitalWrite(_dataIn, LOW); // send 0
    }
    digitalWrite(_clock, HIGH);   // tock
    --i;                         // move to lesser bit
  }
}

void Intensity::maxSingle( byte _reg, byte _col) {    
//maxSingle is the "easy"  function to use for a single max7219
  digitalWrite(_load, LOW);       // begin     
  putByte(_reg);                  // specify _register
  putByte(_col);//((data & 0x01) * 256) + data >> 1); // put data   
  digitalWrite(_load, LOW);       // and _load da stuff
  digitalWrite(_load,HIGH); 
}
