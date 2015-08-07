package dronesim;

import android.graphics.Color;
import android.os.AsyncTask;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import dronesim.activity.DroneControlActivity;
import dronesim.task.MoveGameObjectsTask;

public class AgricultureMinigame implements IMiniGame, IMovementMinigame{

    private static final int NUMBER_OF_ANIMALS = 3;
    private static final int CAMERA_ZOOM_VALUE = 14;
    private static final int MAX_DESTINATION_DISTANCE = 5;
    private static final int DESTINATION_DISTANCE_MODIFIER = 2;
    private static final int COORDINATE_DIVISOR = 1000;
    private static final double ANIMAL_SPEED = 0.02;
    private static final double TARGET_DISTANCE_FROM_DEST = 0.001;
    private static final int DESTINATION_MAP_RADIUS = 100;
    private static final double BOUNDARY_SHRINK_VALUE = 0.0043;
    private static final double ANIMAL_TURN_DEGREE = 6;
    private static final int ANIMAL_MOVEMENT_SLEEP_TIME = 300;
    private static final int STARTUP_MOVEMENT_SLEEP = 100;
    private static final int ANIMAL_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int ANIMAL_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_RADIUS = 20;

    GoogleMap googleMap;
    float startLat;
    float startLon;
    Marker droneMarker;
    Circle droneCircle;
    ArrayList<Marker> animalMarkers = new ArrayList<>();
    ArrayList<LatLng> animalStartCoords = new ArrayList<>();
    HashMap<Marker, Float> animalMarkerToHeading = new HashMap<>();
    HashMap<Marker, Circle> animalMarkerToCircle = new HashMap<>();
    Random rnd;
    LatLngBounds visibleBounds;
    MoveGameObjectsTask moveGameObjectsTask;
    boolean areObjectsMoving = false;
    DroneControlActivity droneControlActivity;
    Toast toast;

