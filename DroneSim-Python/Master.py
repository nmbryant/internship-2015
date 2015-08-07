__author__ = 'V646078'

import DweetController
import DweetReceiver
import DweetMonitor
import os
import json

here = os.path.dirname(__file__)
filename = "dweet_config.json"
config_file = os.path.join(here, filename)

json_data = open(config_file)
config = json.load(json_data)

config_dweet_name = config["drone_config"]
drone_dweet_name = config["drone_name"]
controller_dweet_name = config["dweet_controller"]
receiver_dweet_name = config["dweet_receiver"]

# Create and start up the controller
controller = DweetController.DweetController(config_dweet_name, controller_dweet_name, drone_dweet_name)
controller.start()

# Create and start up the receiver
receiver = DweetReceiver.DweetReceiver(receiver_dweet_name, controller_dweet_name)
receiver.start()

# Create and start up the monitor
# monitor = DweetMonitor.DweetMonitor("config.json")
# monitor.start()
