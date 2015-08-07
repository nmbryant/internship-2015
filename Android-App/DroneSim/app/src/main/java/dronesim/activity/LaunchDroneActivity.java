package dronesim.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import dronesim.IDroneLauncher;
import dronesim.task.GatherDataTask;

import com.example.dronesim.R;

import dronesim.task.StartDroneTask;
import dronesim.task.StopDroneTask;

public class LaunchDroneActivity extends Activity implements IDroneLauncher{

    // The app sends out dweets for this dweet name
    private String dweetConfigName = "DroneConfig";

    // The UUID used for the drone that the app will launch
    private UUID droneUUID = UUID.randomUUID();

    // Hash map that maps drone names to their UUID
    private HashMap<String, UUID> dweetToUUIDMap = new HashMap<String, UUID>();

    // Hash map that maps a user given drone name to the dweet name
    private HashMap<String, String> usernameToDweetMap = new HashMap<String, String>();

    // The dweet thing name for the app's drone
    private String currentDroneDweet = "";
    private String currentDroneUsername = "";
    private UUID currentDroneUUID;

    // The AsyncTask that gathers data from the drone
    private GatherDataTask dataTask;
    private boolean isGatheringData = false;

    // Google Maps API key
    private String googleMapsKey = "AIzaSyACg_lKSG3GZBxScfEXBAonTWN8soW7j10";

    // True if the user currently has an active drone
    boolean hasLaunchedDrones = false;

    // Used to prevent the drone picker from firing a selection changed call when not wanted
    boolean isDronePickerActive = false;

    // True if a user has entered a name into the name edit text, false otherwise
    boolean hasDroneName = false;

    // True if a StartDroneTask is currently running, used to disable UI elements while that is happening
    boolean isDroneLaunching = false;

    // GUI components
    private Button launchButton;
    private Button disableButton;
    private Spinner countryPicker;
    private Spinner dronePicker;
    private TextView speedText;
    private TextView latText;
    private TextView longText;
    private TextView tempText;
    private TextView errorText;
    private EditText nameEditText;

