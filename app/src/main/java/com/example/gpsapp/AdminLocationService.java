package com.example.gpsapp;

import android.Manifest;
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

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    System.out.println("startForegroundService called.");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService();
                    }
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show();

        // Start location tracking
        getLocation();
    }

    private void stopForegroundService() {
        Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show();
        System.out.println("stopForegroundService called.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Toast.makeText(this, "onDestroy called", Toast.LENGTH_SHORT).show();
        System.out.println("onDestroy called.");
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        float speed = updateSpeedTextView(location);
        String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
        String movingStatus = "Stopped";

        if (speed <= 0.05)
            movingStatus = MapsActivity.movingStatus;
        else if (speed > 0.05 && speed <= 2)
            movingStatus = "Walking";
        else if (speed > 2)
            movingStatus = "Driving";


        if (AdminActivity.tracking) {
            // Get the time
            String time = timeSDF.format(new Date());

            // Append the speed, status and time to the output
            String textToAppend = time + " -- " + speedString + " -- " + movingStatus + "\n";
            AdminActivity.saveData += textToAppend;

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isInteractive();
            System.out.println(isScreenOn + " -- " + textToAppend);

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
}

