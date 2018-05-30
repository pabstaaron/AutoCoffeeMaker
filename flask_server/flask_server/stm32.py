import serial
import re


class STM32(object):
    """
    Class Object that represents a communication link through UART to \
    an STM32 Board
    """
    def __init__(self, COMPORT):
        """
        Initializes a device @ COMPORT with settings baudrate=15200
        and timeout=1sec
        """
        m = re.search("^COM[0-9]{1,}$", COMPORT)
        if m is None:
            raise Exception('COMPORT must be of COMXX format')
        self._COMPORT = COMPORT
        self.TERMINATOR = "\r\n"
        self.logger = None  # Add Logger mechanism
        try:
            self.stm = serial.Serial(port=self._COMPORT,
                                     baudrate=15200,
                                     timeout=1)
        except serial.serialutil.SerialException:
            print("Could not open {}".format(self._COMPORT))
            exit()

    def __str__(self):
        return "STM32F0 Board @ {}".format(self._COMPORT)

    def open_port(self):
        """
        Tries to open the port, else throws
        """
        try:
            self.stm.open()
        except serial.serialutil.SerialException:
            return "Could not open {}".format(self._COMPORT)

    def is_port_open(self):
        """
        returns true if port is open, else false
        """
        return self.stm.isOpen()

    def close(self):
        """
        Closes the port
        """
        self.stm.close()

    def _cmd(self, cmd):
        """
        Writes 'cmd' to the stm board and reads back the response. If the
        device does not provide a response it will throw, else returns
        """
        try:
            if not self.is_port_open():
                self.stm.open()
            self.stm.write("{}{}".format(cmd, self.TERMINATOR).encode())
            return self._get_serial_data().decode('utf8')
        except ValueError:
            exit()

    def _get_serial_data(self):
        """
        Reads a line from the stm device and then closes
        """
        x = self.stm.readline()
        self.stm.close()
        return x

    def sample_cmd(self):
        """
        This is an example of how we will make our cmd calls.
        'see _cmd'
        """
        return self._cmd("Sample CMD")