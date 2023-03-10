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
import android.text.method.ScrollingMovementMethod;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity implements LocationListener {

    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    TextView locationTextView, consoleTextView;
    Button refreshButton;
    LocationManager mLocationManager;
    TextInputEditText filenameEditText;
    private boolean tracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        getSupportActionBar().setTitle("Admin Portal");

        // Get the ids to access them
        locationTextView = findViewById(R.id.locationTextView);
        filenameEditText = findViewById(R.id.filenameTextInputEditText);
        consoleTextView = findViewById(R.id.consoleTextView);

        // Start with date there
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.CANADA);
        consoleTextView.setText("Start: " + sdf.format(new Date()));
        consoleTextView.setMovementMethod(new ScrollingMovementMethod());

        // Have location auto load when loading screen
        getLocation();

        // Enable location to be tracked and populate textfield with location
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> getLocation());

        Button saveButton = findViewById(R.id.saveButton);
        Button startTrackingButton = findViewById(R.id.startTrackingButton);

        // SAVE BUTTON
        saveButton.setOnClickListener(v -> {
            saveLocationToFile(consoleTextView.getText().toString(), filenameEditText.getText().toString().trim());
            filenameEditText.setText(null);
            filenameEditText.clearFocus();
            saveButton.setEnabled(false);

            // Stop Tracking
            startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_green));
            startTrackingButton.setText("Start Tracking");
            tracking = false;

            // Clear Screen
            consoleTextView.setText("Start: " + sdf.format(new Date()));
            consoleTextView.setMovementMethod(new ScrollingMovementMethod());
        });

        // START TRACKING BUTTON
        startTrackingButton.setOnClickListener(v -> {
            if (tracking) {
                startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_green));
                startTrackingButton.setText("Start Tracking");
                tracking = false;
            } else {
                startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_red));
                startTrackingButton.setText("Stop Tracking");
                tracking = true;
            }
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
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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
     * This function will change the speed label on the admin card view to the current speed in
     * real-time.
     *
     * @param location The location parameter
     * @return The speed to be set in the TextView
     */
    private float updateSpeedTextView(Location location) {
        if (location.hasSpeed())
            return location.getSpeed();
        else
            return 0.0f;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update the speed on the card view on the screen
        float speed = updateSpeedTextView(location);
        String speedString = String.format(Locale.CANADA, "%.6f m/s", speed);
        String movingStatus = "Stopped";

        if (speed <= 0.05) {
            //movingStatus = checkStop(location);
            movingStatus = "Not Moving";
        } else if (speed > 0.05 && speed <= 2) {
            movingStatus = "Walking";
        } else if (speed > 2) {
            movingStatus = "Driving";
        }

        if (tracking) {
            // Get the time
            SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss z", Locale.CANADA);
            String time = sdf2.format(new Date());

            // Append the speed, status and time to the output
            String textToAppend = consoleTextView.getText() + "\n" + time + " -- " + speedString + " -- " + movingStatus;
            consoleTextView.setText(textToAppend);

            // Keep scrolled to the latest appended text
            int scrollAmount = consoleTextView.getLayout().getLineTop(consoleTextView.getLineCount()) - consoleTextView.getHeight();
            consoleTextView.scrollTo(0, Math.max(scrollAmount, 0));
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);

            // Get GPS coordinates of last location to update the text view
            Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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