/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
| 
| **To be used with Shift Knob PCBs**
|
| Test communication over USB with Android host 
| to read in and display current gear.
|
| Using a 74hc595 shift register and a
| 7-sement display to display the numbers 0 - 9. It
| is used to test future implementations of the 
| display control.
---------------------------------------------------
*/

// 0bEDC*BAFG
//7segment digits.
const byte all_digits[10] = {
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
int motor_on = 750; //number of milliseconds the motor vibrates
int motor_off = 100; //pause between pulses

int buttonPin = 2;

int redLED = 9; //pwm
int blueLED = 10; //pwm
int greenLED = 11; //pwm
int digitLED = 6;

String inputString = "";
boolean stringComplete = false;
boolean USB_connected = false;
volatile unsigned long time = 0;

int motorCount = 1;
int motorPulse = 1;
volatile int motorState = LOW;
boolean motorCommand = false;

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
  
  //handle motor control
  if (motorState == HIGH && (millis() - time) >= motor_on && motorCommand) {
    motorState = LOW;
    digitalWrite(motorPin, motorState);
    time = millis();
  }
  
  if (motorState == LOW && (millis() - time) >= motor_off 
                          && motorPulse > 0 && motorCommand) {
    motorState = HIGH;
    digitalWrite(motorPin, motorState);
    motorPulse -= 1;
    time = millis();
    
    if (motorPulse <= 0) {
      motorCommand = false;
    }
  }
  
  if ((millis()-time) >= motor_on && !motorCommand) {
    motorState = LOW;
    digitalWrite(motorPin, motorState);
    motorPulse = motorCount;
  }
  
  if (!USB_connected) {
    for (int c = 0; c < 6; c++) {
      sendDigit(circle[c]);
      delay(50);
    }
  }
  
  if (stringComplete) {
    int index = inputString.length()-2;
    
    if (inputString[inputString.length()-1] == '}') {
      Serial.print('1');
    }
    if (inputString[inputString.length()-1] == '>') {
      int gear_pos = (inputString[index] - '0');
      sendDigit(all_digits[gear_pos]);
    }
    
    if (inputString[inputString.length()-1] == ']') {
      motorState = HIGH;
      motorCommand = true;
      digitalWrite(motorPin, motorState);
      motorPulse -= 1;
      time = millis();
    }
    
    if (inputString[inputString.length()-1] == ')') {
      int LED_value = 0;
      int scale = 1;
      for (int i = inputString.length()-2; i > 0; i--) {
        LED_value += (inputString[i] - '0')*scale;
        scale *= 10;
      }
      
      if (LED_value >= 0 && LED_value <= 85) {
        analogWrite(redLED, -1*LED_value*255/85+255);
        analogWrite(greenLED, LED_value*255/85);
        analogWrite(blueLED, 0);
      }
      
      if (LED_value > 85 && LED_value <= 170) {
        analogWrite(greenLED, -1*(LED_value-85)*255/85+255);
        analogWrite(blueLED, (LED_value-85)*255/85);
        analogWrite(redLED, 0);
      }
      
      if (LED_value > 170 && LED_value <= 255) {
        analogWrite(blueLED, -1*(LED_value-170)*255/85+255);
        analogWrite(redLED, (LED_value-170)*255/85);
        analogWrite(greenLED, 0);
      } 
    }
    
    inputString = "";
    stringComplete = false;
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

void serialEvent() {
  USB_connected = true;
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    inputString += inChar;
    if (inChar == '>' || inChar == ']' || inChar == ')' || inChar == '}') {
      stringComplete = true;
    }
  }
}
