#include "stm32f0xx_hal.h"
#include "leds.h"

void LEDS_Init(void) { 
	GPIO_InitTypeDef initStrC = {GPIO_PIN_6 | GPIO_PIN_7 | GPIO_PIN_8 | GPIO_PIN_9,
															GPIO_MODE_OUTPUT_PP,
															GPIO_SPEED_FREQ_LOW,
															GPIO_NOPULL};
	HAL_GPIO_Init(GPIOC, &initStrC);
}

void red_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_6;
}

void red_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_6;
}

void red_toggle(int enable) {
	if(enable)
		red_on();
	else
		red_off();
}

void orange_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_8;
}

void orange_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_8;
}

void orange_toggle(int enable) {
	if(enable)
		orange_on();
	else 
		orange_off();
}

void blue_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_7;
}

void blue_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_7;
}

void blue_toggle(int enable) {
	if(enable)
		blue_on();
	else
		blue_off();
}

void green_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_9;
}

void green_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_9;
}

void green_toggle(int enable) {
	if(enable)
		green_on();
	else
		green_off();
}

void clear() {
	GPIOC->ODR = 0x00000000;
}
