
SETUP:
    To run the flask server you need to install

    Python 3 (3.5.2). Make sure you add Python to %PATH% on install
    Using the defualt python package manager (pip) install flask, and pyserial
        - If pip is not available as a command, it means pip is not in your system path

    pip install Flask
    pip install pyserial

RUN:
    To run the program we use python app.py
    The console will stay active and display DEBUG information about any calls recieved.
    To contact the server it will automatically use the port on 127.0.0.1:5000 for the device
    that you are running the server on. (i.e. if the computer is running the server,
    use 127.0.0.1:5000 to contact with the server. This does not change from network to network)
    If trying to run on a seperate machine you use the network IP at port 5000.


