/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
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

//7segment digits.
const byte all_digits[10] = {
  0b11110111,0b11101110,0b10010100,0b11000100, //-,1,2,3
  0b11100010,0b11000001,0b10000011,0b11101100, //4,5,6,7
  0b10000000,0b11100000};                    //8,9

const byte circle[6] = {
  0b10101000,0b11001000,0b10001100,0b10001010,
  0b10001001,0b10011000};                    
  
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
long motor_time = 500; //number of milliseconds the motor vibrates

int redLED = 9; //pwm
int blueLED = 10; //pwm
int greenLED = 11; //pwm

String inputString = "";
boolean stringComplete = false;
boolean USB_connected = false;
unsigned long time = 0;

void setup() {
  //set pins to output so you can control the shift register
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  pinMode(motorPin, OUTPUT);
  
  pinMode(redLED, OUTPUT);
  pinMode(blueLED, OUTPUT);
  pinMode(greenLED, OUTPUT);
  
  Serial.begin(115200);
}

void loop() {
  
  if (millis() - time >= motor_time) analogWrite(motorPin, 0);
  
  if (!USB_connected) {
    for (int c = 0; c < 6; c++) {
      sendDigit(circle[c]);
      delay(50);
    }
  }
  
  if (stringComplete) {
    int index = inputString.length()-2;
    
    if (inputString[inputString.length()-1] == '>') {
      int gear_pos = (inputString[index] - '0');
      sendDigit(all_digits[gear_pos]);
    }
    
    if (inputString[inputString.length()-1] == ']') {
      analogWrite(motorPin, 200);
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
    if (inChar == '>' || inChar == ']' || inChar == ')') {
      stringComplete = true;
    }
  }
}
    
    
    
