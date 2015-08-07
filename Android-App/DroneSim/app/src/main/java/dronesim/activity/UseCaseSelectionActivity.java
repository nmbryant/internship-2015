package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.dronesim.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import dronesim.AppUtilities;
import dronesim.IDroneLauncher;
import dronesim.task.StartDroneTask;

public class UseCaseSelectionActivity extends Activity implements IDroneLauncher{

    public static final int NUMBER_OF_USE_CASES = 6;

    public String clickedUseCase;

    ImageButton governmentButton;
    ImageButton agricultureButton;
    ImageButton insuranceButton;
    ImageButton mediaButton;
    ImageButton energyButton;
    ImageButton healthcareButton;

    ImageView governmentStatusImage;
    ImageView agricultureStatusImage;
    ImageView insuranceStatusImage;
    ImageView mediaStatusImage;
    ImageView energyStatusImage;
    ImageView healthcareStatusImage;

    boolean isGovernmentComplete = false;
    boolean isAgricultureComplete = false;
    boolean isInsuranceComplete = false;
    boolean isMediaComplete = false;
    boolean isEnergyComplete = false;
    boolean isHealthcareComplete = false;

    HashMap<String, UUID> cityNameToUUID = new HashMap<String, UUID>();

    HashMap<ImageButton, Integer> buttonToIndex = new HashMap<ImageButton, Integer>();

    HashMap<ImageButton, ImageView> buttonToStatusImage = new HashMap<ImageButton, ImageView>();

    HashMap<String, ImageButton> useCaseToButton = new HashMap<String, ImageButton>();

    ArrayList<String> completedList = new ArrayList<String>();

    boolean isStartup = true;

