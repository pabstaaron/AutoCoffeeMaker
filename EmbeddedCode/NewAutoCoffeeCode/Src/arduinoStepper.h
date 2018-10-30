#ifndef arduinostepper_H_
#define arduinostepper_H_

#include "stm32f0xx_hal.h"

/**
 * Ports the Arduino stepper library to a C implementation
 */

typedef struct{
	unsigned long pinNumber;
	GPIO_TypeDef* gpioBlock;
} gpio_pin_t;

// Struct for holding instance specific variables
typedef struct{
	gpio_pin_t motor_pin_1;
	gpio_pin_t motor_pin_2;
	gpio_pin_t motor_pin_3;
	gpio_pin_t motor_pin_4;
	int direction;            // Direction of rotation
  unsigned long step_delay; // delay between steps, in ms, based on speed
  int number_of_steps;      // total number of steps this motor can take
  int pin_count;            // how many pins are in use.
	int step_number; // which step the motor is on
	unsigned long last_step_time; // time stamp in us of when the last step was taken
} stepper_t;

// Creates a new stepper instance
stepper_t stepper_init(int numSteps, gpio_pin_t mPin1, gpio_pin_t mPin2, 
												gpio_pin_t mPin3, gpio_pin_t mPin4);

// Set motor speed in RPMs
void setSpeed(stepper_t stepper, long whatSpeed);

// mover method:
void step(stepper_t stepper, int number_of_steps);

#endif 