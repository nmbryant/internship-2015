package dronesim;

import android.graphics.Color;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dronesim.activity.DroneControlActivity;

public class InsuranceMinigame implements IMiniGame{

    private static final double BUILDING_LAT = 34.04317;
    private static final double BUILDING_LON = -118.2696;
    private static final double CAMERA_ZOOM_VALUE = 17.35;
    private static final int VERIZON_GREEN = Color.argb(180, 74, 154, 77);
    private static final int VISITED_POLYGON_STROKE_COLOR = Color.argb(125, 59, 59, 59);
    private static final int DRONE_CIRCLE_STROKE_COLOR = Color.rgb(74, 154, 77);
    private static final int DRONE_CIRCLE_FILL_COLOR = Color.argb(55, 74, 154, 77);
    private static final int DRONE_CIRCLE_RADIUS = 2;

    GoogleMap googleMap;
    float startLat;
    float startLon;
    Marker droneMarker;
    Circle droneCircle;
    ArrayList<Polyline> buildingWalls = new ArrayList<>();
    ArrayList<Polygon> buildingRooms = new ArrayList<>();
    ArrayList<Polygon> visitedRooms = new ArrayList<>();
    HashMap<Polygon, Integer> polygonToRequiredPoints = new HashMap<>();
    HashMap<Polygon, List<LatLng>> polygonToPointsVisited = new HashMap<>();
    double previousLat;
    double previousLon;
    Polygon buildingPolygon;
    double visitedSquares = 0;
    double minBuildingLat;
    double maxBuildingLat;
    double minBuildingLon;
    double maxBuildingLon;
    int numberOfRooms = 0;
    DroneControlActivity droneControlActivity;
    Toast toast;

    public InsuranceMinigame(GoogleMap googleMap, float startLat, float startLon, DroneControlActivity droneControlActivity){
        this.googleMap = googleMap;
        this.startLat = startLat;
        this.startLon = startLon;
        this.previousLat = startLat;
        this.previousLon = startLon;
        this.droneControlActivity = droneControlActivity;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setIndoorLevelPickerEnabled(false);
    }

    /**
     * The insurance game is complete if all the rooms have been visited and lit up
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the game is complete, false otherwise
     */
    @Override
    public boolean isGameComplete(float currentLat, float currentLon) {
        LatLng currentPoint = new LatLng(currentLat, currentLon);

        // Iterate through all the building rooms and check to see if the drone is currently in it
        for (int i = 0; i < buildingRooms.size(); i++){
            Polygon currentRoom = buildingRooms.get(i);
            if(AppUtilities.isPointInPolygon(currentPoint, currentRoom.getPoints()) && !visitedRooms.contains(currentRoom)){
                List<LatLng> roomVisitedList = polygonToPointsVisited.get(currentRoom);

                // If the drone has not visited that point yet, add it to the list of visited
                if (!roomVisitedList.contains(currentPoint)) {
                    roomVisitedList.add(currentPoint);
                    polygonToPointsVisited.put(currentRoom, roomVisitedList);
                }

                // If the target number of points per room has been visited, light up the room and add it to the visited list
                if (roomVisitedList.size() >= polygonToRequiredPoints.get(currentRoom)) {
                    visitedRooms.add(currentRoom);
                    currentRoom.setVisible(true);
                    droneControlActivity.addBatteryPower(10);
                    Log.d("InsuranceMinigame", "VisitedRooms size = " + Integer.toString(visitedRooms.size()));
                }
            }
        }

        // Game is complete if all the rooms have been added to the visited rooms list
        return (visitedRooms.size() >= numberOfRooms);
    }

