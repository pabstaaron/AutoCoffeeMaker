
from flask import jsonify
from flask import request
from flask import Flask
from serialUART import SerialUART
import time
import settings
import json

app = Flask(__name__)
_settings = settings.grab_type()
_serial = settings.grab_serial()


@app.route('/')
def simple_connect():
    """
    simple_connect: The most basic call you can make to a Flask server.

    NOT INCLUDED IN FINAL RELEASE
    """
    return "Hello world!"


@app.route('/connected/<serial>', methods=['GET'])
def am_i_connected(serial):
    """
    am_i_connected: Method to be used as a connection check to outside
    IP's. This call can / should be used as a setup call before proceeding
    into coffee making process.

    returns: 400 if flask connection was recieved but without the correct
    serial number or in computer mode, else 200
    """
    post_data = {'utc': int(time.time()),
                 'request': 'Connected',
                 'type': _settings,
                 'serial number': _serial}
    if(serial != _serial):
        print("Returning because bad serial")
        return jsonify(post_data), 400
    else:
        return jsonify(post_data), 200


@app.route('/pwm/<serial>', methods=["POST"])
def change_duty_cycle(serial): 
    """
    change_duty_cycle: Takes in a payload of 'dutyCyle' and changes the pwm signal
    resulting in a change of the LED
    """
    if(request.is_json):
        content = request.get_json()
    
    if "dutyCycle" not in content:
        return _make_bad_data_return("dutyCycle")
    
    dut = SerialUART("COM9")
    reply = dut.pwm(content["dutyCycle"])
    print(reply)
    post_data = {'utc': int(time.time()),
                 'request': 'PWM Changed',
                 'type': _settings,
                 'reply': reply,
                 'serial number': _serial}
    if(_settings == "computer" or serial != _serial):
        return jsonify(post_data), 400
    else:
        return jsonify(post_data), 200

@app.route('/coffee/<serial>', methods=['POST'])
def make_me_a_coffee(serial):
    """
    make_me_a_coffee: Method to be used to initiate coffee making procedure.
    Payload without the correct serial number will be discarded

    returns: 400 if flask connection was recieved without the correct serial
    number or in computer mode, else 200

    content contains all the data being sent over
    """
    if(request.is_json):
        content = request.get_json()
    print(content)

    # Payload: {u'waterDisp': 40, u'waterTemp': 100, u'coffeeDisp': 15, u'frothStr': 0, u'milkDisp': 40}
    if "waterTemp" not in content:
        print("Return because waterTemp not in payload")
        return _make_bad_data_return("waterTemp")
    elif content["waterTemp"] < 0:
        print("Return because waterTemp OOB")
        return _data_out_of_bounds("waterTemp")

    if "waterDisp" not in content:
        print("Return because waterDisp not in payload")
        return _make_bad_data_return("waterDisp")
    elif content["waterDisp"] < 0:
        print("Return because waterDisp OOB")
        return _data_out_of_bounds("waterDisp")
    
    if "coffeeDisp" not in content:
        print("Return because coffeeDisp not in payload")
        return _make_bad_data_return("coffeeDisp")
    elif content["coffeeDisp"] < 0:
        print("Return because coffeeDisp OOB")
        return _data_out_of_bounds("coffeeDisp")
    
    if "frothStr" not in content:
        print("Return because frothStr not in payload")
        return _make_bad_data_return("frothStr")
    elif content["frothStr"] < 0:
        print("Return because frothStr OOB")
        return _data_out_of_bounds("frothStr")
    
    if "milkDisp" not in content:
        print("Return because milkDisp not in payload")
        return _make_bad_data_return("milkDisp")
    elif content["milkDisp"] < 0:
        print("Return because milkDisp OOB")
        return _data_out_of_bounds("milkDisp")

    if(serial != _serial):
        post_data = {'utc': int(time.time()),
                     'request': 'Make me a Coffee',
                     'type': _settings,
                     'serial number': _serial}
        print("Returning because serial error")
        return jsonify(post_data), 400
    else:
        print("UART Command Trying to send")
        serial = SerialUART("/dev/ttyACM0")
        # Start the process
        # Demo
        # reply = serial.demo(content["waterTemp"], content["waterDisp"])
        # Final
        reply = serial.final(content["waterTemp"],
                            content["waterDisp"],
                            content["coffeeDisp"],
                            content["frothStr"],
                            content["milkDisp"])
        if "Demo started" not in reply:
            post_data = {'utc': int(time.time()),
                         'request': 'Make me a Coffee',
                         'type': _settings,
                         'reply': reply,
                         'serial number': _serial}
            return jsonify(post_data), 200
        else:   
            print(reply)
            post_data = {'utc': int(time.time()),
                         'request': 'Make me a Coffee',
                         'type': _settings,
                         'reply': reply,
                         'serial number': _serial}
            return jsonify(post_data), 201


def _make_bad_data_return(reason):
    return jsonify({"results": "Body must include {}".format(reason)}), 400


def _data_out_of_bounds(reason):
    return jsonify({"results": "{} is not in the correct bounds".format(reason)}), 400


if __name__ == '__main__':
    # Running in 0.0.0.0 will trust all other IP's.
    # Connecting to this machine is YOUR COMPUTER IP on port 5000
    app.run(host='0.0.0.0')
