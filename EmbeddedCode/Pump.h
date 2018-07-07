
#ifndef PUMP_H_
#define PUMP_H_

#include <stdio.h>
#include <stdlib.h>

/* -------------------------------------------------------------------------------------------------------------
 *  Global Variable and Type Declarations
 *  -------------------------------------------------------------------------------------------------------------
 */
extern volatile int16_t error_integral;    // Integrated error signal

/* -------------------------------------------------------------------------------------------------------------
 *  Global Variables for Debug Viewing (no real purpose to be global otherwise)
 * -------------------------------------------------------------------------------------------------------------
 */
extern volatile uint8_t duty_cycle;    // Output PWM duty cycle
extern volatile int16_t target_flow;    // Desired flow rate target
extern volatile int16_t pump_flow;   // Measured flow rate
extern volatile int8_t adc_current_value;      // ADC measured motor current
extern volatile int16_t error_flow;         // Flow error signal
extern volatile uint8_t Kpf;            // Proportional gain flow
extern volatile uint8_t Kif;            // Integral gain flow


/* -------------------------------------------------------------------------------------------------------------
 *  Motor Control and Initialization Functions
 * -------------------------------------------------------------------------------------------------------------
 */

// Sets up the entire motor drive system
void pumpInit(void);

// Set the duty cycle of the PWM, accepts (0-100)
void pwm_setDutyCycle(uint8_t duty);

// PI control code is called within a timer interrupt
void PI_update(void);

void timer_init(void);

/* -------------------------------------------------------------------------------------------------------------
 *  Internal-Use Initialization Functions
 * -------------------------------------------------------------------------------------------------------------
 */

// Sets up the PWM and direction signals to drive the H-Bridge
//void pwm_init(void);

// Initilize flow sensor
//void flowInit(void);

// Sets up encoder interface to read motor speed
//void encoder_init(void);

// Sets up ADC to measure motor current
//void ADC_init(void);

void pulseHandler();

#endif /* MOTOR_H_ */
