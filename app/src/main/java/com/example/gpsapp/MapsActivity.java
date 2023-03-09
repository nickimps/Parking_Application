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
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gpsapp.databinding.ActivityMapsBinding;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private ActivityMapsBinding binding;
    private TextView name, speedAdminTextView;
    FirebaseFirestore firestore;
    Boolean isAdmin;
    private String username;
    private List<Polygon> parkingSpaces = new ArrayList<>();
    private List<String> parkingSpacesDocIDs = new ArrayList<>();

    //geofence
    //GEOFENCE -----------------------------------------------------------------------------
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private float GEOFENCE_RADIUS = 200;
    private String GEOFENCE_ID = "SOME_GEOFENCE_ID"; //each geofence has a unique id
    private static final String TAG = "MapsActivity";
    //GEOFENCE -----------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
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

        // Get the username of the current logged in user
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        username = sharedPref.getString("username", null);

        // Gets user administrator access
        firestore = FirebaseFirestore.getInstance();
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // do not show your position
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
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("ParkingSpaces")
                //.whereEqualTo("subLotID", "R9")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Load in the spaces from the DB and create the polygon
                            Polygon polygon = googleMap.addPolygon(new PolygonOptions().add(
                                    new LatLng(document.getGeoPoint("x1").getLatitude(), document.getGeoPoint("x1").getLongitude()),
                                    new LatLng(document.getGeoPoint("x2").getLatitude(), document.getGeoPoint("x2").getLongitude()),
                                    new LatLng(document.getGeoPoint("x3").getLatitude(), document.getGeoPoint("x3").getLongitude()),
                                    new LatLng(document.getGeoPoint("x4").getLatitude(), document.getGeoPoint("x4").getLongitude())
                            ));
                            parkingSpaces.add(polygon);
                            parkingSpacesDocIDs.add(document.getId());

                            // Check if it is filled, empty, or your own and style the space accordingly
                            String parkedUsername = document.getString("user");
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
        mMap.setOnMapLongClickListener(this);
        // -----------------------------------------------------------------------
    }

    @Override
    public void onLocationChanged(Location location) {
        float speed = 0.0f;
        if (location.hasSpeed()) {
            speed = location.getSpeed();
        } else {
            speed = 0.0f;
        }

        String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
        speedAdminTextView.setText(speedString);

        // Check if the user's location is inside any of the polygons
        if (mMap != null) {
            for (Polygon polygon : parkingSpaces) {
                // Create a LatLngBounds object that contains the polygon
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng latLng : polygon.getPoints()) {
                    builder.include(latLng);
                }
                LatLngBounds bounds = builder.build();

                // Check if the user's location is inside the bounds of the polygon
                if (bounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                    // The user's location is inside the polygon
                    System.out.println("Inside  of " + parkingSpacesDocIDs.get(parkingSpaces.indexOf(polygon)));
                    styleParkingYourSpace(polygon);
                }
                else {
                    System.out.println("Outside of " + parkingSpacesDocIDs.get(parkingSpaces.indexOf(polygon)));
                    styleParkingEmptySpace(polygon);
                }
            }
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

    @Override //Geofence
    public void onMapLongClick(@NonNull LatLng latLng) {
        addMarker(latLng);
        addCircle(latLng, GEOFENCE_RADIUS);
        addGeofence(latLng, GEOFENCE_RADIUS);
    }

    private void addGeofence(LatLng latLng, float radius) {
        //trigger geofence when entering dwelling or exiting (maybe change)
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    //adds a marker for geofence
    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    //adds radius for geofence
    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
        circleOptions.fillColor(Color.argb(64, 255, 0, 0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

}


