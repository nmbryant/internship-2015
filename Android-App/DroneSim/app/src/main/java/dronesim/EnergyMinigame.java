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

import java.util.ArrayList;

public class EnergyMinigame implements IMiniGame{

    private static final double CAMERA_ZOOM_VALUE = 15.25;
    private static final double VISITED_SQUARE_EDGE_LENGTH = 0.00005;
    private static final int PIPELINE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int PIPELINE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int VISITED_SQUARE_STROKE_COLOR = Color.rgb(239, 29, 29);
    private static final int VISITED_SQUARE_FILL_COLOR = Color.argb(55, 239, 29, 29);
    private static final LatLng CAMERA_START_POSITION = new LatLng(65.06358673820515, -147.81376004219055);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_RADIUS = 5;

    GoogleMap googleMap;
    float startLat;
    float startLon;
    Marker droneMarker;
    Circle droneCircle;
    double targetSquares;
    double visitedSquares;
    ArrayList<LatLng> pipelinePointsVisited = new ArrayList<>();
    LatLng cameraPosition;
    LatLng startPosition;
    PolygonOptions pipelinePolygonOptions;
    Polygon pipelinePolygon;
    Context activityContext;
    Toast toast;

    public EnergyMinigame(GoogleMap googleMap, float startLat, float startLon, Context activityContext){
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.activityContext = activityContext;
        startPosition = new LatLng(startLat, startLon);
        visitedSquares = 0;
    }

    /**
     * On the first update, it draws the pipeline polygon and creates the drone marker
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon){
        if (isFirstUpdate) {
            // Move the camera to the starting location of the drone
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(CAMERA_START_POSITION);
            googleMap.moveCamera(startLocationCamera);
            CameraUpdate zoomCamera = CameraUpdateFactory.zoomTo(Float.parseFloat(Double.toString(CAMERA_ZOOM_VALUE)));
            googleMap.moveCamera(zoomCamera);
            cameraPosition = CAMERA_START_POSITION;

            // Add and save the pipeline to the map
            pipelinePolygon = googleMap.addPolygon(pipelinePolygonOptions);

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
        }

        // Move the drone marker
        LatLng newDronePosition = new LatLng(currentLat, currentLon);
        droneMarker.setPosition(newDronePosition);
        droneCircle.setCenter(newDronePosition);

        // If the camera position does not match the start, move the camera position to the camera start position
        if (cameraPosition != startPosition) {
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(CAMERA_START_POSITION);
            googleMap.moveCamera(startLocationCamera);
            cameraPosition = CAMERA_START_POSITION;
        }
    }

    /**
     * Calculates the number of points that the drone can move to within the pipeline polygon
     * Calculates the target number of points for the drone to visit to complete the game based on a
     * percentage of the total points
     */
    public void createGoal(){
        // Creates the polygon options for the pipeline
        pipelinePolygonOptions = new PolygonOptions().add(new LatLng(65.06201706938978, -147.81221508979797))
                .add(new LatLng(65.06215277927967, -147.81185030937195))
                .add(new LatLng(65.06302130620539, -147.81269788742065))
                .add(new LatLng(65.06384457121307, -147.81407117843628))
                .add(new LatLng(65.06489397076511, -147.8153908252716))
                .add(new LatLng(65.06487587801486, -147.81598091125488))
                .add(new LatLng(65.06372244009934, -147.814382314682))
                .add(new LatLng(65.06298059463826, -147.8132128715515))
                .add(new LatLng(65.062066829763, -147.81233310699463))
                .strokeColor(PIPELINE_STROKE_COLOR)
                .fillColor(PIPELINE_FILL_COLOR);

        // The target squares is the number that the drone has to visit to complete the game and is
        // a percentage of the total squares
        targetSquares = 50;
    }

    /**
     * Checks to see if the drone is visiting a new point in the pipeline and if it is, adds it to visited
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the number of points visited by the drone is greater than target
     */
    public boolean isGameComplete(float currentLat, float currentLon){
        LatLng currentPoint = new LatLng(currentLat, currentLon);
        boolean isInPipeline = AppUtilities.isPointInPolygon(currentPoint, pipelinePolygonOptions.getPoints());
        // If it is in the pipeline and hasn't visited the point before, mark it as visited and add
        // to visited percentage
        if (isInPipeline && !pipelinePointsVisited.contains(currentPoint)){
            pipelinePointsVisited.add(currentPoint);
            LatLng visitedTopLeft = new LatLng(currentLat - VISITED_SQUARE_EDGE_LENGTH, currentLon - VISITED_SQUARE_EDGE_LENGTH);
            LatLng visitedTopRight = new LatLng(currentLat + VISITED_SQUARE_EDGE_LENGTH, currentLon - VISITED_SQUARE_EDGE_LENGTH);
            LatLng visitedBotLeft = new LatLng(currentLat - VISITED_SQUARE_EDGE_LENGTH, currentLon + VISITED_SQUARE_EDGE_LENGTH);
            LatLng visitedBotRight = new LatLng(currentLat + VISITED_SQUARE_EDGE_LENGTH, currentLon + VISITED_SQUARE_EDGE_LENGTH);
            PolygonOptions visitedSquare = new PolygonOptions().add(visitedTopLeft)
                    .add(visitedBotLeft)
                    .add(visitedBotRight)
                    .add(visitedTopRight)
                    .add(visitedTopLeft)
                    .strokeColor(VISITED_SQUARE_STROKE_COLOR)
                    .fillColor(VISITED_SQUARE_FILL_COLOR);
            googleMap.addPolygon(visitedSquare);
            visitedSquares += 1;

            // Display a toast to indicate progress if certain milestones are hit
            // Display a toast when the user visits 10 squares
            if (visitedSquares == 10){
                toast = AppUtilities.displayToast("Good start - " + Double.toString(targetSquares - 10) + " points left",
                        toast, activityContext);
            }
            // Display a toast when the user is halfway to the target number of points
            else if (visitedSquares == targetSquares / 2){
                toast = AppUtilities.displayToast("Halfway there - 25 points left", toast, activityContext);
            }
            // Display a toast when the user only needs 10 more points to complete the game
            else if (visitedSquares == targetSquares - 10){
                toast = AppUtilities.displayToast("Almost finished - 10 points left", toast, activityContext);
            }
        }

        // The game is complete if the user has visited more squares than the target amount
        return (visitedSquares > targetSquares);
    }

    /**
     * Clears the map, and resets points visited
     */
    public void restartGame(){
        googleMap.clear();
        pipelinePointsVisited = new ArrayList<>();
        visitedSquares = 0;
    }

    /**
     * The energy game has no special conditions that would cause a restart, so this function always returns false
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - False since there are no restart conditions for this game
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        return false;
    }

    /**
     * This game requires no clean up when the game finishes
     */
    @Override
    public void endGame(){
        return;
    }
}
