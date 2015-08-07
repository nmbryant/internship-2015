#! /usr/bin/python

####################-----------------------------------####################
#
#	Author: David Gallant, Cloud CTO Team
#
####################-----------------------------------####################

import json
import datetime
import dateutil.parser
from pytz import reference
import time
import dweepy
import uuid
import os.path
import traceback
import logging
import collections

from cassandra.cluster import Cluster


class DweetMonitor:
    active_drones = {}

    def __init__(self, configfile):
        # Initialize the config json file path
        here = os.path.dirname(__file__)
        filename = configfile
        self.config_file = os.path.join(here, filename)

        json_data = open(self.config_file)
        config = json.load(json_data)
        self._cassandra_host_names = config["cassandra_host_names"]
        self._cassandra_host_port = config["cassandra_host_port"]
        self._cassandra_keyspace = config["cassandra_keyspace"]
#        self._cassandra_session = None

        self._bookmark_file = config["bookmark_file"]

        self._sleep_time = config["sleep_time"]
        self._sleep_incr = config["sleep_incr"]

        self._dweet_controller = config["dweet_controller"]

        logfile = config["log_file"]
        loglevel = str(config["log_level"]).upper()

        logging.basicConfig(filename=logfile, level=loglevel)

    def _now(self):
        today = datetime.datetime.now()
        localtime = reference.LocalTimezone()  # used to pull out the local timezone of the device

        return today.strftime('%Y-%m-%d %H:%M:%S ') + localtime.tzname(today)

    # Gets all dweets, but discards dweets that it's already seen based on last time stamp
    # Returns true if any dweets were stored, false if no new dweets were stored
    def _get_all_dweets(self, dweet_thing):
        dweets = dweepy.get_dweets_for(dweet_thing, '')
        last_create_date = dateutil.parser.parse('1900-01-01 0:0:0Z')

        stored_dweets = False

        for dweet in dweets:
            print "Monitor dweet = " + str(dweet)
            createdate = dateutil.parser.parse(dweet['created'])

            if createdate > self.active_drones[dweet_thing]:
                thing_uuid = uuid.uuid1()
                logging.debug('Storing to Cassandra: ' + str(thing_uuid))
                try:

                    cassandra_command = self.build_cassandra_command(dweet, thing_uuid, createdate)
                    self._cassandra_session.execute(
                        cassandra_command.command, cassandra_command.values)
                    stored_dweets = True
                except Exception, e:
                    traceback.print_exc(e)
                    logging.error(str(e))
                    logging.error("Cassandra: Failed executing INSERT.")

            if createdate > last_create_date:
                last_create_date = createdate

        self.active_drones[dweet_thing] = last_create_date
        return stored_dweets

    def build_cassandra_command(self, dweet_to_store, thing_uuid, createdate):
        command = "INSERT INTO mykeyspace.ovi (rowkey, thing"
        command_values = " VALUES (%s, %s, %s"
        content_json_array = dweet_to_store['content']
        values_array = [str(thing_uuid), str(dweet_to_store['thing'])]
        for key, value in content_json_array.items():
            command += ", " + str(key)
            values_array.append(str(value))
            command_values += ", %s"
        command += ", ts)"
        command_values += ")"
        values_array.append(createdate.__str__())
        final_command = command + command_values
        command_and_values = collections.namedtuple('Command', ['command', 'values'])
        c = command_and_values(final_command, values_array)
        return c

    def get_all_controller_dweets(self):
        dweets = dweepy.get_dweets_for(self._dweet_controller, '')
        last_create_date = dateutil.parser.parse('1900-01-01 0:0:0Z')
        print dweets

        for dweet in dweets:
            create_date = dateutil.parser.parse(dweet['created'])
            if create_date > self._last_dweet:
                # Extract the relevant data from the dweet
                dweet_content = dweet["content"]
                is_launch = dweet_content["is_launch"]
                drone_name = dweet_content["drone_name"]

                # If the dweet says that it is a drone launch and the drone is not currently being followed by the
                # receiver and the drone has not been already deactivated by the receiver, add it to list of drones
                if is_launch and drone_name not in self.active_drones:
                    self.active_drones[drone_name] = dateutil.parser.parse('1900-01-01 0:0:0Z')
                    logging.debug("Monitor adding drone")
                    print "monitor adding drone"

                # If the dweet says that it is not a launch (a disable) and the drone is currently being followed by
                # the receiver, remove it from the list of drones being followed
                elif not is_launch and drone_name in self.active_drones:  # Remove this drone from the following list
                    self.active_drones.pop(drone_name)
                    logging.debug("Monitor removing drone")
                    print "monitor removing drone"

            if create_date > last_create_date:
                last_create_date = create_date

        # persist the last read dweet by writing the oldest create data from all read dweets
        bkfile = open(self._bookmark_file, 'w')
        bkfile.write('{ "last_dweet" : "' + str(last_create_date) + '" }')
        self._last_dweet = last_create_date

    def start(self):
        logging.info(self._now() + "\n***************************************\nStarting Dweet Monitor\n")

        logging.debug(
            "Connecting to Cassandra on " + str(self._cassandra_host_names) + " / port: " + self._cassandra_host_port)
        try:
            cluster = Cluster(self._cassandra_host_names, port=int(self._cassandra_host_port))
            self._cassandra_session = cluster.connect(self._cassandra_keyspace)

        except Exception, e:
            logging.error(e)
            logging.error("Connecting to Cassandra failed.")

        latest_controller_dweet = dweepy.get_latest_dweet_for(self._dweet_controller)
        self._last_dweet = dateutil.parser.parse(latest_controller_dweet[0]["created"])
        print self._last_dweet
        # start with max sleep, if data is found, checks are increased to more often until data stops
        sleep_time = self._sleep_time

        while True:
            time.sleep(5)
            logging.debug("Checking for new Dweets from Controller...")
            self.get_all_controller_dweets()
            logging.debug("Checking for new Dweets from drones...")
            for drone in self.active_drones.keys():
                self._get_all_dweets(drone)

    def test(self):
        latest_controller_dweet = dweepy.get_latest_dweet_for(self._dweet_controller)
        self._last_dweet = dateutil.parser.parse(latest_controller_dweet[0]["created"])
        print self._last_dweet

        while True:
            self.get_all_controller_dweets()
            time.sleep(5)
