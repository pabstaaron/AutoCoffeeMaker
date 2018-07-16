#include "stm32f0xx_hal.h"
#include "rotaryEncoder.h"

void rotartyEncoder_init(void){
		GPIOC->MODER |= 2 << 12; // Set PA15 to AF mode; AF2
		GPIOC->MODER |= 2 << 14; // Set PA1 to AF mode; AF2, TODO change to PB3
		// Should already be in AF0
		GPIOC->AFR[1] |= 2 << 28;
		GPIOC->AFR[0] |= 2 << 4;
	
	
    /* Hint: MAKE SURE THAT YOU USE 5V TOLERANT PINS FOR THE ENCODER INPUTS!
     *       You'll fry the processor otherwise, read the lab to find out why!
     */

    // Set up encoder interface (TIM3 encoder input mode)
    RCC->APB1ENR |= RCC_APB1ENR_TIM3EN;
    TIM2->CCMR1 = 0;    //Clear control registers
    TIM2->CCER = 0;
    TIM2->SMCR = 0;
    TIM2->CR1 = 0;

    TIM2->CCMR1 |= (TIM_CCMR1_CC1S_0 | TIM_CCMR1_CC2S_0);   // TI1FP1 and TI2FP2 signals connected to CH1 and CH2
    TIM2->SMCR |= (TIM_SMCR_SMS_1 | TIM_SMCR_SMS_0);        // Capture encoder on both rising and falling edges
    TIM2->ARR = 0xFFFF;                                     // Set ARR to top of timer (longest possible period)
    TIM2->CNT = 0;                                     
    TIM2->CR1 |= TIM_CR1_CEN;
		
		// Setup a pin for the encoder pushbutton w/ interrupt
		// PC1
//		EXTI->IMR = 0x0001; // Configure mask bit
//		EXTI->RTSR = 0x0001; // Configure trigger selection bits of the interrupt line on rising edge
		
//		RCC->APB2ENR |= RCC_APB2RSTR_SYSCFGRST; // Enable the clock for syscfg
//		SYSCFG->EXTICR[1] &= (uint16_t)~SYSCFG_EXTICR1_EXTI1_PC; // Select port c for pin 1 external interrupt
//		
//		/* Configure NVIC for External Interrupt */
//		/* (1) Enable Interrupt on EXTI0_1 */
//		/* (2) Set priority for EXTI0_1 */
//		NVIC_EnableIRQ(EXTI0_1_IRQn); /* (1) */
//		NVIC_SetPriority(EXTI0_1_IRQn, 3); /* (2) */
//		//NVIC_SetPriority(SysTick_IRQn, 2);
		
}


