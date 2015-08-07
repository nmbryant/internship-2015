package dronesim;

public interface IMovementMinigame{

    /**
     * Called by the movement task and the function handles moving any game objects that move freely
     * of user input
     */
    void moveGameObjects();

    /**
     * Called by the movement task after moving the game objects and it checks to see if the game needs
     * to be restarted as a result of moving the game objects
     */
    void checkGameStatus();

}