    /**
     * The restart condition for the insurance game is if the drone has crashed into a wall
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     * @return - True if the game needs to be restarted, false otherwise
     */
    @Override
    public boolean isRestart(float currentLat, float currentLon) {
        LatLng previousDronePosition = new LatLng(previousLat, previousLon);
        LatLng currentDronePosition = new LatLng(currentLat, currentLon);
        boolean isWallHit = false;

        // Iterate through all the walls and check to see if the drone has collided with any of them
        // with the previous move that was made
        for (int i = 0; i < buildingWalls.size(); i++){
            Polyline currentWall = buildingWalls.get(i);
            // Check to see if the drone intersects with any of the lines that are part of the wall polyline
            for (int j = 0; j < currentWall.getPoints().size() - 1; j++){
                // If the polylines intersect, the function will return true and display a toast
                if(AppUtilities.doPolyLinesIntersect(previousDronePosition, currentDronePosition, currentWall.getPoints().get(j), currentWall.getPoints().get(j + 1))){
                    Log.d("InsuranceMinigame", "Line hit");
                    toast = AppUtilities.displayToast("Drone crashed into wall! - Restarting", toast, droneControlActivity.getApplicationContext());
                    isWallHit = true;
                }
            }
        }

        // Save the current coords in the previous fields for the next call of this function
        previousLat = currentLat;
        previousLon = currentLon;
        return isWallHit;
    }

    /**
     * The insurance game initializes the goal (rooms) in the update map function so does not need
     * to do anything extra to create a goal
     */
    @Override
    public void createGoal() {
        return;
    }

    /**
     * On the first update it will add all the rooms and walls to the map
     * Every other update it just moves the drone
     * @param isFirstUpdate - True if it is the first time the function is being called so it can handle initialization
     * @param currentLat - The current latitude of the drone
     * @param currentLon - The current longitude of the drone
     */
    @Override
    public void updateMap(boolean isFirstUpdate, float currentLat, float currentLon) {
        if(isFirstUpdate){
            // Move the camera to the starting location of the drone
            LatLng startLocation = new LatLng(BUILDING_LAT, BUILDING_LON);
            CameraUpdate startLocationCamera = CameraUpdateFactory.newLatLng(startLocation);
            CameraPosition newCameraPosition = new CameraPosition.Builder()
                    //.bearing(Float.parseFloat(Double.toString(CAMERA_ROTATION_VALUE)))
                    .target(startLocation)
                    .zoom(Float.parseFloat(Double.toString(CAMERA_ZOOM_VALUE)))
                    .build();
            googleMap.moveCamera(startLocationCamera);
            CameraUpdate startLocationRotation = CameraUpdateFactory.newCameraPosition(newCameraPosition);
            googleMap.moveCamera(startLocationRotation);
            CameraUpdate zoomCamera = CameraUpdateFactory.zoomTo(Float.parseFloat(Double.toString(CAMERA_ZOOM_VALUE)));
            googleMap.moveCamera(zoomCamera);

            // Add and save the drone location marker
            LatLng newDroneCoords = new LatLng(startLat, startLon);
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

            // Add the walls for the building
            addBuildingWalls();

            // Add the room polygons
            addAllRooms();
        }
        if (!isFirstUpdate) {
            // Move the drone marker
            LatLng newDronePosition = new LatLng(currentLat, currentLon);
            droneMarker.setPosition(newDronePosition);
            droneCircle.setCenter(newDronePosition);
        }
    }

    /**
     * Clears the map, resets the array lists and restarts the position of the drone
     */
    @Override
    public void restartGame() {
        googleMap.clear();
        visitedSquares = 0;
        numberOfRooms = 0;
        visitedRooms = new ArrayList<>();
        buildingRooms = new ArrayList<>();
        previousLat = startLat;
        previousLon = startLon;
    }

    /**
     * The insurance game doesn't require any clean up to end the game
     */
    @Override
    public void endGame(){
        return;
    }

