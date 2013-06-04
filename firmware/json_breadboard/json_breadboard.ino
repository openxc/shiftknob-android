#include <aJSON.h>

/*
-------------------------------------------------
| Zachary Nelson
| znelson1@ford.com
|
| Test JSON communication protocol. Read incoming
| JSON message and display the correct value on 
| the 7 segment display.
---------------------------------------------------
*/

//Pin connected to ST_CP of 74HC595
int latchPin = 7;
//Pin connected to SH_CP of 74HC595
int clockPin = 8;
////Pin connected to DS of 74HC595
int dataPin = 4;

//segments are turned on when the cooresponding pin on the 
//shift register is low.
const byte all_digits[10] = {
  0b10001000,0b11101110,0b10010100,0b11000100,
  0b11100010,0b11000001,0b10000011,0b11101100,
  0b10000000,0b11100000};
// if you want to add in the decimal point simply subtract 0b10000000 from each number

aJsonStream serial_stream(&Serial);

void setup() {
  //set pins to output so you can control the shift register
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  if (serial_stream.available()) {
    serial_stream.skip();
  }
  
  if (serial_stream.available()) {
    aJsonObject *msg = aJson.parse(&serial_stream);
    processMessage(msg);
    aJson.deleteItem(msg);
  }
}

/*
 Three test cases so far:
 {"name": "shift", "value": true}  value is a boolean
 {"name": "color", "value": 100}   value is an integer 0 to 255
 {"name": "gear", "value": 1}      value is 0 to 9
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
  if (sName == "gear") {
    Serial.println(name->valuestring);
    Serial.println(value->valueint);
    sendDigit(value->valueint);
  }
}

void sendDigit(int x){
  // take the latchPin low so 
  // the LEDs don't change while you're sending in bits:
  digitalWrite(latchPin, LOW);
  // shift out the bits:
  shiftOut(dataPin, clockPin, LSBFIRST, all_digits[x]);  
  //take the latch pin high so the LEDs will light up:
  digitalWrite(latchPin, HIGH);
}

