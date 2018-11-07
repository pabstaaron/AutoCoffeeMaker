/*
 * Spins a motor on channel 1 on a loop
 */
#include <Wire.h>
#include <Servo.h> 
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "utility/Adafruit_MS_PWMServoDriver.h"

//NOT SURE IF USING THESE
//#define LEFTHOME 8 // TODO - Decide on these pins
//#define RIGHTHOME 9

#define TAMPSWITCH A0
#define DISPOSE_SWITCH A5

#define MIN_ACTUATOR_TRANISTION 6000 // The time the acuators need to make a transition

#define TAMPER_SERVO_PIN 11 // The control pin for the tamper

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
uint16_t DISPOSE_CALIBRATE_POSITION = 300;

void brewHome();
void tampStep();
void tamperInit();
void runTamper();
//NOT SURE IF USING THESE
//void accelStep(Adafruit_DCMotor motor, int steps, int dir);
//void brewMove(int steps, int dir);

Adafruit_MotorShield AFMS = Adafruit_MotorShield();
Adafruit_StepperMotor *brewMotor_t = AFMS.getStepper(200, 1);

//NOT SURE IF USING THESE
void brewForward(){
  brewMotor_t->step(1, FORWARD, SINGLE);
}

void brewBackward(){
  brewMotor_t->step(1, BACKWARD, SINGLE);
}

AccelStepper brewMotor(brewForward, brewBackward);

Servo actuator;
int linearValue = 1500;

void setup() {
  Serial.begin(9600);
  Serial.println("In setup");

  AFMS.begin();

  

//  NOT SURE IF USING
//  pinMode(LEFTHOME, INPUT);
//  pinMode(RIGHTHOME, INPUT);

  pinMode(TAMPSWITCH, INPUT);
  pinMode(DISPOSE_SWITCH, INPUT);
  
 actuator.attach(TAMPER_SERVO_PIN, 1050, 2000);
 pinMode(TAMPER_SERVO_PIN, OUTPUT);

  //brewHome(); // Home the brewing motor
  //brewMotor->setSpeed(1000);

  brewMotor.setMaxSpeed(1000.0);
  brewMotor.setAcceleration(200.0);
  
  Serial.println("Motor Initilized");

  actuator.writeMicroseconds(0);
  delay(2000);
}

void loop() {

  actuator.writeMicroseconds(2000);
  delay(6000);
  actuator.writeMicroseconds(0);
  delay(6000);
  
//  String incoming = "";
//  brewMotor.setAcceleration(200.0);
//  // Read Serial Input
//  if(Serial.available() > 0) {
//    incoming = Serial.readString();
//  }
//  
//  if(incoming == TAMP_STRING) {
//    Serial.println("TAMP RECIEVED");
//    //tampStep();
//    
//    stopped = true;
//  }
//  
//  else if(incoming == BREW_STRING) {
//    Serial.println("BREW RECIEVED");
//    brewMotor.moveTo(endPosition / 2);
//    brewing = true;
//    stopped = false;
//  }
//  
//  else if(incoming == DISPOSE_STRING) {
//    Serial.println("DISPOSE RECIEVED");
//    disposeStep();
//    stopped = true;
//  }
//  
//  else if(incoming == "STOP\r\n") {
//    stopped = true;
//  }
//  
//  else if (incoming == "CALIBRATE\r\n" || calibrating) { 
//    Serial.println("Calibrate recieved");
//    calibrating = true;
//    if(digitalRead(DISPOSE_SWITCH) != LOW){
//      disposeStep();
//      Serial.println("End position: ");
//      Serial.println(brewMotor.currentPosition());
//      delay(500);
//      tampStep();
//      Serial.println("Start position: ");
//      Serial.println(brewMotor.currentPosition());
//    }
//    //This runs if already at the end when calibrating
//    else{
//      tampStep();
//      Serial.println("Start position: ");
//      Serial.println(brewMotor.currentPosition());
//      delay(500);
//      disposeStep();
//      Serial.println("End position: ");
//      Serial.println(brewMotor.currentPosition());
//      delay(500);
//      tampStep();
//      Serial.println("Start position: ");
//      Serial.println(brewMotor.currentPosition());
//    }
//    calibrating = false;
//    stopped = true;
//  }
//  else {
//    Serial.flush();
//   }
//
//   if(brewMotor.distanceToGo() == 0) {
//    if(brewing){
//      Serial.println("Current Brewing position: ");
//      Serial.println(brewMotor.currentPosition());
//      brewing = false;
//    }
//    stopped = true;
//   }
//   
//   if(!stopped)
//   {
//    brewMotor.run();
//   }
//   else
//    brewMotor.stop();
}

