import json


def grab_type():
    return "computer"
    # with open('/home/pi/Desktop/AutoCoffeeMaker/flask_server/setup.json') as json_data:
    #     data = json.load(json_data)
    #     return data["setting"]


def grab_serial():
    # return "beanster1"
    with open('/home/pi/Documents/Project/AutoCoffeeMaker/flask_server/setup.json') as json_data:
        data = json.load(json_data)
        return data["serial"]
