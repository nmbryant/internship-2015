package dronesim;

import android.content.Context;
import android.graphics.Color;
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

import java.util.Random;

import dronesim.activity.DroneControlActivity;

public class HealthcareMiniGame implements IMiniGame {

    private static final int NUMBER_OF_DESTINATIONS = 4;
    private static final int MAX_DESTINATION_DISTANCE = 6;
    private static final int DESTINATION_DISTANCE_MODIFIER = 4;
    private static final int CAMERA_ZOOM_VALUE = 14;
    private static final int COORDINATE_DIVISOR = 1000;
    private static final int DESTINATION_MAP_RADIUS = 100;
    private static final double TARGET_DISTANCE_FROM_DEST = 0.001;
    private static final double LANDING_ZONE_HEIGHT = 0.002;
    private static final double LANDING_ZONE_WIDTH = 0.002;
    private static final float WITHOUT_SUPPLIES_MARKER_COLOR = BitmapDescriptorFactory.HUE_AZURE;
    private static final float BATTERY_CHARGE_VALUE = 20;
    private static final int TARGET_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int TARGET_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_RADIUS = 20;

    GoogleMap googleMap;
    Marker destMarker;
    Circle droneCircle;
    Circle destCircle;
    float startLat;
    float startLon;
    Random rnd;
    Marker droneMarker;
    Context activityContext;
    double landingZoneMaxLon;
    double landingZoneMinLon;
    double landingZoneMaxLat;
    double landingZoneMinLat;
    boolean hasSupplies = true;
    DroneControlActivity droneControlActivity;
    Toast toast;
    int destinationsReached = 0;
    LatLng nextDestination;

    public HealthcareMiniGame(GoogleMap googleMap, float startLat, float startLon, DroneControlActivity droneControlActivity) {
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.activityContext = droneControlActivity.getApplicationContext();
        this.droneControlActivity = droneControlActivity;
        rnd = new Random();
    }

