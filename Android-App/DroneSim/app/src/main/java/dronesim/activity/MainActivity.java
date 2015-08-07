package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.dronesim.R;

public class MainActivity extends Activity {

    // GUI components
    private RadioGroup startGroup;
    private RadioButton radioLaunch;
    private RadioButton radioExisting;
    private RadioButton radioDemo;
    private RadioButton radioMap;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Radio group containing start buttons
        startGroup = (RadioGroup) findViewById(R.id.radioStart);

        // Radio button for launching a drone
        radioLaunch = (RadioButton) findViewById(R.id.radioLaunch);

        // Radio button for choosing an existing drone
        radioExisting = (RadioButton) findViewById(R.id.radioExisting);

        // Radio button for the city demo
        radioDemo = (RadioButton) findViewById(R.id.radioDemo);

        // Radio button for the map demo
        radioMap = (RadioButton) findViewById(R.id.testMapRadioButton);

        // Button to start the app
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setEnabled(false);

        // The radio group enables the start button when a selection is made, disables it when there is no selection
        startGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) {
                    startButton.setEnabled(false);
                } else {
                    startButton.setEnabled(true);
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            // When the start button is pressed, check the radio buttons
            public void onClick(View v) {
                int checkedButton = startGroup.getCheckedRadioButtonId();
                if (checkedButton == radioLaunch.getId()) {
                    startLaunchActivity();
                } else if (checkedButton == radioExisting.getId()) {
                    startChooseExistingActivity();
                } else if (checkedButton == radioDemo.getId()) {
                    startCityDemoActivity();
                } else if (checkedButton == radioMap.getId()) {
                    startUseCaseSelectionActivity();
                }
            }
        });
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
     * Starts the launch drone activity
     */
    private void startLaunchActivity() {
        Intent intent = new Intent(this, LaunchDroneActivity.class);
        startActivity(intent);
    }

    /**
     * Starts the choose existing activity
     */
    private void startChooseExistingActivity() {
        Log.d("Main Activity", "Starting ChooseExistingActivity");
        Intent intent = new Intent(this, ChooseExistingActivity.class);
        startActivity(intent);
    }

    /**
     * Starts the city demo activity
     */
    private void startCityDemoActivity() {
        Log.d("Main Activity", "Starting CityLaunchActivity");
        Intent intent = new Intent(this, CityLaunchActivity.class);
        startActivity(intent);
    }

    /**
     * Starts a UseCaseSelectionActivity
     */
    private void startUseCaseSelectionActivity() {
        Log.d("Main Activity", "Starting UseCaseSelectionActivity activity");
        Intent intent = new Intent(this, UseCaseSelectionActivity.class);
        startActivity(intent);
    }
}
