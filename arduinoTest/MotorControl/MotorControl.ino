int motorPin = 5;

void setup() {
  pinMode(motorPin, OUTPUT);
}

void loop() {
  // put your main code here, to run repeatedly: 
  analogWrite(motorPin, 255);
  delay(1000);
  analogWrite(motorPin, 0);
  delay(1000);
  analogWrite(motorPin, 128);
  delay(1000);
  analogWrite(motorPin, 0);
  delay(1000);
}
