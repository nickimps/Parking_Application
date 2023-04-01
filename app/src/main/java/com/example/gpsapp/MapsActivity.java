package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {
    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    public static final int RUNNABLE_TIME = 2500;
    private static final String TAG = "MapsActivity";
    private static final String CHANNEL_ID = "my_channel";
    public static boolean geoFenceStatus, available_spot, my_spot, isAdmin, follow = false, inPolygon;
    public static GoogleMap mMap;
    public static LocationManager mLocationManager;
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
    public boolean animationInProgress;
    public static Handler parkedHandler = new Handler();
    public static Runnable parkedRunnable = new Runnable() {  // This runnable will check if we are driving or walking after so many seconds after being parked.
        @Override
        public void run() {
            // do something depending on the status
            switch (movingStatus) {
                case "Driving":
                    // Get the user that has parked in that parking space
                    parkedUser = "";
                    firestore.collection("ParkingSpaces").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            parkedUser = Objects.requireNonNull(task.getResult().getDocuments().get(1).get("user")).toString();

                            if (Boolean.TRUE.equals(isAdmin))
                                Toast.makeText(this_context, "Parked user: " + parkedUser, Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Get the polygon for the parking space being driven away from
                    Polygon parking_space = parkingSpaces.get(parkingSpacesDocIDs.indexOf(parkedBestOption));

                    // Get the distance to polygon center to see if we are even close to our own parking space
                    double distance = MapsLocationService.last_known_location_runnable.distanceTo(MapsLocationService.getPolygonCenter(parking_space));
                    boolean inVicinity = distance < 6;

                    // Print the vicinity distance as a toast message
                    if (Boolean.TRUE.equals(isAdmin))
                        Toast.makeText(this_context, "Vicinity Distance: " + distance, Toast.LENGTH_SHORT).show();

                    // Check to make sure it is this users parking spot we are removing
                    if (parkedUser.equals(username) || inVicinity) {
                        if (Boolean.TRUE.equals(isAdmin))
                            Toast.makeText(this_context, "Clearing Parking Space", Toast.LENGTH_SHORT).show();

                        // Set the parking space to empty
                        firestore.collection("ParkingSpaces").document(parkedBestOption).update("user", "");
                    }
                    break;
                case "Walking":
                    // Remove any parking spaces if they have any
                    firestore.collection("ParkingSpaces")
                            .whereEqualTo("user", username)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Loop through and erase any parking spots the user may have already as we can't be parked in two spots at once
                                    for (QueryDocumentSnapshot document : task.getResult())
                                        if (!document.getId().equals(parkedBestOption))
                                            firestore.collection("ParkingSpaces").document(document.getId()).update("user", "");

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
                                            });
                                }
                            });
                    break;
                case "Parked":
                    if (Boolean.TRUE.equals(isAdmin))
                        Toast.makeText(this_context, "Parked - Runnable Restarting", Toast.LENGTH_SHORT).show();

                    // If we are still parked, we want to restart this and keep polling
                    restartRunnable();
                    break;
                default:
                    if (Boolean.TRUE.equals(isAdmin))
                        Toast.makeText(this_context, "Stopped - Runnable Restarting", Toast.LENGTH_SHORT).show();

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
        parkedHandler.postDelayed(parkedRunnable, RUNNABLE_TIME);
    }

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

        // If the user is not logged in, go to login screen, otherwise go to maps activity like normal
        if (username == null || sharedPref.getString("password", null) == null) {
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

        // For foreground notifications
        createNotificationChannel();

        // To use context in other scenarios
        this_context = getApplicationContext();

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
            getGPSData();
        }

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        else
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

    /**
     * When the user moves, this function is called. It checks the speed, if we have determined the user as being stopped, we call the
     * checkStop() function. Otherwise, we determine if the user is walking or driving and display that label and the speed accordingly.
     *
     * @param location The location parameter
     */
    @Override
    public void onLocationChanged(Location location) {
        // Have the camera follow the user if the follow boolean is set to true
        if (follow) {
            GoogleMap.CancelableCallback cancelableCallback = new GoogleMap.CancelableCallback() {
                @Override
                public void onCancel() {
                    animationInProgress = false;
                }

                @Override
                public void onFinish() {
                    animationInProgress = false;
                }
            };

            if(!animationInProgress) {
                animationInProgress = true;
                CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(mMap.getCameraPosition().zoom).build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                mMap.animateCamera(cameraUpdate, cancelableCallback);
            }
        }

        if (!geoFenceStatus) {
            String speed_text = "N/A";
            MapsActivity.speedAdminTextView.setText(speed_text);
            String moving_test = "Outside Geofence";
            MapsActivity.movingStatusTextView.setText(moving_test);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Called when the user enables the GPS provider
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Called when the user disables the GPS provider
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Called when the status of the GPS provider changes
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
     * Populates the textfield with the location
     * Also starts the .requestLocationUpdates for the onLocationChanged function
     */
    @SuppressLint("SetTextI18n")
    private void getGPSData() {
        //Request permissions if they have not already been accepted
        if (ActivityCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // Start the listener to manage location updates
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
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


