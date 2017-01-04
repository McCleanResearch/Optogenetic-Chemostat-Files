#include <Intensity.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <PID_v1.h>
//USE THIS ONLY FOR TROUBLESHOOTING
//To check the temperature go to tools> Serial monitor and senbd "immeC,1,0.08" where the first number is the media pump ratio, the second is the sample pump ratio

//**************************DEFINE PINS***************************************************************************
// LED ARRAY SETUP*************                                                                                  
//DIN, pin 1 on MAX7219, is connected to pin 2 on the arduino
# define DATA_IN 2

//LOAD(CS), pin 12 on MAX7219, is connected to pin 3 on arduino
# define LOAD 12//formerly 3

//CLK, pin 13 on MAX7219, is connected to pin 4 on arduino
# define CLOCK 13//formerly 4

//THERMOMETER SETUP***************
// Data wire of thermometer high temp DS18B20 is plugged into port 8 on the Arduino
#define ONE_WIRE_BUS 8

//PID CONTROLLED HEATING PAD
#define PIN_HEAT 3

# define MEDIA_PUMP 4  //Pin controlling media pump
# define SAMPLING_PUMP 5  //Pin controlling sampling pump

//Size of array to store timecourse message
# define ROWS 150
# define COLUMNS 2

# define MEDIA_PUMP_RATIO 0.5
# define SAMPLE_PUMP_RATIO 0.5

#define COMMAND_TERMINATOR "#"


//***********************DEFINE GLOBAL VARIABLES***************************************************************************
//LED ARRAY INITIAL VARIABLES- default off
int LEDs=0;//Number of LEDs to turn on
int pwm=0;//Brightness of LED array
Intensity blueArray(DATA_IN, LOAD, CLOCK);

//THERMOMETER INITIAL VARIABLES
// Setup a oneWire instance to communicate with any OneWire devices (not just Maxim/Dallas temperature ICs)
OneWire oneWire(ONE_WIRE_BUS);
// Pass our oneWire reference to Dallas Temperature. 
DallasTemperature sensors(&oneWire);

//PID CONTROLLED HEATING PAD INITIAL VARIABLE
//Define Variables we'll be connecting to
double Setpoint=30;//Default to 30C target. I intentionally did not make a way to change this because I wanted to keep it at 30C.
      //However, a temperature experiment could be done by feeding a set of temperatures to the timecourse array
double Output=64;//64
double Input=3.14;//A non-zero initial value is useful for troubleshooting because
                  //an improper connection often returns the value 0.00 (if not connected to anything)
//Specify initial tuning parameters
double Kp=.02, Ki=.05, Kd=.01;

PID myPID(&Input, &Output, &Setpoint, Kp, Ki, Kd, DIRECT);

//PUMP INTIAL VARIABLES
int mediapumpPWM=255; //starting ON is useful because they can be manually turned off
int samplingpumpPWM=255;
float mediaRatioOn=MEDIA_PUMP_RATIO;
float sampleRatioOn=SAMPLE_PUMP_RATIO;

//VARIABLES TO STORE TIMECOURSE MESSAGE
long updateTime[ROWS]; //This will store update times in seconds. Not in milliseconds
char updateVals[ROWS][COLUMNS]; //A char array is used to conserve memory
int timecourseRow=-1; //This will be used to track the row in the timecourse array
                      //-1 indicates that the timecourse array is not ready.
unsigned long startTime=0;
unsigned long thirtiesPast=0; //used to pulse media pump within a 30 seconds period

unsigned long counter=0;

//***********BEGINING OF METHODS AND FUNCTIONS**************************************************************************

void setup() {
  Serial.setTimeout(50);
  pinMode(PIN_HEAT,   OUTPUT);
  Serial.begin(9600);
  sensors.begin();
  sensors.requestTemperatures();
  Input=sensors.getTempCByIndex(0);
  myPID.SetMode(AUTOMATIC);
}

