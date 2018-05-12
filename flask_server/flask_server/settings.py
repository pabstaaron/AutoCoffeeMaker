import json


def grab_type():
    with open('../setup.json') as json_data:
        data = json.load(json_data)
        return data["setting"]


def grab_serial():
    with open('../setup.json') as json_data:
        data = json.load(json_data)
        return data["serial"]
