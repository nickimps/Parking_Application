package com.parking.linkandpark;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeoBrdcstReceiver";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    /**
     * When the broadcast receiver is receiving, this function is called and it initializes or resets
     * some variables depending on if it is an enter or exit.
     * @param context The application context
     * @param intent The current intent
     */
    @SuppressLint("VisibleForTests")
    @Override
    public void onReceive(Context context, Intent intent) {
        //This method is called when the BroadcastReceiver is receiving

        // Grab the event that occurred
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        // If there is an error, throw an error code and message
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        // If they enter the geofence
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (MapsActivity.isAdmin)
                Toast.makeText(context,"Entering Geofence",Toast.LENGTH_SHORT).show();

            // Start foreground tracking
            Intent service_intent = new Intent(context, MapsLocationService.class);
            service_intent.setAction(ACTION_START_FOREGROUND_SERVICE);
            context.startService(service_intent);

            //Enable the geofence status
            MapsActivity.geoFenceStatus = true;
            //MapsActivity.follow = true;
            MapsActivity.inPolygon = false;

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) { //If they leave the geofence
            if (MapsActivity.isAdmin)
                Toast.makeText(context,"Leaving Geofence",Toast.LENGTH_SHORT).show();

            // Stop foreground tracking
            Intent service_intent = new Intent(context, MapsLocationService.class);
            service_intent.setAction(ACTION_STOP_FOREGROUND_SERVICE);
            context.startService(service_intent);

            // Stop the runnable if there is one in progress
            MapsLocationService.parkedHandler.removeCallbacks(MapsLocationService.parkedRunnable);

            //Disable the geofence status
            MapsActivity.geoFenceStatus = false;
            //MapsActivity.follow = false;
            MapsActivity.inPolygon = false;
        }
    }
}