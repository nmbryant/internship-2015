package dronesim.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import dronesim.task.CheckDweetThingTask;

import com.example.dronesim.R;

public class ChooseExistingActivity extends Activity {

    private CheckBox hasKeyBox;
    private TextView keyLabel;
    private TextView errorText;
    private EditText keyEditText;
    private EditText dweetEditText;
    private Button chooseStartButton;

    private boolean hasDweetName = false;
    private boolean hasKey = false;
    private boolean hasKeyText = false;

    private String dweetName;
    private String keyString;
    private String latestDweet = "";
    private String defaultName = "dweet_client";

    private CheckDweetThingTask isValidDweetTask;
    private ChooseExistingActivity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_existing);

        currentActivity = this;

        // Checked if the existing dweet thing has a key
        hasKeyBox = (CheckBox) findViewById(R.id.hasKeyCheckBox);

        // Text View that labels the key edit text entry
        keyLabel = (TextView) findViewById(R.id.keyLabel);

        // Edit Text for entering in a dweet key
        keyEditText = (EditText) findViewById(R.id.keyEditText);

        // Edit Text for entering a dweet thing name
        dweetEditText = (EditText) findViewById(R.id.dweetEditText);
        dweetEditText.setText(defaultName);
        hasDweetName = true;

        // Button to submit the dweet info
        chooseStartButton = (Button) findViewById(R.id.chooseStartButton);

        // Text View that displays error messages
        errorText = (TextView) findViewById(R.id.errorText);

        hasKeyBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // When the key box is checked, display the key entry UI components
                if (hasKeyBox.isChecked()) {
                    hasKey = true;
                    keyLabel.setVisibility(View.VISIBLE);
                    keyEditText.setVisibility(View.VISIBLE);
                }
                // When the key box is not checked, set the visibility of the key entry UI components to invisible
                else {
                    hasKey = false;
                    hasKeyText = false;
                    keyEditText.setText("");
                    keyLabel.setVisibility(View.INVISIBLE);
                    keyEditText.setVisibility(View.INVISIBLE);
                }

            }
        });

        dweetEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                if (!dweetEditText.getText().toString().equals("")) {
                    hasDweetName = true;
                } else {
                    hasDweetName = true;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub

            }

        });

        keyEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (!keyEditText.getText().toString().equals("")) {
                    hasKeyText = true;
                } else {
                    hasKeyText = false;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // TODO Auto-generated method stub

            }

        });

        chooseStartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // If it has no dweet name, display error message
                if (!hasDweetName) {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Enter a dweet name");
                }
                // If key box is checked but has no key text, display error message
                else if (hasKey && !hasKeyText) {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Enter a key or deselect key box");
                }
                // If everything is filled out, check to see if it is a valid dweet object + key
                else {
                    dweetName = dweetEditText.getText().toString();
                    if (hasKey) {
                        keyString = keyEditText.getText().toString();
                    } else {
                        keyString = "";
                    }
                    isValidDweetTask = new CheckDweetThingTask(dweetName, keyString, errorText);
                    isValidDweetTask.execute();
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Checking dweet thing...");
                    while (!isValidDweetTask.getIsFinished()) {

                    }
                    latestDweet = isValidDweetTask.getLatestDweet();
                    goToDataScreen();
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
     * Starts a DataFromExistingActivity passing it dweetName, keyString and latestDweet
     */
    public void goToDataScreen() {
        Intent intent = new Intent(getApplicationContext(), DataFromExistingActivity.class);
        intent.putExtra("DWEET_NAME", dweetName);
        intent.putExtra("KEY_STRING", keyString);
        intent.putExtra("LATEST_DWEET", latestDweet);
        startActivity(intent);
    }

}
