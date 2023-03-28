package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {
    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    public static final int RUNNABLE_TIME = 3000;
    private static final String TAG = "MapsActivity";
    private static final String CHANNEL_ID = "my_channel";
    public static boolean geoFenceStatus, available_spot, my_spot, isAdmin, follow = false;
    public static String movingStatus;
    public static GoogleMap mMap;
    public static LocationManager mLocationManager;
    public static TextView name, speedAdminTextView, movingStatusTextView;
    public static FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    public static String username, parkedBestOption, parkedUser;
    public static final List<Polygon> parkingSpaces = new ArrayList<>();
    public static final List<String> parkingSpacesDocIDs = new ArrayList<>();
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    public static Button findMyCarButton;
    public static Context this_context;

    public Polygon campus;

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;

    public boolean animationInProgress;

    //To be used for EULA
//    public boolean acceptedTerms;

    public static Handler parkedHandler = new Handler();
    public static Runnable parkedRunnable = new Runnable() {  // This runnable will check if we are driving or walking after so many seconds after being parked.
        @Override
        public void run() {
            // do something depending on the status
            switch(movingStatus) {
                case "Driving":
                    // Get the user that has parked in that parking space
                    parkedUser = "";
                    firestore.collection("ParkingSpaces").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful())
                            parkedUser = Objects.requireNonNull(task.getResult().getDocuments().get(1).get("user")).toString();
                    });

                    if (Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(this_context, "Parked user: " + parkedUser, Toast.LENGTH_SHORT).show();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this_context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Parking Spotter")
                                .setContentText("Parked User: " + parkedUser)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_context);
                        int notificationId = 14;
                        notificationManager.notify(notificationId, builder.build());
                    }

                    // Get the polygon
                    Polygon parking_space = parkingSpaces.get(parkingSpacesDocIDs.indexOf(parkedBestOption));

                    // Get the distance to polygon center to see if we are even close to our own parking space
                    double distance = MapsLocationService.last_known_location_runnable.distanceTo(MapsLocationService.getPolygonCenter(parking_space));
                    boolean inVicinity = distance < 6;

                    if (Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(this_context, "Vicinity Distance: " + distance, Toast.LENGTH_SHORT).show();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this_context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Parking Spotter")
                                .setContentText("Vicinity Distance: " + distance)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_context);
                        int notificationId = 15;
                        notificationManager.notify(notificationId, builder.build());
                    }

                    // Check to make sure it is this users parking spot we are removing
                    if (parkedUser.equals(username) || inVicinity) {
                        if (Boolean.TRUE.equals(isAdmin)) {
                            Toast.makeText(this_context, "Clearing Parking Space", Toast.LENGTH_SHORT).show();

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(this_context, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentTitle("Parking Spotter")
                                    .setContentText("Clearing Parking Space")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_context);
                            int notificationId = 13;
                            notificationManager.notify(notificationId, builder.build());
                        }

                        // Set the parking space to empty
                        firestore.collection("ParkingSpaces").document(parkedBestOption).update("user", "");

                        // Hide the find my car button
                        findMyCarButton.setVisibility(View.INVISIBLE);

                        // parking space ID and its polygon
                        String id = parkingSpacesDocIDs.get(parkingSpacesDocIDs.indexOf(parkedBestOption));

                        // Restore the style depending on what type of parking space it is
                        if (id.startsWith("EV")) {
                            styleEVParkingSpace(parking_space);
                        } else if (id.startsWith("METER")) {
                            styleMeterParkingSpace(parking_space);
                        } else {
                            styleParkingEmptySpace(parking_space);
                        }
                    }
                    break;
                case "Walking":
                    if (Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(this_context, "Filling Parking Space", Toast.LENGTH_SHORT).show();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this_context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Parking Spotter")
                                .setContentText("Filling Parking Space")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_context);
                        int notificationId = 12;
                        notificationManager.notify(notificationId, builder.build());
                    }

                    // Set the firebase to be occupied by current user
                    firestore.collection("ParkingSpaces").document(parkedBestOption).update("user", username);

                    // Check to see if we already have a parked car, we do not want to parked cars right
                    if (findMyCarButton.getVisibility() != View.VISIBLE) {
                        // Get index of the parking space and then change the colour of that polygon
                        styleParkingYourSpace(parkingSpaces.get(parkingSpacesDocIDs.indexOf(parkedBestOption)));
                        findMyCarButton.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    if (Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(this_context, "Runnable Restarting", Toast.LENGTH_SHORT).show();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this_context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Parking Spotter")
                                .setContentText("Runnable Restarting")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_context);
                        int notificationId = 11;
                        notificationManager.notify(notificationId, builder.build());
                    }

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

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createNotificationChannel();

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

        //Geofencing Code --------------------------------------------------------
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
        // -----------------------------------------------------------------------

        // For the admin speed card view
        speedAdminTextView = findViewById(R.id.speedAdminTextView);
        movingStatusTextView = findViewById(R.id.movingStatusTextView);

        // Get the username of the current logged in user
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        username = sharedPref.getString("username", null);

        // Gets user administrator access
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                            name = findViewById(R.id.welcomeText);
                            String newName = "Welcome " + document.getString("name");
                            name.setText(newName);
                        }
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


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Create a listener to navigate to the settings screen when clicked
        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> startActivity(new Intent(MapsActivity.this, InfoActivity.class)));

        // Default the button to invisible
        findMyCarButton = findViewById(R.id.findMyCarButton);
        findMyCarButton.setVisibility(View.INVISIBLE);

        // Show the button if they have a parked car
        firestore.collection("ParkingSpaces").whereEqualTo("user", username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (!task.getResult().isEmpty())
                    findMyCarButton.setVisibility(View.VISIBLE);
        });

        // Set the onClick listener for the center button to zoom in the users parked car
        findMyCarButton.setOnClickListener(view -> firestore.collection("ParkingSpaces").whereEqualTo("user", username).get().addOnCompleteListener(task -> {
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

        // Set a listener for the maps current location button
        mMap.setOnMyLocationButtonClickListener(() -> {
            follow = true;
            return false; // Need this here don't remove or change this line
        });

        // Disable follow if the map is moved
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == 1) {
                    follow = false;
                }
            }
        });

        // Relocate the center location button on the mapview
        mMap.setPadding(0, 255, 15, 0);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_map));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else
            mMap.setMyLocationEnabled(true);

        // Create sub lot R9
        Polygon r9 = googleMap.addPolygon(new PolygonOptions()  // could possibly store this in the DB too I think but we need to figure out how to check for different number of vertices
                .clickable(true)
                .add(
                        new LatLng(48.422224, -89.259120),
                        new LatLng(48.421916, -89.258169),
                        new LatLng(48.422527, -89.257643),
                        new LatLng(48.422590, -89.257864),
                        new LatLng(48.422444, -89.258527)
                ));
        // Set the tag for clicking
        r9.setTag("R9");
        // Style it as a parking sub lot
        styleParkingSubLot(r9);

        campus = googleMap.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(48.422068652386756, -89.25903004659057),
                        new LatLng(48.421790245113186, -89.25873683680199),
                        new LatLng(48.42146393787311, -89.25875036956144),
                        new LatLng(48.419916195185614, -89.26029761506119),
                        new LatLng(48.42016168169639, -89.2614343668569),
                        new LatLng(48.419628794825826, -89.26209296116528),
                        new LatLng(48.42131724862186, -89.26422211535001),
                        new LatLng(48.42293380075808, -89.26041940991185)
                ));
        campus.setTag("campus");
        styleParkingSubLot(campus);

        // Make it clickable to zoom in on the chosen sub lot
        mMap.setOnPolygonClickListener(polygon -> {
            if ("R9".equals(polygon.getTag()))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.42232555839978, -89.25824351676498), 18.5f));
        });

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
                                    styleEVParkingSpace(polygon);
                                else if (document.getId().startsWith("METER"))
                                    styleMeterParkingSpace(polygon);
                                else
                                    styleParkingEmptySpace(polygon);
                            else if (parkedUsername.equalsIgnoreCase(username))
                                styleParkingYourSpace(polygon);
                            else
                                styleParkingFilledSpace(polygon);
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
                                    styleEVParkingSpace(parkingSpaces.get(parkingSpacePolygonIndex));
                                else if (dc.getDocument().getId().startsWith("METER"))
                                    styleMeterParkingSpace(parkingSpaces.get(parkingSpacePolygonIndex));
                                else
                                    styleParkingEmptySpace(parkingSpaces.get(parkingSpacePolygonIndex));
                                findMyCarButton.setVisibility(View.INVISIBLE);
                            } else if (newUser.equals(username)) {
                                styleParkingYourSpace(parkingSpaces.get(parkingSpacePolygonIndex));
                                findMyCarButton.setVisibility(View.VISIBLE);
                            } else
                                styleParkingFilledSpace(parkingSpaces.get(parkingSpacePolygonIndex));

                        }
                    }
                });

        // move the camera to default position
        LatLng Lot = new LatLng(48.42101, -89.25828);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 17.5f));       // Need to figure out a way to not reset this everytime we enter the map I feel

        //Insert a geofence at time of map creation centered around the parking lot with a radius of 500
        float GEOFENCE_RADIUS = 500;
        addGeofence(Lot, GEOFENCE_RADIUS);
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

        // Check if we are on campus boundaries to stop service or start it if we are back on ya know
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : campus.getPoints()) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();

        // Check if the user's location is inside the bounds of the polygon
        if (bounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
            // Stop foreground tracking
            Intent service_intent = new Intent(this, MapsLocationService.class);
            service_intent.setAction(MapsLocationService.ACTION_STOP_FOREGROUND_SERVICE);
            startService(service_intent);

            if(isAdmin)
                Toast.makeText(this_context, "On Campus - service stopped", Toast.LENGTH_SHORT).show();
        } else if (geoFenceStatus) {
            // Start foreground tracking
            Intent service_intent = new Intent(this, MapsLocationService.class);
            service_intent.setAction(MapsLocationService.ACTION_START_FOREGROUND_SERVICE);
            startService(service_intent);

            if(isAdmin)
                Toast.makeText(this_context, "Off Campus - service started", Toast.LENGTH_SHORT).show();
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

    private static void styleParkingEmptySpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this_context, R.color.green));
    }

    private void styleParkingFilledSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this_context, R.color.red));
    }

    private static void styleParkingYourSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this_context, R.color.your_car_blue));
    }

    private static void styleEVParkingSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this_context, R.color.ev_spot));
    }

    private static void styleMeterParkingSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this_context, R.color.meter_spot));
    }

    private void styleParkingSubLot(Polygon polygon){
        polygon.setStrokeWidth(0);
        //polygon.setFillColor(ContextCompat.getColor(this, R.color.parking_space_purple));
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


