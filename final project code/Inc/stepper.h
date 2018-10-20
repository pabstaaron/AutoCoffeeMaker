#ifndef stepper_H_
#define stepper_H_

void STEPPER_Init(void);
void rotate(int steps, float speed);
void rotate_degrees(float deg, float speed);

#endif 
