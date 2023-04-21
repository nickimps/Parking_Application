package com.parking.linkandpark;

import static com.parking.linkandpark.MapsActivity.animationInProgress;
import static com.parking.linkandpark.MapsActivity.firestore;
import static com.parking.linkandpark.MapsActivity.follow;
import static com.parking.linkandpark.MapsActivity.geoFenceStatus;
import static com.parking.linkandpark.MapsActivity.isAdmin;
import static com.parking.linkandpark.MapsActivity.mMap;
import static com.parking.linkandpark.MapsActivity.parkedBestOption;
import static com.parking.linkandpark.MapsActivity.parkingSpaces;
import static com.parking.linkandpark.MapsActivity.parkingSpacesDocIDs;
import static com.parking.linkandpark.MapsActivity.this_context;
import static com.parking.linkandpark.MapsActivity.username;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsLocationService extends Service implements LocationListener {
    private static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    private static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int RUNNABLE_TIME = 8000;
    private static final int RUNNABLE_TIME_SHORT = 6000;
    private static final int NOTIFICATION_ID = 6;
    public static final String CHANNEL_ID = "my_channel";
    private static final String TAG = "MapsLocationService";
    public static String current_shared_spot;
    LocationManager mLocationManager;
    private static Location last_known_location_runnable;
    static boolean runnableRunning = false;
    public static String movingStatus;
    public static final Handler parkedHandler = new Handler();

    // This runnable will check if we are driving or walking after so many seconds after being parked.
    public static final Runnable parkedRunnable = new Runnable() {
        @Override
        public void run() {
            // do something depending on the status
            switch (movingStatus) {
                case "Driving":
                    // Get the polygon for the parking space being driven away from
                    Polygon my_parking_space = parkingSpaces.get(parkingSpacesDocIDs.indexOf(current_shared_spot));

                    // Get the distance to polygon center to see if we are even close to our own parking space
                    double distance = last_known_location_runnable.distanceTo(getPolygonCenter(my_parking_space));
                    boolean inVicinity = distance < 9;

                    // Print the vicinity distance as a toast message
                    if (Boolean.TRUE.equals(isAdmin))
                        Toast.makeText(this_context, "Vicinity Distance: " + distance, Toast.LENGTH_SHORT).show();

                    // Check to make sure it is this users parking spot we are removing
                    if (inVicinity) {
                        if (Boolean.TRUE.equals(isAdmin))
                            Toast.makeText(this_context, "Clearing Parking Space", Toast.LENGTH_SHORT).show();

                        // Set the parking space to empty
                        firestore.collection("ParkingSpaces").document(current_shared_spot).update("user", "");

                        // Update current parked spot
                        current_shared_spot = "";
                    }
                    runnableRunning = false;
                    break;
                case "Walking":
                    Log.d(TAG, "Walking");
                    // Remove any parking spaces if they have any
                    firestore.collection("ParkingSpaces")
                            .whereEqualTo("user", username)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Need to check if it is empty, then we just simply assign
                                    if (task.getResult().isEmpty()) {
                                        firestore.collection("ParkingSpaces").document(parkedBestOption)
                                                .update("user", username)
                                                .addOnFailureListener(e -> {
                                                    if (Boolean.TRUE.equals(isAdmin))
                                                        Toast.makeText(this_context, "Failed to Fill Parking Space", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnSuccessListener(e -> {
                                                    if (Boolean.TRUE.equals(isAdmin))
                                                        Toast.makeText(this_context, "Filled Parking Space", Toast.LENGTH_SHORT).show();

                                                    Log.d(TAG, "Filled");
                                                    // Save current parked spot
                                                    current_shared_spot = parkedBestOption;
                                                });
                                    } else {
                                        // Loop through and erase any parking spots the user may have already as we can't be parked in two spots at once
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            if (!document.getId().equals(parkedBestOption)) {
                                                firestore.collection("ParkingSpaces")
                                                        .document(document.getId())
                                                        .update("user", "")
                                                        .addOnCompleteListener(task_up -> {
                                                            if (task_up.isSuccessful()) {
                                                                Log.d(TAG, "Cleared " + document.getId());
                                                                // Set the new parking space to be occupied by current user
                                                                firestore.collection("ParkingSpaces").document(parkedBestOption)
                                                                        .update("user", username)
                                                                        .addOnFailureListener(e -> {
                                                                            if (Boolean.TRUE.equals(isAdmin))
                                                                                Toast.makeText(this_context, "Failed to Fill Parking Space", Toast.LENGTH_SHORT).show();
                                                                        })
                                                                        .addOnSuccessListener(e -> {
                                                                            if (Boolean.TRUE.equals(isAdmin))
                                                                                Toast.makeText(this_context, "Filled Parking Space", Toast.LENGTH_SHORT).show();

                                                                            Log.d(TAG, "Filled");
                                                                            // Save current parked spot
                                                                            current_shared_spot = parkedBestOption;
                                                                        });
                                                            }
                                                        });
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Parking Spot Removal Task Unsuccessful");
                                }
                            })
                            .addOnFailureListener(t -> Log.d(TAG, "FAILURE", t));
                    runnableRunning = false;
                    break;
                default:
                    if (Boolean.TRUE.equals(isAdmin))
                        Toast.makeText(this_context, "Restarting", Toast.LENGTH_SHORT).show();

                    runnableRunning = true;
                    // If we are still stopped, we want to restart this and keep polling
                    restartRunnable();
            }
        }
    };

    /**
     * Restarts the runnable to continue to poll
     */
    private static void restartRunnable() {
        // Stop current runnable
        parkedHandler.removeCallbacks(parkedRunnable);
        // Start a new runnable
        parkedHandler.postDelayed(parkedRunnable, RUNNABLE_TIME_SHORT);
    }

    /**
     * Called when the service is created, default
     */
    @Override
    public void onCreate() {
        super.onCreate();

        movingStatus = "Stopped";

        // Get a reference to the location manager
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Need permissions to be good
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("No", (dialog, which) -> dialog.cancel());
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            // Get Location and start requesting updates
            getLocation();
        }
    }

    /**
     * When the service is called, we check if we are trying to start or stop the foreground service. If we are starting
     * then we should create the notification and notification channel so that we can track and then start the service itself.
     *
     * @param intent Current intent
     * @param flags any flags
     * @param startId the id of the service
     * @return some value
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            // Get start or stop action
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    createNotificationChannel();

                    // Create a notification
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentText("Background GPS tracking active.")
                            .setSilent(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW);

                    // Start the service as a foreground service
                    startForeground(NOTIFICATION_ID, builder.build());

                    if (isAdmin)
                        Toast.makeText(this, "Service Start Function", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    // Stop the service
                    stopForegroundService();
                    break;
            }
        }
        return START_STICKY;
    }

    /**
     * This stopped the foreground service and lets the user know that is has stopped
     */
    private void stopForegroundService() {
        Toast.makeText(this, "Service Stop Function", Toast.LENGTH_SHORT).show();

        // Stop the foreground service.
        stopSelf();

        // Stop foreground service and remove the notification.
        stopForeground(true);
    }

    /**
     * We disregard this, here because we need it
     *
     * @param intent The intent
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    /**
     * Called when the service is stopped.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Gets the center of a polygon
     *
     * @param polygon The polygon to get the center of
     * @return the center LatLng of the polygon
     */
    public static Location getPolygonCenter(Polygon polygon) {
        // Get the points of this polygon
        List<LatLng> points = polygon.getPoints();
        double latSum = 0, lngSum = 0;
        for (LatLng point : points) {
            latSum += point.latitude;
            lngSum += point.longitude;
        }

        // Compute the center of the polygon
        LatLng centerPoint = new LatLng(latSum / points.size(), lngSum / points.size());

        // Turn the center point into a location for measuring
        Location centerOfPolygon = new Location("");
        centerOfPolygon.setLatitude(centerPoint.latitude);
        centerOfPolygon.setLongitude(centerPoint.longitude);

        return centerOfPolygon;
    }

    /**
     * This function runs whenever the user has stopped moving. It is supposed to check if we have stopped inside or outside of a parking spot.
     * It starts by looking at which parking spaces we may be inside of and adds those to a list of possible candidates. It then looks at the
     * distance to the center of those possible parking spaces and choose the closest EMPTY parking space. Once it has chosen a parking space,
     * it will change the colour of the parking space to make the 'Your Car' colour scheme.
     *
     * @param location The location parameter
     * @return stoppedStatus, will be either 'Stopped' or 'Parked'
     */
    private String checkStop(Location location) {
        // Default moving status
        String stoppedStatus = "Stopped";

        // Check permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Hashmap to store a key-value pair of the possible parking spaces that we may be occupying
            HashMap<Polygon, Double> possibleParkedSpaces = new HashMap<>();

            // Check if the user's location is inside any of the polygons
            if (mMap != null) {
                // Need to go through each parking space and gather which parking spaces we think we are parked in
                for (Polygon polygon : parkingSpaces) {
                    // Create a LatLngBounds object that contains the polygon
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polygon.getPoints()) {
                        builder.include(latLng);
                    }
                    LatLngBounds bounds = builder.build();

                    // Check if the user's location is inside the bounds of the polygon
                    if (bounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                        // Get the new distance
                        double distance = location.distanceTo(getPolygonCenter(polygon));

                        // Save the parking spaces and their distance to the users current location.
                        possibleParkedSpaces.put(polygon, distance);

                        // Set the status to 'parked'
                        stoppedStatus = "Parked";

                        Log.d(TAG, "Option: " + parkingSpacesDocIDs.get(parkingSpaces.indexOf(polygon)));
                    }
                }

                // If we changed the status to parked, we need to choose a single parking spot
                if (stoppedStatus.equals("Parked")) {
                    String bestOption = "";
                    double lowestDistance = 10000;

                    // Go through the hashmap and check if the lowest spot is empty
                    for(Map.Entry<Polygon, Double> possibleSpace : possibleParkedSpaces.entrySet()) {
                        // Get the parking space id and distance for comparisons
                        String docID = parkingSpacesDocIDs.get(parkingSpaces.indexOf(possibleSpace.getKey()));
                        double polyDistance = possibleSpace.getValue();

                        if (docID.equals(current_shared_spot)) {
                            bestOption = docID;
                            break;
                        } else if (polyDistance < lowestDistance) {
                            lowestDistance = polyDistance;
                            bestOption = docID;
                        }
                    }

                    Log.d(TAG, "Best Option: " + bestOption);

                    // If we have a best option, then start the runnable to tell if we have parked or not
                    if (!bestOption.equals("")) {
                        if (Boolean.TRUE.equals(isAdmin))
                            Toast.makeText(this_context, "Runnable Initial Start", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Runnable Initial Start");

                        // Get best parked option and run the runnable to check if we need to style a new parking space
                        runnableRunning = true;
                        parkedBestOption = bestOption;
                        parkedHandler.removeCallbacks(parkedRunnable);
                        parkedHandler.postDelayed(parkedRunnable, RUNNABLE_TIME);

                    } else
                        stoppedStatus = "Stopped";
                }
            }
        }

        return stoppedStatus;
    }

    /**
     * Called when the location changes, this just gathers the moving status to append to the data
     *
     * @param location the current location of user
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Do things when we are inside the geofence, help lower battery consumption
        if(geoFenceStatus) {
            // Update the speed on the card view on the screen
            float speed;
            if (location.hasSpeed())
                speed = location.getSpeed();
            else
                speed = 0.0f;

            if (!runnableRunning)
                last_known_location_runnable = location;

            // Get the label based on the speed
            if (speed <= 0.065) {
                if (!runnableRunning)
                    movingStatus = checkStop(location);
            } else if (speed > 0.065 && speed <= 2) {
                movingStatus = "Walking";
            } else if (speed > 2) {
                movingStatus = "Driving";
            }

            // If we are admin, adjust the banner
            if (isAdmin) {
                String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
                MapsActivity.speedAdminTextView.setText(speedString);

                MapsActivity.movingStatusTextView.setText(movingStatus);
            }

            // Have the camera follow the user if the follow boolean is set to true
            if (follow) {
                GoogleMap.CancelableCallback cancelableCallback = new GoogleMap.CancelableCallback() {
                    @Override
                    public void onCancel() {animationInProgress = false;}
                    @Override
                    public void onFinish() {animationInProgress = false;}
                };

                // Pause things if we are or are not moving
                if(!animationInProgress) {
                    animationInProgress = true;
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(mMap.getCameraPosition().zoom).build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                    mMap.animateCamera(cameraUpdate, cancelableCallback);
                }
            }
        }
    }

    /**
     * Function to get permissions and then provide an alert if they are not set
     * If permissions are good, then we proceed to getGPSData()
     */
    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
        }
    }

    /**
     * Creates the notification to show the user that we are tracking their location in the background
     */
    private void createNotificationChannel() {
        // Check the build version, some android versions do not need to do this, it is done automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Build the service channel with the following ID
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
