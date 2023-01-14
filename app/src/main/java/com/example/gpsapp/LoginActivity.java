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

    //Fetch the stored information when the app is loaded
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);

        // Get stored username and password to log in user
        String username = sharedPref.getString("username", null);
        String password = sharedPref.getString("password", null);

        //Bypass login if login details exist
        if (username != null && password != null) {
            Toast.makeText(getApplicationContext(), "Logging in...", Toast.LENGTH_SHORT).show();

            firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                                startActivity((intent));
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Failed to auto-login", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        }
    }

    //Saves the information when the app is closed
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        //Only save the text fields if there is information in them.
        if (!getUsername.getText().toString().isEmpty() && !getPassword.getText().toString().isEmpty()) {
            //Store information
            editor.putString("username", getUsername.getText().toString());
            editor.putString("password", getPassword.getText().toString());
            editor.apply();
        }
    }

    //Runs on creation of the app
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        getUsername = findViewById(R.id.usernameInsertText);
        getPassword = findViewById(R.id.passwordInsertText);

        //Login Button Pressed
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> {
            String username = getUsername.getText().toString();
            String password = getPassword.getText().toString();

            //Database
            firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
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

        //Register Button Pressed
        Button regButton = findViewById(R.id.goToRegisterButton);
        regButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity((intent));
        });

    }
}