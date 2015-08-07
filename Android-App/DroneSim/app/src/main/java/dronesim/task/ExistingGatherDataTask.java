package dronesim.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class ExistingGatherDataTask extends AsyncTask<Void, Void, Void> {

    ArrayList<TextView> dataTextViews;
    ArrayList<TextView> dataLabels;
    ArrayList<String> dataValues;
    ArrayList<TextView> dataText;
    String dweetName;
    String latestDweet;
    String currentData;
    TextView currentTextView;

    public ExistingGatherDataTask(ArrayList<TextView> dataTextViews, ArrayList<TextView> dataLabels, String dweetName) {
        this.dataTextViews = dataTextViews;
        this.dataLabels = dataLabels;
        this.dweetName = dweetName;
        dataValues = new ArrayList<String>();
        dataText = new ArrayList<TextView>();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            String previousDweet = "";
            while (true) {
                latestDweet = getLatestDweet();
                if (!previousDweet.equals(latestDweet)) {
                    Log.d("Existing Gather Task", "Received new dweet");
                    JSONObject jsonDweet = new JSONObject(latestDweet);
                    JSONObject dweetData = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content");
                    Iterator<String> dataKeys = dweetData.keys();
                    for (int i = 0; i < dweetData.length(); i++) {
                        String currentKey = dataKeys.next();
                        TextView currentDataText = null;
                        for (int j = 0; j < dataLabels.size(); j++) {
                            Log.d("Existing Gather Task", "Data Label = " + dataLabels.get(j).getText().toString());
                            Log.d("Existing Gather Task", "Current key = " + currentKey);
                            if (dataLabels.get(j).getText().toString().equals(currentKey)) {
                                currentDataText = dataTextViews.get(j);
                                break;
                            }
                        }
                        if (currentDataText != null) {
                            Log.d("Existing Gather Task", "Calling Publish");
                            currentData = dweetData.getString(currentKey);
                            dataValues.add(currentData);
                            currentTextView = currentDataText;
                            dataText.add(currentTextView);
                        }
                    }
                    publishProgress();
                    previousDweet = latestDweet;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves the latest dweet from the app's drone
     *
     * @return String representation of the latest dweet from the drone
     * @throws Exception
     */
    private String getLatestDweet() throws Exception {
        String line;
        String result = "";
        // Set the URL for getting dweets from the controller
        String url_string = "https://dweet.io/get/latest/dweet/for/" + dweetName;
        URL url = new URL(url_string);
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
        return result;
    }

    protected void onProgressUpdate(Void... params) {
        // Iterate through all the data text fields and set the string to their respective value
        for (int i = 0; i < dataValues.size(); i++) {
            Log.d("Existing Gather Task", "Publishing!");
            dataText.get(i).setText(dataValues.get(i));
        }
    }

}
