#ifndef HEATER_H_
#define HEATER_H_

#include <stdio.h>
#include <stdlib.h>
#include "stm32f0xx.h"

#define BOILING 212 // Boiling point in degrees farenheit
#define HEAT_PIN GPIO_PIN_5 // Relay is on pin PA5
#define TOLERABLE 2 // Acceptable error window

extern volatile uint16_t setTemp;

void heater_init(void);

void heater_update(void);

#endif /* HEATER_H_ */