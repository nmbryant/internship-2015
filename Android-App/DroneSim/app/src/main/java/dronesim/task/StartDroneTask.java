package dronesim.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;

import dronesim.IDroneLauncher;
import dronesim.activity.CityLaunchActivity;
import dronesim.activity.LaunchDroneActivity;
import dronesim.activity.UseCaseSelectionActivity;

// A task that handles sending a dweet to start a drone and grabbing the name of the drone from the controller
public class StartDroneTask extends AsyncTask<Void, Void, Void> {

    private UUID droneUUID;
    private String dweetThingName = "";
    private String droneName = "";
    private String country;
    private String dweetControllerName;
    private float launchLatitude;
    private float launchLongitude;
    IDroneLauncher parentActivity;
    private boolean hasException = false;
    private Random random;
    private ImageButton buttonSelected;
    private boolean isGameDrone;

    public StartDroneTask(UUID droneUUID, String dweetThingName, String dweetControllerName, float launchLatitude, float launchLongitude, IDroneLauncher parentActivity,
                          String country, ImageButton buttonSelected, boolean isGameDrone) {
        this.droneUUID = droneUUID;
        this.dweetThingName = dweetThingName;
        this.dweetControllerName = dweetControllerName;
        this.launchLatitude = launchLatitude;
        this.launchLongitude = launchLongitude;
        this.parentActivity = parentActivity;
        this.country = country;
        this.buttonSelected = buttonSelected;
        this.isGameDrone = isGameDrone;
        random = new Random();
    }

    @Override
    protected Void doInBackground(Void... args) {
        // Send a dweet to the config dweet thing to notify the controller to launch a drone with this drone's UUID
        try {
            sendDweet();
            JSONArray dweetsArray;
            boolean hasDroneLaunched = false;
            int attemptsLeft = 4;
            // Attempt to start a drone until a drone is launched or the attempts left hits 0
            while (!hasDroneLaunched && attemptsLeft > 0) {
                TimeUnit.SECONDS.sleep(2);
                // Get all the controller dweets and extract the dweet array
                JSONObject controllerDweets = new JSONObject(getControllerDweets());
                dweetsArray = controllerDweets.getJSONArray("with");
                Log.d("Controller Dweets", "Iterating through controller dweets");
                Log.d("Drone ID", droneUUID.toString());
                // Iterate through the array of controller dweets
                for (int i = 0; i < dweetsArray.length(); i++) {
                    JSONObject obj = dweetsArray.getJSONObject(i);
                    Log.d("Controller Dweets", obj.getJSONObject("content").getString("droneID"));
                    // If the UUID of the dweet matches this drone's UUID, then the drone has been launched so extract
                    // the drone name and continue
                    if (obj.getJSONObject("content").getString("droneID").equals(droneUUID.toString())) {
                        droneName = obj.getJSONObject("content").getString("drone_name");
                        hasDroneLaunched = true;
                        Log.d("Drone Name", droneName);
                    }
                }
                attemptsLeft--;
            }
            // If it enters this block, then the controller has not started the drone so it needs to
            // notify the parent activity that the drone has not launched
            if (attemptsLeft == 0 && !hasDroneLaunched){
                hasException = true;
            }

        } catch (Exception e) {
            Log.d("Exception", "Caught exception when trying to start");
            hasException = true;
        }

        return null;
    }

    @Override
    protected void onPreExecute(){
        Log.d("Start Drone Task", "Starting");
    }

    @Override
    protected void onPostExecute(Void params){
        // If there was no exception, tell the activity that created the start task that the drone was launched successfully
        if (!hasException){
            parentActivity.droneStarted(buttonSelected, droneUUID, country, droneName);
        }
        // If an exception was caught trying to start the drone, notify the activity that created the start task
        // that the drone failed to launch
        else {
            parentActivity.droneStartFailed(buttonSelected);
        }
    }

    /**
     * Send a dweet using an HTTP Post request
     * The dweet tells the master program (Python script) to launch a drone
     *
     * @throws Exception
     */
    private void sendDweet() throws Exception {
        // Set the URL for dweeting
        String url_string = "https://dweet.io/dweet/for/" + dweetThingName;
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);

        // Set request to take in JSON
        conn.setRequestProperty("content-type", "application/json; charset=utf-8");

        // Write the JSON data to the request
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        JSONObject data = new JSONObject().put("Content", "Launch a drone");
        data.put("droneID", droneUUID);
        data.put("is_launch", true);

        data.put("launch_latitude", launchLatitude);
        data.put("launch_longitude", launchLongitude);
        data.put("country", country);
        data.put("is_real", false);
        data.put("is_game_drone", isGameDrone);

        wr.write(data.toString());
        wr.flush();

        // Print out the response
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            Log.d("HTTP Result", line);
        }
        wr.close();
        rd.close();
    }

    /**
     * Gets the latest dweets from the controller
     * Used to see if its drone has been launch and if it has it can get the dweet thing name for it
     *
     * @return the JSON array containing all of the controller dweets
     * @throws Exception
     */
    private String getControllerDweets() throws Exception {
        Log.d("Controller Dweets", "Getting dweets from controller");
        String line;
        String result = "";
        // Set the URL for getting dweets from the controller
        String url_string = "https://dweet.io/get/dweets/for/" + dweetControllerName;
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
        Log.d("Controller Dweets", result);
        return result;
    }

    /**
     * Drone name getter
     *
     * @return String that is the name of the drone
     */
    public String getDroneName() {
        return this.droneName;
    }

}