    // List of dweet names for the drones that the user has launched
    ArrayList<String> droneUsernames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_launch);

        // Button to launch a drone
        launchButton = (Button) findViewById(R.id.launchButton);
        launchButton.setEnabled(false);

        // Button to disable a drone
        disableButton = (Button) findViewById(R.id.disableButton);
        disableButton.setEnabled(false);

        // Spinner to select a country to deploy the drone
        countryPicker = (Spinner) findViewById(R.id.countrySpinner);

        // Spinner to select a drone for viewing data
        dronePicker = (Spinner) findViewById(R.id.droneSpinner);
        initializeDroneSpinner();

        // Text view that displays the speed of the drone
        speedText = (TextView) findViewById(R.id.speedText);

        // Text view that displays the latitude of the drone
        latText = (TextView) findViewById(R.id.latitudeText);

        // Text view that displays the longitude of the drone
        longText = (TextView) findViewById(R.id.longitudeText);

        // Text view that displays the temperature from the drone
        tempText = (TextView) findViewById(R.id.tempText);

        // Edit text for the user to input a drone name
        nameEditText = (EditText) findViewById(R.id.droneNameEdit);

        // Text view to display error messages
        errorText = (TextView) findViewById(R.id.errorText);
        errorText.setVisibility(View.INVISIBLE);

        nameEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                enableLaunchButton();
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub

            }

        });

        dronePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedName = dronePicker.getSelectedItem().toString();
                // If the drone picker is active, the user has launched drones and the selection is different
                // from the drone currently being displayed, start a new gather data task and stop the old one
                if (hasLaunchedDrones && !selectedName.equals(currentDroneDweet) && isDronePickerActive) {

                    // Get the dweet name for the selected username
                    String selectedDweetName = usernameToDweetMap.get(selectedName);

                    // Set all the current data to match that of the selected drone
                    currentDroneDweet = selectedDweetName;
                    currentDroneUUID = dweetToUUIDMap.get(currentDroneDweet);
                    currentDroneUsername = selectedName;

                    // Stop the previous data gather task if one is running
                    if (isGatheringData) {
                        dataTask.stopTask(true);
                        isGatheringData = false;
                    }

                    // Notify the user that data is being gathered
                    loadingData();

                    // Start a new data gather task passing the selected dweet name
                    dataTask = new GatherDataTask(selectedDweetName, speedText, latText, longText, tempText);
                    dataTask.execute();
                    isGatheringData = true;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                Log.d("Drone Picker", "Nothing selected");
            }

        });

        launchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            // When the launch button is pressed, send out a dweet to start a drone
            public void onClick(View v) {

                // Since a drone is being launched, set the boolean to true
                isDroneLaunching = true;

                // Retrieve the name of the drone from the name edit text
                currentDroneUsername = nameEditText.getText().toString();

                // Clear the name entry field
                nameEditText.setText("");

                // Get a random UUID for this drone
                droneUUID = UUID.randomUUID();

                // In the error text view, notify the user that the drone is being launched
                errorText.setText("Launching drone...");
                errorText.setVisibility(View.VISIBLE);

                // Stop the data task if it is currently running
                if (isGatheringData) {
                    Log.d("Stopping Task", "Stopping data task");
                    dataTask.stopTask(true);
                    isGatheringData = false;
                }

                // Set the data labels to notify user of loading
                loadingData();

                // Get the country latitude and longitude based on the selection in the spinner
                int countryIndex = countryPicker.getSelectedItemPosition();
                String countryName = countryPicker.getSelectedItem().toString();
                float countryLatitude = Float.parseFloat(getResources().getStringArray(R.array.latitudes)[countryIndex]);
                float countryLongitude = Float.parseFloat(getResources().getStringArray(R.array.longitudes)[countryIndex]);

                // Run a start drone task
                StartDroneTask startTask = createStartTask(countryLatitude, countryLongitude, countryName);
            }
        });

        disableButton.setOnClickListener(new View.OnClickListener() {

            @Override
            // When the disable button is pressed, start a StopDroneTask
            public void onClick(View v) {

                // Disable the button until the task is finished
                disableButton.setEnabled(false);

                // Get a new UUID for the next drone the app starts
                droneUUID = UUID.randomUUID();
                boolean isLoading = !(droneUsernames.size() == 1);
                dataTask.stopTask(isLoading);
                isGatheringData = false;

                // Stop only the currently selected drone
                createStopTask();

                // Remove the disabled drone from the names list and the map
                droneUsernames.remove(currentDroneUsername);
                dweetToUUIDMap.remove(currentDroneDweet);
                usernameToDweetMap.remove(currentDroneUsername);

                // Update the drone spinner to remove the deactivated drone name
                updateDroneSpinner();

                // If there are no more active drones disable this button and reinitialize the drone
                // spinner
                if (droneUsernames.size() == 0) {
                    initializeDroneSpinner();
                    hasLaunchedDrones = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the drone spinner to include all of the launched drone names
     */
    private void updateDroneSpinner() {
        isDronePickerActive = false;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, droneUsernames);
        dronePicker.setAdapter(adapter);
        // Set the selection to match the current drone being displayed
        for (int i = 0; i < adapter.getCount(); i++) {
            if (dronePicker.getItemAtPosition(i).equals(currentDroneUsername)) {
                dronePicker.setSelection(i);
                break;
            }
        }
        isDronePickerActive = true;
    }

    /**
     * Initializes the drone spinner to contain only the entry "No drones"
     */
    private void initializeDroneSpinner() {
        ArrayList<String> initDronePickerStrings = new ArrayList<String>();
        initDronePickerStrings.add("No drones");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, initDronePickerStrings);
        dronePicker.setAdapter(adapter);
    }

    /**
     * Sets all the data text labels to 'loading data' instead of 'N/A'
     */
    private void loadingData() {
        speedText.setText("Getting data...");
        latText.setText("Getting data...");
        longText.setText("Getting data...");
        tempText.setText("Getting data...");
    }

    /**
     * Creates and starts a StartDroneTask
     *
     * @return the task that was started
     */
    private StartDroneTask createStartTask(float countryLatitude, float countryLongitude, String countryName) {
        String dweetControllerName = getString(R.string.controller_dweet_name);
        StartDroneTask startTask = new StartDroneTask(droneUUID, dweetConfigName, dweetControllerName, countryLatitude, countryLongitude, this, countryName, null, false);
        startTask.execute();
        return startTask;
    }

    /**
     * Creates and starts a StopDroneTask
     * @return the task that was started
     */
    private StopDroneTask createStopTask(){
        String dweetControllerName = getString(R.string.controller_dweet_name);
        StopDroneTask stopTask = new StopDroneTask(dweetConfigName, dweetControllerName, currentDroneUUID, this, null, "");
        stopTask.execute();
        return stopTask;
    }

    /**
     * Enables the launch button if there is text in the name entry box and a drone is not currently launching
     */
    private void enableLaunchButton(){
        // If a new name is given and a drone is not currently launching enable the launch button
        if (!nameEditText.getText().toString().trim().equals("") && !isDroneLaunching && isNewUsername()) {
            hasDroneName = true;
            launchButton.setEnabled(true);
        } else {
            // Otherwise disable the launch button
            hasDroneName = false;
            launchButton.setEnabled(false);
        }
    }

    /**
     * Checks to see if the currently entered drone name has already been used
     */
    private boolean isNewUsername(){
        String currentName = nameEditText.getText().toString().trim();
        boolean isNewName = true;
        ArrayList<String> usernames = new ArrayList<String>(usernameToDweetMap.keySet());
        // Iterate through all the names and check to see if the currently entered name matches any of these
        for (int i = 0; i < usernames.size(); i++){
            if (usernames.get(i).equals(currentName)){
                isNewName = false;
                break;
            }
        }
        return isNewName;
    }

    /**
     * Called by start drone tasks after completion
     */
    @Override
    public void droneStarted(ImageButton buttonPressed, UUID droneUUID, String country, String droneName) {
        Log.d("Launch Drone Activity", "Drone launched");

        // Hide the error message in case there was an error last launch attempt
        errorText.setVisibility(View.INVISIBLE);

        currentDroneDweet = droneName;
        currentDroneUUID = droneUUID;
        droneUsernames.add(currentDroneUsername);
        dweetToUUIDMap.put(currentDroneDweet, droneUUID);
        usernameToDweetMap.put(currentDroneUsername, currentDroneDweet);

        // Add the new drone name to the drone spinner
        updateDroneSpinner();

        // Run a GatherDataTask to collect the data from the launched drone
        dataTask = new GatherDataTask(currentDroneDweet, speedText, latText, longText, tempText);
        dataTask.execute();
        isGatheringData = true;

        // Set launched drones to true since a drone has been launched
        hasLaunchedDrones = true;

        // Enable the 'Disable Drone' button
        disableButton.setEnabled(true);

        // Now that the drone is fully deployed, set the boolean to false
        isDroneLaunching = false;
        enableLaunchButton();
    }

    /**
     * Called by StopDroneTasks when this finish
     */
    @Override
    public void droneStopped(ImageButton cityButton, String city){
        if (hasLaunchedDrones){
            disableButton.setEnabled(true);
        }
    }

    /**
     * Called by start drone task when it receives an exception
     */
    @Override
    public void droneStartFailed(ImageButton buttonPressed){
        Log.d("Launch Drone Activity", "Drone unable to launch");
        errorText.setText("Error launching drone");
        errorText.setVisibility(View.VISIBLE);
        isDroneLaunching = false;
        enableLaunchButton();
        if (!hasLaunchedDrones){
            resetDataTextViews();
        }
    }

    /**
     * Resets all the data text views to display the message "N/A"
     */
    public void resetDataTextViews(){
        speedText.setText("N/A");
        latText.setText("N/A");
        longText.setText("N/A");
        tempText.setText("N/A");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Stops the data task if it is currently running when the back button is pressed
        if (isGatheringData) {
            dataTask.stopTask(false);
            isGatheringData = false;
        }
        this.finish();
    }
}
