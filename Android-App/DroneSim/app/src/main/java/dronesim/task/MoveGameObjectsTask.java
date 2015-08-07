package dronesim.task;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import dronesim.IMovementMinigame;

public class MoveGameObjectsTask extends AsyncTask<Void, Void, Void> {

    IMovementMinigame movementMinigame;
    int sleepTime;
    boolean isRunning;
    int startSleepTime;
    boolean isStartup;

    public MoveGameObjectsTask(IMovementMinigame movementMinigame, int sleepTime, int startSleepTime){
        this.movementMinigame = movementMinigame;
        this.sleepTime = sleepTime;
        this.startSleepTime = startSleepTime;
        isRunning = true;
        isStartup = true;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        // While the running boolean is true and it has not been cancelled, move the game objects
        while (isRunning && !isCancelled()){
            try {
                if (isStartup){
                    TimeUnit.MILLISECONDS.sleep(startSleepTime);
                    isStartup = false;
                }
                publishProgress();
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
            catch(Exception e){
                Log.d("MoveGameObjectsTask", "Caught exception moving the animal markers");
                Log.d("MoveGameObjectsTask", e.getMessage());
                Log.d("MoveGameObjectsTask", e.getStackTrace().toString());
                return null;
            }
        }
        return null;
    }

    @Override
    // Updates the UI with the drone data when it publishes progress
    protected void onProgressUpdate(Void... params) {
        // Move the game objects in the minigame that created this task
        movementMinigame.moveGameObjects();

        // Check to see if the minigame needs to be restarted
        movementMinigame.checkGameStatus();
    }

    /**
     * Stops the task by setting the isRunning boolean to false
     */
    public void stopTask(){
        Log.d("MoveGameObjectsTask", "Stopping movement task");
        isRunning = false;
    }
}
