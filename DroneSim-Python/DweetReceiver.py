__author__ = 'V646078'

import urllib
import urllib2
import time
import json
import threading
import random
import traceback
import sys
import logging

'''DweetReceiver receives that dweets from DweetClient and sends out its own dweet in response
'''

class DweetReceiver(threading.Thread):
    client_key = '6Bbobgpx6iNdOwE0mJkwP1'

    '''Dictionary for storing the active drone dweet names and maps them to the previous dweet that the receiver has
    received from that particular drone
    '''
    drone_names = {}

    # Dictionary that maps drone names to whether or not it is connected
    drone_to_connected = {}

    # Dictionary that maps drone names to their battery levels
    drone_to_battery = {}

    # Dictionary that maps drone names to a boolean stating if it is real drone or simulated
    drone_to_is_real = {}

    # Weather data
    country = ""
    temp = 0
    humidity = 0
    wind_speed = 0
    wind_deg = 0

    controller_dweet_name = "DweetController"

    first_controller_dweet = ''

    connected_drones = 0
    disconnected_drones = 0

    high_battery_drones = 0
    low_battery_drones = 0

    current_pic = ''

    def __init__(self, name, controller_dweet_name):
        threading.Thread.__init__(self)
        self.name = name
        self.controller_dweet_name = controller_dweet_name

    # Get the latest dweet from the given object
    def get_latest_dweet(self, dweet_object):
        url = 'https://dweet.io/get/latest/dweet/for/' + dweet_object  # + '?key=' + self.client_key

        # Make the get request
        req = urllib2.Request(url)

        # Acquire the response from Dweet.io
        response = urllib2.urlopen(req)

        # Get the content of the response and print it
        the_page = response.read()
        return the_page

    def get_all_dweets(self, dweet_object):
        url = 'https://dweet.io/get/dweets/for/' + dweet_object

        # Make the get request
        req = urllib2.Request(url)

        # Acquire the response from Dweet.io
        response = urllib2.urlopen(req)

        # Get the content of the response and print it
        the_page = response.read()
        return the_page

    # Dweet out the given content
    def raw_dweet(self, content, connected_drones, disconnected_drones):
        # Dweet as an object called 'python test', with 'hello world'
        url = 'https://dweet.io/dweet/for/dweet_receiver'
        values = {
            'content': content,
            'drones': len(self.drone_names),
            'connected_drones': connected_drones,
            'disconnected_drones': disconnected_drones,
            'high_battery_drones': self.high_battery_drones,
            'low_battery_drones': self.low_battery_drones,
            'photo_url': self.current_pic,
            'country': self.country,
            'temp': self.temp,
            'humidity': self.humidity,
            'wind_speed': self.wind_speed,
            'wind_deg': self.wind_deg
        }

        # Encode the dweet content
        data = urllib.urlencode(values)

        # Make the post request
        req = urllib2.Request(url, data)

        # Acquire the response from Dweet.io
        response = urllib2.urlopen(req)

        # Get the content of the response and print out the dweeted message
        the_page = response.read()
        json_page = json.loads(the_page)
        # print json_page["with"]["content"]["content"]

    def start_receive(self):
        while True:
            logging.debug("Receiver starting loop")
            # Checks for new dweets from all the active drones every 5 seconds
            time.sleep(5)

            # First, check for dweets from the controller and add or remove drones that the receiver is following
            # accordingly
            try:
                self.get_controller_dweets()
            except KeyboardInterrupt:
                sys.exit("Keyboard Interrupt")
            except:
                print "DweetReceiver: Exception getting controller dweets"
                logging.error("DweetReceiver: Exception getting controller dweets")
                logging.error(traceback.format_exc())

            try:
                chosen_drone = 0
                i = 0
                if len(self.drone_names.keys()) > 0:
                    # Randomly choose a drone to take a picture and weather from
                    chosen_drone = random.randint(0, len(self.drone_names.keys()) - 1)
                    i = 0
                for drone in self.drone_names.keys():
                    # Gets the latest dweet from the drone
                    latest_dweet = self.get_latest_dweet(drone)

                    # If the latest dweet is different from the previous dweet received from that drone, take the data
                    if latest_dweet != self.drone_names[drone]:
                        # Get the relevant data from the latest dweet
                        json_dweet = json.loads(latest_dweet)

                        # If the drone is not real, get the simulated data
                        if not self.drone_to_is_real[drone]:
                            self.data_from_simulated(latest_dweet, json_dweet, i, chosen_drone, drone)

                        # If the drone is real, get data from a real drone
                        else:
                            self.data_from_real_device()

                    i += 1

                # Calculate the connected and disconnected drones
                self.calc_connected()

                # Calculate the battery levels of the drones
                self.calc_battery()

                # Send out a dweet
                dweet_string = "No drones"
                self.raw_dweet(dweet_string, self.connected_drones, self.disconnected_drones)

            except KeyboardInterrupt:
                sys.exit("Keyboard Interrupt")

            except Exception, e:
                print "DweetReceiver: Exception receiving data from drones"
                print traceback.print_exc(e)
                logging.error("DweetReceiver: Exception receiving data from drones")
                logging.error(traceback.print_exc())

    # Adds the dweet name for a drone to the receiver's list of drone names
    def add_drone(self, drone_name):
        self.drone_names[drone_name] = ' '

    # Determines the number of drones connected and drones disconnected based on the dictionary
    def calc_connected(self):
        self.connected_drones = 0
        self.disconnected_drones = 0
        for drone in self.drone_to_connected.keys():
            if self.drone_to_connected[drone] == 'True':
                self.connected_drones += 1
            else:
                self.disconnected_drones += 1

    # Determines the number of drones with high battery and number of drones with low battery
    def calc_battery(self):
        self.low_battery_drones = 0
        self.high_battery_drones = 0
        for drone in self.drone_to_battery.keys():
            if float(self.drone_to_battery[drone]) > 50:
                self.high_battery_drones += 1
            else:
                self.low_battery_drones += 1

    def data_from_real_device(self):
        return True

    def data_from_simulated(self, string_dweet, json_dweet, i, chosen_drone, drone):
        # Get the picture and weather from the drone if it matches the random number
        if i == chosen_drone:
            try:
                self.current_pic = json_dweet["with"][0]["content"]["photo_url"]
                weather_string = json_dweet["with"][0]["content"]["weather"]
                weather_string = weather_string.replace("'", "\"")
                json_weather = json.loads(weather_string)
                self.country = json_dweet["with"][0]["content"]["country"]
                self.temp = json_weather["temp"]
                self.humidity = json_weather["humidity"]
                self.wind_speed = json_weather["wind_speed"]
                self.wind_deg = json_weather["wind_deg"]
            except:
                logging.debug("Caught exception getting data from drone")

        # Check if the drone is connected to the internet
        is_connected = json_dweet["with"][0]["content"]["is_connected"]
        if is_connected == 'True':
            self.drone_to_connected[drone] = 'True'
        else:
            self.drone_to_connected[drone] = 'False'

        # Save the drone's battery levels
        self.drone_to_battery[drone] = json_dweet["with"][0]["content"]["battery"]

        # Set the previous dweet for the drone in the dictionary to the dweet just received
        self.drone_names[drone] = string_dweet

    def get_controller_dweets(self):
        # Get all the dweets from the controller
        all_dweets_response = self.get_all_dweets(self.controller_dweet_name)
        all_dweets_json = json.loads(all_dweets_response)
        controller_dweets = all_dweets_json["with"]
        dweet_bookmark = controller_dweets[0]

        for dweet in controller_dweets:
            # If the dweet is the first one that the controller had on startup, break from the loop since all
            # dweets past this were from a previous run of the application
            if str(dweet) == str(self.first_controller_dweet):
                break

            # Extract the relevant data from the dweet
            dweet_content = dweet["content"]
            is_launch = dweet_content["is_launch"]
            drone_name = dweet_content["drone_name"]
            is_real_boolean = bool(dweet_content["is_real"])

            # If the dweet says that it is a drone launch and the drone is not currently being followed by the
            # receiver and the drone has not been already deactivated by the receiver, add it to list of drones
            if is_launch and drone_name not in self.drone_names and drone_name:
                self.drone_names[drone_name] = ' '
                self.drone_to_is_real[drone_name] = is_real_boolean
                logging.debug("Receiver adding drone")

            # If the dweet says that it is not a launch (a disable) and the drone is currently being followed by
            # the receiver, remove it from the list of drones being followed
            elif not is_launch and drone_name in self.drone_names:
                logging.debug("Receiver removing drone")
                # Remove this drone from the following list
                if self.drone_names.has_key(drone_name):
                    self.drone_names.pop(drone_name)

                # Remove this drone from the connected dictionary
                if self.drone_to_connected.has_key(drone_name):
                    self.drone_to_connected.pop(drone_name)

                # Remove this drone from the battery dictionary
                if self.drone_to_battery.has_key(drone_name):
                    self.drone_to_battery.pop(drone_name)

                # Remove the drone from the is real dictionary
                if self.drone_to_is_real.has_key(drone_name):
                    self.drone_to_is_real.keys().remove(drone_name)

        # Set the first controller dweet to the latest dweet received from the controller
        self.first_controller_dweet = dweet_bookmark

        # If there are no drones active, the following for loop will not be entered so we need to send a
        # dweet that says there are 0 active drones
        if len(self.drone_names) == 0:
            self.connected_drones = 0
            self.disconnected_drones = 0
            dweet_string = "No drones"
            self.raw_dweet(dweet_string, 0, 0)
            logging.debug("Receiver sent default dweet since there are no drones")

    # Called when the thread starts
    def run(self):
        print "Starting receiver"
        logging.debug("Starting receiver")
        caught_exception = True
        while caught_exception:
            try:
                # The receiver will ignore all controller dweets that happened before the receiver started, so it needs
                # to get the latest dweet from the controller
                self.first_controller_dweet = self.get_latest_dweet(self.controller_dweet_name)
                first_controller_json = json.loads(self.first_controller_dweet)
                self.first_controller_dweet = first_controller_json["with"][0]
                caught_exception = False
            except:
                print "Caught exception starting receiver"
                caught_exception = True
                logging.error("Receiver caught exception starting up")
                logging.error(traceback.format_exc())

        try:
            # The receiver dweets on start up
            self.raw_dweet("Starting up", 0, 0)
        except:
            print "Receiver unable to dweet at start up"
            logging.error("Receiver unable to dweet at start up")
            logging.error(traceback.format_exc())

        # Start the receiver
        self.start_receive()
