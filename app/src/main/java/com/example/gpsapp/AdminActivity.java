package com.example.gpsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity implements LocationListener {

    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    TextView locationTextView, speedTextView, isStoppedTextView;
    Button refreshButton;
    LocationManager locationManager;
    TextInputEditText filenameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        getSupportActionBar().setTitle("Admin Portal");

        // Get the ids to access them
        locationTextView = findViewById(R.id.locationTextView);
        filenameEditText = findViewById(R.id.filenameTextInputEditText);
        speedTextView = findViewById(R.id.speedTextView);
        isStoppedTextView = findViewById(R.id.isStoppedTextView);

        // Have location auto load when loading screen
        getLocation();

        // Enable location to be tracked and populate textfield with location
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> getLocation());

        // SAVE BUTTON
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            saveLocationToFile(locationTextView.getText().toString(), filenameEditText.getText().toString().trim());
            filenameEditText.setText(null);
            filenameEditText.clearFocus();
            saveButton.setEnabled(false);
        });

        filenameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    if (locationTextView.getText().toString().isEmpty())
                        saveButton.setEnabled(false);
                    else
                        saveButton.setEnabled(true);
                } else
                    saveButton.setEnabled(true);
            }
        });
    }

    /**
     * Function to get permissions and then provide an alert if they are not set
     * If permissions are good, then we proceed to getGPSData()
     */
    public void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Need permissions to be good
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("No", (dialog, which) -> dialog.cancel());
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            // Get Location
            getGPSData();
        }
    }

    /**
     * Function runs whenever the location of the user changes
     *
     * @param location the location of the user
     */
    @Override
    public void onLocationChanged(Location location) {
        float speed = 0.0f;
        if (location.hasSpeed()) {
            speed = location.getSpeed();
        } else {
            speed = 0.0f;
        }

        //String speedString = "Current Speed:\n" + speed + " m/s";
        String speedString = String.format(Locale.CANADA, "Current Speed:\n%.9f m/s", speed);
        speedTextView.setText(speedString);

        String movingString = "Moving";
        if (speed == 0)
            movingString = "Stopped"; // would be nice to say parked here if in parking spot

        isStoppedTextView.setText(movingString);
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


    /**
     * Populates the textfield with the location
     * Also starts the .requestLocationUpdates for the onLocationChanged function
     */
    @SuppressLint("SetTextI18n")
    private void getGPSData() {
        if (ActivityCompat.checkSelfPermission(AdminActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(AdminActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // Start the listener to manage location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);

            // Get GPS coordinates of last location to update the text view
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                String locationString = "Latitude: " + locationGPS.getLatitude() + "\nLongitude: " + locationGPS.getLongitude();
                locationTextView.setText(locationString);
            } else
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves a file for the data
     *
     * @param data The data to be saved to the file
     */
    private void saveLocationToFile(String data, String filename) {
        try {
            /*    Saves to: /data/data/com.example.gpsapp/files/Location_Data/    */

            // Creates directory to store data into called Location_Data
            File file = new File(AdminActivity.this.getFilesDir(), "Location_Data");
            if (!file.exists()) {
                file.mkdir();
            }

            // Create the file to be saved with the timestamp as the file name
            File fileToSave = new File(file, filename);

            // Perform saving operations
            FileWriter writer = new FileWriter(fileToSave);
            writer.append(data);
            writer.flush();
            writer.close();

            // Success Message
            Toast.makeText(AdminActivity.this, "Location File Saved!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e);
            Toast.makeText(AdminActivity.this, "Failed to save.", Toast.LENGTH_SHORT).show();
        }
    }
}