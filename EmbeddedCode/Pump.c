/* -------------------------------------------------------------------------------------------------------------
 *  Motor Control and Initialization Functions
 * -------------------------------------------------------------------------------------------------------------
 */ 
#include "stm32f0xx.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "Pump.h"
#include "UART.h"
#include "Heater.h"

volatile int16_t error_integral = 0;    // Integrated error signal

/* -------------------------------------------------------------------------------------------------------------
 *  Global Variables for Debug Viewing (no real purpose to be global otherwise)
 * -------------------------------------------------------------------------------------------------------------
 */

/* -------------------------------------------------------------------------------------------------------------
 *  Global Variable and Type Declarations
 *  -------------------------------------------------------------------------------------------------------------
 */
extern volatile int16_t error_integral;    // Integrated error signal

/* -------------------------------------------------------------------------------------------------------------
 *  Global Variables for Debug Viewing (no real purpose to be global otherwise)
 * -------------------------------------------------------------------------------------------------------------
 */
volatile uint8_t duty_cycle;     			 // Output PWM duty cycle
volatile int16_t target_flow;    			 // Desired flow rate target in mL
volatile int16_t pump_flow;      			 // Measured flow rate in pulses per second
volatile int8_t adc_current_value;      // ADC measured motor current
volatile int16_t error_flow;            // Flow error signal
volatile uint8_t Kpf;            			 // Proportional gain flow
volatile uint8_t Kif;            			 // Integral gain flow


uint16_t counter;

/* -------------------------------------------------------------------------------------------------------------
 *  Motor Control and Initialization Functions
 * -------------------------------------------------------------------------------------------------------------
 */

// Sets up the entire motor drive system
void motor_init(void);

// Set the duty cycle of the PWM, accepts (0-100)
void pwm_setDutyCycle(uint8_t duty);

// PI control code is called within a timer interrupt
void PI_update(void);


/* -------------------------------------------------------------------------------------------------------------
 *  Internal-Use Initialization Functions
 * -------------------------------------------------------------------------------------------------------------
 */

// Sets up the PWM and direction signals to drive the H-Bridge
void pwm_init(void);

// Sets up encoder interface to read motor speed
void timer_init(void);

// Sets up ADC to measure motor current
void ADC_init(void);

void flowInit(void);

void pumpInit(void);


// Sets up the entire motor drive system
void pumpInit(void) {
		counter = 0;
		target_flow = 10; // mL / second
    timer_init();
		sendString("PUMP!\n\r");
}




// Set the duty cycle of the PWM, accepts (0-100)
void pwm_setDutyCycle(uint8_t duty) {
    if(duty <= 100) {
        TIM14->CCR1 = ((uint32_t)duty*TIM14->ARR)/100;  // Use linear transform to produce CCR1 value
        // (CCR1 == "pulse" parameter in PWM struct used by peripheral library)
    }
}


// Push button code here
void pulseHandler(){
	// Toggle parameter input
	//sendString("PULSEEEEE!!!!!\n\r");
	counter++;
	EXTI->PR |= 1 << 6;
}

// Sets up the PI update timer
void timer_init(void) {
    // Configure a timer (TIM6) to fire an ISR on update event
    // Used to periodically check and update speed variable
    RCC->APB1ENR |= RCC_APB1ENR_TIM6EN;

    /// TODO: Select PSC and ARR values that give an appropriate interrupt rate

    /* Hint: See section in lab on sampling rate!
     *       Recommend choosing a sample rate that gives 2:1 ratio between encoder value
     *       and target speed. (Example: 200 RPM = 400 Encoder count for interrupt period)
     *       This is so your system will match the lab solution
     */
		 // Should give 0.5RPM resolution with a 26.66Hz refresh rate
     TIM6->PSC = 3999; // TODO: Change this!
     TIM6->ARR = 75; // TODO: Change this!

    TIM6->DIER |= TIM_DIER_UIE;             // Enable update event interrupt
    TIM6->CR1 |= TIM_CR1_CEN;               // Enable Timer

    NVIC_EnableIRQ(TIM6_DAC_IRQn);          // Enable interrupt in NVIC
    NVIC_SetPriority(TIM6_DAC_IRQn,2);
}

// Encoder interrupt to calculate motor speed, also manages PI controller
void TIM6_DAC_IRQHandler(void) {
    // Compute flow rate
		// 450 pulses ~= 1 liter; 1 pulse ~= 2.22mL
		// Capture timer is set to uptick every 8 pulses
		// Approximate frequency at 27Hz to avoid decimal math
		pump_flow = TIM3->CNT * 8 * 27; // Determine flow rate in pulses per second
		itoaPrint(TIM3->CNT);
	
    // Call the PI update function
    PI_update();
		heater_update();
	
		//TIM3->CNT = 0; // Reset counter
    TIM6->SR &= ~TIM_SR_UIF;        // Acknowledge the interrupt
}



void PI_update(void) {

    /* Run PI loop
     */

    /// calculate error signal and write to "error" variable
		
    /* Hint: Remember that your calculated motor speed may not be directly in RPM!
     *       You will need to convert the target or encoder speeds to the same units.
     *       I recommend converting to whatever units result in larger values, gives
     *       more resolution.
     */
		// 200 RPM = 640,000 encoder counts / minute
		// 3,200 minute counts / RPM
		
		uint16_t set = target_flow * 2; // pulses per second. Approximating conversion rate at 2mL...
		uint16_t actual = pump_flow; // TODO Convert from measured pulses
	
		// TODO Add temprature influence here
		error_flow = set - actual; // Gives error in encoder counts / minute
		
		error_integral = error_integral + (Kif * error_flow); 
		
		if(error_integral > 3200)
			error_integral = 3200;
		//if(error_integral < 0)
			//error_integral = 0;

    /// Calculate proportional portion, add integral and write to "output" variable
    int16_t output = (Kpf * error_flow) + error_integral;

    /* Because the calculated values for the PI controller are significantly larger than
     * the allowable range for duty cycle, you'll need to divide the result down into
     * an appropriate range. (Maximum integral clamp / X = 100% duty cycle)
     *
     * Hint: If you chose 3200 for the integral clamp you should divide by 32 (right shift by 5 bits),
     *       this will give you an output of 100 at maximum integral "windup".
     *
     * This division also turns the above calculations into pseudo fixed-point. This is because
     * the lowest 5 bits act as if they were below the decimal point until the division where they
     * were truncated off to result in an integer value.
     *
     * Technically most of this is arbitrary, in a real system you would want to use a fixed-point
     * math library. The main difference that these values make is the difference in the gain values
     * required for tuning.
     */

     /// Divide the output into the proper range for output adjustment
		 output >>= 5; // Divide by 32

     /// Clamp the output value between 0 and 100
		if(output > 100)
			output = 100;
		
    pwm_setDutyCycle(output);
    duty_cycle = output;            // For debug viewing

    // Read the ADC value for current monitoring, actual conversion into meaningful units
    // will be performed by STMStudio
    if(ADC1->ISR & ADC_ISR_EOC) {   // If the ADC has new data for us
        adc_current_value = ADC1->DR;       // Read the motor current for debug viewing
    }
}
