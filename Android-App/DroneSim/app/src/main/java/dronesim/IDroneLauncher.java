package dronesim;

import android.widget.Button;
import android.widget.ImageButton;

import java.util.UUID;

public interface IDroneLauncher {

    /**
     * Called by StopDroneTask after a drone has been stopped
     * @param cityButton - The button that was pressed to disable the drone
     * @param city - The string for the city/use case that is being stopped
     */
    void droneStopped(ImageButton cityButton, String city);

    /**
     * Called by StartDroneTask after a drone has been successfully started
     * @param buttonPressed - The button that was pressed to start a drone
     * @param droneUUID - The UUID for the drone that was launched
     * @param country - The string for the country/city/use case that the drone was launched in
     * @param droneName - The dweet name for the drone given by the simulator
     */
    void droneStarted(ImageButton buttonPressed, UUID droneUUID, String country, String droneName);

    /**
     * Called by StartDroneTask when a drone fails to start in the simulator
     * @param buttonPressed - The button that was pressed to start a drone
     */
    void droneStartFailed(ImageButton buttonPressed);

}
