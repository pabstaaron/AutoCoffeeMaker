#include <Wire.h>
#include <Servo.h> 
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "Arduino.h"
#include "utility/Adafruit_MS_PWMServoDriver.h"
#include "Filter.h"

// Pins
#define TAMPSWITCH A0
#define DISPOSE_SWITCH A5
#define DISPOSE_SERVO_PIN A3 // The control pin for the tamper
#define TAMPER_SERVO_PIN 12 // The control pin for the tamper
#define RELAY_SWITCH1 11
#define RELAY_SWITCH2 10
#define RELAY_SWITCH3 9
#define RELAY_SWITCH4 6
#define TEMP A1
#define FLOW_PIN 5
#define STEAM_SERVO_PIN A4

// Constants 
#define MIN_ACTUATOR_TRANISTION 6000 // The time the acuators need to make a transition
#define TAMP_EXTENSION 60

#define STEPS_PER_OZ 10


// UART Constants
const String TAMP_STRING = "TAMP\r\n";
const String BREW_STRING = "BREW\r\n";
const String DISPOSE_STRING = "DISPOSE\r\n";
const String DISPENSE_STRING = "DISPENSE\r\n";
const String CALIBRATE = "CALIBRATE\r\n";
const String HOME = "HOME\r\n";
const String FIRE_BOILER = "FIRE_BOILER\r\n";
const String EXTEND = "EXTEND\r\n";
const String TEMP_READ = "TEMP\r\n";
const String BOILER_ON = "ON\r\n";
const String BOILER_OFF = "OFF\r\n";
const String PUMP_ON = "PUMP\r\n";
const String PUMP_OFF = "!PUMP\r\n";
const String STEAM_ON = "STEAM\r\n";
const String STEAM_OFF = "!STEAM\r\n";
const String FINAL_DEMO = "FINAL";

// Flags and Global Non Constants
bool stopped = false;
bool brewing = false;
bool calibrating = true;
bool serialShutoff = false;
uint16_t steamerPosition = 0;
uint16_t endPosition = 0;
uint16_t TAMP_POSITION = 0;
uint16_t DISPOSE_CALIBRATE_POSITION = 730;
uint16_t flow_counter = 0;
uint32_t last_flow = 0;
uint8_t brew_temp = 95;
uint8_t brew_mL = 30;
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
float readTemp();
void flowHandler();
void experimentalBrewDrive();

String getSubstringValue(String data, char deliminator, int index);
void clearInputBuffer();

// Main Functions
void startUp();
void parseSerialInput();

Adafruit_MotorShield AFMS(0x60); // Brew motors
Adafruit_MotorShield AFMS2(0x61); // Froth motors

Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *dispenserMotor_t = AFMS.getStepper(200, 2);
Adafruit_StepperMotor *frothMotor_t = AFMS2.getStepper(200, 1);

AccelStepper brewMotor(brewForward, brewBackward);
AccelStepper dispenserMotor(dispenserForward, dispenserBackward);
AccelStepper frothMotor(frothUp, frothDown);

Servo tamping_actuator;
Servo disposing_actuator;
Servo steam_actuator;
int linearValue = 1500;

ExponentialFilter<long> temp_filter(50, 25); 

