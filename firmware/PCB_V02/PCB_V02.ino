#include <aJSON.h>

/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
| 
| **To be used with Shift Knob PCBs**
|
| This program reads in a JSON stream and controls
| the following 3 components accordingly:
| 
| 1. 7 segment display
| 2. Haptic feedback motor
| 3. RGB LEDs
|
| Below is a list of the current support JSON messages:
|
|  {"name": "shift", "value": true}  value is a boolean
|  {"name": "color", "value": 100}   value is an integer 0 to 255
|  {"name": "digit", "value": 1}      value is 0 to 9
|
| A 74hc595 shift register and is used to control the
| 7-sement display. RGB LEDs are controlled with 3 PWM 
| pins and the Haptic feedback motor is controlled with
| another PWM pin. 
|
| There is also a PWM pin connection for controlling the
| brightness of the 7 segment display, but this functionality
| has not yet been implemented.
---------------------------------------------------
*/

// 0bEDC*BAFG
//7 segment digits.
const byte allDigits[10] = {
  0b11111110,0b11010111,0b00110010,0b10010010, //-,1,2,3
  0b11010100,0b10011000,0b00011100,0b11010011, //4,5,6,7
  0b00010000,0b10010000};                    //8,9

const byte circle[6] = {
  0b00010101,0b00011001,0b00110001,0b01010001,
  0b10010001,0b00010011};                    
  
int digit0 = 0b10001000;
int digit1 = 0b11101110;
int digit2 = 0b10010100;
int digit3 = 0b11000100;
int digit4 = 0b11100010;
int digit5 = 0b11000001;
int digit6 = 0b10000011;
int digit7 = 0b11101100;
int digit8 = 0b10000000;
int digit9 = 0b11100000;
int digitN = 0b11110111; // "-" used for nuetral
// if you want to add in the decimal point simply subtract 0b10000000 from each number

//Pin connected to ST_CP of 74HC595
int latchPin = 7;
//Pin connected to SH_CP of 74HC595
int clockPin = 8;
////Pin connected to DS of 74HC595
int dataPin = 4;

int motorPin = 5;
int motorOn = 750; //number of milliseconds the motor vibrates
int motorOff = 100; //pause between pulses

int buttonPin = 2;

int redLED = 9; //pwm
int blueLED = 10; //pwm
int greenLED = 11; //pwm
int digitLED = 6; //pwm. 7 segment brightness

boolean usbConnected = false;
volatile unsigned long time = 0;

int motorCount = 1;
int motorPulse = 1;
volatile int motorState = LOW;
boolean motorCommand = false;

aJsonStream serialStream(&Serial);

void setup() {
  //set pins to output so you can control the shift register
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  pinMode(motorPin, OUTPUT);
  
  pinMode(redLED, OUTPUT);
  pinMode(blueLED, OUTPUT);
  pinMode(greenLED, OUTPUT);
  pinMode(digitLED, OUTPUT);
  
  digitalWrite(digitLED, LOW);
  
  analogWrite(blueLED, 100);
  analogWrite(redLED, 100);
  analogWrite(greenLED, 100);
  
  Serial.begin(115200);
}

void loop() {
  
  // Is the motor on? If so, has it been on for more time than motorOn?
  // Is the motorCommand still ON or has it been turned off by other logic?
  // If all true, then turn off the motor and record the time.
  if (motorState == HIGH && (millis() - time) >= motorOn && motorCommand) {
    motorState = LOW;
    digitalWrite(motorPin, motorState);
    time = millis();
  }
  
  // Is the motor off? Is so, has it been off for more time than motorOff?
  // Is there a pulse count remaining in motorPulse?
  // Is the motorCommand still ON or has it been turned off by other logic?
  // If all true, then turn on the motor, decrease the pulse count by 1 
  // and record the time. If the pulse count goes to 0 then turn off the 
  // motorCommand. The last haptic pulse has been sent.
  if (motorState == LOW && (millis() - time) >= motorOff 
                          && motorPulse > 0 && motorCommand) {
    motorState = HIGH;
    digitalWrite(motorPin, motorState);
    motorPulse -= 1;
    time = millis();
    
    if (motorPulse <= 0) {
      motorCommand = false;
    }
  }
  
  // If the motor has been on for time = motorOn and the motorCommand
  // has been switched to false, then turn off the motor, stop the haptic
  // feedback and reset the motorPulse to the original motorCount.
  if ((millis()-time) >= motorOn && !motorCommand) {
    motorState = LOW;
    digitalWrite(motorPin, motorState);
    motorPulse = motorCount;
  }
  
  if (serialStream.available()) {
    serialStream.skip();
  }
  
  if (serialStream.available()) {
    usbConnected = true;
    aJsonObject *msg = aJson.parse(&serialStream);
    processMessage(msg);
    aJson.deleteItem(msg);
  }
  
  // Until USB has been connected, display a spinning wheel on
  // the 7 segment display.
  if (!usbConnected) {
    for (int c = 0; c < 6; c++) {
      sendDigit(circle[c]);
      delay(50);
    }
  }
}

/*
 Three cases so far:
 {"name": "shift", "value": 1}     value is an integer. either 1 or 0.
 {"name": "color", "value": 100}   value is an integer 0 to 255
 {"name": "digit", "value": 5}      value is 0 to 9
*/
void processMessage(aJsonObject *msg) {
  aJsonObject *name = aJson.getObjectItem(msg, "name");
  aJsonObject *value = aJson.getObjectItem(msg, "value");
  
  if (!name) {
    Serial.println("no useable data");
    return;
  }
  
  String sName = name->valuestring;
  int iValue = value->valueint;
  
  if (sName == "digit") {
    sendDigit(allDigits[iValue]);
    return;
  }
  
  if (sName == "shift" && iValue) {
    motorState = HIGH;
    motorCommand = true;
    digitalWrite(motorPin, motorState);
    motorPulse -= 1;
    time = millis();
    return;
  }
  
  if (sName == "color") {
    int valueLED = iValue;
      
    if (valueLED >= 0 && valueLED <= 85) {
      analogWrite(redLED, -1*valueLED*255/85+255);
      analogWrite(greenLED, valueLED*255/85);
      analogWrite(blueLED, 0);
    }
    
    if (valueLED > 85 && valueLED <= 170) {
      analogWrite(greenLED, -1*(valueLED-85)*255/85+255);
      analogWrite(blueLED, (valueLED-85)*255/85);
      analogWrite(redLED, 0);
    }
    
    if (valueLED > 170 && valueLED <= 255) {
      analogWrite(blueLED, -1*(valueLED-170)*255/85+255);
      analogWrite(redLED, (valueLED-170)*255/85);
      analogWrite(greenLED, 0);
    }
    return;
  }  
}

void sendDigit(int i){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(latchPin, LOW);
  // shift out the bits:
  shiftOut(dataPin, clockPin, LSBFIRST, i);  
  //take the latch pin high so the LEDs will light up:
  digitalWrite(latchPin, HIGH);
}
