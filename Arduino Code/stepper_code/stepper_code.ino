#include <Wire.h>
#include <Servo.h> 
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "utility/Adafruit_MS_PWMServoDriver.h"

#define TAMPSWITCH A0
#define DISPOSE_SWITCH A5

#define MIN_ACTUATOR_TRANISTION 6000 // The time the acuators need to make a transition

#define TAMPER_SERVO_PIN 11 // The control pin for the tamper
#define DISPOSE_SERVO_PIN 12 // The control pin for the tamper

const String TAMP_STRING = "TAMP\r\n";
const String BREW_STRING = "BREW\r\n";
const int BREW_POSITION = 300;
const String DISPOSE_STRING = "DISPOSE\r\n";
const String CALIBRATE = "CALIBRATE\r\n";

uint16_t step_counter = 0;
bool stopped = true;
bool brewing = false;
bool calibrating = true;
uint16_t endPosition = 0;
uint16_t TAMP_POSITION = 0;
uint16_t DISPOSE_CALIBRATE_POSITION = 575;

void tampStep();
void disposeStep();
void brewStep();
void dispenseStep();
void calibrate();

void tamperInit();
void runTamper();
void startUp();
void parseSerialInput();

Adafruit_MotorShield AFMS = Adafruit_MotorShield();
Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *dispenserMotor_t = AFMS.getStepper(200, 2);

void brewForward(){
  brewMotor_t->step(1, FORWARD, SINGLE);
}

void brewBackward(){
  brewMotor_t->step(1, BACKWARD, SINGLE);
}

void dispenserForward(){
  dispenserMotor_t->step(1, FORWARD, SINGLE);
}

void dispenserBackward(){
  dispenserMotor_t->step(1, BACKWARD, SINGLE);
}

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
  
  tamping_actuator.attach(TAMPER_SERVO_PIN, 1050, 2000);
  disposing_actuator.attach(DISPOSE_SERVO_PIN, 1050, 2000);
  pinMode(TAMPER_SERVO_PIN, OUTPUT);
  pinMode(DISPOSE_SERVO_PIN, OUTPUT);

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
  // Move Actuators
  tamping_actuator.writeMicroseconds(1150);
  disposing_actuator.writeMicroseconds(1150);
  delay(3000);
  tamping_actuator.writeMicroseconds(0);
  disposing_actuator.writeMicroseconds(0);
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

/*
 * Parses the input via serial and delegates the response to another function
 */
void parseSerialInput() {
  String incoming = "";
  if(Serial.available() > 0) {
    incoming = Serial.readString();
  }
  
  if(incoming == "DISPENSE\r\n") {
    Serial.println("Dispense Recieved");
    dispenseStep();
  }
  
  else if(incoming == TAMP_STRING) {
    Serial.println("Tamp Recieved");
    disposing_actuator.writeMicroseconds(0);
    delay(2000);
    
    tampStep();

    delay(2000);
    tamping_actuator.writeMicroseconds(2000);
    
    stopped = true;
  }
  
  else if(incoming == BREW_STRING) {
    Serial.println("Brew recieved");
    tamping_actuator.writeMicroseconds(0);
    disposing_actuator.writeMicroseconds(0);
    delay(2000);
    brewStep();
    stopped = false;
  }
  
  else if(incoming == DISPOSE_STRING) {
    Serial.println("Dispose Recieved");
    tamping_actuator.writeMicroseconds(0);
    delay(2000);
    
    disposeStep();
    
    delay(2000);
    disposing_actuator.writeMicroseconds(2000);
    stopped = true;
  }
  
  else if(incoming == "STOP\r\n") {
    stopped = true;
  }
  
  else if (incoming == "CALIBRATE\r\n" || calibrating) { 
    Serial.println("Calibrate recieved");
    calibrate();
    stopped = true;
  }
  else {
    Serial.flush();
  }
}

/*
 * Handles the tamp step:
 * 
 * Will run the normal tamp step, and then decrement 1 until TAMPSWITCH is depressed
 */
void tampStep() {
    brewMotor.moveTo(TAMP_POSITION);

    // Do a blocking step
    while(brewMotor.distanceToGo() != 0 && digitalRead(TAMPSWITCH) != HIGH) {
      brewMotor.run();
    }
    
    // Continually move by 1 step until target is reached
    int extraSteps = 0;
    while(digitalRead(TAMPSWITCH) != HIGH) {
      brewMotor.moveTo(brewMotor.currentPosition() - 1);
      brewMotor.run();
    }
    brewMotor.setAcceleration(0.0);
    brewMotor.stop();

   
    // Set 0 to be wherever the switch is depressed
    brewMotor.setCurrentPosition(0);
}

/*
 * Handles the brew step
 * As of right now we move to the direct middle of
 * the tamp and dispense position
 */
void brewStep() {    
    brewMotor.moveTo(endPosition / 2);
    while(brewMotor.distanceToGo() != 0) {
      brewMotor.run();
    }
    brewing = true;
}

/*
 * Handles the dispose step:
 * 
 * Will run the normal step and then increment 1 until <switch> is depressed
 */
void disposeStep() {    
    if(calibrating)brewMotor.moveTo(DISPOSE_CALIBRATE_POSITION);
    else brewMotor.moveTo(endPosition);

    // Do a blocking step
    int stepsMissed = 0;
    while(brewMotor.distanceToGo() != 0 && digitalRead(DISPOSE_SWITCH) != HIGH) {
      brewMotor.run();
    }
    
    // Continually move by 1 step until target is reached
    // Sets new endPosition if previous calibration wasn't right
    int extraSteps = 0;
    while(digitalRead(DISPOSE_SWITCH) != HIGH) {
      brewMotor.moveTo(brewMotor.currentPosition() + 1);
      extraSteps++;   
      brewMotor.run();
      endPosition = brewMotor.currentPosition();
    }

    brewMotor.setAcceleration(0.0);
    brewMotor.stop();
}

/*
 * Handles the dispense step.
 * As of right now, it will just spin the motor
 */
void dispenseStep() {
    dispenserMotor.moveTo(1000);
    while(dispenserMotor.distanceToGo() != 0) {
      dispenserMotor.run();
    }
}

/*
 * Calibrates the brewMotor and sets the ENV variables for step locations
 */
void calibrate() {
    calibrating = true;
    tampStep();
    Serial.println("Start position: ");
    Serial.println(brewMotor.currentPosition());
    delay(500);
    
    disposeStep();
    Serial.println("End position: ");
    Serial.println(brewMotor.currentPosition());
    delay(500);
    
    tampStep();
    Serial.println("Start position: ");
    Serial.println(brewMotor.currentPosition());
    calibrating = false;
}
