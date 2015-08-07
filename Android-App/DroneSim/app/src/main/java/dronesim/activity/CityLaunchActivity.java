package dronesim.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import dronesim.IDroneLauncher;
import dronesim.task.StartDroneTask;
import dronesim.task.StopDroneTask;

public class CityLaunchActivity extends Activity implements IDroneLauncher{

    ImageButton bostonButton;
    ImageButton nycButton;
    ImageButton seattleButton;
    ImageButton houstonButton;
    ImageButton kansasButton;
    ImageButton wildlifeButton;
    ImageButton anchorageButton;
    ImageButton honoluluButton;

    ImageView bostonPlane;
    ImageView nycPlane;
    ImageView seattlePlane;
    ImageView houstonPlane;
    ImageView kansasPlane;
    ImageView wildlifePlane;
    ImageView anchoragePlane;
    ImageView honoluluPlane;

    HashMap<ImageButton, ImageView> buttonToPlaneImage = new HashMap<ImageButton, ImageView>();

    HashMap<String, UUID> cityNameToUUID = new HashMap<String, UUID>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_launch);

        // Button for launching a drone in Boston
        wildlifeButton = (ImageButton) findViewById(R.id.wildlifeButton);
        wildlifePlane = (ImageView) findViewById(R.id.wildlifePlaneImage);
        buttonToPlaneImage.put(wildlifeButton, wildlifePlane);
        initializeButton(wildlifeButton, "Wildlife");

        // Button for launching a drone in NYC
        nycButton = (ImageButton) findViewById(R.id.newyorkButton);
        nycPlane = (ImageView) findViewById(R.id.newyorkPlaneImage);
        buttonToPlaneImage.put(nycButton, nycPlane);
        initializeButton(nycButton, "NYC");

        // Button for launching a drone in Seattle
        seattleButton = (ImageButton) findViewById(R.id.seattleButton);
        seattlePlane = (ImageView) findViewById(R.id.seattlePlaneImage);
        buttonToPlaneImage.put(seattleButton, seattlePlane);
        initializeButton(seattleButton, "Seattle");

        // Button for launching a drone in San Francisco
        houstonButton = (ImageButton) findViewById(R.id.houstonButton);
        houstonPlane = (ImageView) findViewById(R.id.sanFranPlaneImage);
        buttonToPlaneImage.put(houstonButton, houstonPlane);
        initializeButton(houstonButton, "Houston");

        // Button for launching a drone in Miami
        kansasButton = (ImageButton) findViewById(R.id.kansasButton);
        kansasPlane = (ImageView) findViewById(R.id.miamiPlaneImage);
        buttonToPlaneImage.put(kansasButton, kansasPlane);
        initializeButton(kansasButton, "Kansas");

        // Button for launching a drone in New Orleans
        bostonButton = (ImageButton) findViewById(R.id.bostonButton);
        bostonPlane = (ImageView) findViewById(R.id.bostonPlaneImage);
        buttonToPlaneImage.put(bostonButton, bostonPlane);
        initializeButton(bostonButton, "Boston");

        // Button for launching a drone in Honolulu
        honoluluButton = (ImageButton) findViewById(R.id.honoluluButton);
        honoluluPlane = (ImageView) findViewById(R.id.honoluluPlaneImage);
        buttonToPlaneImage.put(honoluluButton, honoluluPlane);
        initializeButton(honoluluButton, "Honolulu");

        // Button for launching a drone in Anchorage
        anchorageButton = (ImageButton) findViewById(R.id.anchorageButton);
        anchoragePlane = (ImageView) findViewById(R.id.anchoragePlaneImage);
        buttonToPlaneImage.put(anchorageButton, anchoragePlane);
        initializeButton(anchorageButton, "Anchorage");
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
     * Initializes the given button with a launch listener
     * @param cityButton - The button to initialize
     * @param cityName - The city name tied to the button
     */
    private void initializeButton(ImageButton cityButton, String cityName){
        ArrayList<String> cities = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.cities)));
        int cityIndex = cities.indexOf(cityName);
        float latitude1 = Float.parseFloat(getResources().getStringArray(R.array.citylats)[cityIndex]);
        float longitude1 = Float.parseFloat(getResources().getStringArray(R.array.citylongs)[cityIndex]);
        CityLaunchOnClickListener bostonListener = new CityLaunchOnClickListener(cityName, latitude1, longitude1, this, cityButton, getApplicationContext());
        cityButton.setOnClickListener(bostonListener);
    }

    /**
     * Called by a StartDroneTask when a drone is successfully launched, changes the plane to green
     * @param buttonClicked - The button that was clicked to launch a drone
     * @param droneUUID - The UUID of the drone that was started
     * @param city - The city that the drone was launched in
     */
    @Override
    public void droneStarted(ImageButton buttonClicked, UUID droneUUID, String city, String droneName){
        CityDisableOnClickListener disableListener = new CityDisableOnClickListener(buttonClicked, this, droneUUID, city, getApplicationContext());
        buttonClicked.setOnClickListener(disableListener);
        buttonClicked.setEnabled(true);
        buttonToPlaneImage.get(buttonClicked).setImageResource(R.drawable.verizon_green_plane_no_ground_icon);
    }

    /**
     * Called by a StartDroneTask when a drone fails to launch
     * @param buttonClicked - The button that was clicked to launch a drone
     */
    @Override
    public void droneStartFailed(ImageButton buttonClicked){
        buttonClicked.setEnabled(true);
    }

    /**
     * Called by a StopDroneTask when a drone is stopped, changes the plane icon to red
     * @param buttonClicked - The button that was clicked to stop a drone
     * @param city - The city that the disabled drone was in
     */
    @Override
    public void droneStopped(ImageButton buttonClicked, String city){
        initializeButton(buttonClicked, city);
        buttonClicked.setEnabled(true);
        buttonToPlaneImage.get(buttonClicked).setImageResource(R.drawable.verizon_red_plane_no_ground_icon);
    }

    /**
     * Adds a cityName and UUID to the map
     * @param cityName - The key to add to the map
     * @param droneUUID - The UUID to add to the map
     */
    public void addUUIDToMap(String cityName, UUID droneUUID){
        cityNameToUUID.put(cityName, droneUUID);
    }

    /**
     * Listener that is put on all the buttons to launch a drone
     */
    public class CityLaunchOnClickListener implements View.OnClickListener {

        String city;
        String dweetThing = getString(R.string.config_dweet_name);
        UUID droneUUID;
        float latitude;
        float longitude;
        ImageButton cityButton;
        IDroneLauncher cityActivity;
        Context activityContext;

        public CityLaunchOnClickListener(String city, float latitude, float longitude, IDroneLauncher cityActivity, ImageButton cityButton,
                                         Context activityContext){
            this.city = city;
            this.cityButton = cityButton;
            this.cityActivity = cityActivity;
            this.latitude = latitude;
            this.longitude = longitude;
            this.activityContext = activityContext;
        }

        /**
         * Displays a toast and creates + starts a StartDroneTask to launch the drone according to the button
         * that was pressed
         */
        @Override public void onClick(View v){
            // Change the plane image to the yellow version indicating that it is loading
            ImageView planeImage = buttonToPlaneImage.get(cityButton);
            planeImage.setImageResource(R.drawable.verizon_yellow_plane_no_ground_icon);

            // Display a toast to notify the user that a drone is being launched
            Context context = activityContext;
            CharSequence text = "Launching drone!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            cityButton.setEnabled(false);
            droneUUID = UUID.randomUUID();
            String dweetControllerName = getString(R.string.controller_dweet_name);
            StartDroneTask startTask= new StartDroneTask(droneUUID, dweetThing, dweetControllerName, latitude, longitude, this.cityActivity, city, cityButton, false);
            startTask.execute();
            addUUIDToMap(this.city, this.droneUUID);
        }

    }

    /**
     * Listener that is put on buttons to disable a drone
     */
    public class CityDisableOnClickListener implements View.OnClickListener {

        ImageButton cityButton;
        String dweetConfigName = getString(R.string.config_dweet_name);
        IDroneLauncher cityActivity;
        UUID droneUUID;
        String city;
        Context activityContext;

        public CityDisableOnClickListener(ImageButton cityButton, IDroneLauncher cityActivity, UUID droneUUID, String city, Context activityContext){
            this.cityButton = cityButton;
            this.cityActivity = cityActivity;
            this.droneUUID = droneUUID;
            this.city = city;
            this.activityContext = activityContext;
        }


        @Override
        /**
         * Displays a toast and creates + starts a StopDroneTask to stop the drone that is tied to the
         * button that was pressed
         */
        public void onClick(View v){
            // Change the plane image to the yellow version indicating that it is loading
            ImageView planeImage = buttonToPlaneImage.get(cityButton);
            planeImage.setImageResource(R.drawable.verizon_yellow_plane_no_ground_icon);

            // Display a toast to notify the user that the drone is being disabled
            Context context = activityContext;
            CharSequence text = "Disabling drone!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            cityButton.setEnabled(false);
            String dweetControllerName = getString(R.string.controller_dweet_name);
            StopDroneTask stopTask = new StopDroneTask(dweetConfigName, dweetControllerName, droneUUID, cityActivity, cityButton, city);
            stopTask.execute();
        }

    }
}
