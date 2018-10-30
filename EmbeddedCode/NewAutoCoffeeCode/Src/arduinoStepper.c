#include "arduinoStepper.h"
#include "stm32f0xx_hal.h"
#include "stdlib.h"

/*
 * Moves the motor forward or backwards.
 */
void stepMotor(stepper_t stepper, int thisStep){
	switch (thisStep) {
      case 0:  // 1010
					HAL_GPIO_WritePin(stepper.motor_pin_1.gpioBlock, stepper.motor_pin_1.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_2.gpioBlock, stepper.motor_pin_2.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_3.gpioBlock, stepper.motor_pin_3.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_4.gpioBlock, stepper.motor_pin_4.pinNumber, GPIO_PIN_RESET);
      break;
      case 1:  // 0110
					HAL_GPIO_WritePin(stepper.motor_pin_1.gpioBlock, stepper.motor_pin_1.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_2.gpioBlock, stepper.motor_pin_2.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_3.gpioBlock, stepper.motor_pin_3.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_4.gpioBlock, stepper.motor_pin_4.pinNumber, GPIO_PIN_RESET);
      break;
      case 2:  //0101
					HAL_GPIO_WritePin(stepper.motor_pin_1.gpioBlock, stepper.motor_pin_1.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_2.gpioBlock, stepper.motor_pin_2.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_3.gpioBlock, stepper.motor_pin_3.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_4.gpioBlock, stepper.motor_pin_4.pinNumber, GPIO_PIN_SET);
      break;
      case 3:  //1001
					HAL_GPIO_WritePin(stepper.motor_pin_1.gpioBlock, stepper.motor_pin_1.pinNumber, GPIO_PIN_SET);
					HAL_GPIO_WritePin(stepper.motor_pin_2.gpioBlock, stepper.motor_pin_2.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_3.gpioBlock, stepper.motor_pin_3.pinNumber, GPIO_PIN_RESET);
					HAL_GPIO_WritePin(stepper.motor_pin_4.gpioBlock, stepper.motor_pin_4.pinNumber, GPIO_PIN_SET);
      break;
	}
}

stepper_t stepper_init(int numSteps, gpio_pin_t mPin1, gpio_pin_t mPin2, gpio_pin_t mPin3, gpio_pin_t mPin4){
	stepper_t stepper;
	
	stepper.step_number = 0;    // which step the motor is on
  stepper.direction = 0;      // motor direction
  stepper.last_step_time = 0; // time stamp in us of the last step taken
	stepper.number_of_steps = numSteps; // total number of steps for this motor
	
	// Arduino pins for the motor control connection:
  stepper.motor_pin_1 = mPin1;
  stepper.motor_pin_2 = mPin2;
  stepper.motor_pin_3 = mPin3;
	stepper.motor_pin_4 = mPin4;
	
	stepper.pin_count = 4;
	
	return stepper;
}
												
void setSpeed(stepper_t stepper, long whatSpeed){
	stepper.step_delay = 60L * 1000L * 1000L / stepper.number_of_steps / whatSpeed;
}

void step(stepper_t stepper, int steps_to_move){
	int steps_left = abs(steps_to_move); // how many steps to take
	
	// determine direction based on whether steps_to_mode is + or -:
  if (steps_to_move > 0) { stepper.direction = 1; }
	if (steps_to_move < 0) { stepper.direction = 0; }
	
	// decrement the number of steps, moving one step each time:
  while (steps_left > 0)
  {
    unsigned long now = HAL_GetTick() * 1000; // TODO Could use a more precise measure, but we'll be able to get up to 300 RPM like this
    // move only if the appropriate delay has passed:
    if (now - stepper.last_step_time >= stepper.step_delay)
    {
      // get the timeStamp of when you stepped:
      stepper.last_step_time = now;
      // increment or decrement the step number,
      // depending on direction:
      if (stepper.direction == 1)
      {
        stepper.step_number++;
        if (stepper.step_number == stepper.number_of_steps) {
          stepper.step_number = 0;
        }
      }
      else
      {
        if (stepper.step_number == 0) {
          stepper.step_number = stepper.number_of_steps;
        }
        stepper.step_number--;
      }
      // decrement the steps left:
      steps_left--;
      // step the motor to step number 0, 1, ..., {3 or 10}
      if (stepper.pin_count == 5)
        stepMotor(stepper, stepper.step_number % 10);
      else
        stepMotor(stepper, stepper.step_number % 4);
    }
	}
}

