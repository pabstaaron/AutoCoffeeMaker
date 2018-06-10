#include "stm32f0xx_hal.h"
#include "UART.h" 

uint8_t buffIndex; // The current index into the buffer
char cmdBuffer[10]; // Buffer for storing command strings

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
	if(buffIndex > 10){ // Reset and bail if too many chars are punched in
		sendString("OVERFLOW\n\r ");
		buffIndex = 0;
		return;
	}
	
	if(c == '\r'){
		cmdBuffer[buffIndex] = 0; // Make sure to null-terminate string
		// YOUR FUNCTION CALL HERE
		//parseCmd(cmdBuffer); 
		buffIndex = 0;
	}
	else { 
		cmdBuffer[buffIndex] = c;
		buffIndex++;
		sendChar(c);
	}
}

/*
 * Handler for rx interrupt
 */
void USART3_4_IRQHandler(){
	rcvChar(USART3->RDR);
}

/*
 * Sends a single character on USART3
 */
void sendChar(char c){
	while(!(USART3->ISR & 0x80)); // Wait until the transmit register is clear
	USART3->TDR = c;
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
	sendString("\n\r");
}
