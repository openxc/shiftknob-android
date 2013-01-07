//Zachary Nelson
//znelson1@ford.com
//This program uses a 74hc595 shift register and a
//7-sement display to display the numbers 0 - 9. It
//is used to test future implementations of the 
//display control.

//Pin connected to ST_CP of 74HC595
int latchPin = 8;
//Pin connected to SH_CP of 74HC595
int clockPin = 12;
////Pin connected to DS of 74HC595
int dataPin = 11;

//segments are turned on when the cooresponding pin on the 
//shift register is low.
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

void setup() {
  //set pins to output so you can control the shift register
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
}

void loop() {
    sendDigit(digit0);
    sendDigit(digit1);
    sendDigit(digit2);
    sendDigit(digit3);
    sendDigit(digit4);
    sendDigit(digit5);
    sendDigit(digit6);
    sendDigit(digit7);
    sendDigit(digit8);
    sendDigit(digit9);
}

void sendDigit(int x){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(latchPin, LOW);
  // shift out the bits:
  shiftOut(dataPin, clockPin, LSBFIRST, x);  

  //take the latch pin high so the LEDs will light up:
  digitalWrite(latchPin, HIGH);
  // pause before next value:
  delay(1000);
}
