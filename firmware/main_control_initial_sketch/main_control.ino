/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
| This is the main program for the shift knob
| firmware. Attached components and functionality include:
| 1) vibration motor control
| 2) user input button
| 3) 7 segment display w/ shift register
| 4) 3 RGB LEDs 
| 5) brightness control for 7 segment
| 7) serial communication with Android host device
---------------------------------------------------
*/ 

// pin definitions
int motorPin = 5; //pwm
int buttonPin = 12;
int 7segBrightness = 6; //pwm
int redLED = 9; //pwm
int blueLED = 10; //pwm
int greenLED = 11; //pwm

// shift register pins
int shiftDataPin = 4;  //SERIAL
int shiftClockPin = 8; //SCK
int shiftLatchPin = 7; //RCK

//7 segment digits. individual segments are turned on when 
//the cooresponding pin on the shift register is low.
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
// if you want to add in the decimal point simply subtract 0b10000000 from each number

String inputString = "";
boolean stringComplete = false;
int i =0;

void setup(){
  pinMode(motorPin, OUTPUT);
  pinMode(7segBrightness, OUTPUT);
  pinMode(redLED, OUTPUT);
  pinMode(blueLED, OUTPUT);
  pinMode(greenLED, OUTPUT);
  pinMode(shiftDataPin, OUTPUT);
  pinMode(shiftClockPin, OUTPUT);
  pinMode(shiftLatchPin, OUTPUT);
  pinMode(buttonPin, INPUT);
  
  digitalWrite(buttonPin, HIGH); //turn on internal resistor
  digitalWrite(motorPin, LOW);
  digitalWrite(7segBrightness, HIGH);
  digitalWrite(redLED, LOW);
  digitalWrite(blueLED, LOW);
  digitalWrite(greenLED, LOW);
  
  Serial.begin(115200);
}

void onClick() {
  delay(50); // button debounce. could be adjusted
  // let Android app know that a button has been clicked

}

void serialEvent() {
  //properly decode input serial message
 while (Serial.available()){
   char inChar = (char)Serial.read();
   inputString += inChar;
   //.......
   
}

void motorControl(boolean t) {
  // could add fancy motor control profiles here
  if (t) {
    analogWrite(motorPin, 255);
  }
  else {
    analogWrite(motorPin, 0);
  }
}

void update7seg(int digit) {
  digitalWrite(shiftLatchPin, LOW);
  shiftOut(shiftDataPin, shiftClockPin, LSBFIRST, digit);
  digitalWrite(shiftLatchPin, HIGH);
}

void loop(){
 //main loop function
 if (stringComplete) {
   // do something...
 }
 
 inputString = "";
 stringComplete = false;
}
  
