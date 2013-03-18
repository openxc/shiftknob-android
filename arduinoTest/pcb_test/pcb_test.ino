/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
| 
| **To be used with Shift Knob PCBs**
|
| This program tests all the functional components
| of the shift knob pcbs. Once both pcbs are soldered
| and assembled, this firmware can be run with the 
| Arduino serial monitor to check all systems and 
| functions. Below is a list of the features tested:
|
| 1. RGB LEDs - switch between Red, Blue, and Green
| 2. Push-button interrupt - sends a message to the 
|    serial terminal
| 3. 7 segment display - plays a loop around the 
|    outside segments
| 4. Motor control - turns on the motor for 1 second
| 
| In addition to this, an external power source can
| be connected and run at 10+ V to check the 7805.
---------------------------------------------------
*/

int buttonPin = 2;
int motorPin = 5;

int redLED = 9; //pwm
int blueLED = 3; //pwm. used to be pin 10 until that pin died...
int greenLED = 11; //pwm

// shift register pins
int shiftDataPin = 4;  //SERIAL
int shiftClockPin = 8; //SCK
int shiftLatchPin = 7; //RCK

// 7segment digits
const byte all_digits[10] = {
  0b11111110,0b11010111,0b00110010,0b10010010, //-,1,2,3
  0b11010100,0b10011000,0b00011100,0b11010011, //4,5,6,7
  0b00010000,0b10010000};                    //8,9

const byte circle[6] = {
  0b00010101,0b00011001,0b00110001,0b01010001,
  0b10010001,0b00010011};                    

void setup() {
  Serial.begin(115200);
  pinMode(redLED,OUTPUT);
  pinMode(blueLED,OUTPUT); //used to be 10 until that pin fried...
  pinMode(greenLED,OUTPUT);
  pinMode(motorPin,OUTPUT);
    
  pinMode(shiftDataPin,OUTPUT);
  pinMode(shiftLatchPin,OUTPUT);
  pinMode(shiftClockPin,OUTPUT);

  pinMode(buttonPin,OUTPUT);
  digitalWrite(buttonPin,HIGH);
  attachInterrupt(0, buttonPress, FALLING);
  pinMode(10,INPUT); // killed pin 10
  digitalWrite(10,HIGH); // killed pin 10
}

void loop() {
  // print the string when a newline arrives:
  analogWrite(redLED,10);
  analogWrite(blueLED,0);
  analogWrite(greenLED,0); 
  delay(1000);

  for (int c = 0; c < 6; c++) {
    sendDigit(circle[c]);
    delay(50);
  }

  analogWrite(redLED,0);
  analogWrite(blueLED,10);
  analogWrite(greenLED,0);
  delay(1000);

  digitalWrite(motorPin,HIGH);
  analogWrite(redLED,0);
  analogWrite(blueLED,0);
  analogWrite(greenLED,10);
  delay(1000);
  digitalWrite(motorPin,LOW);
}

void buttonPress() {
  Serial.println("Button Pressed!");
}

void sendDigit(int i){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(shiftLatchPin, LOW);
  // shift out the bits:
  shiftOut(shiftDataPin, shiftClockPin, LSBFIRST, i);  
  //take the latch pin high so the LEDs will light up:
  digitalWrite(shiftLatchPin, HIGH);
}



