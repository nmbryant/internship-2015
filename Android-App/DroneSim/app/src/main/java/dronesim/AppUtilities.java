package dronesim;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public final class AppUtilities {

    /**
     * Checks to see if two polylines on a map intersect
     * @param line1_point1 - Vertex 1 of the first polyline
     * @param line1_point2 - Vertex 2 of the first polyline
     * @param line2_point1 - Vertex 1 of the second polyline
     * @param line2_point2 - Vertex 2 of the second polyline
     * @return - True if the two polylines intersect, false otherwise
     */
    public static boolean doPolyLinesIntersect(LatLng line1_point1, LatLng line1_point2, LatLng line2_point1, LatLng line2_point2){
        double A1 = line1_point2.latitude - line1_point1.latitude;
        double B1 = line1_point1.longitude - line1_point2.longitude;
        double C1 = A1 * line1_point1.longitude + B1 * line1_point1.latitude;

        double A2 = line2_point2.latitude - line2_point1.latitude;
        double B2 = line2_point1.longitude - line2_point2.longitude;
        double C2 = A2 * line2_point1.longitude + B2 * line2_point1.latitude;

        double determinate = A1 * B2 - A2 * B1;

        boolean isIntersect;

        if (determinate != 0){
            double x = (B2 * C1 - B1 * C2)/determinate;
            double y = (A1 * C2 - A2 * C1)/determinate;

            LatLng intersect = new LatLng(y, x);
            isIntersect = (isInBoundedBox(line1_point1, line1_point2, intersect) &&
                    isInBoundedBox(line2_point1, line2_point2, intersect));
        }
        else {
            isIntersect = false;
        }

        return isIntersect;
    }

    /**
     * Used by doPolylinesIntersect function, checks to see if a point is within a bounded box created by
     * two other points
     * @param latlong1 - The point to check see if it is in the bounding box
     * @param latlong2 - Corner 1 of the bounding box
     * @param latlong3 - Corner 2 of the bounding box
     * @return - True if the point is in the bounded box, false otherwise
     */
    public static boolean isInBoundedBox(LatLng latlong1, LatLng latlong2, LatLng latlong3){
        boolean betweenLats;
        boolean betweenLons;

        if(latlong1.latitude < latlong2.latitude)
            betweenLats = (latlong1.latitude <= latlong3.latitude &&
                    latlong2.latitude >= latlong3.latitude);
        else
            betweenLats = (latlong1.latitude >= latlong3.latitude &&
                    latlong2.latitude <= latlong3.latitude);

        if(latlong1.longitude < latlong2.longitude)
            betweenLons = (latlong1.longitude <= latlong3.longitude &&
                    latlong2.longitude >= latlong3.longitude);
        else
            betweenLons = (latlong1.longitude >= latlong3.longitude &&
                    latlong2.longitude <= latlong3.longitude);

        return (betweenLats && betweenLons);
    }

    /**
     * Checks to see if a point is within a polygon on the map
     * @param tap - The point to check
     * @param vertices - The vertices of the polygon
     * @return - Returns true if 'tap' is in the polygon, false otherwise
     */
    public static boolean isPointInPolygon(LatLng tap, List<LatLng> vertices) {
        int intersectCount = 0;
        for(int j=0; j<vertices.size()-1; j++) {
            if( rayCastIntersect(tap, vertices.get(j), vertices.get(j+1)) ) {
                intersectCount++;
            }
        }
        return ((intersectCount%2) == 1); // odd = inside, even = outside;
    }

    /**
     * Used by isPointInPolygon to check to see if the point intersects with an edge
     * @param tap - The point to check
     * @param vertA - The first vertex of the edge to check
     * @param vertB - The second vertex of the edge to check
     * @return - Returns true if it intersects with the edge
     */
    public static boolean rayCastIntersect(LatLng tap, LatLng vertA, LatLng vertB) {

        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = tap.latitude;
        double pX = tap.longitude;

        if ( (aY>pY && bY>pY) || (aY<pY && bY<pY) || (aX<pX && bX<pX) ) {
            return false; // a and b can't both be above or below pt.y, and a or b must be east of pt.x
        }

        double m;
        if (aX - bX == 0){
            m = 0;
        }
        else {
            m = (aY - bY) / (aX - bX);               // Rise over run
        }
        double bee = (-aX) * m + aY;                // y = mx + b
        double x;
        if (m == 0){
            x = 0;
        }
        else {
            x = (pY - bee) / m;
        }

        return x > pX;
    }

    /**
     * Displays a toast and removes the previous toast if it is still being displayed
     * @param text - The text to display in the toast
     * @param toast - The previous toast that was displayed
     * @param activityContext - The context of the activity so the toast can be displayed
     * @return - The toast that was displayed
     */
    public static Toast displayToast(CharSequence text, Toast toast, Context activityContext){
        int duration = Toast.LENGTH_SHORT;

        // If the toast is not null, cancel it so that the new toast will appear
        if (toast != null){
            toast.cancel();
        }

        // Set the text of the toast and display it
        toast = Toast.makeText(activityContext, text, duration);
        toast.show();
        return toast;
    }

}
