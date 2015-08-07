package dronesim.activity;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import dronesim.task.ExistingGatherDataTask;

import com.example.dronesim.R;

public class DataFromExistingActivity extends Activity {

    String dweetName = "";
    String keyString = "";
    String latestDweet = "";
    boolean hasKey = false;
    TextView deviceLabel;
    ArrayList<TextView> textViews = new ArrayList<TextView>();
    ArrayList<TextView> dataTextViews = new ArrayList<TextView>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_existing);

        // Get the dweet name and key string
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dweetName = extras.getString("DWEET_NAME");
            keyString = extras.getString("KEY_STRING");
            latestDweet = extras.getString("LATEST_DWEET");
        }

        deviceLabel = (TextView) findViewById(R.id.deviceLabel);
        deviceLabel.setText(dweetName + " data");

        // Set hasKey to true if it was passed a key
        if (!keyString.equals("")) {
            hasKey = true;
        }

        // Create textviews to display the data in the latest dweet from the dweet device
        try {
            JSONObject jsonDweet = new JSONObject(latestDweet);
            JSONObject dweetData = jsonDweet.getJSONArray("with").getJSONObject(0).getJSONObject("content");
            Iterator<String> dataKeys = dweetData.keys();
            for (int i = 0; i < dweetData.length(); i++) {
                // Add a text view that labels the name of the piece of data
                String currentKey = dataKeys.next();
                TextView tv = new TextView(getApplicationContext());
                tv.setId(i * 1000);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.deviceLabel);
                params.leftMargin = 107;
                params.topMargin = 75 * i;
                RelativeLayout rl = (RelativeLayout) findViewById(R.id.dataExistingLayout);
                tv.setText(currentKey);
                tv.setTextColor(Color.BLACK);
                rl.addView(tv, params);
                textViews.add(tv);

                // Add a text view for the data
                String currentData = dweetData.getString(currentKey);
                TextView dataTextView = new TextView(getApplicationContext());
                RelativeLayout.LayoutParams dataParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dataParams.addRule(RelativeLayout.RIGHT_OF, R.id.deviceLabel);
                dataParams.addRule(RelativeLayout.BELOW, R.id.deviceLabel);
                dataParams.topMargin = 75 * i;
                dataParams.leftMargin = 80;
                dataTextView.setText(currentData);
                dataTextView.setTextColor(Color.BLACK);
                rl.addView(dataTextView, dataParams);
                dataTextViews.add(dataTextView);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        ExistingGatherDataTask dataTask = new ExistingGatherDataTask(dataTextViews, textViews, dweetName);
        dataTask.execute();
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

}
