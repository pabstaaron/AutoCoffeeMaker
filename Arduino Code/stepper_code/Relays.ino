/**
 * Sets relay1 to `enable`
 */
void setRelay1(bool enable) {
  if(enable)
    digitalWrite(RELAY_SWITCH1, LOW);
  else
    digitalWrite(RELAY_SWITCH1, HIGH);  
}

/**
 * Sets relay2 to `enable`
 */
void setRelay2(bool enable) {
  if(enable)
    digitalWrite(RELAY_SWITCH2, LOW);
  else
    digitalWrite(RELAY_SWITCH2, HIGH);  
}

/**
 * Sets relay3 to `enable`
 */
void setRelay3(bool enable) {
  if(enable)
    digitalWrite(RELAY_SWITCH3, LOW);
  else
    digitalWrite(RELAY_SWITCH3, HIGH);  
}

/**
 * Sets relay4 to `enable`
 */
void setRelay4(bool enable) {
  if(enable)
    digitalWrite(RELAY_SWITCH4, LOW);
  else
    digitalWrite(RELAY_SWITCH4, HIGH);  
}

/**
 * Sets all relays in the system to off
 */
void resetAllRelays() {
  setRelay1(false);
  setRelay2(false);
  setRelay3(false);
  setRelay4(false);
}
