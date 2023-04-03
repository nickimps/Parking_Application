package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gpsapp.databinding.ActivityMapsBinding;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int REQUEST_LOCATION = 1;
    private static final String TAG = "MapsActivity";
    public static boolean geoFenceStatus, isAdmin, follow = false, inPolygon, animationInProgress;
    public static GoogleMap mMap;
    @SuppressLint("StaticFieldLeak")
    public static TextView name, speedAdminTextView, movingStatusTextView;
    @SuppressLint("StaticFieldLeak")
    public static FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    public static String username, parkedBestOption, parkedUser, movingStatus;
    public static final List<Polygon> parkingSpaces = new ArrayList<>();
    public static final List<String> parkingSpacesDocIDs = new ArrayList<>();
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    @SuppressLint("StaticFieldLeak")
    public static Button findMyCarButton;
    @SuppressLint("StaticFieldLeak")
    public static Context this_context;
    public static Polygon campus;

    /**
     * Called on activity creation, many things initialize and happen here.
     *
     * @param savedInstanceState The instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the stored information within the shared preference
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        // Get the username of the current logged in user
        username = sharedPref.getString("username", null);
        String password = sharedPref.getString("password", null);

        System.out.println(username + " " + password);

        // If the user is not logged in, go to login screen, otherwise go to maps activity like normal
        if (username == null || password == null) {
            startActivity(new Intent(MapsActivity.this, LoginActivity.class));
            finish();
        } else {
            ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        }

        // Get ID if name TextView
        name = findViewById(R.id.welcomeText);

        // Gets user administrator access
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                            String newName = "Welcome " + document.getString("name");
                            name.setText(newName);
                        }

                        // Display admin information accordingly
                        if (Boolean.TRUE.equals(isAdmin)) {
                            findViewById(R.id.adminText).setVisibility(View.VISIBLE);
                            findViewById(R.id.adminBannerCardView).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.adminText).setVisibility(View.GONE);
                            findViewById(R.id.adminBannerCardView).setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });

        // To use context in other scenarios
        this_context = getApplicationContext();

        // Geofencing Code
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        // For the admin speed card view
        speedAdminTextView = findViewById(R.id.speedAdminTextView);
        movingStatusTextView = findViewById(R.id.movingStatusTextView);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Create a listener to navigate to the settings screen when clicked
        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> startActivity(new Intent(MapsActivity.this, InfoActivity.class)));

        // Get ID of find my car button
        findMyCarButton = findViewById(R.id.findMyCarButton);
        // Set the onClick listener for the center button to zoom in the users parked car
        findMyCarButton.setOnClickListener(view -> firestore.collection("ParkingSpaces")
                .whereEqualTo("user", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LatLng whereIParked = new LatLng(Objects.requireNonNull(document.getGeoPoint("x3")).getLatitude(), Objects.requireNonNull(document.getGeoPoint("x3")).getLongitude());
                            CameraPosition cameraPosition = new CameraPosition.Builder().target(whereIParked).zoom(18.5f).build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                            mMap.animateCamera(cameraUpdate);
                        }
                    }
                }));
    }

    /**
     * This function is called when the map is ready, loads in polygons and changes things accordingly
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Move the camera to default position
        LatLng Lot = new LatLng(48.42151037144106, -89.25831461845203);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 17.8f));

        // Insert a geofence at time of map creation centered around the parking lot with a radius of 500
        float GEOFENCE_RADIUS = 500;
        addGeofence(Lot, GEOFENCE_RADIUS);

        // Set a listener for the maps current location button
        mMap.setOnMyLocationButtonClickListener(() -> {
            follow = true;
            return false; // Need this here
        });

        // Disable follow if the map is moved
        mMap.setOnCameraMoveStartedListener(i -> {
            if(i == 1)
                follow = false;
        });

        // Relocate the center location button on the mapview
        mMap.setPadding(0, 255, 15, 0);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_map));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);

        // Get the parking spaces from the database and dynamically load them in
        firestore.collection("ParkingSpaces")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Load in the spaces from the DB and create the polygon
                            Polygon polygon = googleMap.addPolygon(new PolygonOptions().add(
                                    new LatLng(Objects.requireNonNull(document.getGeoPoint("x1")).getLatitude(), Objects.requireNonNull(document.getGeoPoint("x1")).getLongitude()),
                                    new LatLng(Objects.requireNonNull(document.getGeoPoint("x2")).getLatitude(), Objects.requireNonNull(document.getGeoPoint("x2")).getLongitude()),
                                    new LatLng(Objects.requireNonNull(document.getGeoPoint("x3")).getLatitude(), Objects.requireNonNull(document.getGeoPoint("x3")).getLongitude()),
                                    new LatLng(Objects.requireNonNull(document.getGeoPoint("x4")).getLatitude(), Objects.requireNonNull(document.getGeoPoint("x4")).getLongitude())
                            ));
                            parkingSpaces.add(polygon);
                            parkingSpacesDocIDs.add(document.getId());

                            // Check if it is filled, empty, or your own and style the space accordingly
                            String parkedUsername = document.getString("user");
                            assert parkedUsername != null;
                            if (parkedUsername.equals(""))
                                if (document.getId().startsWith("EV"))
                                    stylePolygon(polygon, "EV");
                                else if (document.getId().startsWith("METER"))
                                    stylePolygon(polygon, "Meter");
                                else
                                    stylePolygon(polygon, "Empty");
                            else if (parkedUsername.equalsIgnoreCase(username))
                                stylePolygon(polygon, "Yours");
                            else
                                stylePolygon(polygon, "Filled");
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });

        // Create a listener to respond to database updates in real time
        firestore.collection("ParkingSpaces")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    // Get which documents have been updated
                    assert snapshots != null;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            // Get the index of the polygon that we are wanting to change the style of
                            int parkingSpacePolygonIndex = parkingSpacesDocIDs.indexOf(dc.getDocument().getId());
                            // Get the new value from the user field that has been updated
                            String newUser = Objects.requireNonNull(dc.getDocument().get("user")).toString();

                            // Check if the new user is empty, current user, or someone else and style appropriately
                            // Also, show or hide the find my car button
                            if (newUser.equals("")) {
                                if (dc.getDocument().getId().startsWith("EV"))
                                    stylePolygon(parkingSpaces.get(parkingSpacePolygonIndex), "EV");
                                else if (dc.getDocument().getId().startsWith("METER"))
                                    stylePolygon(parkingSpaces.get(parkingSpacePolygonIndex), "Meter");
                                else
                                    stylePolygon(parkingSpaces.get(parkingSpacePolygonIndex), "Empty");
                                findMyCarButton.setVisibility(View.INVISIBLE);
                            } else if (newUser.equals(username)) {
                                stylePolygon(parkingSpaces.get(parkingSpacePolygonIndex), "Yours");
                                findMyCarButton.setVisibility(View.VISIBLE);
                            } else
                                stylePolygon(parkingSpaces.get(parkingSpacePolygonIndex), "Filled");
                        }
                    }
                });

        // Show the button if they have a parked car
        firestore.collection("ParkingSpaces").whereEqualTo("user", username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (!task.getResult().isEmpty())
                    findMyCarButton.setVisibility(View.VISIBLE);
        });

        // Get the polygon for the campus so we can use this to tell if we are in the parking or not
        campus = googleMap.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(48.42176462322088, -89.25887980779537),
                        new LatLng(48.42149778468855, -89.25886084171513),
                        new LatLng(48.421364364897094, -89.25918705829504),
                        new LatLng(48.42101445095027, -89.25937292588127),
                        new LatLng(48.420772782357716, -89.25990018291158),
                        new LatLng(48.42016105359544, -89.26063986005005),
                        new LatLng(48.42021895416382, -89.26116711708035),
                        new LatLng(48.421213323198685, -89.26180817059202),
                        new LatLng(48.422373815372865, -89.26070055150677),
                        new LatLng(48.421978596501226, -89.25953603418084),
                        new LatLng(48.42196349253433, -89.25909222790354)
                ));
        campus.setTag("campus");
        stylePolygon(campus, "Sub Lot");
    }

    private void stylePolygon(Polygon polygon, String style) {
        polygon.setStrokeWidth(3);
        switch (style) {
            case "Empty":
                polygon.setFillColor(ContextCompat.getColor(this_context, R.color.green));
                break;
            case "Filled":
                polygon.setFillColor(ContextCompat.getColor(this_context, R.color.red));
                break;
            case "Yours":
                polygon.setFillColor(ContextCompat.getColor(this_context, R.color.your_car_blue));
                break;
            case "EV":
                polygon.setFillColor(ContextCompat.getColor(this_context, R.color.ev_spot));
                break;
            case "Meter":
                polygon.setFillColor(ContextCompat.getColor(this_context, R.color.meter_spot));
                break;
            case "Sub Lot":
                polygon.setStrokeWidth(0);
                break;
        }
    }

    /**
     * Adds the geofence to the map so we can do some tracking in that instance
     *
     * @param latLng the location of the geofence
     * @param radius the radius of the geofence
     */
    private void addGeofence(LatLng latLng, float radius) {
        //trigger geofence when entering dwelling or exiting (maybe change)
        //each geofence has a unique id
        String GEOFENCE_ID = "SOME_GEOFENCE_ID";
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        //If permissions are not given, request for access to location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            //Add geofence if permissions are accepted, add listeners to see if successfully created
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(unused -> Log.d(TAG, "onSuccess: Geofence Added..."))
                    .addOnFailureListener(e -> {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    });
        }
    }
}


