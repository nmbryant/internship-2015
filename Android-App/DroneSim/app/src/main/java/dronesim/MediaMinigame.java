package dronesim;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.dronesim.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dronesim.activity.DroneControlActivity;
import dronesim.task.MoveGameObjectsTask;

public class MediaMinigame implements IMiniGame, IMovementMinigame{

    private static final int CAMERA_ZOOM_VALUE = 14;
    private static final int MAX_DESTINATION_DISTANCE = 2;
    private static final int COORDINATE_DIVISOR = 1000;
    private static final int TARGET_CIRCLE_RADIUS = 275;
    private static final double TARGET_LAT_DISTANCE_FROM_DEST = 0.0025;
    private static final double TARGET_LON_DISTANCE_FROM_DEST = 0.0030;
    private static final int GOAL_NUMBER_OF_MOVES = 50;
    private static final double TARGET_SPEED = 0.0004;
    private static final int MOVEMENT_SLEEP_TIME = 250;
    private static final int STARTUP_MOVEMENT_SLEEP = 2000;
    private static final int OBSTACLE_STROKE_COLOR = Color.rgb(239, 29, 29);
    private static final int OBSTACLE_FILL_COLOR = Color.argb(55, 239, 29, 29);
    private static final int TARGET_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int TARGET_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_RADIUS = 20;

    GoogleMap googleMap;
    float startLat;
    float startLon;
    Marker droneMarker;
    Circle droneCircle;
    Marker targetMarker;
    Circle targetCircle;
    Random rnd;
    LatLng targetCoords;
    int currentMoves = 0;
    MoveGameObjectsTask moveTask;
    DroneControlActivity droneControlActivity;
    List<Polygon> obstaclePolygons = new ArrayList<>();
    List<LatLng> targetPath = new ArrayList<>();
    int currentTargetPathIndex = 0;
    boolean hasTargetStartedMoving = false;
    boolean isDestLatGreater, isDestLonGreater;
    double targetLatitudeSpeed, targetLongitudeSpeed;
    Toast toast;
    boolean isStartup;

    public MediaMinigame(final GoogleMap googleMap, float startLat, float startLon, DroneControlActivity droneControlActivity){
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.droneControlActivity = droneControlActivity;
        rnd = new Random();
        initializeMovementTask();
        isStartup = true;
    }

    /**
     * Initializes the starting position for the target marker
     */
    @Override
    public void createGoal() {
        targetCoords = createDestination();
    }

    /**
     * Checks to see if the drone has moved outside of the target circle or if the drone has crashed
     * into one of the obstacles
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the drone has moved outside the target circle or crashed
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        float destLatFloat = Float.valueOf(Double.toString(targetMarker.getPosition().latitude));
        float destLonFloat = Float.valueOf(Double.toString(targetMarker.getPosition().longitude));
        float latDiff = currentLat - destLatFloat;
        float lonDiff = currentLon - destLonFloat;
        boolean isNearTarget = (Math.abs(latDiff) <= TARGET_LAT_DISTANCE_FROM_DEST && Math.abs(lonDiff) <= TARGET_LON_DISTANCE_FROM_DEST);
        if (!isNearTarget){
            toast = AppUtilities.displayToast("Drone moved out of range - Restarting", toast, droneControlActivity.getApplicationContext());
        }

        // Check to see if the drone has hit any of the obstacles
        LatLng currentPosition = new LatLng(currentLat, currentLon);
        boolean hasHitObstacle = false;
        for (int i = 0; i < obstaclePolygons.size(); i++){
            Polygon currentPolygon = obstaclePolygons.get(i);
            hasHitObstacle = AppUtilities.isPointInPolygon(currentPosition, currentPolygon.getPoints());
            if (hasHitObstacle){
                toast = AppUtilities.displayToast("Drone crashed into obstacle - Restarting", toast, droneControlActivity.getApplicationContext());
                break;
            }
        }
        return (!isNearTarget || hasHitObstacle);
    }

    /**
     * Does the necessary clean-up and resets to restart the game
     */
    @Override
    public void restartGame() {
        droneMarker.remove();
        droneCircle.remove();
        targetMarker.remove();
        targetCircle.remove();
        currentMoves = 0;
        moveTask.stopTask();
        initializeMovementTask();
        targetPath = new ArrayList<>();
        currentTargetPathIndex = 0;
        hasTargetStartedMoving = false;
    }

