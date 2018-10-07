#include "stm32f0xx_hal.h"
#include "UART.h" 
#include <string.h>


uint8_t buffIndex; // The current index into the buffer
char cmdBuffer[10]; // Buffer for storing command strings

void red_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_6;
}

void red_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_6;
}

void orange_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_8;
}

void orange_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_8;
}

void blue_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_7;
}

void blue_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_7;
}

void green_on() {
	GPIOC->BSRR |= GPIO_BSRR_BS_9;
}

void green_off() {
	GPIOC->BSRR |= GPIO_BSRR_BR_9;
}

void clear() {
	GPIOC->ODR = 0x00000000;
}

void UART_Init(void){
	RCC->APB1ENR |= RCC_APB1ENR_USART3EN; // Enable the clock to USART3
															
	// PC4 -> RX
	// PC5 -> TX
	GPIOC->MODER |= 0x00000A00; // Set PC4 and PC5 to alternate function
	GPIOC->AFR[0] = 0x00110000; // Select AF1 in the alternate function registers
	GPIOC->AFR[1] = 0;
	
	// Configure USART3 
															
	// Set the baud rate
	USART3->BRR = HAL_RCC_GetHCLKFreq() / 115200;
						
	// Enable USART interrupt
	NVIC_EnableIRQ(USART3_4_IRQn); 
	NVIC_SetPriority(USART3_4_IRQn, 2); 
			
															
  // Enable the rcv interrupt
	USART3->CR1 |= 0x20; 
															
	// Enable tx/rx hardware
	USART3->CR1 |= 0xD;
}

/* 
 * Read a single char into the command buffer and execute a function
 */
static void rcvChar(char c){
	if(buffIndex > 100){ // Reset and bail if too many chars are punched in
		sendString("OVERFLOW\r\n");
		buffIndex = 0;
		return;
	}
	
	if(c == '\r'){
		cmdBuffer[buffIndex] = 0; // Make sure to null-terminate string
		// YOUR FUNCTION CALL HERE
		buffIndex = 0;
		red_on();
		parseCmd(cmdBuffer); 
	}
	else { 
		cmdBuffer[buffIndex] = c;
		buffIndex++;
		sendChar(c);
	}
}


void parseCmd(char* str) {
	int i = 0;
	char *p = strtok(str, ",");
	char *array[8];
	sendString(str);
	while(p != NULL) {
		blue_on();
		array[i++] = p;
		p = strtok(NULL, ",");
	}
	i = 0;
	p = 0;
	
	if(strcmp(array[0], "brew") == 0) {
		orange_on();
		sendString("\r\nBrew Initated @\r\n Water Temp:");
		sendString(array[1]);
		sendString("\r\n");
		sendString(" @ Water Pressure:");
		sendString(array[2]);
		sendString("\r\n");
		sendString(" @ Froth Pressure");
		sendString(array[3]);
		sendString("\r\n");
		sendString(" @ Water Dispensed");
		sendString(array[4]);
		sendString("\r\n");
		sendString(" @ Froth Dispensed");
		sendString(array[5]);
		sendString("\r\n");
		sendString(" @ Milk Dispensed");
		sendString(array[6]);
		sendString("\r\n");
		sendString(" @ Coffee Dispensed:");
		sendString(array[7]);
		sendString("\r\n");
		red_off();
	}
	else {
		sendString("\r\nError reading command\r\n");
		green_on();
		orange_off();
		blue_off();
	}
	// memset(array, 0, 10);
	// memset(cmdBuffer, 0, 10);
}
/*
 * Handler for rx interrupt
 */
void USART3_4_IRQHandler(){
	rcvChar(USART3->RDR);
}

void USART1_IRQHandler() {
	rcvChar(USART1->RDR);
}

/*
 * Sends a single character on USART3
 */
void sendChar(char c){
	while(!(USART1->ISR & 0x80)); // Wait until the transmit register is clear
	USART1->TDR = c;
}

/*
 * Sends a null terminated string on USART3
 */
void sendString(char* str){
	while(1){
		sendChar(*str);
		if(*str == 0)
			break;
		str++;
	}
}

void itoaPrint(int i){
	char numStr[10];
	int  var1 = i;

	sprintf(numStr, "%d", var1);
	sendString(numStr);
	sendString("\r\n");
}