void setup() {
  Serial.begin(115200);
  Serial.println("In setup");

  AFMS.begin();
  AFMS2.begin();
  
  pinMode(TAMPSWITCH, INPUT);
  pinMode(DISPOSE_SWITCH, INPUT);
  
  pinMode(TAMPER_SERVO_PIN, OUTPUT);
  pinMode(DISPOSE_SERVO_PIN, OUTPUT);
  pinMode(STEAM_SERVO_PIN, OUTPUT);
  
  pinMode(RELAY_SWITCH1, OUTPUT);
  pinMode(RELAY_SWITCH2, OUTPUT);
  pinMode(RELAY_SWITCH3, OUTPUT);
  pinMode(RELAY_SWITCH4, OUTPUT);

  digitalWrite(RELAY_SWITCH1, HIGH);
  digitalWrite(RELAY_SWITCH2, HIGH);
  digitalWrite(RELAY_SWITCH3, HIGH);
  digitalWrite(RELAY_SWITCH4, HIGH);
  digitalWrite(DISPOSE_SERVO_PIN, LOW);
  
  tamping_actuator.attach(TAMPER_SERVO_PIN, 1050, 2000);
  disposing_actuator.attach(DISPOSE_SERVO_PIN, 1050, 2000);
  steam_actuator.attach(STEAM_SERVO_PIN);

  resetActuators();
  
  brewMotor.setMaxSpeed(1000.0);
  brewMotor.setAcceleration(200.0);

  dispenserMotor.setMaxSpeed(500.0);
  dispenserMotor.setAcceleration(100.0);

  frothMotor.setMaxSpeed(1000.0);
  frothMotor.setAcceleration(200.0);
  frothMotor.moveTo(500);
  
  Serial.println("Motor Initilized");

  attachInterrupt(FLOW_PIN, flowHandler, RISING);

  last_flow = millis();
  //startUp();
}

void loop() {
 /*
  * Reset the acceleration to default on every loop because if we abruptly stop the motor 
  * we have to set acceleration to 0 to do so.
  */
  brewMotor.setAcceleration(200.0);
  
  // Read Serial Input
  if(!serialShutoff) {
    parseSerialInput();
  }
  else {
    clearInputBuffer();
    serialShutoff = false;
  }

  if(frothMotor.distanceToGo() != 0)
    frothMotor.run();

  if(frothMotor.distanceToGo() == 0){
    frothMotor.moveTo(0);
  }

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
 {
   brewMotor.stop();
 }

  

}

/* 
 *  We only want serial data when the machine is idle
 *  so when the device get's an active command we need to
 *  not accept (ignore) any serial input while it is not idle
 */
void clearInputBuffer() {
  // Clear the buffer and reset
  while(Serial.available()) { Serial.read(); }
}

/*
 * Code ran on startup for a visual representation of if
 * things are connected properly
 */
