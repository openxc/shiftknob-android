/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
| 
| This program uses a 74hc595 shift register and a
| 7-sement display to display the numbers 0 - 9. It
| is used to test future implementations of the 
| display control.
| 
| USB serial support has been added to test USB 
| connections with an Android host device.
---------------------------------------------------
*/



//Pin connected to ST_CP of 74HC595
int latchPin = 8;
//Pin connected to SH_CP of 74HC595
int clockPin = 12;
////Pin connected to DS of 74HC595
int dataPin = 11;

String inputString = "";
boolean stringComplete = false;
int i = 0;

void setup() {
  //set pins to output so you can control the shift register
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  
  Serial.begin(115200);
}

void loop() {
  if (stringComplete) {
    // do something...
    sendDigit(i);
    i+=1;
    inputString = "";
    stringComplete = false;
  }
}

void sendDigit(int x){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(latchPin, LOW);
  // shift out the bits:
  shiftOut(dataPin, clockPin, LSBFIRST, x);  

  //take the latch pin high so the LEDs will light up:
  digitalWrite(latchPin, HIGH);
}

void serialEvent() {
  
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    inputString += inChar;
    if (inChar == 'E') {
      stringComplete = true;
    }
  }
}
    
    
    
