package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
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
import com.google.firebase.database.collection.LLRBNode;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    // Default colours
    private static final int COLOR_LIGHT_GREEN_ARGB = 0xff81C784;
    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYGON_STROKE_WIDTH_PX = 4;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView name;
    FirebaseFirestore firestore;
    Boolean isAdmin;

    //geofence
    private GeofencingClient geofenceingClient;
    private float GEOFENCE_RADIUS = 200;

    private LocationRequest locationRequest;
    private LocationManager locationManager;

    private LocationCallback locationCallBack;

    protected static final int REQUEST_CHECK_SETTINGS = 9001;
    public boolean requestingLocationUpdates;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Geofencing Code --------------------------------------------------------
        geofenceingClient = LocationServices.getGeofencingClient(this);
        // -----------------------------------------------------------------------

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Trying some stuff - Jimmy
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> taskM = client.checkLocationSettings(builder.build());

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location: locationResult.getLocations()) {

                }
            }
        };

        updateValuesFromBundle(savedInstanceState);

        taskM.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

            }
        });

        taskM.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {

                    }
                }
            }
        });

        // Get the username of the current logged in user
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        String username = sharedPref.getString("username", null);

        // Gets user administrator access
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            isAdmin = document.getBoolean("isAdmin");
                        }
                        if (Boolean.TRUE.equals(isAdmin))
                            findViewById(R.id.adminText).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.adminText).setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });


        name = findViewById(R.id.welcomeText);
        name.setText("Welcome " + username);

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

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                //name.setText((int) location.getLatitude());
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationString = "Latitude: " + latitude + "\nLongitude: " + longitude;
                //Toast.makeText(getApplicationContext(),locationString,Toast.LENGTH_SHORT).show();
            }
        });

        Polygon r9 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422224, -89.259120),
                        new LatLng(48.421916, -89.258169),
                        new LatLng(48.422527, -89.257643),
                        new LatLng(48.422590, -89.257864),
                        new LatLng(48.422444, -89.258527)));
        r9.setStrokeWidth(5);
        r9.setFillColor(0x33746AB0);

        Polygon spot1 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422477, -89.258081),
                        new LatLng(48.422503, -89.258144),
                        new LatLng(48.422478, -89.258165),
                        new LatLng(48.422453, -89.258100)));
        spot1.setStrokeWidth(3);
        spot1.setFillColor(0x66746AB0);

        Polygon spot2 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422478, -89.258165),
                        new LatLng(48.422453, -89.258100),
                        new LatLng(48.422431, -89.258125),
                        new LatLng(48.422454, -89.258185)));
        spot2.setStrokeWidth(3);
        spot2.setFillColor(0x66746AB0);

        Polygon spot3 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422454, -89.258185),
                        new LatLng(48.422431, -89.258125),
                        new LatLng(48.422407, -89.258145),
                        new LatLng(48.422432, -89.258208)));
        spot3.setStrokeWidth(3);
        spot3.setFillColor(0x66746AB0);

        Polygon spot4 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422432, -89.258208),
                        new LatLng(48.422407, -89.258145),
                        new LatLng(48.422382, -89.258166),
                        new LatLng(48.422405, -89.258227)));
        spot4.setStrokeWidth(3);
        spot4.setFillColor(0x66746AB0);

        Polygon spot5 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422405, -89.258227),
                        new LatLng(48.422382, -89.258166),
                        new LatLng(48.422359, -89.258188),
                        new LatLng(48.422381, -89.258248)));
        spot5.setStrokeWidth(3);
        spot5.setFillColor(0x66746AB0);

        Polygon spot6 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422432, -89.258208),
                        new LatLng(48.422405, -89.258227),
                        new LatLng(48.422432, -89.258293),
                        new LatLng(48.422456, -89.258271)));
        spot6.setStrokeWidth(3);
        spot6.setFillColor(0x66746AB0);

        Polygon spot7 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422432, -89.258293),
                        new LatLng(48.422407, -89.258315),
                        new LatLng(48.422381, -89.258248),
                        new LatLng(48.422405, -89.258227)));
        spot7.setStrokeWidth(3);
        spot7.setFillColor(0x66746AB0);

        Polygon spot8 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422384, -89.258334),
                        new LatLng(48.422358, -89.258263),
                        new LatLng(48.422381, -89.258248),
                        new LatLng(48.422407, -89.258315)));
        spot8.setStrokeWidth(3);
        spot8.setFillColor(0x66746AB0);

        Polygon spot9 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422385, -89.258334),
                        new LatLng(48.422358, -89.258263),
                        new LatLng(48.422333, -89.258288),
                        new LatLng(48.422359, -89.258358)));
        spot9.setStrokeWidth(3);
        spot9.setFillColor(0x66746AB0);

        Polygon spot10 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422381, -89.258248),
                        new LatLng(48.422359, -89.258188),
                        new LatLng(48.422337, -89.258208),
                        new LatLng(48.422358, -89.258263)));
        spot10.setStrokeWidth(3);
        spot10.setFillColor(0x66746AB0);

        Polygon spot11 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.422358, -89.258263),
                        new LatLng(48.422337, -89.258208),
                        new LatLng(48.422311, -89.258228),
                        new LatLng(48.422333, -89.258288)));
        spot11.setStrokeWidth(3);
        spot11.setFillColor(0x66746AB0);

        // move the camera
        LatLng Lot = new LatLng(48.42101, -89.25828);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 15));

        //Geofencing Code --------------------------------------------------------
        mMap.setOnMapLongClickListener(this);
        // -----------------------------------------------------------------------
    }

    /**
     * Styles the parking space
     * @param polygon The polygon that represents the parking space
     */
    private void styleParkingSpace(Polygon polygon) {
        polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
        polygon.setStrokeColor(COLOR_BLACK_ARGB);
        polygon.setFillColor(COLOR_LIGHT_GREEN_ARGB);
    }

    @Override //Geofence
    public void onMapLongClick(@NonNull LatLng latLng) {
        addMarker(latLng);
        addCicle(latLng, GEOFENCE_RADIUS);
    }

    //adds a marker for geofence
    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    //adds radius for geofence
    private void addCicle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
        circleOptions.fillColor(Color.argb(64, 255, 0, 0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    //Trying to implement live tracking will update depreciated methods
    protected void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallBack);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates);

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
        //updateUI();
    }
}
