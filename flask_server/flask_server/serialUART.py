import serial
import re
import time


class SerialUART(object):
    """
    Class Object that represents a communication link through UART to \
    an DUT Board
    """
    def __init__(self, COMPORT):
        """
        Initializes a device @ COMPORT with settings baudrate=15200
        and timeout=1sec
        """
        self._COMPORT = COMPORT
        self.TERMINATOR = "\r\n"
        self.logger = None  # Add Logger mechanism
        try:
            self.dut = serial.Serial(port=self._COMPORT,
                                     baudrate=115200,
                                     timeout=2)
        except serial.serialutil.SerialException:
            print("Could not open {}".format(self._COMPORT))
            exit()

    def __str__(self):
        return "dut32F0 Board @ {}".format(self._COMPORT)

    def open_port(self):
        """
        Tries to open the port, else throws
        """
        try:
            self.dut.open()
        except serial.serialutil.SerialException:
            return "Could not open {}".format(self._COMPORT)

    def is_port_open(self):
        """
        returns true if port is open, else false
        """
        return self.dut.isOpen()

    def close(self):
        """
        Closes the port
        """
        self.dut.close()

    def _cmd(self, cmd):
        """
        Writes 'cmd' to the dut board and reads back the response. If the
        device does not provide a response it will throw, else returns
        """
        try:
            if not self.is_port_open():
                self.dut.open()
            self.dut.write("{}{}".format(cmd, self.TERMINATOR).encode())
            time.sleep(.10)
            return self._get_serial_data()
        except ValueError:
            exit()

    def _get_serial_data(self):
        """
        Reads a line from the dut device and then closes
        """
        x = ""
        try:
            x += self.dut.readline().decode('ascii')
        except serial.SerialTimeoutException:
            print("timeout")
        self.dut.close()
        return x

    def sample_cmd(self):
        """
        This is an example of how we will make our cmd calls.
        'see _cmd'
        """
        return self._cmd("")
    
    def demo(self, val1, val2):
        """
        Sends the DEMO command with values `val1` and `val2`
        """
        return self._cmd("DEMO,{},{}".format(val1, val2))
    
    def final(self, waterTemp, waterDisp, coffeeDisp, frothStr, milkDisp):
        """
        Sends final command over UART to iniate brewing process
        @param: waterTemp- Temperature of the water (F)
        @param: waterDisp- How much water is dispensed (oz)
        @param: coffeeDisp- How much coffee is dispensed (grams)
        @param: frothStr - Strength of the frother (%)
        @param: milkDispensed- How much milk is dispensed
        
        ret: Arduino reply back
        """
        return self._cmd("FINAL,{},{},{},{},{}".format(waterTemp,
                                                       waterDisp,
                                                       coffeeDisp,
                                                       frothStr,
                                                       milkDisp))
    
    def pwm(self, dutyCyle):
        """
        Sets the dutyCycle of the pwm pin to 'dutyCycle'
        """
        return self._cmd("pwm {}".format(dutyCyle))

