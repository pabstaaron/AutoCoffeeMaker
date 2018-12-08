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
#define TEMP A2
#define FLOW_PIN 5
#define STEAM_SERVO_PIN A4

// Constants 
#define MIN_ACTUATOR_TRANISTION 6000 // The time the acuators need to make a transition
#define TAMP_EXTENSION 60

#define STEPS_PER_GRAMS 0.025
#define ML_PER_MS_PERS 1666
#define PERS_UP_SPEED 10l
#define STEAM_TIME 30000
#define ML_PER_SEC 1000 // ml/ms

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
const String PERS_ON = "PERS\r\n";
const String PERS_OFF = "!PERS\r\n";
const String RUN_STEAM = "RUN_STEAM\r\n";

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
uint16_t brew_temp = 95;
uint32_t brew_mL = 30;
uint8_t steamAmt = 100;
uint8_t milkAmt = 150; // mL
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

void pumpOn();
void pumpOff();

void steam();

// Main Functions
void startUp();
void parseSerialInput();

Adafruit_MotorShield AFMS(0x60); // Brew motors
Adafruit_MotorShield AFMS2(0x61); // Froth motors

Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *dispenserMotor_t = AFMS.getStepper(200, 2);
Adafruit_StepperMotor *frothMotor_t = AFMS2.getStepper(200, 1);

Adafruit_DCMotor *pump = AFMS2.getMotor(3);

AccelStepper brewMotor(brewForward, brewBackward);
AccelStepper dispenserMotor(dispenserForward, dispenserBackward);
AccelStepper frothMotor(frothUp, frothDown);

Servo tamping_actuator;
Servo disposing_actuator;
Servo steam_actuator;
int linearValue = 1500;

ExponentialFilter<long> temp_filter(25, 1700); 

