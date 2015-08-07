__author__ = 'V646078'

import urllib
import urllib2
import time
import random
import threading
import math
import pyowm
import json
import sys
import logging
import traceback
import os
import geopy
import simples3
import collections
import textwrap

'''SimulatedDrone sends out a dweet every 5 seconds that contains the number of dweets it has sent, in addition to a
randomly generated integer
'''

# Photo number constants
FIRST_HOUSTON_PHOTO_NUM = 1
FINAL_HOUSTON_PHOTO_NUM = 442
FIRST_BUFFALO_PHOTO_NUM = 11
FINAL_BUFFALO_PHOTO_NUM = 179
FIRST_INSURANCE_PHOTO_NUM = 1
FINAL_INSURANCE_PHOTO_NUM = 539
FIRST_KANSAS_PHOTO_NUM = 1
FINAL_KANSAS_PHOTO_NUM = 89
FIRST_ENERGY_PHOTO_NUM = 1
FINAL_ENERGY_PHOTO_NUM = 186
FIRST_MEDIA_PHOTO_NUM = 22
FINAL_MEDIA_PHOTO_NUM = 177
FIRST_WILDFIRE_PHOTO_NUM = 1
FINAL_WILDFIRE_PHOTO_NUM = 434

# Bounding box constants
MAX_LAT_DISTANCE_FROM_START = 0.01
MAX_LONG_DISTANCE_FROM_START = 0.01
DRONE_TURN_DEGREES = 15

# Strings that correspond to to use cases
GOVERNMENT_USE_CASE_STRINGS = ["Government", "Wildfire", "Anchorage"]
MEDIA_USE_CASE_STRINGS = ["Media", "Boston"]
HEALTHCARE_USE_CASE_STRINGS = ["Healthcare", "Houston"]
ENERGY_USE_CASE_STRINGS = ["Energy", "Honolulu"]
INSURANCE_USE_CASE_STRINGS = ["Insurance", "NYC"]
AGRICULTURE_USE_CASE_STRINGS = ["Agriculture", "Wildlife"]

