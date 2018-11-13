#define MAX_EXTENSION 80

/*
 * Sets the TAMPER actuator to the `percentage` extended
 * if `percentage` < MAX_EXTENSION
 */
void setTamperTo(uint16_t percentage) {
  if(percentage > MAX_EXTENSION)
     percentage = MAX_EXTENSION;
    
  tamping_actuator.writeMicroseconds(1000 + percentage * 10);
  delay(2000);
}

/*
 * Sets the Disposer actuator to the `percentage` extended
 * if `percentage` < MAX_EXTENSION
 */
void setDisposerTo(uint16_t percentage) {
   if(percentage > MAX_EXTENSION)
     percentage = MAX_EXTENSION;
    
  disposing_actuator.writeMicroseconds(1000 + percentage * 10);
  delay(2000);
}

/**
 * Resets the acuators to the fully collapsed position
 */
void resetActuators() {
  tamping_actuator.writeMicroseconds(0);
  disposing_actuator.writeMicroseconds(0);
  delay(2000);
}