void startUp() {
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

void dispenseGrounds(int oz){
   dispenseStep(oz * STEPS_PER_OZ);
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

float readTemp(){
  analogReadResolution(10);
  uint16_t raw = analogRead(TEMP);
  //Serial.println(raw);
  float voltage = raw * (3.0 / 1023.0);
  //Serial.println(voltage);
  float temp = (voltage - 1.25) / 0.005;
  //return map(temp, 0, 50, 0, 100);
  temp_filter.Filter(temp);
  return temp_filter.Current();
}

// Called whenever a pulse from the flow sensor is received
void flowHandler(){
//  uint32_t curr = millis();
//
//  if(curr - last_flow > 100){
//    flow_counter++;
//    Serial.println("flow");
//  }
//
//  last_flow = curr;

    flow_counter++;
    Serial.println("flow");
}

float getMl(){
  return 2.25 * flow_counter;
}

void brewDrive(){
  flow_counter = 0;
  // 30mL = 1 shot
  while(getMl() < brew_mL){ 
      float temp = readTemp();
      Serial.println(temp);
      if(temp < brew_temp){
        setBoiler(true);
        setPump(false);
      }
      else{
        setBoiler(false);
        setPump(true);
      }
      delay(1000);
  }

  setBoiler(false);
  setPump(false);
  Serial.println("Brew Complete!");
}

void experimentalBrewDrive(){
  flow_counter = 0;

  // Heat up the boiler
  while(readTemp() < brew_temp + 10){
    Serial.println(readTemp());
    delay(100);  
  }

  setPump(true);
  
  while(getMl() < brew_mL){
    float temp = readTemp();
    if(temp < brew_temp){
      setBoiler(true);   
    }
    else
      setBoiler(false);
    Serial.println(temp);
    delay(100);
  }

  setBoiler(false);
  setPump(false);
  Serial.println("Brew Complete...");
}

/*
 * Parses the input via serial and delegates the reGIsponse to another function
 */
void parseSerialInput() {
  String incoming = "";
  if(Serial.available() > 0) {
    incoming = Serial.readString();
  }
  
  if(incoming == DISPENSE_STRING) {
    Serial.println("Dispense Recieved");
    dispenseGrounds(250); // Dispense one shot's worth of grounds
    serialShutoff = true;
  }

  else if (incoming.indexOf("DEMO") != -1) {
    String value1 = getSubstringValue(incoming, ',', 1);
    brew_temp = value1.toInt();
    String value2 = getSubstringValue(incoming, ',', 2);
    // brew_mL = value2.toInt();
    brew_mL = value2.toInt();
    // Signals the demo
    Serial.print("Demo started Water Temp:");
    Serial.print(value1);
    Serial.print(" and Water mL: ");
    Serial.print(value2);
    brewStep();
    experimentalBrewDrive();
    //tampStep();
    //dispenseStep();
    //brewStep();
    //delay(1000);
    //disposeStep();
    //tampStep();
    
    Serial.println("Demo completed");
    serialShutoff = true;
  }

  else if (incoming.indexOf(FINAL_DEMO) != -1) {
    String waterTemp_string = getSubstringValue(incoming, ',', 1);
    String waterDisp_string = getSubstringValue(incoming, ',', 2);
    String coffeeDisp_string = getSubstringValue(incoming, ',', 3);
    String frothStr_string = getSubstringValue(incoming, ',', 4);
    String milkDisp_string = getSubstringValue(incoming, ',', 5);
    Serial.print("Demo started: ");
    Serial.print("{ Water Temperature: ");
    Serial.print(waterTemp_string);
    Serial.print("}\t { Water Dispensed: ");
    Serial.print(waterDisp_string);
    Serial.print("}\t { Coffee Dispensed: ");
    Serial.print(coffeeDisp_string);
    Serial.print("}\t { Froth Strength: ");
    Serial.print(frothStr_string);
    Serial.print("} \t { Milk Dispensed: ");
    Serial.print(milkDisp_string);
    Serial.print("}\r\n");
  }

  else if(incoming == STEAM_ON) {
    Serial.println("Steam on");
    openSteamer();
  }
  else if (incoming == STEAM_OFF) {
    Serial.println("Steam off");
    closeSteamer();
  }
  
  else if(incoming == TAMP_STRING) {
    Serial.println("Tamp Recieved");
    setDisposerTo(0);
    
    tampStep();

    setTamperTo(TAMP_EXTENSION);
    stopped = true;
    serialShutoff = true;
  }
  
  else if(incoming == BREW_STRING) {
    Serial.println("Brew recieved");
    resetActuators();
    
    brewStep();
    
    //digitalWrite(RELAY_SWITCH1, LOW);
    stopped = false;
    serialShutoff = true;
  }
  
  else if(incoming == DISPOSE_STRING) {
    Serial.println("Dispose Recieved");
    setTamperTo(0);
    
    disposeStep();
    
    setDisposerTo(TAMP_EXTENSION);
    stopped = true;
    serialShutoff = true;
  }
  
  else if(incoming == "STOP\r\n") {
    stopped = true;
    serialShutoff = true;
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
    brewDrive();
    serialShutoff = true;
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
    serialShutoff = true;
  }
  else if(incoming == TEMP_READ){
    Serial.println(readTemp());  
  }
  else if(incoming == BOILER_ON){
    setBoiler(true);  
    Serial.println("boiler on");
  }
  else if(incoming == BOILER_OFF){
    setBoiler(false);  
    Serial.println("boiler off");
  }
  else if(incoming == PUMP_ON){
    setPump(true);  
    Serial.println("pump on");
  }
  else if(incoming == PUMP_OFF){
    setPump(false);  
    Serial.println("boiler off");
  }
  else if (incoming == "CALIBRATE\r\n" || calibrating) { 
    Serial.println("Calibrate recieved");
    resetActuators();
    calibrate();
    stopped = true;
    serialShutoff = true;
  }
}
