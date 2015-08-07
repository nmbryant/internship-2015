###Verizon Cloud CTO Team
#DroneSim Android App

=======

###Getting Started

##Configuring to work with drone simulation
1. Open up DroneSim/app/src/main/res/values/strings.xml
2. Set 'config_dweet_name' to the config Dweet channel that is used by the simulation
3. Set 'controller_dweet_name' to the Dweet channel that is used by the DweetController in the simulation

##Starting the simulation
1. Start Master.py from the simulation folder
(See the dweet/DroneSim-Python README for more info)

##Starting the app
1. Use an Android emulator or an actual device to run the app
NOTE: The 'Test Map' game mode will not work on an emulator

###Modes

##LaunchDrone
1. Used to launch multiple simulated drones in various chosen locations
2. After a drone is launched it can be selected to view some of its data

##City Demo
1. Used to launch multiple simulated drones in locations specified by the various buttons
2. Indicates the status of the simulated drone for the particular locations

##Use Case Games
1. Gameification project for the innovation center.
2. Users can select a use case to launch a drone for.
3. After choosing a use case, a tutorial screen is displayed that explains the mini game for the use case and its various components
4. After launching a simulated drone, brings up a map and control with the objective varying from game to game
5. Once destination is reached, results screen is displayed that describes the use case with a brief description
6. Keeps track of which use cases have been completed with a plane icon and displays a completion screen once all use cases are finished

##Choose Existing
1. Allows user to type in a Dweet.io thing and displays the data from that device
2. Compatible with locks and keys
