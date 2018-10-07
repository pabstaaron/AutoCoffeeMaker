#ifndef UART_H_
#define UART_H_

void UART_Init(void);
void sendChar(char c);
void sendString(char* str);
void itoaPrint(int i);
void rcvChar(char c);
void parseCmd(char* str);
void MX_TIM14_Init(void);

#endif 