    /**
     * Adds all of the building walls to the map
     */
    private void addBuildingWalls(){
        // Add the exterior wall
        PolylineOptions exteriorWall = new PolylineOptions().add(new LatLng(34.04282, -118.2709))
                .add(new LatLng(34.04417, -118.2700))
                .add(new LatLng(34.04341, -118.2683))
                .add(new LatLng(34.04205, -118.2692))
                .add(new LatLng(34.04282, -118.2709));
        buildingWalls.add(googleMap.addPolyline(exteriorWall));

        PolygonOptions buildingPolygonOptions = new PolygonOptions().add(new LatLng(34.04282, -118.2709))
                .add(new LatLng(34.04417, -118.2700))
                .add(new LatLng(34.04341, -118.2683))
                .add(new LatLng(34.04205, -118.2692))
                .add(new LatLng(34.04282, -118.2709));
        buildingPolygon = googleMap.addPolygon(buildingPolygonOptions);

        minBuildingLat = 34.04205;
        maxBuildingLat = 34.04417;
        minBuildingLon = -118.2709;
        maxBuildingLon = -118.2683;

        // Interior Wall 1
        PolylineOptions interiorWall1 = new PolylineOptions().add(new LatLng(34.0440593457615, -118.26975479722023))
                .add(new LatLng(34.043653738936655, -118.27002570033073));
        buildingWalls.add(googleMap.addPolyline(interiorWall1));

        // Interior Wall 2
        PolylineOptions interiorWall2 = new PolylineOptions().add(new LatLng(34.04363484761102, -118.26879993081093))
                .add(new LatLng(34.0430847748135, -118.26915331184864));
        buildingWalls.add(googleMap.addPolyline(interiorWall2));

        // Interior Wall 3
        PolylineOptions interiorWall3 = new PolylineOptions().add(new LatLng(34.043287579837525, -118.26880998909473))
                .add(new LatLng(34.043136448470555, -118.26848410069942));
        buildingWalls.add(googleMap.addPolyline(interiorWall3));

        // Interior Wall 4
        PolylineOptions interiorWall4 = new PolylineOptions().add(new LatLng(34.042866967224775, -118.26866179704666))
                .add(new LatLng(34.043012542766995, -118.26898567378521));
        buildingWalls.add(googleMap.addPolyline(interiorWall4));

        // Interior Wall 5
        PolylineOptions interiorWall5 = new PolylineOptions().add(new LatLng(34.042274661106106, -118.26967969536781))
                .add(new LatLng(34.04261915484069, -118.26945841312408))
                .add(new LatLng(34.042576371024914, -118.26936386525631));
        buildingWalls.add(googleMap.addPolyline(interiorWall5));

        // Interior Wall 6
        PolylineOptions interiorWall6 = new PolylineOptions().add(new LatLng(34.04322646056371, -118.27063053846359))
                .add(new LatLng(34.04304199122415, -118.27021613717079))
                .add(new LatLng(34.043112556345, -118.27017456293106))
                .add(new LatLng(34.04282251662773, -118.26952748000622))
                .add(new LatLng(34.042755840688514, -118.26957173645496))
                .add(new LatLng(34.04264082453618, -118.26932430267334));
        buildingWalls.add(googleMap.addPolyline(interiorWall6));

        // Interior Wall 7
        PolylineOptions interiorWall7 = new PolylineOptions().add(new LatLng(34.04268305272829, -118.26941415667534))
                .add(new LatLng(34.04282140536251, -118.26932430267334));
        buildingWalls.add(googleMap.addPolyline(interiorWall7));
    }

