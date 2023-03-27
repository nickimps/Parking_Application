package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    //MapsActivity mapsActivity = new MapsActivity();

    @Override
    public void onReceive(Context context, Intent intent) {
        //This method is called when the BroadcastReceiver is receiving

        // Add if to see what kinda of event actually happened
        // to location permissions from precise to approximate.

        System.out.println("1");

        //Grab the event that occurred
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        System.out.println("2");

        //If there is an error, throw an error code and message
        assert geofencingEvent != null;
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        System.out.println("3");

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        System.out.println("4");

        // Test that the reported transition was of interest.
        //If they enter the geofence
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (MapsActivity.isAdmin)
                Toast.makeText(context,"Entering Geofence",Toast.LENGTH_SHORT).show();

            System.out.println("Start geofence");

//            // Start foreground tracking
//            Intent serviceIntent = new Intent(context, MapsLocationService.class);
//            serviceIntent.setAction(MapsLocationService.ACTION_START_FOREGROUND_SERVICE);
//            MapsActivity.this_context.startService(serviceIntent);

            //Enable the geofence status
            MapsActivity.geoFenceStatus = true;
            //Enable recenter button when inside the geofence
            //MapsActivity.centerButton.setVisibility(View.VISIBLE);
            MapsActivity.follow = true;

        }
        //If they leave the geofence
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (MapsActivity.isAdmin)
                Toast.makeText(context,"Leaving Geofence",Toast.LENGTH_SHORT).show();

            System.out.println("Leaving geofence");


//            // Stop foreground tracking
//            Intent serviceIntent = new Intent(context, MapsLocationService.class);
//            serviceIntent.setAction(MapsLocationService.ACTION_STOP_FOREGROUND_SERVICE);
//            MapsActivity.this_context.startService(serviceIntent);

            //Disable the geofence status
            MapsActivity.geoFenceStatus = false;
            //Disable recenter button when outside the geofence
            //MapsActivity.centerButton.setVisibility(View.INVISIBLE);
            MapsActivity.follow = false;

        }
    }
}