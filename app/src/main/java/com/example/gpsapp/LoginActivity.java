package com.example.gpsapp;

import static android.content.ContentValues.TAG;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;



public class LoginActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_CODE = 0;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 2;
    FirebaseFirestore firestore;
    private TextInputEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameLayout, passwordLayout;
    //Fetch the stored information when the app is loaded, this function is called when the app is opened again

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Get the stored information within the shared preference
            SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);

            // Get stored username and password for the logged in user
            String username = sharedPref.getString("username", null);
            String password = sharedPref.getString("password", null);

            // Bypass login if login details exist
            if (username != null && password != null) {
                // Database instance
                firestore = FirebaseFirestore.getInstance();

                // Perform query to get the user that has matching username and password so that we can auto login
                firestore.collection("Users")
                        .whereEqualTo("username", username)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    usernameEditText.setText(username);
                                    passwordEditText.setText(password);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        });
            }
        }
    }

    // Saves the information when the app is closed, this function runs when the app is closed.
    @Override
    protected void onPause() {
        super.onPause();

        // Create the shared preference to store the information
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        // Create an editor that allows us to impute information into the shared preference
        SharedPreferences.Editor editor = sharedPref.edit();

        // Only save the text fields if there is information in them.
        if (!usernameEditText.getText().toString().isEmpty() && !passwordEditText.getText().toString().isEmpty()) {
            // Store information from the text fields
            editor.putString("username", usernameEditText.getText().toString().trim());
            editor.putString("password", passwordEditText.getText().toString().trim());

            // Commit the changes to the editor
            editor.apply();
        }
    }

    //Runs when the app is created
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Have user accept location permissions
        getAllPermissions();

        // REGISTER CLICKABLE TEXT
        createClickableRegisterText();

        // Store the ids of the edit text fields
        usernameEditText = findViewById(R.id.usernameTextInputEditText);
        passwordEditText = findViewById(R.id.passwordTextInputEditText);
        usernameLayout = findViewById(R.id.usernameTextInputLayout);
        passwordLayout = findViewById(R.id.passwordTextInputLayout);

        findViewById(R.id.loginButton).setEnabled(false);

        // Create listeners to remove error message on text fields
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    usernameLayout.setError("Required");
                    findViewById(R.id.loginButton).setEnabled(false);
                } else {
                    usernameLayout.setError(null);

                    // Enable or disable the button depending on if the password field has text
                    findViewById(R.id.loginButton).setEnabled(!passwordEditText.getText().toString().isEmpty());
                }
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    passwordLayout.setError("Required");
                    findViewById(R.id.loginButton).setEnabled(false);
                } else {
                    passwordLayout.setError(null);

                    // Disable or enable the login button if user name has no text
                    findViewById(R.id.loginButton).setEnabled(!usernameEditText.getText().toString().isEmpty());
                }
            }
        });

        //Enable enter button to auto login on phone
        passwordEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                login();
                return true;
            }
            return false;
        });

        // LOGIN BUTTON
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> login());
    }

    private void getAllPermissions() {
        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fine location has been accepted by user

            // Need this for certain versions of android apparently
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    askForBackgroundPermission();
        }
        else {
            // Fine Location Permission is not granted so ask for permission
            tellWhyWeNeedIt();
        }
    }

    private void tellWhyWeNeedIt() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Location Permission Needed!")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(LoginActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE))
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        // Permission is denied
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            // User granted location permission
            // Now check if android version >= 11, if >= 11 check for Background Location Permission
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        askForBackgroundPermission(); // Ask for Background Location Permission if background location not allowed yet
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void askForBackgroundPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Background Location Permission Needed!, please tap \"Allow all the time\" in the next screen")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(LoginActivity.this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE))
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        // User declined for Background Location Permission.
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
        }
    }

    public void login() {
        // Get the text from the text fields
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Database Instance
        firestore = FirebaseFirestore.getInstance();

        // Perform query to get the user that has matching username and password so that we can log in the user
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            findViewById(R.id.loginButton).setEnabled(true);
                            // Send the user to the maps activity
                            Intent intentToMap = new Intent(LoginActivity.this, MapsActivity.class);
                            startActivity((intentToMap));
                            findViewById(R.id.loginButton).setEnabled(false);
                        } else if (username.isEmpty() && password.isEmpty()) {
                            usernameLayout.setError("Required");
                            passwordLayout.setError("Required");
                        } else if (username.isEmpty()) {
                            usernameLayout.setError("Required");
                        } else if (password.isEmpty()) {
                            passwordLayout.setError("Required");
                        } else {
                            usernameLayout.setError(" ");
                            passwordLayout.setError("Username or password are incorrect");
                        }

                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    /**
     * Create a clickable textview to bring the user to the register screen
     */
    public void createClickableRegisterText() {

        // Get id of textview and save its default string
        TextView textView = findViewById(R.id.registerTextView);
        String text = "Don't have an account? Register here";

        // Create spannable string
        SpannableString ss = new SpannableString(text);

        // Create listener for the onClick
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(ds.linkColor);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
            @Override
            public void onClick(@NonNull View view) {
                // Go to register screen on click
                Intent intent = new Intent(LoginActivity.this, eulaActivity.class);
                startActivity((intent));
            }
        };

        // Set which part is clickable (just the register here part)
        ss.setSpan(clickableSpan, 23,36, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the textview to the new clickable text
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}