    /**
     * Updates the map by moving the drone each time it is called
     * If isFirstUpdate is true, it initializes the camera, drone, draws the obstacle polygon, creates the path for the
     * target and initializes the path for the target marker
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    @Override
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon) {
        if (isFirstUpdate){
            // Move the camera to the starting location of the drone
            LatLng startLocation = new LatLng(startLat, startLon);
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(startLocation);
            googleMap.moveCamera(startLocationCamera);
            CameraUpdate zoomCamera = CameraUpdateFactory.zoomTo(CAMERA_ZOOM_VALUE);
            googleMap.moveCamera(zoomCamera);

            // Add and save the drone location marker
            LatLng newDroneCoords = new LatLng(currentLat, currentLon);
            MarkerOptions newDroneLocation = new MarkerOptions().position(newDroneCoords)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_drone_icon))
                    .title("Your Drone");
            droneMarker = googleMap.addMarker(newDroneLocation);

            // Add the circle for the drone marker
            CircleOptions droneCircleOptions = new CircleOptions().center(newDroneCoords)
                    .radius(DRONE_CIRCLE_RADIUS)
                    .strokeColor(DRONE_CIRCLE_STROKE_COLOR)
                    .fillColor(DRONE_CIRCLE_FILL_COLOR);
            droneCircle = googleMap.addCircle(droneCircleOptions);

            // Add and save the target location marker
            MarkerOptions targetMarkerOptions = new MarkerOptions().position(targetCoords)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.boat_icon))
                    .title("Target");
            targetMarker = googleMap.addMarker(targetMarkerOptions);

            // Add the circle around the target marker
            CircleOptions targetCircleOptions = new CircleOptions().center(targetCoords).radius(TARGET_CIRCLE_RADIUS)
                    .strokeColor(TARGET_CIRCLE_STROKE_COLOR)
                    .fillColor(TARGET_CIRCLE_FILL_COLOR);
            targetCircle = googleMap.addCircle(targetCircleOptions);

            // Initialize the path for the target marker
            initializeTargetPath();
        }

        if (isStartup){
            // Draw the obstacles onto the map
            drawObstacles();

            isStartup = false;
        }

        // Move the drone marker
        LatLng newDronePosition = new LatLng(currentLat, currentLon);
        droneMarker.setPosition(newDronePosition);
        droneCircle.setCenter(newDronePosition);
    }

    /**
     * Checks to see if the player has gotten enough points to complete the game
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the player has reached the target points, false otherwise
     */
    @Override
    public boolean isGameComplete(float currentLat, float currentLon) {
        currentMoves += 1;
        // Display a toast to indicate progress if certain milestones are hit
        // Display a toast when user gets 10 points
        if (currentMoves == 10){
            toast = AppUtilities.displayToast("Good start - " + Integer.toString(GOAL_NUMBER_OF_MOVES - 10) + " points left",
                    toast, droneControlActivity.getApplicationContext());
        }
        // Display a toast when the user is halfway to the target number of points
        else if (currentMoves == GOAL_NUMBER_OF_MOVES / 2){
            toast = AppUtilities.displayToast("Halfway there - " + Integer.toString(GOAL_NUMBER_OF_MOVES / 2) + " points left",
                    toast, droneControlActivity.getApplicationContext());
        }
        // Display a toast when the user is 10 points away from the goal
        else if (currentMoves == GOAL_NUMBER_OF_MOVES - 10){
            toast = AppUtilities.displayToast("Almost there - 10 points left", toast, droneControlActivity.getApplicationContext());
        }

        return (currentMoves >= GOAL_NUMBER_OF_MOVES);
    }

