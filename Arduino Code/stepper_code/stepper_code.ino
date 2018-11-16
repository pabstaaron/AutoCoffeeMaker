#include <Wire.h>
#include <Servo.h> 
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "Arduino.h"
#include "utility/Adafruit_MS_PWMServoDriver.h"

// Pins
#define TAMPSWITCH A0
#define DISPOSE_SWITCH A5
#define DISPOSE_SERVO_PIN 13 // The control pin for the tamper
#define TAMPER_SERVO_PIN 12 // The control pin for the tamper
#define RELAY_SWITCH1 11
#define RELAY_SWITCH2 10
#define RELAY_SWITCH3 9
#define RELAY_SWITCH4 6
#define TEMP 

// Constants 
#define MIN_ACTUATOR_TRANISTION 6000 // The time the acuators need to make a transition
#define TAMP_EXTENSION 60


// UART Constants
const String TAMP_STRING = "TAMP\r\n";
const String BREW_STRING = "BREW\r\n";
const String DISPOSE_STRING = "DISPOSE\r\n";
const String DISPENSE_STRING = "DISPENSE\r\n";
const String CALIBRATE = "CALIBRATE\r\n";
const String HOME = "HOME\r\n";
const String FIRE_BOILER = "FIRE_BOILER\r\n";
const String EXTEND = "EXTEND\r\n";

// Flags and Global Non Constants
bool stopped = false;
bool brewing = false;
bool calibrating = true;
uint16_t endPosition = 0;
uint16_t TAMP_POSITION = 0;
uint16_t DISPOSE_CALIBRATE_POSITION = 600;
bool extended = false;

// Stepper Functions
void tampStep();
void disposeStep();
void brewStep();
void dispenseStep();
void calibrate();

// Actuator Functions
void setTamperTo(uint16_t percentage);
void setDisposerTo(uint16_t percentage);
void resetActuators();

// Relay Functions
// Note: duplicated boiler and pump relays to avoid potential current issues
void setRelay1(bool enable); // boiler relay
void setRelay2(bool enable); // boiler relay
void setRelay3(bool enable); // pump relay
void setRelay4(bool enable); // pump relay
void setBoiler(bool enable);
void setPump(bool enable);
void resetAllRelays();

String getSubstringValue(String data, char deliminator, int index);

// Main Functions
void startUp();
void parseSerialInput();

Adafruit_MotorShield AFMS = Adafruit_MotorShield();
Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *dispenserMotor_t = AFMS.getStepper(200, 2);

AccelStepper brewMotor(brewForward, brewBackward);
AccelStepper dispenserMotor(dispenserForward, dispenserBackward);

Servo tamping_actuator;
Servo disposing_actuator;
int linearValue = 1500;

void setup() {
  Serial.begin(115200);
  Serial.println("In setup");

  AFMS.begin();
  
  pinMode(TAMPSWITCH, INPUT);
  pinMode(DISPOSE_SWITCH, INPUT);
  
  pinMode(TAMPER_SERVO_PIN, OUTPUT);
  pinMode(DISPOSE_SERVO_PIN, OUTPUT);
  pinMode(RELAY_SWITCH1, OUTPUT);
  pinMode(RELAY_SWITCH2, OUTPUT);
  pinMode(RELAY_SWITCH3, OUTPUT);
  pinMode(RELAY_SWITCH4, OUTPUT);
  
  tamping_actuator.attach(TAMPER_SERVO_PIN, 1050, 2000);
  disposing_actuator.attach(DISPOSE_SERVO_PIN, 1050, 2000);
  
  brewMotor.setMaxSpeed(1000.0);
  brewMotor.setAcceleration(200.0);

  dispenserMotor.setMaxSpeed(1000.0);
  dispenserMotor.setAcceleration(200.0);
  
  Serial.println("Motor Initilized");
  startUp();
}

void loop() {
 /*
  * Reset the acceleration to default on every loop because if we abruptly stop the motor 
  * we have to set acceleration to 0 to do so.
  */
  brewMotor.setAcceleration(200.0);
  
  // Read Serial Input
  parseSerialInput();

  // Check brewing positioning
  if(brewMotor.distanceToGo() == 0) {
    if(brewing){
      Serial.println("Current Brewing position: ");
      Serial.println(brewMotor.currentPosition());
      brewing = false;
    }
    stopped = true;
   }

 // If we did not signal to stop the motor, continue telling the motor to run
 if(!stopped)
 {
   brewMotor.run();
 }
 else
   brewMotor.stop();
}

/*
 * Code ran on startup for a visual representation of if
 * things are connected properly
 */
