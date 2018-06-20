import json


def grab_type():
    with open('/home/pi/Desktop/AutoCoffeeMaker/flask_server/setup.json') as json_data:
        data = json.load(json_data)
        return data["setting"]


def grab_serial():
    with open('/home/pi/Desktop/AutoCoffeeMaker/flask_server/setup.json') as json_data:
        data = json.load(json_data)
        return data["serial"]
