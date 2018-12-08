#define MAX_EXTENSION 60
#define MAX_ROTATION 180

/*
 * Sets the TAMPER actuator to the `percentage` extended
 * if `percentage` < MAX_EXTENSION
 */
void setTamperTo(uint16_t percentage) {
  if(percentage > MAX_EXTENSION)
     percentage = MAX_EXTENSION;
    
  // tamping_actuator.writeMicroseconds(1050 + percentage * 10);
   for(int i = 1050; i < 1050 + percentage * 10; i = i + 50) {
    tamping_actuator.writeMicroseconds(i);
    delay(300);
  }
  delay(2000);
}

/*
 * Sets the Disposer actuator to the `percentage` extended
 * if `percentage` < MAX_EXTENSION
 */
void setDisposerTo(uint16_t percentage) {
   if(percentage > MAX_EXTENSION)
     percentage = MAX_EXTENSION;
    
  // disposing_actuator.writeMicroseconds(1050 + percentage * 10);
  for(int i = 1050; i < 1050 + percentage * 10; i = i + 10) {
    disposing_actuator.writeMicroseconds(i);
    delay(300);
  }
  delay(2000);
}

void openSteamer() {
  for(int i = steamerPosition; i > 0; i--) {
    steam_actuator.write(i);
    delay(50);
  }
  steamerPosition = 0;
}

void closeSteamer() {
  for(int i = steamerPosition; i < 180; i++) {
    steam_actuator.write(i);
    delay(50);
  }
  steamerPosition = 180;
}

void resetSteamer() {
  steam_actuator.write(0 );
  steamerPosition = 0;
}

/**
 * Resets the acuators to the fully collapsed position
 */
void resetActuators() {
  tamping_actuator.writeMicroseconds(1050);
  disposing_actuator.writeMicroseconds(1050);
  delay(2000);
}
