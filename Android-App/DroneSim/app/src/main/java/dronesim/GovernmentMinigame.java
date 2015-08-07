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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dronesim.activity.DroneControlActivity;
import dronesim.task.MoveGameObjectsTask;

public class GovernmentMinigame implements IMiniGame, IMovementMinigame{

    private static final int CAMERA_ZOOM_VALUE = 14;
    private static final double GOAL_LAT_DISTANCE = 0.015;
    private static final int GOAL_GRID_LAT_DISTANCE = 30;
    private static final double GOAL_LON_DISTANCE = 0.015;
    private static final int GOAL_GRID_LON_DISTANCE = 30;
    private static final int GOAL_CIRCLE_RADIUS = 100;
    private static final double TARGET_DISTANCE_FROM_DEST = 0.001;
    private static final double DRONE_MOVEMENT_DISTANCE = 0.0005;
    private static final int DRONE_GRID_VALUE = 1;
    private static final int GOAL_GRID_VALUE = 2;
    private static final int FIRE_MOVEMENT_SLEEP = 750;
    private static final int STARTUP_MOVEMENT_SLEEP = 100;
    private static final int GOAL_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int GOAL_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int FIRE_STROKE_COLOR = Color.rgb(227, 140, 45);
    private static final int FIRE_FILL_COLOR = Color.argb(55, 227, 140, 45);
    private static final int DRONE_CIRCLE_RADIUS = 20;
    private static final int GAME_TIME_LIMIT = 30;

    GoogleMap googleMap;
    float startLat;
    float startLon;
    double goalLat;
    double goalLon;
    double goalToDroneLatMidpoint;
    double goalToDroneLonMidpoint;
    Marker droneMarker;
    Circle droneCircle;
    Marker goalMarker;
    Circle goalCircle;
    LatLngBounds visibleBounds;
    List<int[]> movementGrid = new ArrayList<>();
    List<LatLng[]> gridToCoords = new ArrayList<>();
    Random rnd;
    boolean areFirePolygonsDrawn = false;
    int droneGridStartX;
    int droneGridStartY;
    int goalGridX;
    int goalGridY;
    MoveGameObjectsTask moveTask;
    boolean areObjectsMoving = false;
    DroneControlActivity droneControlActivity;
    Toast toast;
    FireObstacle fireObstacle1;
    List<FireObstacle> allFireObstacles = new ArrayList<>();
    int timerValue = 0;

