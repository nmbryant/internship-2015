package dronesim;

public interface IMiniGame {

    /**
     * Updates the Google Map to show the current position of the drone and the objectives
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    void updateMap(boolean isFirstUpdate, float currentLat, float currentLon);

    /**
     * Creates the goal for the game
     */
    void createGoal();

    /**
     * Updates the game state each time a move is made, and returns true if the game has been completed
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the game is complete, false otherwise
     */
    boolean isGameComplete(float currentLat, float currentLon);

    /**
     * Called when battery hits 0 and the game needs to be restarted
     */
    void restartGame();

    /**
     * Returns true if the game needs to be restarted due to a condition specific to a minigame
     * If a game does not have a special condition that can cause a restart, returns false
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the game needs to be restarted, false otherwise
     */
    boolean isRestart(float currentLat, float currentLon);

    /**
     * Ends the minigame, handles any clean up that needs to be done before quitting the game
     */
    void endGame();
}
