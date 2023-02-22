package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.example.gpsapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView name;
    FirebaseFirestore firestore;
    Boolean isAdmin;
    private String username;

    private List<Polygon> parkingSpaces = new ArrayList<>();

    //geofence
    //GEOFENCE -----------------------------------------------------------------------------
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private float GEOFENCE_RADIUS = 200;
    private String GEOFENCE_ID = "SOME_GEOFENCE_ID"; //each geofence has a unique id
    private static final String TAG = "MapsActivity";
    //GEOFENCE -----------------------------------------------------------------------------

    private LocationRequest locationRequest;
    LocationCallback locationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location: locationResult.getLocations()) {
                Log.d(TAG, "onLocationResult: " + location.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Geofencing Code --------------------------------------------------------
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
        // -----------------------------------------------------------------------

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, null);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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
                            name.setText("Welcome " + document.getString("name"));
                        }
                        if (Boolean.TRUE.equals(isAdmin))
                            findViewById(R.id.adminText).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.adminText).setVisibility(View.GONE);
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
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 17));

        //Geofencing Code --------------------------------------------------------
        mMap.setOnMapLongClickListener(this);
        // -----------------------------------------------------------------------
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

    private class MyLocationListener implements LocationListener {
        private List<Polygon> parkingSpaces;

        public MyLocationListener(List<Polygon> parkingSpaces) {
            this.parkingSpaces = parkingSpaces;
        }

        @Override
        public void onLocationChanged(Location location) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Iterate over the polygons and check if the user's location is inside any of them
//            for (Polygon parkingSpace : parkingSpaces) {
//                if (parkingSpace.contains(latLng)) {
//                    styleParkingYourSpace(parkingSpace);
//                } else {
//                    styleParkingYourSpace(parkingSpace);
//                }
//            }
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

    private void checkSettings() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //If settings allowed
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallBack);
    }
}


