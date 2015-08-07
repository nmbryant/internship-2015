package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dronesim.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;

import dronesim.AgricultureMinigame;
import dronesim.BasicMiniGame;
import dronesim.EnergyMinigame;
import dronesim.GovernmentMinigame;
import dronesim.HealthcareMiniGame;
import dronesim.IDroneLauncher;
import dronesim.IMiniGame;
import dronesim.InsuranceMinigame;
import dronesim.MediaMinigame;
import dronesim.task.SendGameCoordinatesTask;
import dronesim.task.StopDroneTask;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DroneControlActivity extends Activity implements IDroneLauncher{

    private static final double DRONE_MOVEMENT_DISTANCE = 0.0005;
    private static final double INSURANCE_DRONE_MOVEMENT_DISTANCE = 0.00003;
    private static final double ENERGY_DRONE_MOVEMENT_DISTANCE = 0.0001;
    private static final int BATTERY_STARTING_TOTAL = 100;
    private static final int LOW_BATTERY_POWER = 20;
    private static final int BATTERY_DECREMENT_VALUE = 1;
    private static final int BUTTON_DELAY_VALUE = 100;

    IMiniGame currentGame;

    double droneMovementSpeed;

    public GoogleMap googleMap;
    MapFragment googleMapFragment;
    float startLatitude;
    float startLongitude;
    float currentLatitude;
    float currentLongitude;

    ImageButton upButton;
    ImageButton downButton;
    ImageButton rightButton;
    ImageButton leftButton;
    ImageButton upRightButton;
    ImageButton downRightButton;
    ImageButton downLeftButton;
    ImageButton upLeftButton;

    Button resultsButton;

    TextView objectiveText;
    String objectiveString;
    String useCase;

    String droneDweetName;
    UUID droneUUID;
    String droneConfigDweetThing;

    ArrayList<String> completedList = new ArrayList<String>();

    boolean isStartup = true;
    boolean hasReachedDestination = false;
    boolean hasDisplayedBatteryToast = false;
    boolean hasSimulatedDrone = true;

    int currentBatteryPower = BATTERY_STARTING_TOTAL;

    ProgressBar batteryLevelProgressBar;

    ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_control);

        // Initialize the thread pool executor
        threadPoolExecutor = new ThreadPoolExecutor(3, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5));

        droneConfigDweetThing = getString(R.string.config_dweet_name);

        Log.d("DroneControlActivity", "CREATED");

        // Initialize the battery levels
        batteryLevelProgressBar = (ProgressBar) findViewById(R.id.batteryLevelProgressBar);
        batteryLevelProgressBar.setProgress(BATTERY_STARTING_TOTAL);
        batteryLevelProgressBar.getProgressDrawable().setColorFilter(Color.rgb(46, 103, 178), PorterDuff.Mode.SRC_IN);

        // Initialize the map objects
        googleMapFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.googleMapFragment));
        MapsInitializer.initialize(googleMapFragment.getActivity());
        googleMap = googleMapFragment.getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Disable the UI components/actions of a Google Map that are not necessary for this app
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Initialize the arrow buttons
        upButton = (ImageButton) findViewById(R.id.upArrowButton);
        downButton = (ImageButton) findViewById(R.id.downArrowButton);
        rightButton = (ImageButton) findViewById(R.id.rightArrowButton);
        leftButton = (ImageButton) findViewById(R.id.leftArrowButton);
        upRightButton = (ImageButton) findViewById(R.id.upRightArrowButton);
        downRightButton = (ImageButton) findViewById(R.id.downRightArrowButton);
        downLeftButton = (ImageButton) findViewById(R.id.downLeftArrowButton);
        upLeftButton = (ImageButton) findViewById(R.id.upLeftArrowButton);

        // Initialize the results button to being invisible and not enabled
        resultsButton = (Button) findViewById(R.id.resultsButton);
        resultsButton.setVisibility(View.INVISIBLE);
        resultsButton.setEnabled(false);
        resultsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                displayResultsScreen(false);
            }
        });

        // Initialize the objective description text view
        objectiveText = (TextView) findViewById(R.id.objectiveDescriptionText);

        // Get the dweet name, objective text, and coordinates
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            objectiveString = extras.getString("OBJECTIVE_TEXT");
            startLatitude = Float.parseFloat(extras.getString("LATITUDE"));
            startLongitude = Float.parseFloat(extras.getString("LONGITUDE"));
            droneDweetName = extras.getString("DRONE_NAME");
            droneUUID = UUID.fromString(extras.getString("DRONE_UUID"));
            useCase = extras.getString("USE_CASE");
            completedList = extras.getStringArrayList("COMPLETED");
        }

        // If the given drone name is "NoDrone", then the simulator is not currently running or the drone
        // failed to launch
        if (droneDweetName.equals(getString(R.string.no_drone_name))){
            hasSimulatedDrone = false;
        }

        droneMovementSpeed = DRONE_MOVEMENT_DISTANCE;

        // Initialize the minigame based on the use case
        if (useCase.equals("Healthcare")){
            currentGame = new HealthcareMiniGame(googleMap, startLatitude, startLongitude, this);
        }
        else if (useCase.equals("Energy")){
            currentGame = new EnergyMinigame(googleMap, startLatitude, startLongitude, getApplicationContext());
            droneMovementSpeed = ENERGY_DRONE_MOVEMENT_DISTANCE;
        }
        else if (useCase.equals("Government")){
            currentGame = new GovernmentMinigame(googleMap, startLatitude, startLongitude, this);
        }
        else if (useCase.equals("Agriculture")){
            currentGame = new AgricultureMinigame(googleMap, startLatitude, startLongitude, this);
        }
        else if (useCase.equals("Media")){
            currentGame = new MediaMinigame(googleMap, startLatitude, startLongitude, this);
        }
        else if (useCase.equals("Insurance")){
            currentGame = new InsuranceMinigame(googleMap, startLatitude, startLongitude, this);
            droneMovementSpeed = INSURANCE_DRONE_MOVEMENT_DISTANCE;
        }
        else {
            currentGame = new BasicMiniGame(googleMap, useCase, startLatitude, startLongitude);
        }

        // If it is the media use case, use a set destination
        currentGame.createGoal();

        // Set the current coordinates
        currentLatitude = startLatitude;
        currentLongitude = startLongitude;

        // Update the map with markers, circle for destination and appropriate zoom
        currentGame.updateMap(true, currentLatitude, currentLongitude);

        // Display the objective string
        objectiveText.setText(objectiveString);

        // Sends the drone coordinates to the simulated drone over dweet only if the simulator is running
        if (hasSimulatedDrone) {
            SendGameCoordinatesTask sendCoordsTask = new SendGameCoordinatesTask(droneDweetName, currentLatitude, currentLongitude, false);
            sendCoordsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // Clicking the up button increments the currentLatitude
        upButton.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude += droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude += droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the down button decrements the currentLatitude
        downButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude -= droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude -= droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the right button increments the longitude
        rightButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLongitude += droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLongitude += droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the left button decrements the longitude
        leftButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLongitude -= droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };

        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLongitude -= droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the up right button increments the longitude and increments the latitude
        upRightButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude += droneMovementSpeed;
                    currentLongitude += droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        upRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude += droneMovementSpeed;
                currentLongitude += droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the down right button decrements the latitude and increments the longitude
        downRightButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude -= droneMovementSpeed;
                    currentLongitude += droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        downRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude -= droneMovementSpeed;
                currentLongitude += droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the down left button decrements the longitude and decrements the latitude
        downLeftButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude -= droneMovementSpeed;
                    currentLongitude -= droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        downLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude -= droneMovementSpeed;
                currentLongitude -= droneMovementSpeed;
                moveDrone();
            }
        });

        // Clicking the up left button increments the latitude and decrements the longitude
        upLeftButton.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) {
                            return true;
                        }
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) {
                            return true;
                        }
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    currentLatitude += droneMovementSpeed;
                    currentLongitude -= droneMovementSpeed;
                    moveDrone();
                    mHandler.postDelayed(this, BUTTON_DELAY_VALUE);
                }
            };
        });

        upLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLatitude += droneMovementSpeed;
                currentLongitude -= droneMovementSpeed;
                moveDrone();
            }
        });

        Log.d("DroneControlActivity", "Objective Text = " + objectiveText.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drone_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        // Only send the game coordinates if the simulator is running
        if (hasSimulatedDrone) {
            startSendGameCoordinatesTask(true);
        }
        currentGame.endGame();
        super.onBackPressed();
    }

    @Override
    public void onResume(){
        // If the activity is resuming due to a back button press, display a results screen button
        // and grey out all other functionality
        if (!isStartup) {
            if (hasReachedDestination) {
                Log.d("DroneControlActivity", "Resuming activity");
                upButton.setEnabled(false);
                rightButton.setEnabled(false);
                leftButton.setEnabled(false);
                downButton.setEnabled(false);
                upRightButton.setEnabled(false);
                downRightButton.setEnabled(false);
                downLeftButton.setEnabled(false);
                upLeftButton.setEnabled(false);
                resultsButton.setEnabled(true);
                resultsButton.setVisibility(View.VISIBLE);
            }
        }
        // If it is start up, just set the boolean to false
        else {
            isStartup = false;
            Log.d("DroneControlActivity", "Starting activity");
        }
        super.onResume();
    }

    /**
     * Creates and executes a SendGameCoordinatesTask to send out the current location
     */
    private void startSendGameCoordinatesTask(boolean isRestart){
        SendGameCoordinatesTask sendGameCoordsTask = new SendGameCoordinatesTask(droneDweetName, currentLatitude, currentLongitude, isRestart);
        sendGameCoordsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Called when the drone reaches the destination
     * Disables the drone that reached the destination
     */
    private void displayResultsScreen(boolean isDroneRunning){
        hasReachedDestination = true;

        // Disable the drone that reached the destination
        if (isDroneRunning) {
            StopDroneTask stopTask = new StopDroneTask(droneConfigDweetThing, "", droneUUID, this, null, "");
            stopTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // Add the current use case to the completed list since it has just reached its destination
        if (!completedList.contains(useCase)) {
            completedList.add(useCase);
        }

        // Go to the results screen
        Intent intent = new Intent(this, ResultsScreenActivity.class);
        intent.putExtra("USE_CASE", useCase);
        intent.putExtra("COMPLETED", completedList);
        Log.d("DroneControl", "Completed List Size = " + Integer.toString(completedList.size()));
        startActivity(intent);
    }

    /**
     * Called by each button press after setting the new coordinates
     * Checks if the destination has been reached, reduces battery power, checks if it needs to restart
     * and updates the map to reflect changes
     */
    private void moveDrone(){
        // If the destination is reached, display the results screen
        if (currentGame.isGameComplete(currentLatitude, currentLongitude)) {
            Log.d("DroneControlActivity", "Reached destination");
            currentGame.endGame();
            displayResultsScreen(true);
            return;
        }
        // Reduce the battery power and check if battery is at 0
        reduceBatteryPower();
        boolean isRestart = false;
        if (currentBatteryPower <= 0 || currentGame.isRestart(currentLatitude, currentLongitude)){
            if (currentBatteryPower <= 0){
                displayToast("Drone ran out of battery - Restarting");
            }
            currentLatitude = startLatitude;
            currentLongitude = startLongitude;
            currentBatteryPower = BATTERY_STARTING_TOTAL;
            batteryLevelProgressBar.setProgress(currentBatteryPower);
            isRestart = true;
            batteryLevelProgressBar.getProgressDrawable().setColorFilter(Color.rgb(46, 103, 178), PorterDuff.Mode.SRC_IN);
            currentGame.restartGame();
            hasDisplayedBatteryToast = false;
        }

        // Change the color of the progress bar if the battery goes below a certain value
        else if (!hasDisplayedBatteryToast && currentBatteryPower <= LOW_BATTERY_POWER){
            hasDisplayedBatteryToast = true;
            batteryLevelProgressBar.getProgressDrawable().setColorFilter(Color.rgb(237, 28, 36), PorterDuff.Mode.SRC_IN);
            displayToast("Battery running low");
        }

        // If battery power is at 0, tell the simulator to reset the drone to starting values by
        // sending the isRestart boolean with the game dweet
        if (hasSimulatedDrone) {
            startSendGameCoordinatesTask(isRestart);
        }

        // If it is a restart, update the map as though it was the first update
        currentGame.updateMap(isRestart, currentLatitude, currentLongitude);
    }

    /**
     * Decreases the battery power and displays the result in the battery level progress bar
     */
    private void reduceBatteryPower(){
        currentBatteryPower -= BATTERY_DECREMENT_VALUE;
        batteryLevelProgressBar.setProgress(currentBatteryPower);
    }

    /**
     * Adds the given value to the current battery power for the drone
     * @param addToBattery - The amount to add to the current battery power
     */
    public void addBatteryPower(float addToBattery){
        currentBatteryPower += addToBattery;
        // If the current batery power is greater than the starting total, just set it to the starting total
        if (currentBatteryPower > BATTERY_STARTING_TOTAL){
            currentBatteryPower = BATTERY_STARTING_TOTAL;
        }
        // Update the battery level in the progress bar
        batteryLevelProgressBar.setProgress(currentBatteryPower);
    }

    /**
     * Display a toast to notify the user that a destination has been reached
     */
    private void displayToast(CharSequence text){
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
    }

    /**
     * Restarts the current minigame
     */
    public void restartGame(){
        // Reset the coordinates
        currentLatitude = startLatitude;
        currentLongitude = startLongitude;

        // Reset the battery power
        currentBatteryPower = BATTERY_STARTING_TOTAL;
        batteryLevelProgressBar.setProgress(currentBatteryPower);
        batteryLevelProgressBar.getProgressDrawable().setColorFilter(Color.rgb(46, 103, 178), PorterDuff.Mode.SRC_IN);

        // Restart the minigame
        currentGame.restartGame();
        hasDisplayedBatteryToast = false;

        // Only send out the game dweet if the simulator is running
        if (hasSimulatedDrone) {
            startSendGameCoordinatesTask(true);
        }

        // Update the map to initialize the minigame map components
        currentGame.updateMap(true, currentLatitude, currentLongitude);
    }

    /**
     * Ends the current minigame and displays the results screen
     */
    public void completeGame(){
        currentGame.endGame();
        displayResultsScreen(true);
    }

    /**
     * Called by StopDroneTask when a drone is stopped
     * This activity does not need to take action when a drone is stopped, so this function just returns
     * @param cityButton - The button that was pressed to disable the drone
     * @param city - The string for the city/use case that is being stopped
     */
    @Override
    public void droneStopped(ImageButton cityButton, String city){
        return;
    }

    /**
     * Called by StartDroneTask when a drone is started
     * This activity does not need to take action when a drone is started, so this function just returns
     * @param buttonPressed - The button that was pressed to start a drone
     * @param droneUUID - The UUID for the drone that was launched
     * @param country - The string for the country/city/use case that the drone was launched in
     * @param droneName - The dweet name for the drone given by the simulator
     */
    @Override
    public void droneStarted(ImageButton buttonPressed, UUID droneUUID, String country, String droneName){
        return;
    }

    /**
     * Called by StartDroneTask when a drone fails to start
     * This activity does not need to take action when a drone is started, so this function just returns
     * @param buttonPressed - The button that was pressed to start a drone
     */
    @Override
    public void droneStartFailed(ImageButton buttonPressed){
        return;
    }

}
