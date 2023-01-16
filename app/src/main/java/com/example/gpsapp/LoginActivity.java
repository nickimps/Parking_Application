package com.example.gpsapp;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;
    private EditText getUsername, getPassword;

    //Fetch the stored information when the app is loaded, this function is called when the app is opened again
    @Override
    protected void onResume() {
        super.onResume();

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
                                Toast.makeText(getApplicationContext(), "Logging in...", Toast.LENGTH_SHORT).show();

                                getUsername.setText(username);
                                getPassword.setText(password);

                                // Send the user to the maps activity
                                Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                                startActivity((intent));
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
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
        if (!getUsername.getText().toString().isEmpty() && !getPassword.getText().toString().isEmpty()) {
            // Store information from the text fields
            editor.putString("username", getUsername.getText().toString());
            editor.putString("password", getPassword.getText().toString());

            // Commit the changes to the editor
            editor.apply();
        }
    }

    //Runs when the app is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Store the ids of the edit text fields
        getUsername = findViewById(R.id.usernameInsertText);
        getPassword = findViewById(R.id.passwordInsertText);

        // LOGIN BUTTON
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> {
            // Get the text from the text fields
            String username = getUsername.getText().toString();
            String password = getPassword.getText().toString();

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
                                // Send the user to the maps activity
                                Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                                startActivity((intent));
                            } else if (username.isEmpty() || password.isEmpty()) {
                                Toast.makeText(getApplicationContext(), "Both username & password must be entered!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Username or password invalid!", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        });

        // REGISTER BUTTON
        Button regButton = findViewById(R.id.goToRegisterButton);
        regButton.setOnClickListener(v -> {
            // Send the user to the register screen.
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity((intent));
        });
    }
}