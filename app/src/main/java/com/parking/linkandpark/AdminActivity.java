package com.parking.linkandpark;

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

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AdminActivity extends AppCompatActivity implements LocationListener {

    private static final long POLLING_SPEED = 500L;
    private static final float POLLING_DISTANCE = (float) 0.0001;
    private static final int REQUEST_LOCATION = 1;
    TextView locationTextView;
    @SuppressLint("StaticFieldLeak")
    public static TextView consoleTextView;
    Button refreshButton;
    LocationManager mLocationManager;
    TextInputEditText filenameEditText;
    public static boolean tracking = false;
    public static String saveData;
    private final SimpleDateFormat dayPlusTimeSDF = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.CANADA);

    /**
     * onCreate function that initializes a lot of the text fields and buttons that are used in this activity
     *
     * @param savedInstanceState The saved instance state to be loaded
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Change the title of the action bar at the top of the screen
        Objects.requireNonNull(getSupportActionBar()).setTitle("Admin Portal");

        // Get the ids to access them
        locationTextView = findViewById(R.id.locationTextView);
        filenameEditText = findViewById(R.id.filenameTextInputEditText);
        consoleTextView = findViewById(R.id.consoleTextView);
        consoleTextView.setMovementMethod(new ScrollingMovementMethod());

        // Have location auto load when loading screen
        getLocation();

        // Enable location to be tracked and populate textfield with location
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> getLocation());

        // Get button element IDs
        Button saveButton = findViewById(R.id.saveButton);
        Button startTrackingButton = findViewById(R.id.startTrackingButton);

        // Need to check if background services are active
        if (tracking) {
            startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_red));     // Tracking Started
            startTrackingButton.setText(R.string.stop_tracking);
        } else {
            // Start with date there
            saveData = "Start: " + dayPlusTimeSDF.format(new Date()) + "\n";
            consoleTextView.setText(saveData);
        }

        // SAVE BUTTON
        saveButton.setOnClickListener(v -> {
            // Save the information
            saveLocationToFile(saveData, Objects.requireNonNull(filenameEditText.getText()).toString().trim());
            filenameEditText.setText(null);
            filenameEditText.clearFocus();
            saveButton.setEnabled(false);

            // Stop Tracking
            startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_green));
            startTrackingButton.setText(R.string.start_tracking);
            tracking = false;

            // Stop tracking service
            Intent serviceIntent = new Intent(this, AdminLocationService.class);
            serviceIntent.setAction(AdminLocationService.ACTION_STOP_FOREGROUND_SERVICE);
            startService(new Intent(this, AdminLocationService.class).setAction(AdminLocationService.ACTION_STOP_FOREGROUND_SERVICE));

            // Clear Screen
            saveData = "Start: " + dayPlusTimeSDF.format(new Date()) + "\n";
            consoleTextView.setText(saveData);
            consoleTextView.setMovementMethod(new ScrollingMovementMethod());
        });

        // START TRACKING BUTTON
        startTrackingButton.setOnClickListener(v -> {
            if (tracking) {
                startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_green));   // Tracking Stopped
                startTrackingButton.setText(R.string.start_tracking);
                tracking = false;

                // Stop tracking service
                Intent serviceIntent = new Intent(this, AdminLocationService.class);
                serviceIntent.setAction(AdminLocationService.ACTION_STOP_FOREGROUND_SERVICE);
                startService(serviceIntent);
            } else {
                startTrackingButton.setBackgroundColor(getResources().getColor(R.color.start_red));     // Tracking Started
                startTrackingButton.setText(R.string.stop_tracking);
                tracking = true;

                // Start tracking service
                Intent serviceIntent = new Intent(this, AdminLocationService.class);
                serviceIntent.setAction(AdminLocationService.ACTION_START_FOREGROUND_SERVICE);
                startService(serviceIntent);
            }
        });

        // Listener so that the button enables or disables based on if there is text in the
        // textfield or not, error prevention
        filenameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                saveButton.setEnabled(!Objects.requireNonNull(filenameEditText.getText()).toString().isEmpty());
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

    @Override
    public void onLocationChanged(Location location) {
        // Called when the user's location changes
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
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_SPEED, POLLING_DISTANCE, this);

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
            /*    Saves to: /data/data/com.parking.linkandpark/files/Location_Data/    */

            // Creates directory to store data into called Location_Data
            File file = new File(AdminActivity.this.getFilesDir(), "Location_Data");

            boolean dirMade;
            if (!file.exists())
                dirMade = file.mkdir();
            else
                dirMade = true;

            if (dirMade) {
                // Create the file to be saved with the timestamp as the file name
                File fileToSave = new File(file, filename);

                // Perform saving operations
                FileWriter writer = new FileWriter(fileToSave);
                writer.append(data);
                writer.flush();
                writer.close();

                // Success Message
                Toast.makeText(AdminActivity.this, "Location File Saved!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e);
            Toast.makeText(AdminActivity.this, "Failed to save.", Toast.LENGTH_SHORT).show();
        }
    }
}