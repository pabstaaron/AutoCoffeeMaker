from flask import Flask
from flask import jsonify
from flask import request
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

    if "pressure" not in content:
        return _make_bad_data_return("pressure")
    elif content["pressure"] < 0:
        return _data_out_of_bounds("pressure")

    if "temperature" not in content:
        return _make_bad_data_return("temperature")
    elif content["temperature"] < 0:
        return _data_out_of_bounds("temperature")

    if(serial != _serial):
        post_data = {'utc': int(time.time()),
                     'request': 'Make me a Coffee',
                     'type': _settings,
                     'serial number': _serial}
        return jsonify(post_data), 400
    else:
        post_data = {'utc': int(time.time()),
                     'request': 'Make me a Coffee',
                     'type': _settings,
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