    public GovernmentMinigame(final GoogleMap googleMap, float startLat, float startLon, DroneControlActivity droneControlActivity){
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.droneControlActivity = droneControlActivity;
        rnd = new Random();
        // When the camera moves, it saves the visible bounds
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                visibleBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng boundSouthWest = new LatLng(visibleBounds.southwest.latitude, visibleBounds.southwest.longitude);
                LatLng boundNorthEast = new LatLng(visibleBounds.northeast.latitude, visibleBounds.northeast.longitude);
                visibleBounds = new LatLngBounds(boundSouthWest, boundNorthEast);
                // Now that the visible bounds are initialized, create the grid and draw the fire polygons
                if (!areFirePolygonsDrawn) {
                    createGrid();
                    addFireObstacles();
                }
                areFirePolygonsDrawn = true;
                if (!areObjectsMoving) {
                    initializeMoveTask();
                }
            }
        });
    }

    /**
     * On the first update it initializes the fire obstacles and the drone + goal markers
     * On every other update, it moves the drone marker and circle
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    @Override
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon) {
        if (isFirstUpdate){
            // Move the camera to the starting location of the drone
            LatLng startLocation = new LatLng(goalToDroneLatMidpoint, goalToDroneLonMidpoint);
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(startLocation);
            googleMap.moveCamera(startLocationCamera);
            CameraUpdate zoomCamera = CameraUpdateFactory.zoomTo(CAMERA_ZOOM_VALUE);
            googleMap.moveCamera(zoomCamera);

            // Set the boundary of the map screen
            visibleBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;

            // Add and save the drone location marker
            LatLng newDroneCoords = new LatLng(currentLat, currentLon);
            MarkerOptions newDroneLocation = new MarkerOptions().position(newDroneCoords)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(0))
                    .title("Your Drone")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_drone_icon));

            droneMarker = googleMap.addMarker(newDroneLocation);

            // Add a circle around the drone marker to indicate the hitbox
            CircleOptions droneCircleOptions = new CircleOptions().center(newDroneCoords)
                    .radius(DRONE_CIRCLE_RADIUS)
                    .strokeColor(DRONE_CIRCLE_STROKE_COLOR)
                    .fillColor(DRONE_CIRCLE_FILL_COLOR);
            droneCircle = googleMap.addCircle(droneCircleOptions);

            // Add and save the goal marker
            LatLng goalCoords = new LatLng(goalLat, goalLon);
            MarkerOptions goalLocation = new MarkerOptions().position(goalCoords)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title("Destination");
            goalMarker = googleMap.addMarker(goalLocation);

            // Draw a circle around the goal marker
            CircleOptions destinationCircle = new CircleOptions().center(goalCoords)
                    .radius(GOAL_CIRCLE_RADIUS)
                    .strokeColor(GOAL_CIRCLE_STROKE_COLOR)
                    .fillColor(GOAL_CIRCLE_FILL_COLOR);
            goalCircle = googleMap.addCircle(destinationCircle);
        }

        // Move the drone marker and circle
        LatLng newDronePosition = new LatLng(currentLat, currentLon);
        droneMarker.setPosition(newDronePosition);
        droneCircle.setCenter(newDronePosition);
    }

    /**
     * Initializes are the array lists, resets the timer value, clears the map and stops the MoveGameObjectsTask
     */
    @Override
    public void restartGame() {
        timerValue = 0;
        allFireObstacles = new ArrayList<>();
        movementGrid = new ArrayList<>();
        gridToCoords = new ArrayList<>();
        areFirePolygonsDrawn = false;
        googleMap.clear();
        moveTask.stopTask();
        areObjectsMoving = false;
    }

    /**
     * Calculates the coordinates for the goal, as well as the coordinates for the camera to focus on
     */
    @Override
    public void createGoal() {
        // Calculate the coordinates for the goal marker
        goalLat = startLat + GOAL_LAT_DISTANCE;
        goalLon = startLon + GOAL_LON_DISTANCE;

        // Calculate the midpoint between the drone and the goal for the camera to use
        goalToDroneLatMidpoint = (goalLat + startLat) / 2;
        goalToDroneLonMidpoint = (goalLon + startLon) / 2;
    }

    /**
     * The game is complete when the drone reaches the goal marker
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the drone is within the goal marker circle
     */
    @Override
    public boolean isGameComplete(float currentLat, float currentLon) {
        float destLatFloat = Float.valueOf(Double.toString(goalLat));
        float destLonFloat = Float.valueOf(Double.toString(goalLon));
        float latDiff = currentLat - destLatFloat;
        float lonDiff = currentLon - destLonFloat;

        // Game is complete if the drone is within the target distance of the goal marker
        return (Math.abs(latDiff) <= TARGET_DISTANCE_FROM_DEST && Math.abs(lonDiff) <= TARGET_DISTANCE_FROM_DEST);
    }

    /**
     * The game needs to be restarted if the drone hits the fire polygons
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the drone is in one of the fire polygons
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        LatLng currentLocation = new LatLng(currentLat, currentLon);

        // Iterate through all of the fire obstacles and check to see if the drone is in any of the
        // fire polygons
        for (int i = 0; i < allFireObstacles.size(); i++){
            FireObstacle currentObstacle = allFireObstacles.get(i);
            // If the drone is in one of the fire polygons, return true since the game does need to restart
            if (AppUtilities.isPointInPolygon(currentLocation, currentObstacle.getAllPolygonPoints())){
                toast = AppUtilities.displayToast("Drone burned in fire - Restarting", toast, droneControlActivity.getApplicationContext());
                return true;
            }
        }
        return false;
    }

    /**
     * When the game ends, the MoveGameObjectsTask needs to be stopped
     */
    @Override
    public void endGame(){
        moveTask.stopTask();
    }

    /**
     * Converts the map to a grid of points and maps each point to a LatLng point
     * Randomly generates a path from the drone to the goal, and draws the fire polygons around the path
     */
    private void createGrid(){
        // Get the minimum and maximum visible latitudes/longitudes
        double minVisibleLat = visibleBounds.southwest.latitude;
        double minVisibleLon = visibleBounds.southwest.longitude;
        double maxVisibleLat = visibleBounds.northeast.latitude;
        double maxVisibleLon = visibleBounds.northeast.longitude;

        double minDroneLat = startLat;
        double minDroneLon = startLon;
        double maxDroneLat = startLat;
        double maxDroneLon = startLon;

        // Calculate the minimum latitude that the drone can move to
        while (minDroneLat >= minVisibleLat){
            minDroneLat -= DRONE_MOVEMENT_DISTANCE;
        }
        
        // Calculate the minimum longitude that the drone can move to
        while (minDroneLon >= minVisibleLon){
            minDroneLon -= DRONE_MOVEMENT_DISTANCE;
        }
        
        // Calculate the maximum latitude that the drone can move to
        while (maxDroneLat <= maxVisibleLat){
            maxDroneLat += DRONE_MOVEMENT_DISTANCE;
        }

        // Calculate the maximum longitude that the drone can move to
        while (maxDroneLon <= maxVisibleLon){
            maxDroneLon += DRONE_MOVEMENT_DISTANCE;
        }

        // Calculate the width of the grid based on the movement distance of the drone
        double gridWidth = (maxDroneLon - minDroneLon) / DRONE_MOVEMENT_DISTANCE;
        double gridHeight = (maxDroneLat - minDroneLat) / DRONE_MOVEMENT_DISTANCE;

        int gridWidthInt = (int) gridWidth;
        int gridHeightInt = (int) gridHeight;

        double currentLat = maxDroneLat;
        double currentLon = minDroneLon;

        int droneGridX = 0;
        int droneGridY = 0;
        goalGridX = 0;
        goalGridY = 0;

        // Map the movement coordinates to latitude and longitude points
        for (int j = 0; j < gridHeightInt; j += 1){
            LatLng[] currentRow = new LatLng [gridWidthInt];
            for (int i = 0; i < gridWidthInt; i += 1){
                LatLng currentPos = new LatLng(currentLat, currentLon);

                // If the current coordinates matches the drone, save the points and determine where the
                // goal will be placed in the grid
                if (currentLat == startLat && currentLon == startLon){
                    droneGridY = i;
                    droneGridX = j;
                    droneGridStartX = j;
                    droneGridStartY = i;
                    goalGridX = droneGridX - GOAL_GRID_LAT_DISTANCE;
                    goalGridY = droneGridY + GOAL_GRID_LON_DISTANCE;
                }

                // Add the current point to the grid row
                currentRow[i] = currentPos;
                currentLon += DRONE_MOVEMENT_DISTANCE;
            }
            // Move the latitude down and bring longitude back to the start
            currentLat -= DRONE_MOVEMENT_DISTANCE;
            currentLon = minDroneLon;

            // Add the row to the grid
            gridToCoords.add(j, currentRow);
        }

        // Create the movement grid
        for (int j = 0; j < gridHeightInt; j += 1){
            int[] currentRow = new int [gridWidthInt];
            for (int i = 0; i < gridWidthInt; i += 1){
                // If the point matches the drone point, mark it with the drone grid value
                if (i == droneGridX && j == droneGridY){
                    currentRow[i] = DRONE_GRID_VALUE;
                }
                // If the point matches the goal point, mark it with the goal grid value
                else if(i == goalGridX && j == goalGridY){
                    currentRow[i] = GOAL_GRID_VALUE;
                }
                // Otherwise, mark it with a 0
                else{
                    currentRow[i] = 0;
                }
            }
            // Add the row to the movement grid
            movementGrid.add(j, currentRow);
        }
    }

    /**
     * Expands all the fire obstacles
     */
    public void moveGameObjects(){
        // Expand all of the fire obstacles
        for (int i = 0; i < allFireObstacles.size(); i++){
            FireObstacle currentObstacle = allFireObstacles.get(i);
            currentObstacle.expandFireObstacle();
        }
        timerValue += 1;
    }

    /**
     * Creates a MoveGameObjectsTask and starts it
     * Sets the areObjectsMoving boolean to true so that it will not start another MoveGameObjectsTask
     */
    private void initializeMoveTask(){
        moveTask = new MoveGameObjectsTask(this, FIRE_MOVEMENT_SLEEP, STARTUP_MOVEMENT_SLEEP);
        moveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        areObjectsMoving = true;
    }

    /**
     * Checks to see if the game needs to be restarted as a result of moving the fire obstacles
     */
    @Override
    public void checkGameStatus(){
        double droneLat = droneMarker.getPosition().latitude;
        double droneLon = droneMarker.getPosition().longitude;
        float droneLatFloat = Float.parseFloat(Double.toString(droneLat));
        float droneLonFloat = Float.parseFloat(Double.toString(droneLon));
        boolean isRestart = isRestart(droneLatFloat, droneLonFloat);

        // If the game needs to be restarted, it tells the DroneControlActivity to restart
        if (isRestart){
            droneControlActivity.restartGame();
        }
    }

    /**
     * Initializes all the fire obstacles and adds them to the map
     */
    private void addFireObstacles(){
        // Fire obstacle 1
        List<LatLng> obstacle1Points = new ArrayList<>();
        obstacle1Points.add(gridToCoords.get(10)[22]);
        obstacle1Points.add(gridToCoords.get(12)[21]);
        obstacle1Points.add(gridToCoords.get(11)[23]);
        obstacle1Points.add(gridToCoords.get(12)[25]);
        obstacle1Points.add(gridToCoords.get(10)[27]);
        obstacle1Points.add(gridToCoords.get(10)[30]);
        obstacle1Points.add(gridToCoords.get(9)[28]);
        obstacle1Points.add(gridToCoords.get(7)[29]);
        obstacle1Points.add(gridToCoords.get(8)[25]);
        obstacle1Points.add(gridToCoords.get(7)[20]);
        obstacle1Points.add(gridToCoords.get(10)[22]);
        fireObstacle1 = new FireObstacle(obstacle1Points, 20, 20);
        fireObstacle1.addFireObstacleToMap();
        allFireObstacles.add(fireObstacle1);

        // Fire Obstacle 2
        List<LatLng> obstacle2Points = new ArrayList<>();
        obstacle2Points.add(gridToCoords.get(22)[45]);
        obstacle2Points.add(gridToCoords.get(23)[46]);
        obstacle2Points.add(gridToCoords.get(22)[48]);
        obstacle2Points.add(gridToCoords.get(24)[50]);
        obstacle2Points.add(gridToCoords.get(26)[48]);
        obstacle2Points.add(gridToCoords.get(25)[44]);
        obstacle2Points.add(gridToCoords.get(22)[45]);
        FireObstacle fireObstacle2 = new FireObstacle(obstacle2Points, 18, 18);
        fireObstacle2.addFireObstacleToMap();
        allFireObstacles.add(fireObstacle2);

        // Fire Obstacle 3
        List<LatLng> obstacle3Points = new ArrayList<>();
        obstacle3Points.add(gridToCoords.get(21)[10]);
        obstacle3Points.add(gridToCoords.get(22)[12]);
        obstacle3Points.add(gridToCoords.get(23)[10]);
        obstacle3Points.add(gridToCoords.get(22)[8]);
        FireObstacle fireObstacle3 = new FireObstacle(obstacle3Points, 25, 25);
        fireObstacle3.addFireObstacleToMap();
        allFireObstacles.add(fireObstacle3);

        // Fire Obstacle 4
        List<LatLng> obstacle4Points = new ArrayList<>();
        obstacle4Points.add(gridToCoords.get(31)[25]);
        obstacle4Points.add(gridToCoords.get(32)[26]);
        obstacle4Points.add(gridToCoords.get(32)[24]);
        obstacle4Points.add(gridToCoords.get(31)[25]);
        FireObstacle fireObstacle4 = new FireObstacle(obstacle4Points, 15, 15);
        fireObstacle4.addFireObstacleToMap();
        allFireObstacles.add(fireObstacle4);

        // Fire Obstacle 5
        List<LatLng> obstacle5Points = new ArrayList<>();
        obstacle5Points.add(gridToCoords.get(29)[32]);
        obstacle5Points.add(gridToCoords.get(29)[35]);
        obstacle5Points.add(gridToCoords.get(31)[33]);
        obstacle5Points.add(gridToCoords.get(29)[32]);
        FireObstacle fireObstacle5 = new FireObstacle(obstacle5Points, 20, 20);
        fireObstacle5.addFireObstacleToMap();
        allFireObstacles.add(fireObstacle5);
    }

    private class FireObstacle {

        List<LatLng> topLeftPoints = new ArrayList<>();
        List<LatLng> topRightPoints = new ArrayList<>();
        List<LatLng> botLeftPoints = new ArrayList<>();
        List<LatLng> botRightPoints = new ArrayList<>();
        List<LatLng> allPolygonPoints = new ArrayList<>();
        Polygon fireObstaclePolygon;
        int maxWidth;
        int maxHeight;
        boolean isMaxSizeReached = false;

        /**
         * Constructor for a fire obstacle, takes in the points that it will start at
         * @param allPolygonPoints
         */
        public FireObstacle(List<LatLng> allPolygonPoints, int maxWidth, int maxHeight){
            this.allPolygonPoints = allPolygonPoints;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            initializeFireObstacle();
        }

        /**
         * Initializes the obstacle by determining which quadrant each of the points lie in which is needed
         * to know when the fire obstacle is expanded
         */
        private void initializeFireObstacle(){
            double minLat = -10000;
            double maxLat = -10000;
            double minLon = -10000;
            double maxLon = -10000;
            double midLat, midLon;

            // Determine the min/max latitudes and longitudes in order to determine which quadrant
            // a point lies in
            for (int i = 0; i < allPolygonPoints.size(); i++){
                LatLng currentPoint = allPolygonPoints.get(i);
                double currentLat = currentPoint.latitude;
                double currentLon = currentPoint.longitude;
                if (currentLat < minLat || minLat == -10000){
                    minLat = currentLat;
                }
                else if (currentLat > maxLat || maxLat == -10000){
                    maxLat = currentLat;
                }
                if (currentLon < minLon || minLon == -10000){
                    minLon = currentLon;
                }
                else if (currentLon > maxLon || maxLon == -10000){
                    maxLon = currentLon;
                }
            }

            midLat = (minLat + maxLat) / 2;
            midLon = (minLon + maxLon) / 2;

            // Determine which quadrant each point lies in
            for (int i = 0; i < allPolygonPoints.size(); i++){
                LatLng currentPoint = allPolygonPoints.get(i);
                double currentLat = currentPoint.latitude;
                double currentLon = currentPoint.longitude;
                if (currentLat >= midLat && currentLon <= midLon){
                    topLeftPoints.add(currentPoint);
                }
                else if (currentLat >= midLat && currentLon > midLon){
                    topRightPoints.add(currentPoint);
                }
                else if (currentLat < midLat && currentLon <= midLon){
                    botLeftPoints.add(currentPoint);
                }
                else {
                    botRightPoints.add(currentPoint);
                }
            }
        }

        /**
         * Adds the fire obstacle to the map
         */
        public void addFireObstacleToMap(){
            // Initialize the PolygonOptions with the fire colors
            PolygonOptions fireObstaclePolygonOptions = new PolygonOptions()
                    .strokeColor(FIRE_STROKE_COLOR)
                    .fillColor(FIRE_FILL_COLOR);

            // Add all the points to the polygon options
            for (int i = 0; i < allPolygonPoints.size(); i++){
                LatLng currentPoint = allPolygonPoints.get(i);
                fireObstaclePolygonOptions.add(currentPoint);
            }

            // Add the obstacle to the map and save the polygon
            fireObstaclePolygon = googleMap.addPolygon(fireObstaclePolygonOptions);
        }

        /**
         * Increases the size of the fire obstacle if the max size has not been reached
         * Decreased the size of the fire obstacle is it is at or greater than its given max size
         */
        public void expandFireObstacle() {
            double minLat = -10000;
            double maxLat = -10000;
            double minLon = -10000;
            double maxLon = -10000;

            boolean isTimeReached = false;

            // When the time limit is reached, the fire obstacles all expand past their max size
            if (timerValue >= GAME_TIME_LIMIT){
                isTimeReached = true;
            }

            // Determine the min/max latitudes and longitudes in order to determine if the max size
            // has been reached
            for (int i = 0; i < allPolygonPoints.size(); i++) {
                LatLng currentPoint = allPolygonPoints.get(i);
                double currentLat = currentPoint.latitude;
                double currentLon = currentPoint.longitude;
                if (currentLat < minLat || minLat == -10000) {
                    minLat = currentLat;
                } else if (currentLat > maxLat || maxLat == -10000) {
                    maxLat = currentLat;
                }
                if (currentLon < minLon || minLon == -10000) {
                    minLon = currentLon;
                } else if (currentLon > maxLon || maxLon == -10000) {
                    maxLon = currentLon;
                }
            }

            double latDiff = Math.abs(maxLat - minLat);
            double lonDiff = Math.abs(maxLon - minLon);

            // If the max size is reached and the time limit has not been reached reduce the size
            boolean maxSizeReached = ((latDiff / DRONE_MOVEMENT_DISTANCE) >= maxHeight || (lonDiff / DRONE_MOVEMENT_DISTANCE) >= maxWidth);
            if (maxSizeReached && !isTimeReached) {

                // Remove the previous polygon from the map and create a new PolygonOptions
                fireObstaclePolygon.remove();
                PolygonOptions newFireObstacleOptions = new PolygonOptions()
                        .strokeColor(FIRE_STROKE_COLOR)
                        .fillColor(FIRE_FILL_COLOR);

                // Randomly generate the amount that both the latitude and longitude will change by
                double coordChange = (Double.parseDouble(Integer.toString(rnd.nextInt(8))) / 10000) + 0.0003;

                // Iterate through all of the points in the fire obstacle and change the latitude/longitude based
                // on the quadrant that the point falls in
                for (int i = 0; i < allPolygonPoints.size(); i++){
                    LatLng currentPoint = allPolygonPoints.get(i);

                    // If the point is in the top left quadrant, decrement latitude and increment longitude
                    if (topLeftPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude - coordChange;
                        double newLon = currentPoint.longitude + coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = topLeftPoints.indexOf(currentPoint);
                        topLeftPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the top right quadrant, decrement latitude and decrement longitude
                    else if (topRightPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude - coordChange;
                        double newLon = currentPoint.longitude - coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = topRightPoints.indexOf(currentPoint);
                        topRightPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the bottom right quadrant, increment latitude and decrement longitude
                    else if (botRightPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude + coordChange;
                        double newLon = currentPoint.longitude - coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = botRightPoints.indexOf(currentPoint);
                        botRightPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the bottom left, increment latitude and increment longitude
                    else if (botLeftPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude + coordChange;
                        double newLon = currentPoint.longitude + coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = botLeftPoints.indexOf(currentPoint);
                        botLeftPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }
                }
                // Add the new polygon to the map
                fireObstaclePolygon = googleMap.addPolygon(newFireObstacleOptions);
                allPolygonPoints = fireObstaclePolygon.getPoints();
            }

            // If the max size for the fire obstacle has not been reached, increase the size of it
            else {

                // Remove the previous polygon from the map and create a new PolygonOptions
                fireObstaclePolygon.remove();
                PolygonOptions newFireObstacleOptions = new PolygonOptions()
                        .strokeColor(FIRE_STROKE_COLOR)
                        .fillColor(FIRE_FILL_COLOR);

                // Randomly generate the amount that both the latitude and longitude will change by
                double coordChange = (Double.parseDouble(Integer.toString(rnd.nextInt(8))) / 10000) + 0.0003;

                // Iterate through all of the points in the fire obstacle and change the latitude/longitude based
                // on the quadrant that the point falls in
                for (int i = 0; i < allPolygonPoints.size(); i++){
                    LatLng currentPoint = allPolygonPoints.get(i);

                    // If the point is in the top left quadrant, increment the latitude and decrement the longitude
                    if (topLeftPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude + coordChange;
                        double newLon = currentPoint.longitude - coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = topLeftPoints.indexOf(currentPoint);
                        topLeftPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the top right quadrant, increment the latitude and increment the longitude
                    else if (topRightPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude + coordChange;
                        double newLon = currentPoint.longitude + coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = topRightPoints.indexOf(currentPoint);
                        topRightPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the bottom right quadrant, decrement the latitude and increment the longitude
                    else if (botRightPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude - coordChange;
                        double newLon = currentPoint.longitude + coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = botRightPoints.indexOf(currentPoint);
                        botRightPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }

                    // If the point is in the bottom left quadrant, decrement the latitude and decrement the longitude
                    else if (botLeftPoints.contains(currentPoint)){
                        double newLat = currentPoint.latitude - coordChange;
                        double newLon = currentPoint.longitude - coordChange;
                        LatLng newPoint = new LatLng(newLat, newLon);
                        int indexOfPoint = botLeftPoints.indexOf(currentPoint);
                        botLeftPoints.set(indexOfPoint, newPoint);
                        newFireObstacleOptions.add(newPoint);
                    }
                }

                // Add the new polygon to the map
                fireObstaclePolygon = googleMap.addPolygon(newFireObstacleOptions);
                allPolygonPoints = fireObstaclePolygon.getPoints();
            }
        }

        /**
         * Getter for the list of polygon points of the fire obstacle
         * @return - The list of all the polygon points (allPolygonPoints)
         */
        public List<LatLng> getAllPolygonPoints(){
            return allPolygonPoints;
        }
    }
}
