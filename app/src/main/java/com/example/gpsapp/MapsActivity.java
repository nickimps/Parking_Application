package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.gpsapp.databinding.ActivityMapsBinding;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location userCurrentLocation;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        String username = sharedPref.getString("username", null);

        TextView name = findViewById(R.id.welcomeText);
        name.setText("Welcome " + username);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        initLocationListener();
        getUserCurrentLocation();

        Button button = findViewById(R.id.settingsButton);
        button.setOnClickListener(view -> startActivity(new Intent(MapsActivity.this, InfoActivity.class)));
    }

    private void getUserCurrentLocation() {
        if (isPermissionGranted()) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.v("method called", "here");
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    isPermissionGranted();
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else {
                userCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        if (userCurrentLocation != null) {
            Log.v("lat_long", "lat:" + userCurrentLocation.getLatitude() + ",longi:" + userCurrentLocation.getLongitude());
        }
    }

    private boolean isPermissionGranted() {
        Boolean permissionGranted = true;
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, 123);
            }
        }
        return permissionGranted;
    }

    private void initLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                userCurrentLocation = location;
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                LocationListener.super.onProviderEnabled(provider);
                Toast.makeText(MapsActivity.this, "Provider Enabled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                LocationListener.super.onProviderDisabled(provider);
                Toast.makeText(MapsActivity.this, "Provider disable", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                LocationListener.super.onStatusChanged(provider, status, extras);
                Toast.makeText(MapsActivity.this, "Status changed", Toast.LENGTH_SHORT).show();
            }
        };
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

        // Add a marker in Sydney and move the camera
        LatLng Lot = new LatLng(48.42101, -89.25828);
        googleMap.addMarker(new MarkerOptions().position(Lot).title("Marker at Lot"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 18));
        setMarkerAtMyLocation();

    }

    private void setMarkerAtMyLocation() {
        if (userCurrentLocation != null) {
            Log.v("curent_lat_long", "lat:" + userCurrentLocation.getLatitude() + ",longitute:" + userCurrentLocation.getLongitude());
            mMap.clear();
            Geocoder geocoder = new Geocoder(this);
            String userAddress = "";
            try {
                List<Address> addressList = geocoder.getFromLocation(userCurrentLocation.getLatitude(), userCurrentLocation.getLongitude(), 5);
                Address address = addressList.get(0);
                userAddress = address.getLocality() + "," + address.getAdminArea() + "," + address.getCountryName() + "," + address.getPostalCode();

            } catch (IOException e) {
                e.printStackTrace();
                LatLng myLocation = new LatLng(userCurrentLocation.getLatitude(), userCurrentLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(myLocation).title("" + userAddress));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
            }
        }
    }
}