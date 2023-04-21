package com.parking.linkandpark;

import static com.parking.linkandpark.MapsLocationService.CHANNEL_ID;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

/**
 * Displays rationale for allowing the activity recognition permission and allows user to accept
 * the permission. After permission is accepted, finishes the activity so main activity can
 * show transitions.
 */
public class PermissionRationalActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "PermissionRational";
    private static final int PERMISSIONS_ID = 6;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 2;


    /**
     * onCreate check the permissions and continue on or ask for permissions
     *
     * @param savedInstanceState The state to be loaeded if needed
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int perms = 0;

        // If permissions granted, we start the main activity (shut this activity down).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            perms++;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            perms++;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
            perms++;

        // If we have all the permissions we need, we can not load this activity
        if (perms == 3)
            finish();

        Log.v(TAG, perms + " onCreate of perms");

        // Change the view the user sees
        setContentView(R.layout.activity_permission_rational);
    }

    /**
     * If we click the approve permissions button then lets ask for the permissions
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void onClickApprovePermissionRequest(View view) {
        Log.d(TAG, "onClickApprovePermissionRequest()");

        // Ask for location and activity permissions
        ActivityCompat.requestPermissions(PermissionRationalActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_ID);

        // Create notification channels
        createNotificationChannel();
    }

    /**
     * We also need background permissions, but this has to be separated otherwise the prompt is
     * not shown to the user.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void askForBackgroundPermission() {
        // If we have location permission, then we can ask for background permission
        if (ActivityCompat.shouldShowRequestPermissionRationale(PermissionRationalActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Background Location Permission Needed!, please tap \"Allow all the time\" in the next screen")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(PermissionRationalActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE))
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        // User declined for Background Location Permission.
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
        }
    }

    /**
     * Close the activity if they deny giving permissions
     *
     * @param view The view of the view
     */
    public void onClickDenyPermissionRequest(View view) {
        Log.d(TAG, "onClickDenyPermissionRequest()");
        finish();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Log the request information
        String permissionResult = "Request code: " + requestCode + ", Permissions: " + Arrays.toString(permissions) + ", Results: " + Arrays.toString(grantResults);
        Log.d(TAG, "onRequestPermissionsResult(): " + permissionResult);

        // If the user just accepted the bulk permissions, then ask for background
        if (requestCode == PERMISSIONS_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                askForBackgroundPermission();

        // If the user accepted background, then we can continue on
        if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && activityRecognitionPermissionApproved()) {
                    startActivity(new Intent(this, MapsActivity.class));
                    finish();
                } else {
                    ActivityCompat.requestPermissions(PermissionRationalActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_ID);

                    createNotificationChannel();
                }
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, MapsActivity.class));
                finish();
            }
        }
    }

    /**
     * Function to ask for all three permissions
     *
     * @return a permission prompt
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean activityRecognitionPermissionApproved() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Creates the notification to show the user that we are tracking their location in the background
     */
    private void createNotificationChannel() {
        // Check the build version, some android versions do not need to do this, it is done automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Build the service channel with the following ID
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
