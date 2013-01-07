//Pin connected to ST_CP of 74HC595
int latchPin = 8;
//Pin connected to SH_CP of 74HC595
int clockPin = 12;
////Pin connected to DS of 74HC595
int dataPin = 11;

int digit0 = 0b0;
int digit1 = 0b00010001;
int digit2 = 0b01101011;
int digit3 = 0b00111011;
int digit4 = 0b00011101;
int digit5 = 0b00111110;
int digit6 = 0b01111100;
int digit7 = 0b00010011;
int digit8 = 0b01111111;
int digit9 = 0b00011111;
// if you want to add in the decimal point simply add 1 to each number

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
}

void sendDigit(int x){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(latchPin, LOW);
  // shift out the bits:
  shiftOut(dataPin, clockPin, MSBFIRST, x);  

  //take the latch pin high so the LEDs will light up:
  digitalWrite(latchPin, HIGH);
  // pause before next value:
  delay(1000);
}
