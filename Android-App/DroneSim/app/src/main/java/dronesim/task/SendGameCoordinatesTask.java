package dronesim.task;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class SendGameCoordinatesTask extends AsyncTask<Void, Void, Void>{

    String droneName;
    float latitude;
    float longitude;
    boolean isRestart;

    public SendGameCoordinatesTask(String droneName, float latitude, float longitude, boolean isRestart){
        this.droneName = droneName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isRestart = isRestart;
    }

    @Override
    protected Void doInBackground(Void... params) {
        sendLatLongDweet();
        return null;
    }

    /**
     * Sends out a dweet as 'dweet_client_X_game' that contains the current latitude and longitude
     * of the drone.
     */
    void sendLatLongDweet() {
        try {
            // Set the dweet name
            String gameDweetName = droneName + "_game";
            Log.d("DroneControlActivity", "Dweet name = " + gameDweetName);
            // Set the URL for dweeting
            String url_string = "https://dweet.io/dweet/for/" + gameDweetName;
            URL url = new URL(url_string);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);

            // Set request to take in JSON
            conn.setRequestProperty("content-type", "application/json; charset=utf-8");

            // Write the JSON data to the request
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            JSONObject data = new JSONObject();
            data.put("lat", latitude);
            data.put("long", longitude);
            data.put("is_restart", isRestart);

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
        catch(Exception e){
            Log.d("DroneControlActivity", "Exception sending game dweet");
            e.printStackTrace();
        }

    }

}