    ImageButton lastButtonPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_case_selection);

        // Get the use case name from the extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.d("UseCaseSelection", "Grabbing extras");
            completedList = extras.getStringArrayList("COMPLETED");
            Log.d("UseCaseSelection", "Completed List size = " + Integer.toString(completedList.size()));
        }

        // Check to see which use cases have already been completed by the user
        checkCompletedContents();

        // Button for launching the government use case minigame
        governmentButton = (ImageButton) findViewById(R.id.governmentImageButton);
        initializeButton(governmentButton, "Government");
        governmentStatusImage = (ImageView) findViewById(R.id.govermentStatusImage);
        buttonToStatusImage.put(governmentButton, governmentStatusImage);
        // If the government game is complete, disable its button and set its plane icon to green
        if (isGovernmentComplete){
            governmentButton.setEnabled(false);
            governmentStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }

        // Button for launching the agriculture use case minigame
        agricultureButton = (ImageButton) findViewById(R.id.agricultureImageButton);
        initializeButton(agricultureButton, "Agriculture");
        agricultureStatusImage = (ImageView) findViewById(R.id.agricultureStatusImage);
        buttonToStatusImage.put(agricultureButton, agricultureStatusImage);
        // If the agriculture game is complete, disable its button and set its plane icon to green
        if (isAgricultureComplete){
            agricultureButton.setEnabled(false);
            agricultureStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }

        // Button for launching the insurance use case minigame
        insuranceButton = (ImageButton) findViewById(R.id.insuranceImageButton);
        initializeButton(insuranceButton, "Insurance");
        insuranceStatusImage = (ImageView) findViewById(R.id.insuranceStatusImage);
        buttonToStatusImage.put(insuranceButton, insuranceStatusImage);
        // If the insurance game is complete, disable its button and set its plane icon to green
        if (isInsuranceComplete){
            insuranceButton.setEnabled(false);
            insuranceStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }

        // Button for launching the media use case minigame
        mediaButton = (ImageButton) findViewById(R.id.mediaImageButton);
        initializeButton(mediaButton, "Media");
        mediaStatusImage = (ImageView) findViewById(R.id.mediaStatusImage);
        buttonToStatusImage.put(mediaButton, mediaStatusImage);
        // If the media minigame is complete, disable its button and set its plane icon to green
        if (isMediaComplete){
            mediaButton.setEnabled(false);
            mediaStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }

        // Button for launching the energy use case minigame
        energyButton = (ImageButton) findViewById(R.id.energyImageButton);
        initializeButton(energyButton, "Energy");
        energyStatusImage = (ImageView) findViewById(R.id.energyStatusImage);
        buttonToStatusImage.put(energyButton, energyStatusImage);
        // If the energy game is complete, disable its button and set its plane icon to green
        if (isEnergyComplete){
            energyButton.setEnabled(false);
            energyStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }

        // Button for launching the healthcare use case minigame
        healthcareButton = (ImageButton) findViewById(R.id.healthcareImageButton);
        initializeButton(healthcareButton, "Healthcare");
        healthcareStatusImage = (ImageView) findViewById(R.id.healthcareStatusImage);
        buttonToStatusImage.put(healthcareButton, healthcareStatusImage);
        // If the healthcare minigame is complete, disable its button and set its plane icon to green
        if (isHealthcareComplete){
            healthcareButton.setEnabled(false);
            healthcareStatusImage.setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_use_case_selection, menu);
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
    public void onResume(){
        if (isStartup){
            isStartup = false;
        }
        else {
            // If the last button pressed is not null, set the plane icon associated with it to green
            // since it was changed to yellow when the button was pressed
            if (lastButtonPressed != null) {
                ImageView lastButtonStatusImage = buttonToStatusImage.get(lastButtonPressed);
                lastButtonStatusImage.setImageResource(R.drawable.verizon_red_plane_no_ground_icon);
            }
            // Enable all the use case buttons
            enableAllButtons(true);

            // Disable all the buttons for the use cases already completed
            disableCompletedButtons();
        }
        super.onResume();
    }

    /**
     * Initializes the given button with a listener
     * @param useCaseButton - The button that is initialized
     * @param useCase - The name of the use case
     */
    private void initializeButton(ImageButton useCaseButton, String useCase) {
        ArrayList<String> useCases = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.use_cases)));
        int useCaseIndex = useCases.indexOf(useCase);
        float latitude1 = Float.parseFloat(getResources().getStringArray(R.array.use_case_lats)[useCaseIndex]);
        float longitude1 = Float.parseFloat(getResources().getStringArray(R.array.use_case_longs)[useCaseIndex]);
        UseCaseOnClickListener buttonListener = new UseCaseOnClickListener(useCase, latitude1, longitude1, this, useCaseButton);
        useCaseButton.setOnClickListener(buttonListener);
        buttonToIndex.put(useCaseButton, useCaseIndex);
        useCaseToButton.put(useCase, useCaseButton);
    }

    /**
     * Called by a StartDroneTask when a drone is successfully launched
     * @param buttonClicked - The button that was clicked to launch a drone
     * @param droneUUID - The UUID of the launched drone
     * @param useCase - The name of the use case
     * @param droneName - The dweet thing name for the drone that was launched
     */
    @Override
    public void droneStarted(ImageButton buttonClicked, UUID droneUUID, String useCase, String droneName) {
        // Go to the control drone screen
        Intent intent = new Intent(this, TutorialActivity.class);
        int useCaseIndex = buttonToIndex.get(buttonClicked);
        intent.putExtra("OBJECTIVE_TEXT", getResources().getStringArray(R.array.use_case_descriptions)[useCaseIndex]);
        intent.putExtra("LATITUDE", getResources().getStringArray(R.array.use_case_lats)[useCaseIndex]);
        intent.putExtra("LONGITUDE", getResources().getStringArray(R.array.use_case_longs)[useCaseIndex]);
        intent.putExtra("DRONE_NAME", droneName);
        intent.putExtra("DRONE_UUID", droneUUID.toString());
        intent.putExtra("USE_CASE", useCase);
        intent.putExtra("COMPLETED", completedList);
        startActivity(intent);
    }

    /**
     * Called by a StartDroneTask when a drone fails to launch
     * @param buttonClicked - The button that was clicked to try to launch the drone
     */
    @Override
    public void droneStartFailed(ImageButton buttonClicked) {
        // Go to the control drone screen
        Intent intent = new Intent(this, TutorialActivity.class);
        int useCaseIndex = buttonToIndex.get(buttonClicked);
        intent.putExtra("OBJECTIVE_TEXT", getResources().getStringArray(R.array.use_case_descriptions)[useCaseIndex]);
        intent.putExtra("LATITUDE", getResources().getStringArray(R.array.use_case_lats)[useCaseIndex]);
        intent.putExtra("LONGITUDE", getResources().getStringArray(R.array.use_case_longs)[useCaseIndex]);
        intent.putExtra("DRONE_NAME", getString(R.string.no_drone_name));
        intent.putExtra("DRONE_UUID", UUID.randomUUID().toString());
        intent.putExtra("USE_CASE", clickedUseCase);
        intent.putExtra("COMPLETED", completedList);
        startActivity(intent);
    }

    /**
     * Called by a StopDroneTask when a drone is successfully stopped
     * @param buttonClicked - The button that was clicked to disable a drone
     * @param useCase - The name of the use case tied to the drone
     */
    public void droneStopped(ImageButton buttonClicked, String useCase) {
        initializeButton(buttonClicked, useCase);
        buttonClicked.setEnabled(true);
    }

    /**
     * Add a cityName and UUID to the map
     * @param cityName - The key to add to the map
     * @param droneUUID - The value to add to the map
     */
    public void addUUIDToMap(String cityName, UUID droneUUID) {
        cityNameToUUID.put(cityName, droneUUID);
    }

    /**
     * Checks to see which use cases have been completed based on the bundle extras
     * Disables the buttons for all the use cases that have already been completed
     */
    public void checkCompletedContents(){
        if (completedList.contains("Government")){
            isGovernmentComplete = true;
        }
        if (completedList.contains("Agriculture")){
            isAgricultureComplete = true;
        }
        if (completedList.contains("Insurance")){
            isInsuranceComplete = true;
        }
        if (completedList.contains("Media")){
            isMediaComplete = true;
        }
        if (completedList.contains("Energy")){
            isEnergyComplete = true;
        }
        if (completedList.contains("Healthcare")){
            isHealthcareComplete = true;
        }
    }

    /**
     * Disables all the buttons for use cases that have already been completed
     */
    private void disableCompletedButtons(){
        governmentButton.setEnabled(!isGovernmentComplete);
        agricultureButton.setEnabled(!isAgricultureComplete);
        insuranceButton.setEnabled(!isInsuranceComplete);
        mediaButton.setEnabled(!isMediaComplete);
        energyButton.setEnabled(!isEnergyComplete);
        healthcareButton.setEnabled(!isHealthcareComplete);
    }

    /**
     * Sets all the use case buttons to enabled or disabled
     * @param isEnable - Enable all buttons if true, disable all buttons if false
     */
    private void enableAllButtons(boolean isEnable){
        governmentButton.setEnabled(isEnable);
        agricultureButton.setEnabled(isEnable);
        insuranceButton.setEnabled(isEnable);
        mediaButton.setEnabled(isEnable);
        energyButton.setEnabled(isEnable);
        healthcareButton.setEnabled(isEnable);
    }

    /**
     * Listener that is put on all the buttons to launch a drone
     */
    public class UseCaseOnClickListener implements View.OnClickListener {

        String useCase;
        String dweetThing = getString(R.string.config_dweet_name);
        UUID droneUUID;
        float latitude;
        float longitude;
        ImageButton useCaseButton;
        UseCaseSelectionActivity useCaseSelectionActivity;

        public UseCaseOnClickListener(String useCase, float latitude, float longitude, UseCaseSelectionActivity useCaseSelectionActivity, ImageButton useCaseButton) {
            this.useCase = useCase;
            this.useCaseButton = useCaseButton;
            this.useCaseSelectionActivity = useCaseSelectionActivity;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        /**
         * Displays a toast and creates + starts a StartDroneTask to launch the drone according to the button
         * that was pressed
         */
        public void onClick(View v) {
            lastButtonPressed = useCaseButton;

            // Tell the use case selection activity the use case that was pressed
            useCaseSelectionActivity.clickedUseCase = useCase;

            // Disable all the other buttons since only one drone can be launched at a time
            enableAllButtons(false);

            // Change the plane image to the yellow version indicating that it is loading
            ImageView planeImage = buttonToStatusImage.get(useCaseButton);
            planeImage.setImageResource(R.drawable.verizon_yellow_plane_no_ground_icon);

            // Display a toast to notify the user that a drone is being launched
            Toast toast = null;
            toast = AppUtilities.displayToast("Launching drone!", toast, useCaseSelectionActivity.getApplicationContext());

            useCaseButton.setEnabled(false);
            droneUUID = UUID.randomUUID();
            Log.d("UseCaseSelection", "Button pressed - Creating start task");
            String controllerName = getString(R.string.controller_dweet_name);
            StartDroneTask startTask = new StartDroneTask(droneUUID, dweetThing, controllerName, latitude, longitude, this.useCaseSelectionActivity, useCase, useCaseButton, true);
            startTask.execute();
            addUUIDToMap(this.useCase, this.droneUUID);
        }
    }
}

