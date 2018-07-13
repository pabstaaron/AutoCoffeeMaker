#include "stm32f0xx.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "Heater.h"
#include "UART.h"

volatile uint16_t setTemp;

void heater_init(){
	setTemp = BOILING  - 20;
}

uint16_t getHeat(){
	return 0;
}

void relayLow(){
	HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);
}

void relayHigh(){
	HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_RESET);
}

void heater_update(void){
	int error = setTemp - getHeat(); // Error value
	
	if(error > TOLERABLE) // Heat too low
		relayHigh();
	else if(error < TOLERABLE) // Heat too high
		relayLow();
}