    /**
     * Stops the task that moves the game objects
     */
    @Override
    public void endGame(){
        moveTask.stopTask();
    }

    /**
     * Generates the starting position for the target marker
     * @return - The LatLng coordinates for the starting position of the target marker
     */
    private LatLng createDestination(){
        // Generate a random number for latitude to add to the starting location for the destination
        double latDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        boolean isLatNegative = rnd.nextBoolean();
        if (isLatNegative){
            latDistance = 0 - latDistance;
        }
        latDistance += 1;
        double realLatDistance = latDistance/COORDINATE_DIVISOR;

        // Generate a random number for longitude and add to the starting location for the destination
        double longDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        boolean isLongNegative = rnd.nextBoolean();
        if (isLongNegative){
            longDistance = 0 - longDistance;
        }
        longDistance += 1;
        double realLongDistance = longDistance/COORDINATE_DIVISOR;

        // Add the two random numbers to the starting coordinates to generate a destination
        double newLat = startLat + realLatDistance;
        double newLong = startLon + realLongDistance;
        return new LatLng(newLat, newLong);
    }

    /**
     * Moves the target marker and the circle that goes with it according to the path that was created for it
     */
    @Override
    public void moveGameObjects(){
        // Get the target and current destination coordinates
        LatLng targetPosition = targetMarker.getPosition();
        LatLng destinationPosition = targetPath.get(currentTargetPathIndex);

        double targetLat = targetPosition.latitude;
        double targetLon = targetPosition.longitude;
        double destinationLat = destinationPosition.latitude;
        double destinationLon = destinationPosition.longitude;

        double newLat = targetPosition.latitude;
        double newLon = targetPosition.longitude;
        boolean isDestLatReached = false;
        boolean isDestLonReached = false;

        // If the minigame is just starting or if the target has just reached a path node, determine
        // where the destination coords are in relation to the target position
        if (!hasTargetStartedMoving){
            isDestLatGreater = (targetLat < destinationLat);
            isDestLonGreater = (targetLon < destinationLon);

            // Determine whether the latitude destination or longitude destination is further from the target
            double latDiff = Math.abs(destinationLat - targetLat);
            double lonDiff = Math.abs(destinationLon - targetLon);

            if (latDiff > lonDiff){
                double numberOfMoves = Math.ceil(latDiff / TARGET_SPEED);
                targetLatitudeSpeed = TARGET_SPEED;
                targetLongitudeSpeed = lonDiff / numberOfMoves;
            }
            else {
                double numberOfMoves = Math.ceil(lonDiff / TARGET_SPEED);
                targetLatitudeSpeed = latDiff / numberOfMoves;
                targetLongitudeSpeed = TARGET_SPEED;
            }

            hasTargetStartedMoving = true;
        }

        // If the target latitude is greater than the destination latitude but it originally the dest
        // lat was greater, then the destination latitude has been reached/passed by the target
        if (targetLat >= destinationLat && isDestLatGreater){
            isDestLatReached = true;
        }

        // If the target latitude is less than the destination latitude but it originally the dest
        // lat was lesser, then the destination latitude has been reached/passed by the target
        else if (targetLat < destinationLat && !isDestLatGreater){
            isDestLatReached = true;
        }

        // If the destination latitude has not been reached, determine whether to increment or decrement
        // the current latitude of the target
        else if (targetLat < destinationLat){
            newLat = targetLat + targetLatitudeSpeed;
        }
        else {
            newLat = targetLat - targetLatitudeSpeed;
        }

        // If the target longitude is greater than the destination longitude but it originally the dest
        // lon was greater, then the destination longitude has been reached/passed by the target
        if (targetLon >= destinationLon && isDestLonGreater){
            isDestLonReached = true;
        }

        // If the target longitude is less than the destination longitude but it originally the dest
        // lon was lesser, then the destination longitude has been reached/passed by the target
        else if (targetLon < destinationLon && !isDestLonGreater){
            isDestLonReached = true;
        }

        // If the destination longitude has not been reached, determine whether to increment or decrement
        // the current longitude of the target
        else if (targetLon < destinationLon){
            newLon = targetLon + targetLongitudeSpeed;
        }
        else {
            newLon = targetLon - targetLongitudeSpeed;
        }

        // If the destination has been reached, increment the path index
        if (isDestLatReached && isDestLonReached){
            Log.d("MediaMinigame", "Incrementing path index");
            currentTargetPathIndex += 1;

            // Reset the path index if it becomes larger than or equal to the path size
            if (currentTargetPathIndex >= targetPath.size()){
                currentTargetPathIndex = 0;
            }

            // Reset the hasTargetStartedMoving boolean so that it will re-determine where the dest
            // points are in relation to the target
            hasTargetStartedMoving = false;
        }

        // Set the position of the target marker to the new position
        LatLng newTargetPosition = new LatLng(newLat, newLon);
        targetMarker.setPosition(newTargetPosition);

        // Update the target circle
        targetCircle.setCenter(newTargetPosition);
    }