/*
 * Handles the tamp step:
 * 
 * Will run the normal tamp step, and then decrement 1 until TAMPSWITCH is depressed
 */
void tampStep() {
    brewMotor.moveTo(TAMP_POSITION);

    // Do a blocking step
    while(brewMotor.distanceToGo() != 0 && digitalRead(TAMPSWITCH) != LOW) {
      brewMotor.run();
    }
    
    // Continually move by 1 step until target is reached
    int extraSteps = 0;
    while(digitalRead(TAMPSWITCH) != LOW) {
      brewMotor.moveTo(brewMotor.currentPosition() - 1);
      brewMotor.run();
    }
    brewMotor.setAcceleration(0.0);
    brewMotor.stop();

   
    // Set 0 to be wherever the switch is depressed
    brewMotor.setCurrentPosition(0);
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
    while(brewMotor.distanceToGo() != 0 && digitalRead(DISPOSE_SWITCH) != LOW) {
      brewMotor.run();
    }

    //stepsMissed = DISPOSE_POSITION - brewMotor.distanceToGo();
    
    
    // Continually move by 1 step until target is reached
    // Sets new endPosition if previous calibration wasn't right
    int extraSteps = 0;
    while(digitalRead(DISPOSE_SWITCH) != LOW) {
      brewMotor.moveTo(brewMotor.currentPosition() + 1);
      extraSteps++;   
      brewMotor.run();
      endPosition = brewMotor.currentPosition();
    }

    brewMotor.setAcceleration(0.0);
    brewMotor.stop();

    // Update the position to the new calibrated position
    //DISPOSE_POSITION = DISPOSE_POSITION + extraSteps - stepsMissed;
    // Serial.println("New Dispose Position: ");
    // Serial.println(DISPOSE_POSITION);
}

//NOT SURE IF USING

// Home the motor
//void brewHome(){
//  brewMove(100, FORWARD);
//  while(!digitalRead(LEFTHOME)){
//    brewMove(1, BACKWARD);
//  } 
//}
//
//// Move the brew motor while checking to make sure that we haven't hit an endstop
//// Return if an endstop is hit
//void brewMove(int steps, int dir){
//  uint16_t stepsTaken = 0;
//  if(dir == FORWARD){
//    while(!digitalRead(LEFTHOME) && stepsTaken < steps){
//      brewMotor_t->step(1, FORWARD, MICROSTEP);
//      stepsTaken++;
//    }
//  }
//  else{
//    while(!digitalRead(RIGHTHOME) && stepsTaken < steps){
//      brewMotor_t->step(1, BACKWARD, MICROSTEP);
//      stepsTaken++;
//    }
//  }
//}
//
//// Step the motor with acceleration
//// TODO - Implement
////  Need to ramp up speed over the steps
//void accelStep(Adafruit_StepperMotor* motor, int steps, int dir){
//  motor->setSpeed(10); // RPMs
//  motor->step(steps, dir, MICROSTEP);
//}


//void tamperInit(){
//  ACTUATOR.attach(ACTUATOR, 1050, 2000);
//  ACTUATOR.writeMicroseconds(1500);
//}
//
//void runTamper(){
//  ACTUATOR.writeMicroseconds(1700);
//}
