package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dronesim.R;

import java.util.ArrayList;
import java.util.UUID;

import dronesim.IDroneLauncher;
import dronesim.task.StopDroneTask;

public class TutorialActivity extends Activity implements IDroneLauncher{

    TextView useCaseTitleText;
    TextView useCaseText;
    TextView useCaseDescriptionText;
    TextView gameDescriptionText;
    TextView image1Text;
    TextView image2Text;
    TextView image3Text;

    ImageView tutorialImage1;
    ImageView tutorialImage2;
    ImageView tutorialImage3;

    Button startGameButton;

    String useCase;
    String objectiveText;
    String latitude;
    String longitude;
    String droneName;
    String droneUUID;
    String droneConfigDweetThing;
    ArrayList<String> completedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Get the drone config dweet thing name
        droneConfigDweetThing = getString(R.string.config_dweet_name);

        // Initialize the UI elements
        useCaseTitleText = (TextView) findViewById(R.id.useCaseTitleText);
        useCaseText = (TextView) findViewById(R.id.useCaseText);
        useCaseDescriptionText = (TextView) findViewById(R.id.useCaseDescriptionText);
        gameDescriptionText = (TextView) findViewById(R.id.gameDescriptionText);
        image1Text = (TextView) findViewById(R.id.tutorialImage1Text);
        image2Text = (TextView) findViewById(R.id.tutorialImage2Text);
        image3Text = (TextView) findViewById(R.id.tutorialImage3Text);
        tutorialImage1 = (ImageView) findViewById(R.id.tuturialImage1);
        tutorialImage2 = (ImageView) findViewById(R.id.tutorialImage2);
        tutorialImage3 = (ImageView) findViewById(R.id.tutorialImage3);
        startGameButton = (Button) findViewById(R.id.finishTutorialButton);

