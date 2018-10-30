#include "stm32f0xx_hal.h"
#include "stepper.h"

int counter = 0;

void STEPPER_Init(void) {
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN1, GPIO_PIN_SET);
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN2, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN1, GPIO_PIN_SET);
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN2, GPIO_PIN_RESET);
}

void m1BlockACurrentForward(){
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN1, GPIO_PIN_SET);
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN2, GPIO_PIN_RESET);
}

void m1BlockACurrentBackward(){
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN1, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(M1_AIN_BLOCK, M1_AIN2, GPIO_PIN_SET);
}

void m1BlockBCurrentForward(){
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN1, GPIO_PIN_SET);
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN2, GPIO_PIN_RESET);
}

void m1BlockBCurrentBackward(){
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN1, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(M1_BIN_BLOCK, M1_BIN2, GPIO_PIN_SET);
}

void rotate(int steps, float speed) {
	int dir = (steps > 0) ? 1: 0;
	int absSteps = (steps < 0) ? steps * -1: steps;
	float usDelay = (1 / speed) * 70;
	
	// Figure out which one is clockwise and which is counter-clockwise
	if(dir) {
		for(int i = 0; i < absSteps; i++) {
			// Set One pair Pair High and the other pair low
			HAL_GPIO_TogglePin(M1_AIN_BLOCK, M1_AIN1);
			HAL_Delay(usDelay);
			HAL_GPIO_TogglePin(M1_AIN_BLOCK, M1_AIN2);
		}
	}
	else {
		for(int i = 0; i < absSteps; i++) {
			// Set the other pair high and the other pair low
			HAL_GPIO_TogglePin(M1_BIN_BLOCK, M1_BIN1);
			HAL_Delay(usDelay);
			HAL_GPIO_TogglePin(M1_BIN_BLOCK, M1_BIN2);
		}
	}
}

void rotate_degrees(float deg, float speed) {
	
	int dir = (deg > 0) ? 1: 0;
	int degrees = (deg < 0) ? deg * -1 * 1/0.225: deg * 1/0.225;
	float usDelay = (1 / speed) * 70;
	// Figure out which one is clockwise and which is counter
	if(dir) {
		for(int i = 0; i < degrees; i++) {
			// Set One pair Pair High and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			if(enable == 0){
			
			}
			else{
				
			}
		}
	}
	else {
		for(int i = 0; i < degrees; i++) {
			// Set the other pair high and the other pair low
			int enable = (i % 2 == 0) ? 0: 1;
			if(enable == 0){
			
			}
			else{
				
			}
		}
	}
}