void loop() {
  counter++;
  char nextChar;
  Input=0;
  sensors.requestTemperatures();
  Input=sensors.getTempCByIndex(0);
  
    
  while (Serial.available() > 0) {
    //Read in "i,the ratio of time the media pump should be ON (pwm=255 vs pwm=0), PWM power to sampling pump pump,e"
    // 'i' initiates a message. 'e' ends the message
    nextChar=Serial.read();
    //"timeC" is the codeword indicating that a timecourse command will follow
    if (nextChar=='t'&&Serial.read()=='i'&&Serial.read()=='m'&&Serial.read()=='e'&&Serial.read()=='C'){//arbitrarily chosen charecter to indicate timecourse commands
      readTimecourseCommands(); //fills in timecourse array
      //"immeC" is the codeword indicating that an immediate command (for the pumps and lights and pinch valve) will follow 
    }else if(nextChar == 'i'&&Serial.read()=='m'&&Serial.read()=='m'&&Serial.read()=='e'&&Serial.read()=='C') { //arbitrarily chosen charecter to indicate immediate commands
      //This reduces the chance that  noise is read as signal
      readImmediateCommands();//updates mediaRatioOn, samplingpumpPWM
      sendTimeAndTemp();
    }
    //don't know what to do with this character in the Serial  buffer, must ignore it
  }
  
    while(timecourseRow>-1&&(millis()-startTime)>(updateTime[timecourseRow]*1000)){ // multiply timecourserow by 1000 to make into milliseconds
      // timecourse==-1 would indicate that the timecourse array should not be read. The second part
      //should should act as an IF statement under most conditions. But, if it ends up multiple rows behind, This lets it skip ahead to the proper row
      LEDs=(int)updateVals[timecourseRow][0];// The array holds chars (to be more compact) but other code expected an (int). To optimize this code,
      // the other code could be changed to expect a (char) 
      pwm=(int)updateVals[timecourseRow][1];
      //Setpoint=timecourse[timecourseRow][2];
      //If COLUMNS is set to 3, then timecourse could include a Setpoint (temperature) timecourse too
      //This was not done for now to conserve dynamic memory.
      timecourseRow++;
      if(timecourseRow>=ROWS)
        timecourseRow=-1; //indicate that there is nothing to do until the next message. It will stay on the last setting
  }
  myPID.Compute();
  analogWrite(PIN_HEAT, Output);

  blueArray.numLEDs(counter%65);//LEDs
  blueArray.PWM(counter%16);//pwm

  //The media pump is OFF for 25 seconds and ON for 5 seconds of every 30 seconds
  if((millis()-(thirtiesPast*30000))<((1-mediaRatioOn)*30000)){
    mediapumpPWM=0;
  }
  else if((millis()-(thirtiesPast*30000))<30000){
    mediapumpPWM=255;
  }
  else if((millis()-(thirtiesPast*30000))>=30000){
    thirtiesPast++;
    mediapumpPWM=0;
  }

    //The media pump is OFF for 25 seconds and ON for 5 seconds of every 30 seconds
  if((millis()-(thirtiesPast*30000))<((1-sampleRatioOn)*30000)){
    samplingpumpPWM=0;
  }
  else if((millis()-(thirtiesPast*30000))<30000){
    samplingpumpPWM=255;
  }
  else if((millis()-(thirtiesPast*30000))>=30000){
    thirtiesPast++;
    samplingpumpPWM=0;
  }
  analogWrite(MEDIA_PUMP,mediapumpPWM);
  analogWrite(SAMPLING_PUMP,samplingpumpPWM);
}

void readImmediateCommands(){
    mediaRatioOn=Serial.parseFloat();
    mediaRatioOn=MEDIA_PUMP_RATIO;//constrain(mediaRatioOn,0,1);
    sampleRatioOn=Serial.parseFloat();
    sampleRatioOn=SAMPLE_PUMP_RATIO;//constrain(sampleRatioOn,0,1);
}
void readTimecourseCommands(){
  int row=Serial.parseInt();
  if(row==0){//This signals the beginning of a new timecourse
    timecourseRow=0;
    startTime=millis();
    //signalProblem(1);
  }
  updateTime[row]=Serial.parseInt();//recieve time. ParseInt actually returns (long)
  //for below: conversion from long to char may cause compiler warnings because it is lossy. But this code implicitly requires that
  //the next two recieved values are less than 255, so we're fine.
  updateVals[row][0]=(char)Serial.parseInt();//receive number of LEDs. Convert to char (0-255 is a large enough range. Therefore, save as char)
  updateVals[row][1]=(char)Serial.parseInt();//recieve pwm.  Convert to char (0-255 is a large enough range. Therefore, save as char)
  Serial.print("[");
  Serial.print(updateTime[row]);
  Serial.print(", ");
  Serial.print((int)updateVals[row][0]);
  Serial.print(", ");
  Serial.print((int)updateVals[row][1]);
  Serial.print("]");
  Serial.print(COMMAND_TERMINATOR);
}
void sendTimeAndTemp(){
      Serial.print("StartTime (milliseconds) ->,");
      Serial.print(millis()-startTime);
      Serial.print(",Temperature ->,");
      Serial.print(Input);
      Serial.print(",Heating Pad PWM ->,");
      Serial.print(Output);
      Serial.print(",number of LEDs ON ->,");
      Serial.print(LEDs);
      Serial.print(",LED PWM ->,");
      Serial.print(pwm);
      Serial.print(',');
      Serial.print(COMMAND_TERMINATOR); //return the current time and temp in CSV format
}