        // Get all the extras from the bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            objectiveText = extras.getString("OBJECTIVE_TEXT");
            latitude = extras.getString("LATITUDE");
            longitude = extras.getString("LONGITUDE");
            droneName = extras.getString("DRONE_NAME");
            droneUUID = extras.getString("DRONE_UUID");
            useCase = extras.getString("USE_CASE");
            completedList = extras.getStringArrayList("COMPLETED");
        }

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMinigame();
            }
        });

        initializeUIElements();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tutorial, menu);
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

    /**
     * Sets all the UI elements based on the minigame that was started
     */
    private void initializeUIElements(){
        switch(useCase){
            case "Government":
                useCaseTitleText.setText(getString(R.string.tutorial_government_use_case));
                useCaseText.setText(getString(R.string.tutorial_government_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_government_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_government_game_description));
                tutorialImage1.setImageResource(R.drawable.government_drone);
                image1Text.setText(getString(R.string.tutorial_government_image1_text));
                tutorialImage2.setImageResource(R.drawable.government_goal_marker);
                image2Text.setText(getString(R.string.tutorial_government_image2_text));
                tutorialImage3.setImageResource(R.drawable.government_fire);
                image3Text.setText(getString(R.string.tutorial_government_image3_text));
                break;

            case "Energy":
                useCaseTitleText.setText(getString(R.string.tutorial_energy_use_case));
                useCaseText.setText(getString(R.string.tutorial_energy_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_energy_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_energy_game_description));
                tutorialImage1.setImageResource(R.drawable.energy_drone);
                image1Text.setText(getString(R.string.tutorial_energy_image1_text));
                tutorialImage2.setImageResource(R.drawable.energy_pipeline);
                image2Text.setText(getString(R.string.tutorial_energy_image2_text));
                tutorialImage3.setImageResource(R.drawable.energy_pipeline_visited);
                image3Text.setText(R.string.tutorial_energy_image3_text);
                break;

            case "Agriculture":
                useCaseTitleText.setText(getString(R.string.tutorial_agriculture_use_case));
                useCaseText.setText(getString(R.string.tutorial_agriculture_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_agriculture_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_agriculture_game_description));
                tutorialImage1.setImageResource(R.drawable.agriculture_drone);
                image1Text.setText(getString(R.string.tutorial_agriculture_image1_text));
                tutorialImage2.setImageResource(R.drawable.agriculture_animal);
                image2Text.setText(getString(R.string.tutorial_agriculture_image2_text));

                // The agriculture use case has no third image, so make them invisible
                tutorialImage3.setVisibility(View.INVISIBLE);
                image3Text.setVisibility(View.INVISIBLE);
                break;

            case "Insurance":
                useCaseTitleText.setText(getString(R.string.tutorial_insurance_use_case));
                useCaseText.setText(getString(R.string.tutorial_insurance_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_insurance_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_insurance_game_description));
                tutorialImage1.setImageResource(R.drawable.insurance_drone);
                image1Text.setText(getString(R.string.tutorial_insurance_image1_text));
                tutorialImage2.setImageResource(R.drawable.insurance_game_wall);
                image2Text.setText(getString(R.string.tutorial_insurance_image2_text));
                tutorialImage3.setImageResource(R.drawable.insurance_green_room);
                image3Text.setText(getString(R.string.tutorial_insurance_image3_text));
                break;

            case "Media":
                useCaseTitleText.setText(getString(R.string.tutorial_media_use_case));
                useCaseText.setText(getString(R.string.tutorial_media_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_media_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_media_game_description));
                tutorialImage1.setImageResource(R.drawable.media_drone);
                image1Text.setText(getString(R.string.tutorial_media_image1_text));
                tutorialImage2.setImageResource(R.drawable.media_boat);
                image2Text.setText(getString(R.string.tutorial_media_image2_text));
                tutorialImage3.setImageResource(R.drawable.media_obstacle);
                image3Text.setText(getString(R.string.tutorial_media_image3_text));
                break;

            case "Healthcare":
                useCaseTitleText.setText(getString(R.string.tutorial_healthcare_use_case));
                useCaseText.setText(getString(R.string.tutorial_healthcare_game_use_case));
                useCaseDescriptionText.setText(getString(R.string.tutorial_healthcare_use_case_description));
                gameDescriptionText.setText(getString(R.string.tutorial_healthcare_game_description));
                tutorialImage1.setImageResource(R.drawable.healthcare_drone);
                image1Text.setText(getString(R.string.tutorial_healthcare_image1_text));
                tutorialImage2.setImageResource(R.drawable.healthcare_goal_marker);
                image2Text.setText(getString(R.string.tutorial_healthcare_image2_text));
                tutorialImage3.setImageResource(R.drawable.healthcare_landing_zone);
                image3Text.setText(getString(R.string.tutorial_healthcare_image3_text));
                break;

        }
    }

    /**
     * Starts a DroneControlActivity passing all of the extras that this activity received when it was created
     */
    private void startMinigame(){
        Intent minigameIntent = new Intent(this, DroneControlActivity.class);
        minigameIntent.putExtra("OBJECTIVE_TEXT", objectiveText);
        minigameIntent.putExtra("LATITUDE", latitude);
        minigameIntent.putExtra("LONGITUDE", longitude);
        minigameIntent.putExtra("DRONE_NAME", droneName);
        minigameIntent.putExtra("DRONE_UUID", droneUUID);
        minigameIntent.putExtra("USE_CASE", useCase);
        minigameIntent.putExtra("COMPLETED", completedList);
        startActivity(minigameIntent);
    }

    @Override
    public void onBackPressed(){
        // When the back button is pressed, stop the drone
        StopDroneTask stopTask = new StopDroneTask(droneConfigDweetThing, "", UUID.fromString(droneUUID), this, null, "");
        stopTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        super.onBackPressed();
    }

    /**
     * Called by StopDroneTask when a drone is stopped
     * This activity does not need to take any action when a drone is stopped, so the function just returns
     * @param cityButton - The button that was pressed to disable the drone
     * @param city - The string for the city/use case that is being stopped
     */
    @Override
    public void droneStopped(ImageButton cityButton, String city){
        return;
    }

    /**
     * Called by StartDroneTask when a drone is successfully started
     * This activity does not need to take any action when a drone is started, so this function just returns
     * @param buttonPressed - The button that was pressed to start a drone
     * @param droneUUID - The UUID for the drone that was launched
     * @param country - The string for the country/city/use case that the drone was launched in
     * @param droneName - The dweet name for the drone given by the simulator
     */
    @Override
    public void droneStarted(ImageButton buttonPressed, UUID droneUUID, String country, String droneName){
        return;
    }

    /**
     * Called by StartDroneTask when a drone fails to start
     * This activity does not need to take any action when a drone fails to start, so this function just returns
     * @param buttonPressed - The button that was pressed to start a drone
     */
    @Override
    public void droneStartFailed(ImageButton buttonPressed){
        return;
    }
}
