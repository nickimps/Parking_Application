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

        //Grab the event that occurred
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        //If there is an error, throw an error code and message
        assert geofencingEvent != null;
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        //If they enter the geofence
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (MapsActivity.isAdmin)
                Toast.makeText(context,"Entering Geofence",Toast.LENGTH_SHORT).show();

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

            //Disable the geofence status
            MapsActivity.geoFenceStatus = false;
            //Disable recenter button when outside the geofence
            //MapsActivity.centerButton.setVisibility(View.INVISIBLE);
            MapsActivity.follow = false;

        }
    }
}