    /**
     * Adds all of the room polygons to the map
     */
    private void addAllRooms(){
        // Room 1
        ArrayList<LatLng> room1Points = new ArrayList<>();
        room1Points.add(new LatLng(34.043772087439734, -118.27027782797813));
        room1Points.add(new LatLng(34.04417324874249, -118.27001497149467));
        room1Points.add(new LatLng(34.04405879017091, -118.26975345611572));
        room1Points.add(new LatLng(34.04365096080081, -118.27002301812172));
        room1Points.add(new LatLng(34.043772087439734, -118.27027782797813));
        addRoom(room1Points, 5);

        // Room 2
        ArrayList<LatLng> room2Points = new ArrayList<>();
        room2Points.add(new LatLng(34.04376747571346, -118.27027849853039));
        room2Points.add(new LatLng(34.04322851637591, -118.27062584459782));
        room2Points.add(new LatLng(34.0430446026974, -118.2702188193798));
        room2Points.add(new LatLng(34.04358950835807, -118.26989561319351));
        room2Points.add(new LatLng(34.04376747571346, -118.27027849853039));
        addRoom(room2Points, 5);

        // Room 3
        ArrayList<LatLng> room3Points = new ArrayList<>();
        room3Points.add(new LatLng(34.043014209661074, -118.26898768544197));
        room3Points.add(new LatLng(34.043080885397174, -118.26913923025131));
        room3Points.add(new LatLng(34.043628735710655, -118.26879724860191));
        room3Points.add(new LatLng(34.04355983789417, -118.268633633852));
        room3Points.add(new LatLng(34.043014209661074, -118.26898768544197));
        addRoom(room3Points, 5);

        // Room 4
        ArrayList<LatLng> room4Points = new ArrayList<>();
        room4Points.add(new LatLng(34.04355983789417, -118.2686322927475));
        room4Points.add(new LatLng(34.04341426329144, -118.26830238103867));
        room4Points.add(new LatLng(34.043136448470555, -118.26848477125168));
        room4Points.add(new LatLng(34.043290913623316, -118.26880529522896));
        room4Points.add(new LatLng(34.04355983789417, -118.2686322927475));
        addRoom(room4Points, 5);

        // Room 5
        ArrayList<LatLng> room5Points = new ArrayList<>();
        room5Points.add(new LatLng(34.0432820235502, -118.26880663633347));
        room5Points.add(new LatLng(34.04313200342603, -118.2684887945652));
        room5Points.add(new LatLng(34.04286974538631, -118.26866313815117));
        room5Points.add(new LatLng(34.043014209661074, -118.26898232102394));
        room5Points.add(new LatLng(34.0432820235502, -118.26880663633347));
        addRoom(room5Points, 5);

        // Room 6
        ArrayList<LatLng> room6Points = new ArrayList<>();
        room6Points.add(new LatLng(34.04308533045287, -118.2691539824009));
        room6Points.add(new LatLng(34.04286418906316, -118.26866447925568));
        room6Points.add(new LatLng(34.0424735786317, -118.26890252530575));
        room6Points.add(new LatLng(34.0426863865633, -118.26941013336182));
        room6Points.add(new LatLng(34.04308533045287, -118.2691539824009));
        addRoom(room6Points, 8);

        // Room 7
        ArrayList<LatLng> room7Points = new ArrayList<>();
        room7Points.add(new LatLng(34.04261193164155, -118.26945707201958));
        room7Points.add(new LatLng(34.04256970344791, -118.26936721801758));
        room7Points.add(new LatLng(34.042637490801155, -118.26932162046432));
        room7Points.add(new LatLng(34.042469689255164, -118.26891124248505));
        room7Points.add(new LatLng(34.04206296351828, -118.26919287443161));
        room7Points.add(new LatLng(34.04227577239545, -118.26967567205429));
        room7Points.add(new LatLng(34.04261470977752, -118.26945707201958));
        room7Points.add(new LatLng(34.04261193164155, -118.26945707201958));
        addRoom(room7Points, 10);

        // Room 8
        ArrayList<LatLng> room8Points = new ArrayList<>();
        room8Points.add(new LatLng(34.04268138582773, -118.26941549777985));
        room8Points.add(new LatLng(34.042639157668695, -118.26932698488235));
        room8Points.add(new LatLng(34.042579149195994, -118.26936788856983));
        room8Points.add(new LatLng(34.04261971048311, -118.26945506036282));
        room8Points.add(new LatLng(34.04268138582773, -118.26941549777985));
        addRoom(room8Points, 2);

        // Room 9
        ArrayList<LatLng> room9Points = new ArrayList<>();
        room9Points.add(new LatLng(34.04268016346108, -118.26941817998886));
        room9Points.add(new LatLng(34.0422789395115, -118.26968304812908));
        room9Points.add(new LatLng(34.04236006235352, -118.26985873281956));
        room9Points.add(new LatLng(34.04275178454002, -118.26957307755947));
        room9Points.add(new LatLng(34.04268016346108, -118.26941817998886));
        addRoom(room9Points, 5);

        // Room 10
        ArrayList<LatLng> room10Points = new ArrayList<>();
        room10Points.add(new LatLng(34.04236006235352, -118.26985873281956));
        room10Points.add(new LatLng(34.04282068301297, -118.269532173872));
        room10Points.add(new LatLng(34.04311016711426, -118.2701738923788));
        room10Points.add(new LatLng(34.04263571263046, -118.27047497034073));
        room10Points.add(new LatLng(34.04236006235352, -118.26985873281956));
        addRoom(room10Points, 10);

        // Room 11
        ArrayList<LatLng> room11Points = new ArrayList<>();
        room11Points.add(new LatLng(34.04263571263046, -118.27047497034073));
        room11Points.add(new LatLng(34.04304015761415, -118.27022083103657));
        room11Points.add(new LatLng(34.04322240444624, -118.27063053846359));
        room11Points.add(new LatLng(34.04282068301297, -118.27088803052902));
        room11Points.add(new LatLng(34.04263571263046, -118.27047497034073));
        addRoom(room11Points, 6);

        // Room 12
        ArrayList<LatLng> room12Points = new ArrayList<>();
        room12Points.add(new LatLng(34.04358950835807, -118.26989561319351));
        room12Points.add(new LatLng(34.043651349712704, -118.27001698315144));
        room12Points.add(new LatLng(34.04405417846023, -118.26975144445896));
        room12Points.add(new LatLng(34.04369413299464, -118.26894074678421));
        room12Points.add(new LatLng(34.04282568373185, -118.26952748000622));
        room12Points.add(new LatLng(34.04311572345522, -118.27016986906528));
        room12Points.add(new LatLng(34.04358950835807, -118.26989561319351));
        addRoom(room12Points, 10);

        // Room 13
        ArrayList<LatLng> room13Points = new ArrayList<>();
        room13Points.add(new LatLng(34.04268788674659, -118.26941683888435));
        room13Points.add(new LatLng(34.042755674005356, -118.26956637203693));
        room13Points.add(new LatLng(34.04369413299464, -118.26894074678421));
        room13Points.add(new LatLng(34.04363301404776, -118.2688032835722));
        room13Points.add(new LatLng(34.04268788674659, -118.26941683888435));
        addRoom(room13Points, 5);
    }

    /**
     * Adds a room polygon using the given points
     * @param polygonPoints - The points/corners for the room polygon
     * @param requiredPoints - The number of points required to visit before it lights up
     */
    private void addRoom(List<LatLng> polygonPoints, int requiredPoints){
        PolygonOptions roomPolygonOptions = new PolygonOptions().visible(false)
                .strokeColor(VISITED_POLYGON_STROKE_COLOR)
                .fillColor(VERIZON_GREEN);

        // Add all of the given points to the polygon options
        for (int i = 0; i < polygonPoints.size(); i++){
            LatLng currentPoint = polygonPoints.get(i);
            roomPolygonOptions.add(currentPoint);
        }

        // Add the polygon to the map and the various lists and hashmaps that it needs to be put in
        Polygon roomPolygon = googleMap.addPolygon(roomPolygonOptions);
        buildingRooms.add(roomPolygon);
        polygonToPointsVisited.put(roomPolygon, new ArrayList<LatLng>());
        polygonToRequiredPoints.put(roomPolygon, requiredPoints);
        numberOfRooms += 1;
    }
}
