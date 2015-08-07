__author__ = 'V646078'

import SimulatedDrone
import threading
import time
import dweepy
import json
import urllib2
import sys
import logging
import traceback

class DweetController(threading.Thread):

    # Number of drones that have been started
    drones_started = 0

    # Available names
    available_names = []

    # An array of the threads started by the controller
    threads = []

    # An array of the drones started by the controller
    drones = []

    # Dictionary that maps drone ids to names
    drone_id_to_name = {}

    # Dweet name for the controller
    controller_dweet_name = 'DweetController'

    # Base name for simulated drones
    base_drone_dweet_name = 'dweet_client_'

    def __init__(self, config_dweet_thing, controller_dweet_name, base_drone_dweet_name):
        threading.Thread.__init__(self)
        self.config_dweet_thing = config_dweet_thing
        self.controller_dweet_name = controller_dweet_name
        self.base_drone_dweet_name = base_drone_dweet_name
        pass

    def start_drone(self, d_uuid, latitude, longitude, country, is_real, is_game):
        current_name = self.get_name_for_drone()
        drone_name = ""
        # If a name was chosen from the available name list, remove it from that list
        if current_name is not "":
            drone_name = current_name
            self.available_names.remove(current_name)
        # Otherwise, just use the number of drones started to make the name and increment this value
        else:
            drone_name = self.base_drone_dweet_name + str(self.drones_started + 1)
            self.drones_started += 1

        logging.debug("Starting drone with name = " + drone_name)

        # If the drone is not real, launch a simulated drone
        if not is_real:
            new_drone = SimulatedDrone.SimulatedDrone(drone_name, d_uuid, latitude, longitude, country, is_game)
            new_drone.start()
            self.threads.append(new_drone)
            self.drones.append(new_drone)

        return drone_name

    def get_name_for_drone(self):
        current_name = ""
        # Check available names list
        if len(self.available_names) > 0:
            current_lowest = -1
            # For each name in the name list get the number and choose the name that has the lowest number
            for name in self.available_names:
                split_name = name.split('_')
                client_number = split_name[2]
                if client_number < current_lowest or current_lowest == -1:
                    current_lowest = client_number
                    current_name = name
        return current_name

    def string_is_int(self, s):
        try:
            int(s)
            return True
        except ValueError:
            return False

    def stop_drone(self, drone_uuid):
        drone_client = None
        for drone in self.drones:
            if drone.drone_uuid == drone_uuid:
                print "Drone being turned off"
                drone_dweet = drone.get_dweet_name()
                self.available_names.append(drone_dweet)
                drone.turn_off_client()
                drone_client = drone
        if drone_client is not None:
            self.drones.remove(drone_client)

    # Get the latest dweet from the given object
    def get_latest_dweet(self, dweet_object):
        # Dweet as an object called 'python test', with 'hello world'
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

    def send_controller_dweet(self, is_launch, drone_id, latitude, longitude, country, is_real, is_game):
        # If the dweet says that it is a launch, start a new drone
        is_real_boolean = bool(is_real)
        is_game_boolean = bool(is_game)
        if is_launch:
            drone_name = self.start_drone(drone_id, latitude, longitude, country, is_real_boolean, is_game_boolean)
            dweet_to_app = {
                'content': "Drone Launched",
                'droneID': drone_id,
                'drone_name': drone_name,
                'is_launch': True,
                'country': country,
                'is_real': is_real_boolean,
                'is_game_boolean': is_game_boolean
            }
            dweepy.dweet_for(self.controller_dweet_name, dweet_to_app)
            logging.debug("Drone launched!")
            self.drone_id_to_name[drone_id] = drone_name
        # If the dweet says that it is not a launch, disable the drone with the dweeted UUID
        else:
            self.stop_drone(drone_id)
            dweet_to_app = {
                'content': "Drone disabled",
                'droneID': drone_id,
                'drone_name': self.drone_id_to_name[drone_id],
                'is_launch': False,
                'country': "N/A",
                'is_real': is_real_boolean
            }
            dweepy.dweet_for(self.controller_dweet_name, dweet_to_app)
            logging.debug("Drone stopped!")

    def run(self):
        logging.basicConfig(filename='dronesim.log', level=logging.ERROR, filemode='w')
        print "Starting controller"
        logging.debug("Starting controller")

        previous_config_dweet = self.get_latest_dweet(self.config_dweet_thing)
        first_config_json = json.loads(previous_config_dweet)

        if first_config_json["this"] != "failed":
            first_config_dweet = first_config_json["with"][0]
        else:
            first_config_dweet = "No dweet"

        while True:
            try:
                # Gather dweets from the config dweet thing (DroneConfig) every 2 seconds
                time.sleep(1)
                # Get the latest dweet from the config
                latest_dweets = self.get_all_dweets(self.config_dweet_thing)
                all_dweets_json = json.loads(latest_dweets)
                config_dweets = all_dweets_json["with"]
                dweet_bookmark = config_dweets[0]
                logging.debug("Dweet bookmark = " + str(dweet_bookmark))

                logging.debug("First config dweet = " + str(first_config_dweet))

                # If the latest dweet from config is different from the previous one, look at the data
                for dweet in config_dweets:
                    logging.debug("Current dweet = " + str(dweet))
                    if str(dweet) == str(first_config_dweet):
                        break
                    dweet_content = dweet["content"]
                    is_launch = dweet_content["is_launch"]
                    drone_id = dweet_content["droneID"]
                    latitude = dweet_content["launch_latitude"]
                    longitude = dweet_content["launch_longitude"]
                    country = dweet_content["country"]
                    is_real = dweet_content["is_real"]
                    is_game = dweet_content["is_game_drone"]
                    self.send_controller_dweet(is_launch, drone_id, latitude, longitude, country, is_real, is_game)
                first_config_dweet = dweet_bookmark
            except KeyboardInterrupt:
                sys.exit("Keyboard Interrupt")
            except:
                print "Caught exception running controller loop"
                logging.error("Caught exception running controller loop")
                logging.error(traceback.format_exc())
