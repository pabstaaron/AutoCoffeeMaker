#ifndef stepper_H_
#define stepper_H_

#include "stm32f0xx.h"         

#define M1_AIN1 GPIO_PIN_3
#define M1_AIN2 GPIO_PIN_4
#define M1_AIN_BLOCK GPIOB

#define M1_BIN1 GPIO_PIN_5
#define M1_BIN2 GPIO_PIN_6
#define M1_BIN_BLOCK GPIOB

void STEPPER_Init(void);
void rotate(int steps, float speed);
void rotate_degrees(float deg, float speed);

#endif 
