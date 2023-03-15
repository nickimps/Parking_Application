package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {

    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    public static boolean geoFenceStatus;
    public static String movingStatus;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private TextView name, speedAdminTextView, movingStatusTextView;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    Boolean isAdmin;
    private String username;
    private final List<Polygon> parkingSpaces = new ArrayList<>();
    private final List<String> parkingSpacesDocIDs = new ArrayList<>();
    private List<String> parkedSpacesIDs = new ArrayList<>();
    private List<String> parkedSpacesUsers = new ArrayList<>();
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    public static Button centerButton;

    //Create a flag to see if the camera should follow
    Boolean follow = false;
    private static final String TAG = "MapsActivity";
    //GEOFENCE -----------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                            isAdmin = document.getBoolean("isAdmin");
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

        Button button = findViewById(R.id.settingsButton);
        button.setOnClickListener(view -> startActivity(new Intent(MapsActivity.this, InfoActivity.class)));

        centerButton = findViewById(R.id.recenterButton);
        @SuppressLint("MissingPermission") Location currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(currentLocation
                        .getLatitude(), currentLocation.getLongitude())).zoom(19.0f).build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                mMap.animateCamera(cameraUpdate);
                follow = true;
            }
        });

        int action = MotionEvent.ACTION_MOVE;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

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

        // Create sub lot R9
        Polygon r10 = googleMap.addPolygon(new PolygonOptions()  // could possibly store this in the DB too I think but we need to figure out how to check for different number of vertices
                .clickable(true)
                .add(
                        new LatLng(48.42177096009731, -89.25813853884138),
                        new LatLng(48.42152531583647, -89.25747603324294),
                        new LatLng(48.420697592740154, -89.2582163228995),
                        new LatLng(48.42093434072241, -89.25888687512463)
                ));
        // Set the tag for clicking
        r10.setTag("R10");
        // Style it as a parking sub lot
        styleParkingSubLot(r10);

        // Make it clickable to zoom in on the chosen sub lot
        mMap.setOnPolygonClickListener(polygon -> {
            if ("R9".equals(polygon.getTag()))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.42232555839978, -89.25824351676498), 19));
            else if ("R10".equals(polygon.getTag()))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.42126780490169, -89.25815691384268), 19));
        });

        // Get the parking spaces from the database and dynamically load them in
        firestore.collection("ParkingSpaces")
                //.whereEqualTo("subLotID", "R9")
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

        // move the camera to default position
        LatLng Lot = new LatLng(48.42101, -89.25828);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 17));       // Need to figure out a way to not reset this everytime we enter the map I feel

        //Geofencing Code --------------------------------------------------------
        //Insert a geofence at time of map creation centered around the parking lot with a radius of 500
        float GEOFENCE_RADIUS = 500;
        addGeofence(Lot, GEOFENCE_RADIUS);
        // -----------------------------------------------------------------------


        // This timer will update the current list of parked spaces - helps with computation time
        // in the checkStop() function so that it doesn't have to pull from the firestore database
        // everytime that you move.
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    try {
                        updateParkedSpacesList();
                    } catch (Exception e) {
                        Log.d(TAG, "onFailure: " + Arrays.toString(e.getStackTrace()));
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000); // 60000 ms = 1 min
    }

    /**
     * Refreshes the list of parked spaces on function call and stores them in a global arrayList to
     * be used in the checkStop() function.
     */
    private void updateParkedSpacesList() {
        // Get list of non-empty parking spaces and add them to the parkedSpaces list
        parkedSpacesIDs = new ArrayList<>();
        parkedSpacesUsers = new ArrayList<>();
        firestore.collection("ParkingSpaces")
                .whereNotEqualTo("user", "")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            parkedSpacesIDs.add(document.getId());
                            parkedSpacesUsers.add(document.getString("user"));
                        }
                });
    }

    /**
     * This function will change the speed label on the admin card view to the current speed in
     * real-time.
     *
     * @param location The location parameter
     * @return The speed to be set in the TextView
     */
    private float updateSpeedTextView(Location location) {
        float speed;
        if (location.hasSpeed()) {
            speed = location.getSpeed();
        } else {
            speed = 0.0f;
        }

        String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
        speedAdminTextView.setText(speedString);

        return speed;
    }

    /**
     * This function runs whenever the user has stopped moving. It is supposed to check if we have
     * stopped inside or outside of a parking spot.
     * It starts by looking at which parking spaces we may be inside of and adds those to a list of
     * possible candidates. It then looks at the distance to the center of those possible parking
     * spaces and choose the closest EMPTY parking space. Once it has chosen a parking space, it will
     * change the colour of the parking space to make the 'Your Car' colour scheme.
     *
     * @param location The location parameter
     * @return stoppedStatus, will be either 'Stopped' or 'Parked'
     */
    private String checkStop(Location location) {
        // Default moving status
        String stoppedStatus = "Stopped";

        // Check permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // Get the current location of the user
            Location currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Hashmap to store a key-value pair of the possible parking spaces that we may be occupying
            HashMap<Polygon, Double> possibleParkedSpaces = new HashMap<>();

            // Check if the user's location is inside any of the polygons
            if (mMap != null) {
                // Need to go through each parking space and gather which parking spaces we think we are parked in
                for (Polygon polygon : parkingSpaces) {
                    // Create a LatLngBounds object that contains the polygon
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polygon.getPoints()) {
                        builder.include(latLng);
                    }
                    LatLngBounds bounds = builder.build();

                    // Check if the user's location is inside the bounds of the polygon
                    if (bounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                        // Get the points of this polygon
                        List<LatLng> points = polygon.getPoints();
                        double latSum = 0, lngSum = 0;
                        for (LatLng point : points) {
                            latSum += point.latitude;
                            lngSum += point.longitude;
                        }

                        // Compute the center of the polygon
                        LatLng centerPoint = new LatLng(latSum / points.size(), lngSum / points.size());

                        // Turn the center point into a location for measuring
                        Location centerOfPolygon = new Location("");
                        centerOfPolygon.setLatitude(centerPoint.latitude);
                        centerOfPolygon.setLongitude(centerPoint.longitude);

                        // Get the new distance
                        double distance = currentLocation.distanceTo(centerOfPolygon);

                        // Save the parking spaces and their distance to the users current location.
                        possibleParkedSpaces.put(polygon, distance);

                        // Set the status to 'parked'
                        stoppedStatus = "Parked";
                    }
                }

                // If we changed the status to parked, we need to choose a single parking spot
                if (stoppedStatus.equals("Parked")) {
                    // If its only size of 1, we can skip unnecessary computations
                    if (possibleParkedSpaces.size() == 1) {
                        Map.Entry<Polygon, Double> entry = possibleParkedSpaces.entrySet().iterator().next();
                        String docID = parkingSpacesDocIDs.get(parkingSpaces.indexOf(entry.getKey()));

                        if (!parkedSpacesIDs.contains(docID))
                            styleParkingYourSpace(parkingSpaces.get(parkingSpacesDocIDs.indexOf(docID)));

                    } else {
                        String bestOption = "";
                        double lowestDistance = 10000;

                        // Go through the hashmap and check if the lowest spot is empty
                        for(Map.Entry possibleSpace : possibleParkedSpaces.entrySet()) {
                            // Get the parking space id and distance for comparisons
                            String docID = parkingSpacesDocIDs.get(parkingSpaces.indexOf(possibleSpace.getKey()));
                            double polyDistance = (double) possibleSpace.getValue();

                            if (polyDistance < lowestDistance) {
                                // If the parking space has someone parked there, we cant park there so
                                // don't consider it as a good option.
                                if (!parkedSpacesIDs.contains(docID)) {
                                    lowestDistance = polyDistance;
                                    bestOption = docID;
                                }
                            }
                        }

                        // Change appearance of the parking space if we have parked in it
                        if (!bestOption.equals("")) {
                            // Get index of the parking space and then change the colour of that polygon
                            styleParkingYourSpace(parkingSpaces.get(parkingSpacesDocIDs.indexOf(bestOption)));

                            if (parkedSpacesUsers.contains(username)) {
                                System.out.println("This is filler.");
                            }

                            // Update the DB to your new spot
                            // Remove any other spaces we may be parked inside of
                        } else {
                            stoppedStatus = "Stopped";
                        }
                    }
                }
            }
        }

        return stoppedStatus;
    }

    /**
     * When the user moves, this function is called. It checks the speed, if we have determined the
     * user as being stopped, we call the checkStop() function. Otherwise, we determine if the user
     * is walking or driving and display that label and the speed accordingly.
     *
     * @param location The location parameter
     */
    @Override
    public void onLocationChanged(Location location) {
        //Set a flag to see if user touches the screen or moves the map at all, surround code below
        //with if statement so if the user moves the map, do not update camera position, add button
        //to let the user recenter and have it track the camera once again

        //Possible methods: Override onTouch(...), use Events
        //Toggle zoom within Geofence -> To be added


        //Update camera position every time user location changes
        //Create an object to capture the position of the camera based on Lat and Long
        //then update the camera position
        if (geoFenceStatus && follow == true/* && Check if re-center button is not visible */) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location
                    .getLatitude(), location.getLongitude())).zoom(19.0f).build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            mMap.animateCamera(cameraUpdate);
        }

        // Update the speed on the card view on the screen
        float speed = updateSpeedTextView(location);
        movingStatus = "Stopped";

        if (speed <= 0.05) {
            movingStatus = checkStop(location);
        } else if (speed > 0.05 && speed <= 2) {
            movingStatus = "Walking";
        } else if (speed > 2) {
            movingStatus = "Driving";
        }

        movingStatusTextView.setText(movingStatus);

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


    private void styleParkingEmptySpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this, R.color.green));
    }

    private void styleParkingFilledSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this, R.color.red));
    }

    private void styleParkingYourSpace(Polygon polygon) {
        polygon.setStrokeWidth(3);
        polygon.setFillColor(ContextCompat.getColor(this, R.color.your_car_blue));
    }

    private void styleParkingSubLot(Polygon polygon){
        polygon.setStrokeWidth(5);
        polygon.setFillColor(ContextCompat.getColor(this, R.color.parking_space_purple));
    }

    /**
     * Populates the textfield with the location
     * Also starts the .requestLocationUpdates for the onLocationChanged function
     */
    @SuppressLint("SetTextI18n")
    private void getGPSData() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // Start the listener to manage location updates
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);
        }
    }

    private void addGeofence(LatLng latLng, float radius) {
        //trigger geofence when entering dwelling or exiting (maybe change)
        //each geofence has a unique id
        String GEOFENCE_ID = "SOME_GEOFENCE_ID";
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();


        // PRETTY SURE THIS CODE BELOW IS USELESS, IT WORKS WITHOUT THIS CODE BTW
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(unused -> Log.d(TAG, "onSuccess: Geofence Added..."))
                    .addOnFailureListener(e -> {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    });
        }

    }
}


