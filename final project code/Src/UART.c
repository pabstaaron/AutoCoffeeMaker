#include "stm32f0xx_hal.h"
#include "leds.h"
#include "UART.h" 
#include <string.h>
#include <stdlib.h>

uint8_t buffIndex; // The current index into the buffer
char cmdBuffer[10]; // Buffer for storing command strings
TIM_HandleTypeDef htim14;
void HAL_TIM_MspPostInit(TIM_HandleTypeDef *htim);

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
	sendString("\r\nBOOTED\n\r");
	MX_TIM14_Init();
	HAL_TIM_PWM_Start(&htim14, TIM_CHANNEL_1);
}

/*
 *  Prints the serial command help menu
 */
void printHelpMenu() {
	sendString("\r\n SERIAL COMMAND GUIDE \r\n");
		sendString("\t brew: brew 1 2 3 4 5 6 7\r\n");
			sendString("\t\t <waterTemp> \r\n");
			sendString("\t\t <waterPressure> \r\n");
			sendString("\t\t <frothPressure> \r\n");
			sendString("\t\t <waterDispensed> \r\n");
			sendString("\t\t <frothDispensed> \r\n");
			sendString("\t\t <milkDispensed> \r\n");
			sendString("\t\t <coffeeDispensed> \r\n");
		sendString("\t pwm: pwm 1\r\n");
			sendString("\t\t <dutyCycle> \r\n");
		sendString("\t ?: ? \r\n");
	sendString("\r\n STM32 LED SIGNIFICANCE \r\n");
		sendString("\t RED: pwm \r\n");
		sendString("\t GREEN: brew command received \r\n"); 
		sendString("\t BLUE: pwm command received\r\n");
		sendString("\t ORANGE: incorrect command or error \r\n");
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
	char *p = strtok(str, " ");
	char *array[8];
	while(p != NULL) {
		array[i++] = p;
		p = strtok(NULL, " ");
	}
	i = 0;
	p = 0;
	
	if(strcmp(array[0], "brew") == 0) {
		clear();
		green_on();
		sendString("\r\nBrew Initated @\r\n Water Temp: ");
		sendString(array[1]);
		sendString("\r\n");
		sendString(" @ Water Pressure: ");
		sendString(array[2]);
		sendString("\r");
		sendString(" @ Froth Pressure: ");
		sendString(array[3]);
		sendString("\r\n");
		sendString(" @ Water Dispensed: ");
		sendString(array[4]);
		sendString("\r\n");
		sendString(" @ Froth Dispensed: ");
		sendString(array[5]);
		sendString("\r\n");
		sendString(" @ Milk Dispensed: ");
		sendString(array[6]);
		sendString("\r\n");
		sendString(" @ Coffee Dispensed: ");
		sendString(array[7]);
		sendString("\r\n");
	}
	else if (strcmp(array[0], "pwm") == 0) {
		clear();
		blue_on();
		sendString("\r\nPWM Output to LED @ ");
		sendString(array[1]);
		sendString("\r\n");
		TIM14->CCR1 = atoi(array[1]);
	}
	else if(strcmp(array[0], "?") == 0) {
		printHelpMenu();
	}
	else {
		sendString("\r\nError reading command\r\n");
		sendString(array[0]);
		clear();
		orange_on();
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
	sendString("\r\n");
}

/* TIM14 init function */
void MX_TIM14_Init(void)
{

  TIM_OC_InitTypeDef sConfigOC;

  htim14.Instance = TIM14;
  htim14.Init.Prescaler = 16;
  htim14.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim14.Init.Period = 100;
  htim14.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim14.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim14) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

  if (HAL_TIM_PWM_Init(&htim14) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  if (HAL_TIM_PWM_ConfigChannel(&htim14, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

  HAL_TIM_MspPostInit(&htim14);

}

