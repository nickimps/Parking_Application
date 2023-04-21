package com.parking.linkandpark;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

public class GeofenceHelper extends ContextWrapper {
    PendingIntent pendingIntent = null;

    /**
     * Constructor function
     *
     * @param base context of current
     */
    public GeofenceHelper(Context base) {
        super(base);
    }

    /**
     * Create a new geofence given the specified parameters
     *
     * @param geofence the geofence to be made
     * @return a geofence builder
     */
    public GeofencingRequest getGeofencingRequest(Geofence geofence){
        return new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // Set the initial trigger to enter
                .build();
    }

    /**
     * Builds the geofence
     *
     * @param ID Geofence ID
     * @param latLng the center of the geofence
     * @param radius the size
     * @param transitionTypes the types of transitions to use
     * @return a geofence builder
     */
    @SuppressLint("VisibleForTests")
    public Geofence getGeofence(String ID, LatLng latLng, float radius, int transitionTypes){
        return new Geofence.Builder()
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setRequestId(ID)                               // each geofence has a unique id
                .setTransitionTypes(transitionTypes)            // Transition types: enter, dwell and exit
                .setLoiteringDelay(0)                           // set 0 so no delay when entering
                .setExpirationDuration(Geofence.NEVER_EXPIRE)   // don't want it to expire
                .build();
    }

    /**
     *  Gets the intent of the user, this determines the broadcast message to main calling code
     *
     * @return the intent of the broadcast
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    public PendingIntent getPendingIntent() {

        // Check that we have an intent
        if (pendingIntent != null){
            return pendingIntent;
        }

        // Create the intent
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);

        // Check the build version of android, if greater than android 12, then a mutable flag is
        // needed when a pending intent is needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        else
            pendingIntent = PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }
}
