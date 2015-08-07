package dronesim;

import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Random;

public class BasicMiniGame implements IMiniGame{

    private static final double MEDIA_DESTINATION_LAT = 31.559506;
    private static final double MEDIA_DESTINATION_LONG = -97.115107;
    private static final double ENERGY_DESTINATION_LAT = 36.444003;
    private static final double ENERGY_DESTINATION_LONG = 127.285947;
    private static final int MAX_DESTINATION_DISTANCE = 7;
    private static final int DESTINATION_DISTANCE_MODIFIER = 6;
    private static final int CAMERA_ZOOM_VALUE = 14;
    private static final int COORDINATE_DIVISOR = 1000;
    private static final int DESTINATION_MAP_RADIUS = 100;
    private static final double TARGET_DISTANCE_FROM_DEST = 0.001;

    GoogleMap googleMap;
    String useCase;
    LatLng destinationCoords;
    float startLat;
    float startLon;
    double latMidpoint;
    double lonMidpoint;
    Random rnd;

    public BasicMiniGame(GoogleMap googleMap, String useCase, float startLat, float startLon){
        this.googleMap = googleMap;
        this.useCase = useCase;
        this.startLat = startLat;
        this.startLon = startLon;

        // Initialize the random number generator
        rnd = new Random();
    }

    /**
     * On the first update, it initializes the camera location
     * On all other updates, it adds the drone and goal markers
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon){
        // Clear the map of markers
        googleMap.clear();

        // Update the camera if it is the first time updating the map
        if (isFirstUpdate) {
            LatLng startLocation = new LatLng(latMidpoint, lonMidpoint);
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(startLocation);
            googleMap.moveCamera(startLocationCamera);
            CameraUpdate zoomCamera = CameraUpdateFactory.zoomTo(CAMERA_ZOOM_VALUE);
            googleMap.moveCamera(zoomCamera);
        }

        // Add the drone location marker
        LatLng newDroneCoords = new LatLng(currentLat, currentLon);
        MarkerOptions newDroneLocation = new MarkerOptions().position(newDroneCoords)
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("Your Drone");
        googleMap.addMarker(newDroneLocation);

        // Add the destination marker
        MarkerOptions destinationLocation = new MarkerOptions().position(destinationCoords)
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Destination");
        googleMap.addMarker(destinationLocation);

        // Add the circle around the destination
        CircleOptions destinationCircle = new CircleOptions().center(destinationCoords).radius(DESTINATION_MAP_RADIUS);
        googleMap.addCircle(destinationCircle);
    }

    /**
     * Checks to see if the drone has reached the destination
     * @return True if the drone has reached the destination, false otherwise
     */
    public boolean isGameComplete(float currentLat, float currentLon){
        // Calculate the differences between current coordinates and destination coordinates
        float destinationLatitudeFloat = Float.valueOf(Double.toString(destinationCoords.latitude));
        float latitudeDifference = currentLat - destinationLatitudeFloat;
        float destinationLongitudeFloat = Float.valueOf(Double.toString(destinationCoords.longitude));
        float longitudeDifference = currentLon - destinationLongitudeFloat;
        Log.d("DroneControlActivity", "LatDiff = " + Float.toString(latitudeDifference));
        Log.d("DroneControlActivity", "LongDiff = " + Float.toString(longitudeDifference));

        // Return true if both lat and long are within 0.0002 of the destination lat and long
        return (Math.abs(latitudeDifference) <= TARGET_DISTANCE_FROM_DEST && Math.abs(longitudeDifference) <= TARGET_DISTANCE_FROM_DEST);
    }

    /**
     * Creates the destination marker and saves the coordinates for it
     */
    public void createGoal(){
        if (useCase.equals("Media")){
            destinationCoords = new LatLng(MEDIA_DESTINATION_LAT, MEDIA_DESTINATION_LONG);
        }
        // If it is the energy use case, use a set destination
        else if (useCase.equals("Energy")){
            destinationCoords = new LatLng(ENERGY_DESTINATION_LAT, ENERGY_DESTINATION_LONG);
        }
        // If it is any other use case, randomly generate a destination
        else {
            destinationCoords = createDestination(startLat, startLon);
        }

        // Calculates the midpoint between the gaol and the destination for the camera to use
        latMidpoint = (destinationCoords.latitude + startLat) / 2;
        lonMidpoint = (destinationCoords.longitude + startLon) / 2;
    }

    /**
     * The basic minigame does not need to do anything when restarting
     */
    public void restartGame(){
        Log.d("BasicMiniGame", "Restarting game");
    }

    /**
     * Creates a destination based on the starting latitude and longitude of the drone for the user
     * to fly to
     * @param startLatitude - Latitude that the drone starts at
     * @param startLongitude - Longitude that the drone starts at
     * @return The destination that the user will have to move the drone to
     */
    private LatLng createDestination(double startLatitude, double startLongitude){
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
        double newLat = startLatitude + realLatDistance;
        double newLong = startLongitude + realLongDistance;
        return new LatLng(newLat, newLong);
    }

    /**
     * The basic minigame has no restart conditions
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - False because the basic minigame has no restart conditions
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        return false;
    }

    /**
     * The basic minigame does not require any clean up when it finishes
     */
    @Override
    public void endGame(){
        return;
    }
}
