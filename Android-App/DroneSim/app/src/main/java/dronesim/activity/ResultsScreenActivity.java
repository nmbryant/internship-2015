package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dronesim.R;

import java.util.ArrayList;

public class ResultsScreenActivity extends Activity {

    String useCase;

    ImageView keyImage;
    TextView descriptionText;
    TextView useCaseNameText;
    Button launchAnotherButton;
    ArrayList<String> completedList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_screen);

        // Get all the necessary UI components
        keyImage = (ImageView) findViewById(R.id.useCaseImage);
        descriptionText = (TextView) findViewById(R.id.descriptionText);
        useCaseNameText = (TextView) findViewById(R.id.useCaseText);
        launchAnotherButton = (Button) findViewById(R.id.launchAnotherButton);

        // Get the use case name from the extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            useCase = extras.getString("USE_CASE");
            completedList = extras.getStringArrayList("COMPLETED");
            initializeScreen();
        }

        launchAnotherButton.setOnClickListener(new View.OnClickListener() {

            @Override
            // When it is clicked, return to the use case selection screen
            public void onClick(View v) {
                // If use cases still need to be completed, return the user to the UseCaseSelectionActivity
                if (completedList.size() < UseCaseSelectionActivity.NUMBER_OF_USE_CASES) {
                    returnToSelection();
                }
                // If all the use cases have been completed, bring the user to the completion screen
                else {
                    displayCompletionScreen();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results_screen, menu);
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

    @Override
    public void onBackPressed(){
        // If not all the drones have been launched and the back button is pressed, return to
        // UseCaseSelectionActivity
        if (completedList.size() < UseCaseSelectionActivity.NUMBER_OF_USE_CASES) {
            returnToSelection();
        }

        // If all the drones have been launched, display the completion screen
        else {
            displayCompletionScreen();
        }
    }

    /**
     * Initializes the UI components of the screen according to the use case that was completed
     */
    private void initializeScreen(){
        useCaseNameText.setText("Use Case - " + useCase);
        switch (useCase) {
            case "Healthcare":
                keyImage.setImageResource(R.drawable.healthcare_key_image);
                descriptionText.setText(getString(R.string.healthcare_use_case_description));
                break;
            case "Insurance":
                keyImage.setImageResource(R.drawable.insurance_key_image);
                descriptionText.setText(getString(R.string.insurance_use_case_description));
                break;
            case "Agriculture":
                keyImage.setImageResource(R.drawable.agriculture_key_image);
                descriptionText.setText(getString(R.string.agriculture_use_case_description));
                break;
            case "Media":
                keyImage.setImageResource(R.drawable.rsz_media_key_image);
                descriptionText.setText(getString(R.string.media_use_case_description));
                break;
            case "Energy":
                keyImage.setImageResource(R.drawable.energy_key_image);
                descriptionText.setText(getString(R.string.energy_use_case_description));
                break;
            case "Government":
                keyImage.setImageResource(R.drawable.government_key_image);
                descriptionText.setText(getString(R.string.government_use_case_description));
                break;
        }
    }

    /**
     * Returns the user to the UseCaseSelectionActivity screen to launch another drone
     */
    private void returnToSelection(){
        Intent intent = new Intent(this, UseCaseSelectionActivity.class);
        intent.putExtra("COMPLETED", completedList);
        Log.d("ResultsScreen", "Completed List size = " + Integer.toString(completedList.size()));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Displays the demo completion screen, called only after all drones have been launched
     */
    private void displayCompletionScreen(){
        Intent intent = new Intent(this, CompletionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
