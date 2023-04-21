package com.parking.linkandpark;

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
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminLocationService extends Service implements LocationListener {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private final SimpleDateFormat timeSDF = new SimpleDateFormat("HH:mm:ss z", Locale.CANADA);
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
                            .setContentText("Admin Foreground Tracking Active")
                            .setSilent(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW);

                    // Start the service as a foreground service
                    startForeground(NOTIFICATION_ID, builder.build());

                    Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show();

                    // Check location permissions
                    getLocation();
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
        float speed = updateSpeedTextView(location);
        String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
        String movingStatus = "Stopped";

        // Get status based on the speed
        if (speed <= 0.05)
            movingStatus = MapsLocationService.movingStatus;
        else if (speed > 0.05 && speed <= 2)
            movingStatus = "Walking";
        else if (speed > 2)
            movingStatus = "Driving";

        // If we are tracking, then we want to update the textfield with the speed, status and date
        // so that this can be saved to a text file for tracking and analyzing later on.
        if (AdminActivity.tracking) {
            // Get the time
            String time = timeSDF.format(new Date());

            // Append the speed, status and time to the output
            String textToAppend = time + " -- " + speedString + " -- " + movingStatus + "\n";
            AdminActivity.saveData += textToAppend;

            // Checks if the screen is on
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isInteractive();

            // We don't need to do things if the screen is off
            if (isScreenOn)
                AdminActivity.consoleTextView.setText(textToAppend);
        }
    }

    /**
     * Function to get permissions and then provide an alert if they are not set
     * If permissions are good, then we proceed to getGPSData()
     */
    public void getLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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
            // Create the channel
            serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}