void startUp() {
  // Flick Relays TAKE OUT WHEN ACTUALLY IMPLEMENTED
  setRelay1(true);
  setRelay2(true);
  setRelay3(true);
  setRelay4(true);
  delay(1000);
  resetAllRelays();
  
  // Move Actuators
  setTamperTo(5);
  delay(1000);
  setDisposerTo(5);
  delay(1000);
  resetActuators();
  delay(1000);
  
  // Move dispense motor
  Serial.print("Dispense Motor Startup:");
  dispenserMotor.moveTo(10);
  while(dispenserMotor.distanceToGo() != 0) {
     dispenserMotor.run();
  }
  dispenserMotor.moveTo(0);
  while(dispenserMotor.distanceToGo() != 0) {
      dispenserMotor.run();
  }
  Serial.println("COMPLETE");
  delay(1500);
    
  // Move brew motor
  Serial.print("Brew Motor Startup:");
  brewMotor.moveTo(10);
  while(brewMotor.distanceToGo() != 0 && digitalRead(TAMPSWITCH) != HIGH && digitalRead(DISPOSE_SWITCH) != HIGH) {
     brewMotor.run();
  }
  brewMotor.moveTo(0);
  while(brewMotor.distanceToGo() != 0 && digitalRead(TAMPSWITCH) != HIGH && digitalRead(DISPOSE_SWITCH) != HIGH) {
      brewMotor.run();
  }
  Serial.println("COMPLETE");
  delay(1500);
  Serial.println("STARTUP COMPLETED");
}

/**
 * Will substring `data` with `deliminator` and return the string at `index` if it exists
 * https://stackoverflow.com/questions/29671455/how-to-split-a-string-using-a-specific-delimiter-in-arduino
 */
String getSubstringValue(String data, char deliminator, int index) {
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length() - 1;

  for(int i = 0; i <= maxIndex && found <= index; i++) {
    if(data.charAt(i) == deliminator || i == maxIndex) {
      found++;
      strIndex[0] = strIndex[1]+1;
      strIndex[1] = (i == maxIndex) ? i+1: i;
    }
  }

  return found > index ? data.substring(strIndex[0], strIndex[1]): "";
}

/*
 * Parses the input via serial and delegates the response to another function
 */
void parseSerialInput() {
  String incoming = "";
  if(Serial.available() > 0) {
    incoming = Serial.readString();
  }
  
  if(incoming == DISPENSE_STRING) {
    Serial.println("Dispense Recieved");
    dispenseStep();
    digitalWrite(RELAY_SWITCH1, HIGH);
  }

  else if (incoming.indexOf("DEMO") != -1) {
    String value1 = getSubstringValue(incoming, ',', 1);
    String value2 = getSubstringValue(incoming, ',', 2);
    // Signals the demo
    Serial.print("Demo started with:");
    Serial.print(value1);
    Serial.print(" and ");
    Serial.print(value2);
    tampStep();
    dispenseStep();
    brewStep();
    delay(1000);
    disposeStep();
    tampStep();
    Serial.println("Demo completed");
  }
  
  else if(incoming == TAMP_STRING) {
    Serial.println("Tamp Recieved");
    setDisposerTo(0);
    
    tampStep();

    setTamperTo(TAMP_EXTENSION);
    stopped = true;
  }
  
  else if(incoming == BREW_STRING) {
    Serial.println("Brew recieved");
    resetActuators();
    
    brewStep();
    
    //digitalWrite(RELAY_SWITCH1, LOW);
    stopped = false;
  }
  
  else if(incoming == DISPOSE_STRING) {
    Serial.println("Dispose Recieved");
    setTamperTo(0);
    
    disposeStep();
    
    setDisposerTo(TAMP_EXTENSION);
    stopped = true;
  }
  
  else if(incoming == "STOP\r\n") {
    stopped = true;
  }
  
  else if(incoming == HOME){
    Serial.println("Resetting...");
    resetAllRelays();
    resetActuators();
    tampStep();  
  }
  
  else if(incoming == FIRE_BOILER){
    // test fire the boiler
    resetActuators(); // Open up the brew channel from any potential obstructions
    tampStep();
    resetAllRelays();

    delay(5000);

    setPump(true);
    setBoiler(true);

    delay(20000);

    setPump(false);
    setBoiler(false);
  }
  
  else if(incoming == EXTEND){
    if(!extended){
      setTamperTo(TAMP_EXTENSION);
      setDisposerTo(TAMP_EXTENSION);
      extended = true;
    }
    else{
      resetActuators();
      extended = false;
    }
  }
  
  else if (incoming == "CALIBRATE\r\n" || calibrating) { 
    Serial.println("Calibrate recieved");
    resetActuators();
    calibrate();
    stopped = true;
  }
  
  else {
    Serial.flush();
  }
}
