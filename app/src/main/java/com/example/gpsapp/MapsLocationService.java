package com.example.gpsapp;

import android.Manifest;
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
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapsLocationService extends Service implements LocationListener {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    LocationManager mLocationManager;
    private static final int NOTIFICATION_ID = 6;
    private static final String CHANNEL_ID = "my_channel";

    /**
     * Called when the service is created, default
     */
    @Override
    public void onCreate() {
        super.onCreate();
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

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    createNotificationChannel();

                    // Create a notification
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Parking Spotter")
                            .setContentText("Background GPS tracking active.")
                            .setSilent(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW);

                    // Start the service as a foreground service
                    startForeground(NOTIFICATION_ID, builder.build());

                    if (MapsActivity.isAdmin)
                        Toast.makeText(this, "Vehicle Tracking Started", Toast.LENGTH_SHORT).show();

                    getLocation();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
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
        Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show();

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
            // Get the current location of the user
            Location currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Hashmap to store a key-value pair of the possible parking spaces that we may be occupying
            HashMap<Polygon, Double> possibleParkedSpaces = new HashMap<>();

            // Check if the user's location is inside any of the polygons
            if (MapsActivity.mMap != null) {
                // Need to go through each parking space and gather which parking spaces we think we are parked in
                for (Polygon polygon : MapsActivity.parkingSpaces) {
                    // Create a LatLngBounds object that contains the polygon
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polygon.getPoints()) {
                        builder.include(latLng);
                    }
                    LatLngBounds bounds = builder.build();

                    // Check if the user's location is inside the bounds of the polygon
                    if (bounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                        // Get the new distance
                        double distance = currentLocation.distanceTo(getPolygonCenter(polygon));

                        // Save the parking spaces and their distance to the users current location.
                        possibleParkedSpaces.put(polygon, distance);

                        // Set the status to 'parked'
                        stoppedStatus = "Parked";
                    }
                }

                // If we changed the status to parked, we need to choose a single parking spot
                if (stoppedStatus.equals("Parked")) {
                    String bestOption = "";
                    double lowestDistance = 10000;
                    MapsActivity.my_spot = false;

                    // Go through the hashmap and check if the lowest spot is empty
                    for(Map.Entry<Polygon, Double> possibleSpace : possibleParkedSpaces.entrySet()) {
                        // Get the parking space id and distance for comparisons
                        String docID = MapsActivity.parkingSpacesDocIDs.get(MapsActivity.parkingSpaces.indexOf(possibleSpace.getKey()));
                        double polyDistance = possibleSpace.getValue();

                        MapsActivity.available_spot = true;

                        // Query the database to see if a user is parked in this spot
                        MapsActivity.firestore.collection("ParkingSpaces")
                                .document(docID)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        if(!Objects.equals(task.getResult().getString("user"), ""))
                                            MapsActivity.available_spot = false;
                                        else if (Objects.equals(task.getResult().getString("user"), MapsActivity.username))
                                            MapsActivity.my_spot = true;
                                    }
                                });


                        // We want to secure our spot if we are close to it
                        if (MapsActivity.my_spot) {
                            bestOption = docID;
                            break;
                        } else if (polyDistance < lowestDistance && MapsActivity.available_spot) {  // If the new distance is lower and the spot is available, make it the current best option
                            lowestDistance = polyDistance;
                            bestOption = docID;
                        }
                    }

                    // If we have a best option, then start the runnable to tell if we have parked or not
                    if (!bestOption.equals("")) {
                        if (Boolean.TRUE.equals(MapsActivity.isAdmin))
                            Toast.makeText(getApplicationContext(), "Check Stop Runnable Starting", Toast.LENGTH_SHORT).show();

                        // Get best parked option and run the runnable to check if we need to style a new parking space
                        MapsActivity.parkedBestOption = bestOption;
                        MapsActivity.parkedHandler.removeCallbacks(MapsActivity.parkedRunnable);
                        MapsActivity.parkedHandler.postDelayed(MapsActivity.parkedRunnable, MapsActivity.RUNNABLE_TIME);
                    } else
                        stoppedStatus = "Stopped";
                }
            }
        }

        return stoppedStatus;
    }

    /**
     * This function will change the speed label on the admin card view to the current speed in
     * real-time.
     *
     * @param location The location parameter
     * @return The speed to be set in the TextView
     */
    public static float updateSpeedTextView(Location location) {
        if (location.hasSpeed())
            return location.getSpeed();
        else
            return 0.0f;
    }

    /**
     * Called when the location changes, this just gathers the moving status to append to the data
     *
     * @param location the current location of user
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Update the speed on the card view on the screen
        float speed = updateSpeedTextView(location);
        MapsActivity.movingStatus = "Stopped";

        // Get the label based on the speed
        if (speed <= 0.05) {
            MapsActivity.movingStatus = checkStop(location);
        } else if (speed > 0.05 && speed <= 2) {
            MapsActivity.movingStatus = "Walking";
        } else if (speed > 2) {
            MapsActivity.movingStatus = "Driving";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MapsActivity.this_context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Parking Spotter")
                .setContentText(MapsActivity.movingStatus)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MapsActivity.this_context);
        int notificationId = 8;
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Function to get permissions and then provide an alert if they are not set
     * If permissions are good, then we proceed to getGPSData()
     */
    public void getLocation() {
        mLocationManager = MapsActivity.mLocationManager;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
        }
    }

    /**
     * Creates the notification to show the user that we are tracking their location in the background
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