    /**
     * Creates and starts a MoveGameObjectsTask
     */
    private void initializeMovementTask(){
        moveTask = new MoveGameObjectsTask(this, MOVEMENT_SLEEP_TIME, STARTUP_MOVEMENT_SLEEP);
        moveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    /**
     * Checks to see if the drone has moved out of the target circle and restarts the game if that
     * has happened
     */
    public void checkGameStatus(){
        double droneLat = droneMarker.getPosition().latitude;
        double droneLon = droneMarker.getPosition().longitude;
        float droneLatFloat = Float.parseFloat(Double.toString(droneLat));
        float droneLonFloat = Float.parseFloat(Double.toString(droneLon));
        boolean isRestart = isRestart(droneLatFloat, droneLonFloat);

        // If the restart conditions are met, tell the DroneControlActivity to restart the game
        if (isRestart){
            droneControlActivity.restartGame();
        }
    }

    /**
     * Draws and saves all of the obstacle polygons
     */
    private void drawObstacles(){
        // Obstacle 1
        PolygonOptions obstacle1Options = new PolygonOptions().add(new LatLng(25.78924220270823, -80.17174243927002))
                .add(new LatLng(25.78917264818695, -80.17473578453064))
                .add(new LatLng(25.789655659940244, -80.17476797103882))
                .add(new LatLng(25.789462455475032, -80.1793384552002))
                .add(new LatLng(25.790872840832236, -80.1793920993805))
                .add(new LatLng(25.791201284034287, -80.17163515090942))
                .add(new LatLng(25.78924220270823, -80.17174243927002))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle1Polygon = googleMap.addPolygon(obstacle1Options);
        obstaclePolygons.add(obstacle1Polygon);

        // Obstacle 2
        PolygonOptions obstacle2Options = new PolygonOptions().add(new LatLng(25.79129788488961, -80.17033696174622))
                .add(new LatLng(25.791017742192306, -80.1707124710083))
                .add(new LatLng(25.790409154051858, -80.17098069190979))
                .add(new LatLng(25.78975226175999, -80.17075538635254))
                .add(new LatLng(25.78938517360079, -80.16999363899231))
                .add(new LatLng(25.789617019072374, -80.16634583473206))
                .add(new LatLng(25.790080708655633, -80.1658308506012))
                .add(new LatLng(25.79063133738581, -80.16575574874878))
                .add(new LatLng(25.79113366338872, -80.16599178314209))
                .add(new LatLng(25.79149108636416, -80.16672134399414))
                .add(new LatLng(25.79129788488961, -80.17033696174622))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle2Polygon = googleMap.addPolygon(obstacle2Options);
        obstaclePolygons.add(obstacle2Polygon);

        // Obstacle 3
        PolygonOptions obstacle3Options = new PolygonOptions().add(new LatLng(25.793508090987885, -80.16307353973389))
                .add(new LatLng(25.793324552716957, -80.16233325004578))
                .add(new LatLng(25.792715976417178, -80.16193091869354))
                .add(new LatLng(25.788851927030507, -80.16169488430023))
                .add(new LatLng(25.788397893107867, -80.16189873218536))
                .add(new LatLng(25.78798249871246, -80.1626604795456))
                .add(new LatLng(25.78834959150873, -80.16363680362701))
                .add(new LatLng(25.78897751061528, -80.1638674736023))
                .add(new LatLng(25.79260971674289, -80.1640659570694))
                .add(new LatLng(25.79328108308486, -80.16369044780731))
                .add(new LatLng(25.793508090987885, -80.16307353973389))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle3Polygon = googleMap.addPolygon(obstacle3Options);
        obstaclePolygons.add(obstacle3Polygon);

        // Obstacle 4
        PolygonOptions obstacle4Options = new PolygonOptions().add(new LatLng(25.786216573392025, -80.15862107276917))
                .add(new LatLng(25.786564350721875, -80.15789151191711))
                .add(new LatLng(25.78712465649702, -80.15761256217957))
                .add(new LatLng(25.794833422038742, -80.15807390213013))
                .add(new LatLng(25.79551926539537, -80.15863180160522))
                .add(new LatLng(25.795644841918218, -80.15925407409668))
                .add(new LatLng(25.79530675097673, -80.15996217727661))
                .add(new LatLng(25.794833422038742, -80.16024112701416))
                .add(new LatLng(25.78697008965106, -80.15981197357178))
                .add(new LatLng(25.786467746010345, -80.15942573547363))
                .add(new LatLng(25.786216573392025, -80.15862107276917))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle4Polygon = googleMap.addPolygon(obstacle4Options);
        obstaclePolygons.add(obstacle4Polygon);

        // Obstacle 5
        PolygonOptions obstacle5Options = new PolygonOptions().add(new LatLng(25.78772360112232, -80.15480160713196))
                .add(new LatLng(25.78826458076443, -80.15392184257507))
                .add(new LatLng(25.788728275636238, -80.15373945236206))
                .add(new LatLng(25.793616281806045, -80.15405058860779))
                .add(new LatLng(25.794273152690135, -80.15450119972229))
                .add(new LatLng(25.794466349316348, -80.15517711639404))
                .add(new LatLng(25.79408961560362, -80.15599250793457))
                .add(new LatLng(25.79346172342204, -80.15632510185242))
                .add(new LatLng(25.78846744749396, -80.15597105026245))
                .add(new LatLng(25.787907148063233, -80.15557408332825))
                .add(new LatLng(25.78772360112232, -80.15480160713196))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle5Polygon = googleMap.addPolygon(obstacle5Options);
        obstaclePolygons.add(obstacle5Polygon);

        // Obstacle 6
        PolygonOptions obstacle6Options = new PolygonOptions().add(new LatLng(25.787785427727883, -80.17918825149536))
                .add(new LatLng(25.787862710055084, -80.17729997634888))
                .add(new LatLng(25.787321728579677, -80.17661333084106))
                .add(new LatLng(25.787379691588274, -80.17513275146484))
                .add(new LatLng(25.78699327492663, -80.1738452911377))
                .add(new LatLng(25.786915991443205, -80.17300844192505))
                .add(new LatLng(25.780713827712813, -80.1716136932373))
                .add(new LatLng(25.783322253844958, -80.17794370651245))
                .add(new LatLng(25.786896670564502, -80.17905950546265))
                .add(new LatLng(25.78699327492663, -80.17935991287231))
                .add(new LatLng(25.787785427727883, -80.17918825149536))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle6Polygon = googleMap.addPolygon(obstacle6Options);
        obstaclePolygons.add(obstacle6Polygon);

        // Obstacle 7
        PolygonOptions obstacle7Options = new PolygonOptions().add(new LatLng(25.77594122550141, -80.16775131225586))
                .add(new LatLng(25.7811775521063, -80.17998218536377))
                .add(new LatLng(25.779361287877567, -80.18092632293701))
                .add(new LatLng(25.779496542555037, -80.18126964569092))
                .add(new LatLng(25.778105344222617, -80.18187046051025))
                .add(new LatLng(25.77456930836872, -80.1788878440857))
                .add(new LatLng(25.772057306310536, -80.17294406890869))
                .add(new LatLng(25.77594122550141, -80.16775131225586))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle7Polygon = googleMap.addPolygon(obstacle7Options);
        obstaclePolygons.add(obstacle7Polygon);

        // Obstacle 8
        PolygonOptions obstacle8Options = new PolygonOptions().add(new LatLng(25.78237163383952, -80.16804099082947))
                .add(new LatLng(25.78282569098072, -80.16750454902649))
                .add(new LatLng(25.782960941708044, -80.16680717468262))
                .add(new LatLng(25.778652164166175, -80.15674352645874))
                .add(new LatLng(25.778217413345494, -80.15637874603271))
                .add(new LatLng(25.777502486309604, -80.15633583068848))
                .add(new LatLng(25.77694213508067, -80.15690445899963))
                .add(new LatLng(25.776884167561104, -80.15771985054016))
                .add(new LatLng(25.78116402660129, -80.16776204109192))
                .add(new LatLng(25.781869270721828, -80.168137550354))
                .add(new LatLng(25.78237163383952, -80.16804099082947))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle8Polygon = googleMap.addPolygon(obstacle8Options);
        obstaclePolygons.add(obstacle8Polygon);

        // Obstacle 9
        PolygonOptions obstacle9Options = new PolygonOptions().add(new LatLng(25.785388665915743, -80.16545534133911))
                .add(new LatLng(25.785799239132068, -80.16489207744598))
                .add(new LatLng(25.78584754176995, -80.16417324542999))
                .add(new LatLng(25.78229724547106, -80.15589594841003))
                .add(new LatLng(25.781804542893617, -80.15549898147583))
                .add(new LatLng(25.781104128844333, -80.15548825263977))
                .add(new LatLng(25.780592099405336, -80.15596032142639))
                .add(new LatLng(25.780471337423116, -80.15677034854889))
                .add(new LatLng(25.784156937738757, -80.16517639160156))
                .add(new LatLng(25.78480420041483, -80.1655626296997))
                .add(new LatLng(25.785388665915743, -80.16545534133911))
                .strokeColor(OBSTACLE_STROKE_COLOR)
                .fillColor(OBSTACLE_FILL_COLOR);
        Polygon obstacle9Polygon = googleMap.addPolygon(obstacle9Options);
        obstaclePolygons.add(obstacle9Polygon);
    }

    /**
     * Creates and saves the path for the target marker to follow when moving
     */
    private void initializeTargetPath(){
        // Point 1
        LatLng pathPoint1 = new LatLng(25.78591902928415, -80.16058444976807);
        targetPath.add(pathPoint1);

        // Point 2
        LatLng pathPoint2 = new LatLng(25.79642920901216, -80.16127109527588);
        targetPath.add(pathPoint2);

        // Point 3
        LatLng pathPoint3 = new LatLng(25.79642920901216, -80.15719413757324);
        targetPath.add(pathPoint3);

        // Point 4
        LatLng pathPoint4 = new LatLng(25.784141480322646, -80.15633583068848);
        targetPath.add(pathPoint4);

        // Point 5
        LatLng pathPoint5 = new LatLng(25.787967041794982, -80.16972541809082);
        targetPath.add(pathPoint5);

        // Point 6
        LatLng pathPoint6 = new LatLng(25.77907918406073, -80.16998291015625);
        targetPath.add(pathPoint6);
    }
}