void setup() {
  steam_actuator.write(90);
  
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

  dispenserMotor.setMaxSpeed(75.0);
  dispenserMotor.setAcceleration(15.0);

  frothMotor.setMaxSpeed(500.0);
  frothMotor.setAcceleration(30.0);
  // frothMotor.moveTo(500);

  pump->setSpeed(200);
  
  Serial.println("Motor Initilized");

  attachInterrupt(FLOW_PIN, flowHandler, RISING);

  last_flow = millis();
  closeSteamer();
  //startUp();
  // calibrate();
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

//  if(frothMotor.distanceToGo() == 0){
//    frothMotor.moveTo(0);
//  }

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

void dispenseGrounds(int grams){
   dispenseStep(grams / STEPS_PER_GRAMS);
}

void pumpOn(){
  Serial.println("Pump on...");
  pump->run(FORWARD);  
}

void pumpOff(){
  Serial.println("Pump off...");
  pump->run(RELEASE);
}

// steamAmt
void steam(){
  frothMotor.runToNewPosition(0);
  uint32_t pos = 0;

  // Pump for an amount of time that equals an approximate number of mL
  Serial.println("Dispensing");
  //uint32_t startTime = millis();
  //pumpOn();
//  while(millis() - startTime < milkAmt * ML_PER_MS_PERS){
//    //uint32_t elapsed = millis()-startTime;
//    pos += PERS_UP_SPEED;
//    Serial.println(millis()-startTime);
//    delay(1000);
//  }
//  pumpOff();
  Serial.println(pos);
  //frothMotor.runToNewPosition(pos * (1.0/steamAmt)); // submerse the want by the desired percentage
  int gain = 1;
  int steps = 1 * milkAmt * (steamAmt / 100);
  Serial.println(steps);
  frothMotor.moveTo(steps); // Calibrate gain variable
  while(frothMotor.distanceToGo() != 0) {
       frothMotor.run();
  }
  
  Serial.println("Priming..");
  closeSteamer();
//  setPump(true);
//  delay(10000); // wait for the boiler to load
//  setPump(false);

  Serial.println("Heating");
  setBoiler(true);
  delay(60000); // Heat up
  setBoiler(false);
  
  Serial.println("Frothing");
  openSteamer();
  delay(STEAM_TIME);
  closeSteamer();
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
  analogReadResolution(12);
  temp_filter.Filter(analogRead(TEMP));
  uint16_t raw = temp_filter.Current();
  Serial.println(raw);
  float voltage = raw * (3.0 / 4097.0);
  Serial.println(voltage);
  float temp = (voltage - 1.25) / 0.005;
  //return map(temp, 0, 50, 0, 100);
  //temp_filter.Filter(temp);
  //return temp_filter.Current();
  return temp;
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
    // Serial.println("flow");
}

float getMl(){
  return 2.25 * flow_counter;
}

void brewDrive(){
  flow_counter = 0;
  // 30mL = 1 shot
//  while(temp < brew_temp){
//    Serial.println(temp);
//    setBoiler(true);
//    setPump(false);
//  }
  
  uint32_t start = millis();
  while((millis() - start) < brew_mL){ 
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
  setBoiler(true);
  while(readTemp() < brew_temp){
    Serial.println(readTemp());
    delay(100);  
  }

  setPump(true);

  uint32_t start = millis();
  while(millis()-start < brew_mL * ML_PER_SEC){
    float temp = readTemp();
    if(temp < brew_temp){
      setBoiler(true);   
    }
    else
      setBoiler(false);
    Serial.println(temp);
    delay(100);
  }
  Serial.println();
  Serial.println(brew_mL);
  Serial.println(brew_mL * ML_PER_SEC + 30);
  //delay(brew_mL * ML_PER_SEC);

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
    dispenseGrounds(30); // Dispense one shot's worth of grounds
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
    Serial.print(brew_temp);
    Serial.print(" and Water mL: ");
    Serial.print(brew_mL);
    brewStep();
    experimentalBrewDrive();
    // tampStep();
    // dispenseStep();
    // brewStep();
    // delay(1000);
    // disposeStep();
    // tampStep();
    
    Serial.println("Demo completed");
    serialShutoff = true;
  }

  else if (incoming.indexOf(FINAL_DEMO) != -1) {
    String waterTemp_string = getSubstringValue(incoming, ',', 1);
    brew_temp = waterTemp_string.toInt();
    
    String waterDisp_string = getSubstringValue(incoming, ',', 2);
    brew_mL = waterDisp_string.toInt();
    
    String coffeeDisp_string = getSubstringValue(incoming, ',', 3);
    
    String frothStr_string = getSubstringValue(incoming, ',', 4);
    steamAmt = frothStr_string.toInt();
    
    String milkDisp_string = getSubstringValue(incoming, ',', 5);
    milkAmt = milkDisp_string.toInt();
    
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
    // Home
    Serial.println("Setting Device to home state");
    resetAllRelays();
    resetActuators();
    Serial.println("Running tamp step");
    tampStep();  
    // Dispense
    Serial.println("Dispensing Grounds");
    dispenseGrounds(coffeeDisp_string.toInt());
    // Tamp
    Serial.println("Actuating tamper");
    setTamperTo(TAMP_EXTENSION);
    resetActuators();
    // Brew
    Serial.println("Running brew step");
    brewStep();
    experimentalBrewDrive();
    // Dispose
    Serial.println("Running Dispose step");
    disposeStep();
    resetActuators();
    // Steam
    Serial.println("Steaming");
    if(steamAmt != 0)
    {
      steam();
    }

    delay(5000);
    setPump(true);
    delay(60000);
    setPump(false);

    Serial.println("Setting Device to home state");
    resetAllRelays();
    resetActuators();
    
    Serial.println("Demo completed");
    serialShutoff = true;
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
    
    //setDisposerTo(TAMP_EXTENSION);
    stopped = true;
    serialShutoff = true;
  }

  else if(incoming == "DISPOSE_STEP\r\n"){
    Serial.println("Dispose step Recieved");
    disposeStep();  
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
  else if(incoming == PERS_ON){
    pumpOn();
  }
  else if(incoming == PERS_OFF){
    pumpOff();
  }
  else if(incoming == RUN_STEAM){
    Serial.println("Steaming...");
    steam();
  }
  else if (incoming == "CALIBRATE\r\n" || calibrating) { 
    Serial.println("Calibrate recieved");
    resetActuators();
    calibrate();
    stopped = true;
    serialShutoff = true;
  }
}