    /**
     * Places all of the non-visited destinations on the map, as well as a marker for the drone
     *
     * @param isFirstUpdate - True if it is the first time calling update map or if the game is restarting
     * @param currentLat    - The current latitude of the drone
     * @param currentLon    - The current longitude of the drone
     */
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon) {
        // Update the camera if it is the first time updating the map
        if (isFirstUpdate) {
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

            // Add the landing zone as a polygon
            landingZoneMaxLat = currentLat + LANDING_ZONE_HEIGHT;
            landingZoneMinLat = currentLat - LANDING_ZONE_HEIGHT;
            landingZoneMaxLon = currentLon + LANDING_ZONE_WIDTH;
            landingZoneMinLon = currentLon - LANDING_ZONE_WIDTH;

            LatLng topLeftCorner = new LatLng(landingZoneMaxLat, landingZoneMinLon);
            LatLng topRightCorner = new LatLng(landingZoneMaxLat, landingZoneMaxLon);
            LatLng botLeftCorner = new LatLng(landingZoneMinLat, landingZoneMinLon);
            LatLng botRightCorner = new LatLng(landingZoneMinLat, landingZoneMaxLon);
            PolygonOptions landingZone = new PolygonOptions()
                    .add(topLeftCorner)
                    .add(botLeftCorner)
                    .add(botRightCorner)
                    .add(topRightCorner)
                    .add(topLeftCorner)
                    .fillColor(Color.argb(55, 239, 29, 29))
                    .strokeColor(Color.RED);

            // Get back the mutable Polyline
            Polygon landingZonePolygon = googleMap.addPolygon(landingZone);

            // For each of the destinations, add a circle and marker and save these in the hash maps
            addDestinationMarker(nextDestination);
        }
        // Move the drone marker
        LatLng newDronePosition = new LatLng(currentLat, currentLon);
        droneMarker.setPosition(newDronePosition);
        droneCircle.setCenter(newDronePosition);
    }

    /**
     * Generates a set of destinations that the drone needs to fly to and saves them in the array list and hash map
     */
    public void createGoal() {
        nextDestination = createDestination();
    }

    /**
     * Updates the game state after each move by checking to see if any of the destinations have been reached
     * If a destination is reached, it is removed from non-visited destinations list
     * Returns true if all of the destinations have been reached by the drone
     *
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The currrent longitude of the drone
     * @return - True if all the destinations have been reached and the game is complete, false otherwise
     */
    public boolean isGameComplete(float currentLat, float currentLon) {
        // Check to see if any of the destinations have been reached
        // Calculate the distance the drone is from the destination
        LatLng currentDestination = nextDestination;
        float destLatFloat = Float.valueOf(Double.toString(currentDestination.latitude));
        float destLonFloat = Float.valueOf(Double.toString(currentDestination.longitude));
        float latDiff = currentLat - destLatFloat;
        float lonDiff = currentLon - destLonFloat;
        boolean isAtDest = (Math.abs(latDiff) <= TARGET_DISTANCE_FROM_DEST && Math.abs(lonDiff) <= TARGET_DISTANCE_FROM_DEST);
        // If a destination is reached, remove it from the list of destinations and from the map
        if (isAtDest && hasSupplies) {
            // Display a toast
            toast = AppUtilities.displayToast("Supplies delivered!", toast, droneControlActivity.getApplicationContext());

            // Remove the destination's circle from the map and remove it from the hashmap
            destCircle.remove();

            // Remove the destination's marker from the map and remove it from the hashmap
            destMarker.remove();

            // Remove the supplies from the drone and change the marker color
            hasSupplies = false;

            // Increment destinations reached and create a new destination
            destinationsReached += 1;
        }
        // If they are at a destination but do not have supplies, notify with a toast
        else if (isAtDest && !hasSupplies) {
            toast = AppUtilities.displayToast("Need to pick up supplies!", toast, droneControlActivity.getApplicationContext());
        }

        // If the drone enters the landing zone, add supplies and change the marker color
        if (!hasSupplies && (currentLat <= landingZoneMaxLat && currentLat >= landingZoneMinLat) &&
                (currentLon <= landingZoneMaxLon && currentLon >= landingZoneMinLon)) {
            hasSupplies = true;
            toast = AppUtilities.displayToast("Picked up supplies!", toast, droneControlActivity.getApplicationContext());
            droneControlActivity.addBatteryPower(BATTERY_CHARGE_VALUE);

            // Create a new goal when supplies are picked up
            createGoal();
            addDestinationMarker(nextDestination);
        }

        // Check to see if all of the destinations have been reached and if they have, return true
        return (destinationsReached >= NUMBER_OF_DESTINATIONS);
    }

    /**
     * Creates a destination based on the starting latitude and longitude of the drone for the user
     * to fly to
     *
     * @return The destination that the user will have to move the drone to
     */
    private LatLng createDestination() {
        // Generate a random number for latitude to add to the starting location for the destination
        double latDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        latDistance += DESTINATION_DISTANCE_MODIFIER;
        boolean isLatNegative = rnd.nextBoolean();
        if (isLatNegative) {
            latDistance = 0 - latDistance;
        }
        double realLatDistance = latDistance / COORDINATE_DIVISOR;

        // Generate a random number for longitude and add to the starting location for the destination
        double longDistance = rnd.nextInt(MAX_DESTINATION_DISTANCE);
        longDistance += DESTINATION_DISTANCE_MODIFIER;
        boolean isLongNegative = rnd.nextBoolean();
        if (isLongNegative) {
            longDistance = 0 - longDistance;
        }
        double realLongDistance = longDistance / COORDINATE_DIVISOR;

        // Add the two random numbers to the starting coordinates to generate a destination
        double newLat = startLat + realLatDistance;
        double newLong = startLon + realLongDistance;
        return new LatLng(newLat, newLong);
    }

    public void restartGame() {
        // Notify user that they are out of battery
        toast = AppUtilities.displayToast("Out of battery!", toast, droneControlActivity.getApplicationContext());

        // Clear the map of markers and circles
        googleMap.clear();

        // Drone starts off with supplies
        hasSupplies = true;

        // Create new destinations
        createGoal();

        // Reset the destinations reached value to 0
        destinationsReached = 0;
    }

    /**
     * The healthcare minigame has no special conditions that would cause a restart, so this function
     * just returns false
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - False since there are no restart conditions
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        return false;
    }

    /**
     * No cleanup is required by this game when it is finished, so this function just returns
     */
    @Override
    public void endGame() {
        return;
    }

    /**
     * Adds a destination marker at the given location
     * @param destination - The location to place a destination marker and circle
     */
    private void addDestinationMarker(LatLng destination) {
        // Add the destination marker
        MarkerOptions destinationLocation = new MarkerOptions().position(destination)
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Destination");
        destMarker = googleMap.addMarker(destinationLocation);

        // Add the circle around the destination
        CircleOptions destinationCircle = new CircleOptions().center(destination)
                .radius(DESTINATION_MAP_RADIUS)
                .strokeColor(TARGET_CIRCLE_STROKE_COLOR)
                .fillColor(TARGET_CIRCLE_FILL_COLOR);
        destCircle = googleMap.addCircle(destinationCircle);
    }
}
