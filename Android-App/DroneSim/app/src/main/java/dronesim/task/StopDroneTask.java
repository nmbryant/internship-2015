package dronesim.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;

import dronesim.IDroneLauncher;

public class StopDroneTask extends AsyncTask<Void, Void, Void> {

    private static final int NUMBER_OF_ATTEMPTS = 4;

    private String dweetThingName;
    private String dweetControllerName;
    private UUID droneUUID;
    private IDroneLauncher launcherActivity;
    private String city = "N/A";
    private ImageButton cityButton;

    /**
     * Constructor called by CityLaunchActivity
     * @param dweetThingName - The dweet name of the drone to stop
     * @param droneUUID - The UUID of the drone to stop
     * @param launcherActivity - The activity that created the task
     * @param cityButton - The button that was pressed to stop the drone
     * @param city - The name of the city tied to the drone
     */
    public StopDroneTask(String dweetThingName, String dweetControllerName, UUID droneUUID, IDroneLauncher launcherActivity, ImageButton cityButton, String city) {
        this.dweetThingName = dweetThingName;
        this.dweetControllerName = dweetControllerName;
        this.droneUUID = droneUUID;
        this.launcherActivity = launcherActivity;
        this.cityButton = cityButton;
        this.city = city;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            disableDrone();
            boolean hasDroneDisabled = false;
            JSONArray dweetsArray;
            int numberOfAttempts = NUMBER_OF_ATTEMPTS;
            // Attempt to stop the drone until the drone has been disabled or the attempts left hits 0
            while (!hasDroneDisabled && numberOfAttempts > 0) {
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
                        hasDroneDisabled = true;
                    }
                }
                numberOfAttempts -= 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void params){
        // Notify the activity that created this task that the drone has been stopped
        launcherActivity.droneStopped(cityButton, city);
    }

    /**
     * Send a dweet using an HTTP Post request
     * The dweet tells the master program (Python script) to disable a drone
     *
     * @throws Exception
     */
    private void disableDrone() throws Exception {
        // Set the URL for dweeting
        String url_string = "https://dweet.io/dweet/for/" + dweetThingName;
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);

        // Set request to take in JSON
        conn.setRequestProperty("content-type", "application/json; charset=utf-8");

        // Write the JSON data to the request
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        JSONObject data = new JSONObject().put("Content", "Disable a drone");
        Log.d("Stop drone task", "Drone ID = " + droneUUID.toString());
        data.put("droneID", droneUUID.toString());
        data.put("is_launch", false);
        data.put("launch_latitude", 0);
        data.put("launch_longitude", 0);
        data.put("country", city);
        data.put("is_real", false);
        data.put("is_game_drone", false);

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
     * Get all the dweets from the dweet controller
     * @return The result from the request - All of the controller dweets
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

}
