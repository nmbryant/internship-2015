package dronesim.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class GatherDataTask extends AsyncTask<Void, Void, Void> {

    final String DEGREE = "\u00b0";

    // The dweet name of the app's drone
    private String droneName = "";

    // Flag used to turn off the task
    private boolean running = true;

    // True if the main activity is going to load data after stopping this
    private boolean isLoadingData = false;

    // The speed of the drone
    private int speed = -1;

    // The latitude of the drone
    private float latitude = -1;

    // The longitude of the drone
    private float longitude = -1;

    // Current temperature from drone
    private float temperature = -1;

    // The text view for the speed which this task updates
    private TextView speedText;

    // The text view for the latitude which this task updates
    private TextView latText;

    // The text view for the longitude which this task updates
    private TextView longText;

    // The text view for the temperature which this task updates
    private TextView tempText;

    // Handles gathering the data from the drone and updates the UI every time it receives new data
    public GatherDataTask(String droneName, TextView speedText, TextView latText, TextView longText, TextView tempText) {
        this.droneName = droneName;
        this.speedText = speedText;
        this.latText = latText;
        this.longText = longText;
        this.tempText = tempText;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String latestDweet = "";
        while (running) {
            try {
                Log.d("Is running", Boolean.toString(running));
                // Gather data from the drone every 2 seconds
                TimeUnit.SECONDS.sleep(2);
                String currentDweet = getLatestDroneDweet();
                // If the current dweet is not equal the previous dweet that it received, update the data
                if (!currentDweet.equals(latestDweet)) {
                    // Convert the dweet to a JSONObject
                    JSONObject jsonDweet = new JSONObject(currentDweet);

                    // Get the speed
                    speed = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content").getInt("speed");

                    // Get the latitude and longitude
                    String latString = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content").getString("latitude");
                    latitude = Float.parseFloat(latString);
                    String longString = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content").getString("longitude");
                    longitude = Float.parseFloat(longString);

                    // Get the temperature
                    String weatherString = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content")
                            .getString("weather");
                    JSONObject weatherJSON = new JSONObject(weatherString);
                    String tempString = weatherJSON.getString("temp");
                    temperature = Float.parseFloat(tempString);

                    // Update latest dweet to the current one
                    latestDweet = currentDweet;

                    // Publish progress to update UI
                    publishProgress();
                }
            } catch (Exception e) {
                // Exception can be from making the GET request or the received dweet is not formatted correctly
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Retrieves the latest dweet from the app's drone
     *
     * @return String representation of the latest dweet from the drone
     * @throws Exception
     */
    private String getLatestDroneDweet() throws Exception {
        Log.d("Drone Dweets", "Getting dweets from drone");
        String line;
        String result = "";
        // Set the URL for getting dweets from the controller
        String url_string = "https://dweet.io/get/latest/dweet/for/" + droneName;
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
        Log.d("Drone Dweets", result);
        return result;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d("Data Task", "Stopping data gathering");
        // If data is not being loaded, set all the text to 'N/A'
        if (!isLoadingData) {
            speedText.setText("N/A");
            latText.setText("N/A");
            longText.setText("N/A");
            tempText.setText("N/A");
        }
        // If data is being loaded, notify the user by setting all text fields to 'Getting data...'
        else {
            speedText.setText("Getting data...");
            latText.setText("Getting data...");
            longText.setText("Getting data...");
            tempText.setText("Getting data...");
        }
    }

    @Override
    // Updates the UI with the drone data when it publishes progress
    protected void onProgressUpdate(Void... params) {
        speedText.setText(Integer.toString(speed));
        latText.setText(Float.toString(latitude));
        longText.setText(Float.toString(longitude));
        tempText.setText(Float.toString(temperature) + DEGREE + " F");
    }

    /**
     * Speed getter
     *
     * @return speed as an integer
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Used by MainActivity to stop this task
     */
    public void stopTask(boolean isLoadingData) {
        running = false;
        this.isLoadingData = isLoadingData;
    }
}
