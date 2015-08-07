###Verizon Cloud CTO Team
#Drone Simulation

=======

###Configuring the Drone Simulation
1. config.json
  1. Used to configure the DweetMonitor
  2. Contains the Cassandra database configuration
  3. Reads in the name of the DweetController that it will listen to in order to get launched drone dweet names
2. dweet_config.json
  1. Used to set the dweet thing names for the different components of the system
  2. drone_config - This is the config Dweet.io channel that the DweetController will listen to that tells it when to launch/disable drones
  3. drone_name - This is the base name that the simulated drones will have on Dweet.io
  4. dweet_controller - This is the Dweet.io channel that the Controller will send dweets to when it has launched or disabled drones
  5. dweet_receiver - This is the Dweet.io channel that the DweetReceiver will send out the aggregated data

###Running the Drone Simulation
1. Run Master.py - Run the command 'python Master.py'
2. Accessing logs - Logs are outputted to dronesim.log