class SimulatedDrone(threading.Thread):
    # File name for the photos json file
    photo_json_file = 'photo_urls.json'

    # Dweet lock and key
    lock = '3o0Kvs1RWmyDEEpNrmq457'
    key = '1PInDCfL8BoMTUzO7pdNmZ'

    # Number of dweets the drone has sent
    number_of_dweets = 0

    # Update the weather according to this interval
    update_weather_counter = 5
    update_weather_interval = 5

    # Battery power
    battery_power = random.randint(55, 100)

    # Address values
    address = ''

    # Speed values
    current_speed = 40
    average_speed = 40

    # The interval (in seconds) that the simulated drone dweets
    dweet_interval = 1
    current_photo_num = 1

    # Generate a random heading (0-360 degrees)
    current_heading = random.randint(0, 360)

    # Generate a random latitude
    latitude = random.uniform(-90.0, 90.0)

    # Generate a random longitude
    longitude = random.uniform(-180.0, 180.0)
    running = True

    # Boolean for whether the drone is connected to the internet
    is_connected = True
    lose_connection_counter = random.randint(2, 7)
    gain_connection_counter = 0

    # Signal strength
    signal_rssi = random.randint(-75, 0)

    is_startup = True

    def __init__(self, name="unnamed", uuid=00000, latitude=0, longitude=0, countryName="unnamed_country", is_game=False):
        threading.Thread.__init__(self)
        self.name = name
        self.drone_uuid = uuid
        self.latitude = latitude
        self.longitude = longitude
        self.countryName = countryName
        logging.debug("Drone starting in " + countryName)
        self.weather = self.update_weather()
        self.is_game = is_game
        try:
            self.update_address()
        except:
            self.address = "No address"

        # Initialize the photo json file
        here = os.path.dirname(__file__)
        filename = self.photo_json_file
        self.photo_json_file = os.path.join(here, filename)
        if countryName in AGRICULTURE_USE_CASE_STRINGS:
            self.current_photo_num = FIRST_BUFFALO_PHOTO_NUM
        if countryName in MEDIA_USE_CASE_STRINGS:
            self.current_photo_num = FIRST_MEDIA_PHOTO_NUM

        # Initialize the config json file path
        self.here = os.path.dirname(__file__)

        # Calculate the max/min latitudes and longitudes
        self.max_latitude = latitude + MAX_LAT_DISTANCE_FROM_START
        self.min_latitude = latitude - MAX_LAT_DISTANCE_FROM_START
        self.max_longitude = longitude + MAX_LONG_DISTANCE_FROM_START
        self.min_longitude = longitude - MAX_LONG_DISTANCE_FROM_START
        self.is_turning = False

        # Save the starting latitude/longitude and staring picture number
        self.starting_lat = latitude
        self.starting_long = longitude
        self.starting_photo_num = self.current_photo_num

        # Used to keep track of the last game dweet it handled
        self.game_dweet_bookmark = ""

    def take_photo(self, country=None):  # MK
        # photos = json.load(open(self.photo_json_file))
        # return photos[country][str(random.randint(0, len(photos[country]) - 1))]
        # return 'https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg'
        url = "<img src=\"https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg\"/src>"

        # Healthcare use case and Houston location photos
        if country in HEALTHCARE_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_HOUSTON_PHOTO_NUM:
                self.current_photo_num = FIRST_HOUSTON_PHOTO_NUM
            photo = "houston-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Kansas location photos
        elif country == "Kansas":
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_KANSAS_PHOTO_NUM:
                self.current_photo_num = FIRST_KANSAS_PHOTO_NUM
            photo = "agriculture-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Agriculture use case and wildlife location photos
        elif country in AGRICULTURE_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_BUFFALO_PHOTO_NUM:
                self.current_photo_num = FIRST_BUFFALO_PHOTO_NUM
            photo = "buffalo-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Insurance use case and NYC location photos
        elif country in INSURANCE_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_INSURANCE_PHOTO_NUM:
                self.current_photo_num = FIRST_INSURANCE_PHOTO_NUM
            photo = "fire-damage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Energy use case and Honolulu location photos
        elif country in ENERGY_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_ENERGY_PHOTO_NUM:
                self.current_photo_num = FIRST_ENERGY_PHOTO_NUM
            photo = "energy-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Media use case and Boston location photos
        elif country in MEDIA_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_MEDIA_PHOTO_NUM:
                self.current_photo_num = FIRST_MEDIA_PHOTO_NUM
            photo = "media-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        # Government use case and wildfire location photos
        elif country in GOVERNMENT_USE_CASE_STRINGS:
            # If it reaches the final photo for the use case, loop back to first photo
            if self.current_photo_num > FINAL_WILDFIRE_PHOTO_NUM:
                self.current_photo_num = FIRST_WILDFIRE_PHOTO_NUM
            photo = "wildfire-footage-pics/out" + str(self.current_photo_num) + ".png"
            photo_path = os.path.join(self.here, photo)
            self.current_photo_num += self.dweet_interval
            url = self.store_photo(photo_path)
            self.dweet_photo_data(photo_path)

        print url
        return url

    # Stores a photo to S3 and returns a link
    def store_photo(self, photo):
        # Put the image into S3
        try:
            s = simples3.S3Bucket('pitemplates', access_key='3w35h2nstrhbnyraunaf8f3e1335a78e',
                                  secret_key='cqsrgp5aznd7dfmpikcfo7gzxn4ocx7hkwswepf2nl2jllgahpcgpcos6ykcaoav',
                                  base_url='https://storage-iad3a.cloud.verizon.com/pitemplates')
            photo_file = open(photo, "rb")
            photo_path = "OVIpics/" + str(self.countryName) + "/photo-" + str(self.current_photo_num) + ".png"
            s.put(key=photo_path,
                  data=photo_file.read(), mimetype="image/png")
            url = s.make_url_authed(key=photo_path, expire=1467915870)
            url = "<img src=\"" + url + "\"/src>"
            return url
        except:
            url = "<img src=\"https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg\"/src>"
            return url

    # Function to use to send out an encoded picture in pieces
    def dweet_photo_data(self, photo_path):
        dweet_name = self.name + "_photo"
        encoded = urllib.quote(open(photo_path, "rb").read().encode("base64"))
        uri = "data:image/png;base64," + str(encoded)
        string_array = textwrap.wrap(uri, 9900)
        for i in range(0, len(string_array)):
            json = {
                'photo': string_array[i]
            }
            photo_dweet_thing = dweet_name + str(i)
            self.send_dweet(photo_dweet_thing, json)

    # Updates the address based on the current coordinates of the drone
    def update_address(self):
        try:
            geolocator = geopy.Nominatim()
            location_string = "%s, %s" % (str(self.latitude), str(self.longitude))
            location = geolocator.reverse(location_string)
            self.address = location.address
            # Check to see if the address can be encoded
            urllib.urlencode(self.address)
        except:
            self.address = ""
            logging.error("Caught exception updating address, keeping previous address")
            logging.error(traceback.format_exc())

    # Updates the weather with pyowm
    def update_weather(self):
        logging.debug(self.name + " is calling update weather")
        try:
            new_weather = {}
            # Get the temperature based on drone launch coordinates
            api_key = '158e3cf88c110b5b901a4b9788e012a1'
            owm = pyowm.OWM(api_key)
            current_weather = owm.weather_at_coords(self.latitude, self.longitude)
            obs = current_weather.get_weather()
            new_weather["wind_speed"] = str(obs.get_wind()['speed'])
            new_weather["wind_deg"] = str(obs.get_wind()['deg'])
            new_weather["humidity"] = str(obs.get_humidity())
            new_weather["temp"] = str(obs.get_temperature('fahrenheit')['temp'])
            return new_weather
        except KeyboardInterrupt:
            sys.exit("Keyboard Interrupt")
        except:
            logging.error("Caught exception getting weather")
            logging.error(traceback.format_exc())

    # Sends out a dweet with all of the drone data
    def raw_dweet(self, content, is_activated, speed, heading, current_lat, current_long, battery,
                  weather,
                  photo_url="https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg"):  # MK
        # Dweet as an object called 'python test', with 'hello world'
        number = random.randint(0, 100)
        values = {
            'content': content,
            'photo_url': photo_url,
            'random_number': str(number),
            'is_activated': str(is_activated),
            'speed': str(speed),
            'heading': str(heading),
            'country': self.countryName,
            'latitude': current_lat,
            'longitude': current_long,
            'battery': battery,
            'weather': weather,
            'is_connected': self.is_connected,
            'signal_strength': self.signal_rssi,
            'address': self.address
        }

        self.send_dweet(self.name, values)

    # Sends out a dweet as thing_name with values as the payload
    def send_dweet(self, thing_name, values):
        url = 'https://dweet.io/dweet/for/' + thing_name  # + '?key=' + self.key

        # Encode the dweet content
        data = urllib.urlencode(values)

        # Make the post request
        req = urllib2.Request(url, data)

        # Acquire the response from Dweet.io
        response = urllib2.urlopen(req)

    # Locks the drone with the given lock
    def lock_dweet_object(self, dweet_lock):
        # This is the locking url, uses the dweet_name for this object
        url = 'https://dweet.io/lock/' + self.name + '?lock=' + self.lock + '&key=' + self.key

        # Make the get request
        req = urllib2.Request(url)

        # Acquire the response from Dweet.io
        response = urllib2.urlopen(req)

        # Get the content of the response and print it
        the_page = response.read()
        # print the_page

    # When called, the drone stops running
    def turn_off_client(self):
        logging.debug("Drone dweeting shut down")
        self.running = False

    def start_drone(self):
        # On exit, dweet that the drone is deactivated
        # atexit.register(self.turn_off_client)

        # On start up, lock this object
        # self.lock_dweet_object(self.lock)

        # Dweet every 5 seconds
        while self.running:
            try:
                time.sleep(self.dweet_interval)

                # Radius of the Earth
                R = 6378.1

                # Distance travelled since last dweet
                d = float(self.current_speed * (5.0 / 60.0 / 60.0))

                # Convert heading to radians for calculations
                bearing = math.radians(self.current_heading)

                # Current lat point converted to radians
                lat1 = math.radians(self.latitude)

                # Current long point converted to radians
                lon1 = math.radians(self.longitude)

                # Calculate new latitude in radians
                lat2 = math.asin(
                    math.sin(lat1) * math.cos(d / R) + math.cos(lat1) * math.sin(d / R) * math.cos(bearing))

                # Calculate new longitude in radians
                lon2 = lon1 + math.atan2(math.sin(bearing) * math.sin(d / R) * math.cos(lat1),
                                         math.cos(d / R) - math.sin(lat1) * math.sin(lat2))

                # If the drone is launched from the game, get the coordinates from the game dweet thing
                if self.is_game:
                    game_coords = self.get_game_coordinates()
                    if game_coords is not None:
                        self.latitude = game_coords.lat
                        self.longitude = game_coords.long
                # Set latitude and longitude to the new calculated ones converted to degrees if it is not a game drone
                else:
                    self.latitude = math.degrees(lat2)
                    self.longitude = math.degrees(lon2)

                # Adjust the speed
                if self.current_speed < self.average_speed:
                    self.current_speed += 5
                else:
                    self.current_speed += random.randint(-2, 2)

                # Adjust the battery level
                self.battery_power -= 0.25

                # Adjust the signal strength
                add_or_subtract = random.randint(0, 1)
                rand_value = random.randint(1, 30)
                if add_or_subtract == 0:
                    self.signal_rssi += rand_value
                elif add_or_subtract == 1:
                    self.signal_rssi -= rand_value

                if self.signal_rssi >= 0:
                    self.signal_rssi = 0
                    rand_value = random.randint(20, 30)
                    self.signal_rssi -= rand_value
                elif self.signal_rssi <= -100:
                    self.signal_rssi = -100
                    rand_value = random.randint(20, 30)
                    self.signal_rssi += rand_value

                # Update connectivity
                if self.is_connected:
                    self.lose_connection_counter -= 1
                    if self.lose_connection_counter == 0:
                        self.is_connected = False
                        self.gain_connection_counter = random.randint(2, 7)
                else:
                    self.gain_connection_counter -= 1
                    if self.gain_connection_counter == 0:
                        self.is_connected = True
                        self.lose_connection_counter = random.randint(2, 7)

                # Update the weather data
                if self.update_weather_counter == self.update_weather_interval:
                    self.weather = self.update_weather()  # MK
                    self.update_weather_counter = 0
                else:
                    self.update_weather_counter += 1

                # Update the location/address
                try:
                    self.update_address()
                except:
                    self.address = "No address"

                # Adjust the heading
                if not self.is_turning:
                    # If the drone goes outside of the bounding box, start turning
                    if self.latitude > self.max_latitude or self.latitude < self.min_latitude or self.longitude > \
                            self.max_longitude or self.longitude < self.min_longitude:
                        self.is_turning = True
                        self.current_heading += DRONE_TURN_DEGREES
                    # If the drone is inside the box, continue going straight (with some slight variation in heading)
                    else:
                        self.current_heading += random.randint(-5, 5)
                else:
                    # If the drone was turning and is now within the bounding box, stop turning and go straight
                    if self.min_latitude <= self.latitude <= self.max_latitude and \
                            self.min_longitude <= self.longitude <= self.max_longitude:
                        print "Drone completed turning"
                        self.is_turning = False
                    # If the drone is still turning (it hasn't gone back inside the bounding box) add the turn degrees
                    # to the heading
                    else:
                        print "Drone still turning"
                        self.current_heading += DRONE_TURN_DEGREES

                # If the heading goes over 360 degress, subtract 360 since it will be the same angle
                if self.current_heading >= 360:
                    self.current_heading -= 360
                # If the heading goes below 0 degrees, add 360 since the result will be the same angle
                elif self.current_heading < 0:
                    self.current_heading += 360

                dweet_string = "Dweet %s!" % str(self.number_of_dweets)
                self.raw_dweet(content=dweet_string, is_activated=1, speed=self.current_speed,
                               heading=self.current_heading, current_lat=self.latitude, current_long=self.longitude,
                               battery=self.battery_power, weather=self.weather,
                               photo_url=self.take_photo(country=self.countryName))  # MK
                self.number_of_dweets += 1

            except KeyboardInterrupt:
                sys.exit("Keyboard Interrupt")

            except Exception, e:
                print "Caught exception in drone loop"
                print traceback.print_exc(e)
                logging.error("Caught exception in drone loop")
                logging.error(traceback.format_exc())

        # Dweet out shutdown message
        self.raw_dweet("Drone has landed!", 0, 0, 0, self.latitude, self.longitude, 0, self.weather,
                       "<img src=\"https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg\"/src>")
        logging.debug("Drone has stopped")

    def run(self):
        print "Starting drone"
        logging.debug("Starting drone")
        # Dweet about starting up
        try:
            self.raw_dweet("Starting drone", 1, self.current_speed, self.current_heading, self.latitude, self.longitude,
                           self.battery_power, self.weather, "<img src=\"https://upload.wikimedia.org/wikipedia/"
                                                             "commons/a/ac/No_image_available.svg\"/src>")
        except:
            print "Caught exception starting drone"
            logging.error("Caught exception starting drone")
            logging.error(traceback.format_exc())

        self.start_drone()

    def get_uuid(self):
        return self.drone_uuid

    def get_dweet_name(self):
        return self.name

    # Gets the data that was dweeted by the app for this particular drone
    # Used to get the location that the user moved the drone to with the app
    def get_game_coordinates(self):
        game_dweet_name = self.name + "_game"
        try:
            latest_game_dweet = self.get_latest_dweet(game_dweet_name)

            # If the game dweet has already been handled, ignore it
            if latest_game_dweet == self.game_dweet_bookmark:
                return
            # Otherwise, save the current game dweet in the bookmark
            else:
                self.game_dweet_bookmark = latest_game_dweet

            json_dweet = json.loads(latest_game_dweet)
            is_restart = bool(json_dweet["with"][0]["content"]["is_restart"])
            coordinates = collections.namedtuple('Coordinates', ['lat', 'long'])
            # If the restart boolean is false, use the lat/lon in the game dweet to move the drone
            if not is_restart:
                lat_string = json_dweet["with"][0]["content"]["lat"]
                long_string = json_dweet["with"][0]["content"]["long"]
                lat = float(lat_string)
                lon = float(long_string)
                coordinates_tuple = coordinates(lat, lon)
                return coordinates_tuple
            # If the restart boolean is true, set the lat/lon to the starting coordinates and reset the photo number
            else:
                lat = self.starting_lat
                lon = self.starting_long
                coordinates_tuple = coordinates(lat, lon)
                self.current_photo_num = self.starting_photo_num
                return coordinates_tuple

        except:
            print "Caught exception getting game coordinates"
            logging.error("Error getting dweet from game dweet thing")
            logging.error(traceback.format_exc())
            return None

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

# Testing code
# drone1 = SimulatedDrone(name="example-name", uuid=11111, latitude=32, longitude=32, countryName="United States")
# drone1.start_drone()
