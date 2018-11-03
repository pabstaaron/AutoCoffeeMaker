/*
 * Spins a motor on channel 1 on a loop
 */
#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "utility/Adafruit_MS_PWMServoDriver.h"

#define LEFTHOME 8 // TODO - Decide on these pins
#define RIGHTHOME 9

const String TAMP_STRING = "TAMP\r\n";
const int TAMP_POSITION = 0;
const String BREW_STRING = "BREW\r\n";
const int BREW_POSITION = 200;
const String DISPOSE_STRING = "DISPOSE\r\n";
const int DISPOSE_POSITION = 400;

const String RESET_STRING = "RESET\r\n";

bool stopped = true;

void brewHome();
void accelStep(Adafruit_DCMotor motor, int steps, int dir);
void brewMove(int steps, int dir);

Adafruit_MotorShield AFMS = Adafruit_MotorShield();
Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);

void brewForward(){
  brewMotor_t->step(1, FORWARD, SINGLE);
}

void brewBackward(){
  brewMotor_t->step(1, BACKWARD, SINGLE);
}

AccelStepper brewMotor(brewForward, brewBackward);

void setup() {
  Serial.begin(9600);
  Serial.println("In setup");

  AFMS.begin();
  
  pinMode(LEFTHOME, INPUT);
  pinMode(RIGHTHOME, INPUT);

  //brewHome(); // Home the brewing motor
  //brewMotor->setSpeed(1000);

  brewMotor.setMaxSpeed(1000.0);
  brewMotor.setAcceleration(200.0);
  
  Serial.println("Motor Initilized");
}

void loop() {
  String incoming = "";
  if(Serial.available() > 0) {
    incoming = Serial.readString();
    Serial.println(incoming);
  }
  if(incoming == TAMP_STRING) {
    // brewMotor.moveTo(brewMotor.currentPosition());
    Serial.println("TAMP RECIEVED");
    brewMotor.moveTo(TAMP_POSITION);
    stopped = false;
  }
  else if(incoming == BREW_STRING) {
    Serial.println("BREW RECIEVED");
    brewMotor.moveTo(BREW_POSITION);
    stopped = false;
  }
   else if(incoming == DISPOSE_STRING) {
    Serial.println("DISPOSE RECIEVED");
    brewMotor.moveTo(DISPOSE_POSITION);
    stopped = false;
   }
  else if(incoming == "STOP\r\n") {;
    stopped = true;
   }
   else {
    Serial.flush();
   }

   if(brewMotor.distanceToGo() == 0) {
    stopped = true;
   }
   if(!stopped)
    brewMotor.run();
   else
    brewMotor.stop();
}

// Home the motor
void brewHome(){
  brewMove(100, FORWARD);
  while(!digitalRead(LEFTHOME)){
    brewMove(1, BACKWARD);
  } 
}

// Move the brew motor while checking to make sure that we haven't hit an endstop
// Return if an endstop is hit
void brewMove(int steps, int dir){
  uint16_t stepsTaken = 0;
  if(dir == FORWARD){
    while(!digitalRead(LEFTHOME) && stepsTaken < steps){
      brewMotor_t->step(1, FORWARD, MICROSTEP);
      stepsTaken++;
    }
  }
  else{
    while(!digitalRead(RIGHTHOME) && stepsTaken < steps){
      brewMotor_t->step(1, BACKWARD, MICROSTEP);
      stepsTaken++;
    }
  }
}

// Step the motor with acceleration
// TODO - Implement
//  Need to ramp up speed over the steps
void accelStep(Adafruit_StepperMotor* motor, int steps, int dir){
  motor->setSpeed(10); // RPMs
  motor->step(steps, dir, MICROSTEP);
}