    public AgricultureMinigame(final GoogleMap googleMap, float startLat, float startLon, DroneControlActivity droneControlActivity){
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.droneControlActivity = droneControlActivity;
        rnd = new Random();

        // When the camera changes position, it saves the coordinates for the visible bounds
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                visibleBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng boundSouthWest = new LatLng(visibleBounds.southwest.latitude + BOUNDARY_SHRINK_VALUE,
                        visibleBounds.southwest.longitude + BOUNDARY_SHRINK_VALUE);
                LatLng boundNorthEast = new LatLng(visibleBounds.northeast.latitude - BOUNDARY_SHRINK_VALUE,
                        visibleBounds.northeast.longitude - BOUNDARY_SHRINK_VALUE);
                visibleBounds = new LatLngBounds(boundSouthWest, boundNorthEast);

                // When the camera moves, it starts moving the game objects if they are not already moving
                if (!areObjectsMoving) {
                    initializeMovementTask();
                }
            }
        });
    }

    /**
     * On the first update it moves the camera, creates the drone marker + circle, and adds all of the animal markers to the map
     * On every other update, it moves the drone marker and circle
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    @Override
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon) {
        // Update the camera if it is the first time updating the map
        if (isFirstUpdate) {
            // Move the camera to the starting location of the drone
            LatLng startLocation = new LatLng(startLat, startLon);
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

            // Add circle around the drone marker
            CircleOptions droneCircleOptions = new CircleOptions().center(newDroneCoords)
                    .radius(DRONE_CIRCLE_RADIUS)
                    .strokeColor(DRONE_CIRCLE_STROKE_COLOR)
                    .fillColor(DRONE_CIRCLE_FILL_COLOR);
            droneCircle = googleMap.addCircle(droneCircleOptions);

            // Add and save the animal markers
            for (int i = 0; i < animalStartCoords.size(); i++){
                LatLng currentCoords = animalStartCoords.get(i);
                MarkerOptions animalMarkerOptions = new MarkerOptions().position(currentCoords)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.cow_side))
                        .title("Animal " + Integer.toString(i));
                Marker animalMarker = googleMap.addMarker(animalMarkerOptions);
                animalMarkers.add(animalMarker);
                float animalHeading = rnd.nextInt(360);
                animalMarkerToHeading.put(animalMarker, animalHeading);

                // Add the circle around the animal marker
                CircleOptions animalCircleOptions = new CircleOptions().center(animalStartCoords.get(i))
                        .radius(DESTINATION_MAP_RADIUS)
                        .strokeColor(ANIMAL_CIRCLE_STROKE_COLOR)
                        .fillColor(ANIMAL_CIRCLE_FILL_COLOR);
                Circle animalCircle = googleMap.addCircle(animalCircleOptions);
                animalMarkerToCircle.put(animalMarker, animalCircle);
            }
        }

        // Move the drone marker
        LatLng newDronePosition = new LatLng(currentLat, currentLon);
        droneMarker.setPosition(newDronePosition);
        droneCircle.setCenter(newDronePosition);

        // Move the animal markers
        /*
        if (!isFirstUpdate){
            moveAnimalMarkers();
        }
        */
    }

    /**
     * The game is complete when the user has collected data from all of the animal markers
     * Also checks to see if the user's drone is in range of any of the animal markers
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if all of the animal markers have been hit by the drone, false otherwise
     */
    @Override
    public boolean isGameComplete(float currentLat, float currentLon) {
        // Iterate through all the active animal markers to see if the drone is in range of any
        for (int i = 0; i < animalMarkers.size(); i++) {
            // Calculate the distance the drone is from the destination
            Marker currentAnimalMarker = animalMarkers.get(i);
            LatLng currentAnimalPosition = currentAnimalMarker.getPosition();
            float destLatFloat = Float.valueOf(Double.toString(currentAnimalPosition.latitude));
            float destLonFloat = Float.valueOf(Double.toString(currentAnimalPosition.longitude));
            float latDiff = currentLat - destLatFloat;
            float lonDiff = currentLon - destLonFloat;
            boolean isAtDest = (Math.abs(latDiff) <= TARGET_DISTANCE_FROM_DEST && Math.abs(lonDiff) <= TARGET_DISTANCE_FROM_DEST);

            // If the drone is in range of the animal marker, remove it from the map + list and display a toast
            if (isAtDest){
                toast = AppUtilities.displayToast("Animal data received!", toast, droneControlActivity.getApplicationContext());
                animalMarkers.remove(i);
                currentAnimalMarker.remove();
                Circle animalCircle = animalMarkerToCircle.get(currentAnimalMarker);
                animalCircle.remove();
                animalMarkerToCircle.remove(currentAnimalMarker);
                animalMarkerToHeading.remove(currentAnimalMarker);
            }
        }

        // If there are no active animal markers, the game is complete so the function will return true
        return (animalMarkers.size() == 0);
    }

    /**
     * Creates random locations for each of the animal markers
     */
    @Override
    public void createGoal() {
        for(int i = 0; i < NUMBER_OF_ANIMALS; i++){
            LatLng dest = createDestination();
            animalStartCoords.add(dest);
        }
    }

    /**
     * When the game restarts, the map is cleared, and the array lists and hashmaps are cleared
     */
    @Override
    public void restartGame() {
        googleMap.clear();
        animalMarkers = new ArrayList<>();
        animalMarkerToCircle.clear();
        animalMarkerToHeading.clear();
        moveGameObjectsTask.cancel(true);
        areObjectsMoving = false;
    }

    /**
     * The agriculture minigame has no special restart conditions
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - Always False because there are no additional restart conditions for this game
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        return false;
    }

    /**
     * When the game terminates, it must stop the MoveGameObjectsTask
     */
    @Override
    public void endGame(){
        moveGameObjectsTask.stopTask();
    }

    /**
     * Creates a destination based on the starting latitude and longitude of the drone for the user
     * to fly to
     * @return The destination that the user will have to move the drone to
     */
    private LatLng createDestination(){
        // Generate a random number for latitude to add to the starting location for the destination
        double latDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        latDistance += DESTINATION_DISTANCE_MODIFIER;
        boolean isLatNegative = rnd.nextBoolean();
        if (isLatNegative){
            latDistance = 0 - latDistance;
        }
        double realLatDistance = latDistance/COORDINATE_DIVISOR;

        // Generate a random number for longitude and add to the starting location for the destination
        double longDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        longDistance += DESTINATION_DISTANCE_MODIFIER;
        boolean isLongNegative = rnd.nextBoolean();
        if (isLongNegative){
            longDistance = 0 - longDistance;
        }
        double realLongDistance = longDistance/COORDINATE_DIVISOR;

        // Add the two random numbers to the starting coordinates to generate a destination
        double newLat = startLat + realLatDistance;
        double newLong = startLon + realLongDistance;
        return new LatLng(newLat, newLong);
    }

    /**
     * Moves all of the animal markers
     * If the animal marker is moving off screen, it turns it around so that the user can always see
     * all the markers without having to move the map
     */
    public void moveGameObjects(){
        // Iterate through all the active animal markers
        for (int i = 0; i < animalMarkers.size(); i++){
            // Calculate the new latitude and longitude for the animal
            Marker currentMarker = animalMarkers.get(i);
            LatLng animalPosition = currentMarker.getPosition();
            float animalHeading = animalMarkerToHeading.get(currentMarker);
            double distanceToMove = ANIMAL_SPEED;
            double bearing = Math.toRadians(animalHeading);
            double animalLat = animalPosition.latitude;
            double animalLon = animalPosition.longitude;
            double animalLatRadians = Math.toRadians(animalLat);
            double animalLonRadians = Math.toRadians(animalLon);
            double newLat = Math.asin((Math.sin(animalLatRadians) * Math.cos(distanceToMove / 6378.1)) + (Math.cos(animalLatRadians) *
                    Math.sin(distanceToMove / 6378.1) * Math.cos(bearing)));
            double newLon = animalLonRadians + Math.atan2(Math.sin(bearing) * Math.sin(distanceToMove / 6378.1) * Math.cos(animalLatRadians),
                    Math.cos(distanceToMove / 6378.1) - Math.sin(animalLatRadians) * Math.sin(newLat));
            LatLng newAnimalPosition = new LatLng(Math.toDegrees(newLat), Math.toDegrees(newLon));
            currentMarker.setPosition(newAnimalPosition);

            // Update the heading for the animal
            // If the animal marker is within the visible bounds, randomly adjust the heading
            if (visibleBounds.contains(newAnimalPosition)) {
                float headingAdjustment = rnd.nextInt(5);
                boolean isNegative = rnd.nextBoolean();
                if (isNegative) {
                    headingAdjustment = 0 - headingAdjustment;
                }
                animalHeading += headingAdjustment;
            }
            // If the animal marker is outside of the visible bounds, increment heading by a constant amount
            else {
                animalHeading += ANIMAL_TURN_DEGREE;
            }
            animalMarkerToHeading.put(currentMarker, animalHeading);

            // Update the animal circle
            Circle animalCircle = animalMarkerToCircle.get(currentMarker);
            animalCircle.setCenter(newAnimalPosition);
        }
    }

    /**
     * Creates and starts a MoveGameObjectsTask that handles moving the animal markers
     */
    private void initializeMovementTask(){
        moveGameObjectsTask = new MoveGameObjectsTask(this, ANIMAL_MOVEMENT_SLEEP_TIME, STARTUP_MOVEMENT_SLEEP);
        moveGameObjectsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        areObjectsMoving = true;
    }

    /**
     * Checks to see if the game is complete as a result of moving the animal markers
     */
    @Override
    public void checkGameStatus(){
        double droneLat = droneMarker.getPosition().latitude;
        double droneLon = droneMarker.getPosition().longitude;
        float droneLatFloat = Float.parseFloat(Double.toString(droneLat));
        float droneLonFloat = Float.parseFloat(Double.toString(droneLon));
        boolean isGameComplete = isGameComplete(droneLatFloat, droneLonFloat);

        // If the game is complete, tell the DroneControlActivity to complete the game
        if (isGameComplete){
            droneControlActivity.completeGame();
        }
    }
}
