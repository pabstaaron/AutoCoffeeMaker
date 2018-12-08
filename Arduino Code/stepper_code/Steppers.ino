// Stepper Functions
void tampStep();
void disposeStep();
void brewStep();
void dispenseStep();
void calibrate();

// Functions to give to AccelStepper
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

void frothUp(){
  frothMotor_t->step(1, FORWARD, SINGLE);
}

void frothDown(){
  frothMotor_t->step(1, BACKWARD, SINGLE);
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
void dispenseStep(int steps) {
    dispenserMotor.moveTo(dispenserMotor.currentPosition() + steps);
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
    endPosition = brewMotor.currentPosition();
    delay(500);
    
    tampStep();
    Serial.println("Start position: ");
    Serial.println(brewMotor.currentPosition());
    calibrating = false;
}
