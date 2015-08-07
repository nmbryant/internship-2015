package dronesim.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CheckDweetThingTask extends AsyncTask<Void, Void, Void> {

    private String dweetName;
    private String keyString;
    private boolean isValid = false;
    private TextView errorText;
    private boolean isFinished = false;
    private String latestDweet = "";

    public CheckDweetThingTask(String dweetName, String keyString, TextView errorText) {
        this.dweetName = dweetName;
        this.keyString = keyString;
        this.errorText = errorText;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            isValid = isValidDweetThing();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isFinished = true;
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d("Check Dweet Task", "Stopping data gathering");
        errorText.setVisibility(View.VISIBLE);
        if (isValid) {
            // If the object is found, bring the user to the data screen
            errorText.setText("Dweet object found!");
        } else {
            errorText.setText("Invalid dweet object");
        }
        Log.d("Check Dweet", "Setting isFinished");
        isFinished = true;
    }

    @Override
    protected void onPreExecute(){
        errorText.setText("Checking for dweet device...");
    }

    /**
     * Checks to see if the name entered is a thing on Dweet.io
     * @return - True if the dweet thing is found, false otherwise
     * @throws Exception
     */
    private boolean isValidDweetThing() throws Exception {
        Log.d("Check Dweet Task", "Checking if valid dweet thing");
        String line;
        String result = "";
        // Set the URL for getting dweets from the controller
        String url_string = "https://dweet.io/get/latest/dweet/for/" + dweetName;
        if (!keyString.equals("")) {
            url_string += "?key=" + keyString;
        }
        Log.d("Check Dweet Task", url_string);
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
        Log.d("Check Dweet Task", result);
        latestDweet = result;

        // Check the dweet json to see if it failed or succeeded
        JSONObject jsonDweet = new JSONObject(result);
        String status = jsonDweet.getString("this");

        // If it is a valid dweet object, return true
        if (status.equals("succeeded")) {
            return true;
        }
        // If it is not a valid dweet object, return false
        else {
            return false;
        }
    }

    /**
     * Getter for the isFinished boolean
     * @return - True if the task is finished, false otherwise
     */
    public boolean getIsFinished() {
        return isFinished;
    }

    /**
     * Getter for the latestDweet string
     * @return - latestDweet string
     */
    public String getLatestDweet() {
        return latestDweet;
    }
}
