package com.example.gpsapp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.widget.Switch;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Executable;

public class GeofenceHelper extends ContextWrapper {

    private static final String TAG = "GeofenceHelper";
    PendingIntent pendingIntent = null;

    public GeofenceHelper(Context base) {
        super(base);
    }
    //Create a new geofence given the specified parameters
    public GeofencingRequest getGeofencingRequest(Geofence geofence){
        System.out.println("Trigger Geofencing Request");
        return new GeofencingRequest.Builder()
                .addGeofence(geofence)
                //Set the initial trigger to enter
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();
    }

    public Geofence getGeofence(String ID, LatLng latLng, float radius, int transitionTypes){

        System.out.println("Get Geofencing Request");

        //Transition types: enter, dwell and exit
        return new Geofence.Builder()
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setRequestId(ID) //each geofence has a unique id
                .setTransitionTypes(transitionTypes)
                .setLoiteringDelay(0) //set 0 so no delay when entering
                .setExpirationDuration(Geofence.NEVER_EXPIRE) //dont want it to expire
                .build();
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    public PendingIntent getPendingIntent() {

        if (pendingIntent != null){
            return pendingIntent;
        }

        System.out.println("10");
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);

        System.out.println("11");
        //Check the build version of android, if greater than android 12, then a mutable flag is needed when a pending
        //intent is needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S){
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        }
        else{
            pendingIntent = PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        System.out.println("12");
        return pendingIntent;
    }

    public String getErrorString(Exception e){
        if(e instanceof ApiException){
            ApiException apiException = (ApiException) e;
            switch (apiException.getStatusCode()){
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    return"GEOFENCE_NOT_AVAILABLE";
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    return"GEOFENCE_TOO_MANY_GEOFENCES";
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    return"GEOFENCE_TOO_MANY_PENDING_INTENTS";
            }
        }
        return e.getLocalizedMessage();
    }
}
