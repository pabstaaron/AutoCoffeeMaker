#include "stm32f0xx_hal.h"
#include "stepper.h"
#include "leds.h"

int counter = 0;

void STEPPER_Init(void) {
	/* Instantiate GPIO PINS */
	
}

void rotate(int steps, float speed) {
	int dir = (steps > 0) ? 1: 0;
	int absSteps = (steps < 0) ? steps * -1: steps;
	// float usDelay = (1 / speed) * 70;
	float usDelay = speed;
	// LED logic (REMOVE IN FINAL)
	clear();
	
	
	// Figure out which one is clockwise and which is counter
	if(dir) {
		for(int i = 0; i < absSteps; i++) {
			// Set One pair Pair High and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			red_toggle(enable);
			HAL_Delay(usDelay);
			blue_toggle(!enable);
			HAL_Delay(usDelay);
		}
	}
	else {
		for(int i = 0; i < absSteps; i++) {
			// Set the other pair high and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			green_toggle(enable);
			HAL_Delay(usDelay);
			orange_toggle(!enable);
			HAL_Delay(usDelay);
		}
	}
}

void rotate_degrees(float deg, float speed) {
	int dir = (deg > 0) ? 1: 0;
	int degrees = (deg < 0) ? deg * -1 * 1/0.225: deg * 1/0.225;
	// float usDelay = (1 / speed) * 70;
	float usDelay = speed;
	// LED logic (REMOVE IN FINAL)
	clear();
	
	
	// Figure out which one is clockwise and which is counter
	if(dir) {
		for(int i = 0; i < degrees; i++) {
			// Set One pair Pair High and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			red_toggle(enable);
			HAL_Delay(usDelay);
			blue_toggle(!enable);
			HAL_Delay(usDelay);
		}
	}
	else {
		for(int i = 0; i < degrees; i++) {
			// Set the other pair high and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			green_toggle(enable);
			HAL_Delay(usDelay);
			orange_toggle(!enable);
			HAL_Delay(usDelay);
		}
	}
}
