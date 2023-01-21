package com.example.gpsapp;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class InfoActivity extends AppCompatActivity {

    FirebaseFirestore firestore;
    Boolean isAdmin = false, permitChanged = false;

    TextInputEditText nameEditText, permitEditText;
    TextInputLayout nameLayout, permitLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        getSupportActionBar().setTitle("Settings");

        // Get the text fields ids
        nameEditText = findViewById(R.id.infoNameTextInputEditText);
        permitEditText = findViewById(R.id.infoPermitTextInputEditText);
        nameLayout = findViewById(R.id.infoNameTextInputLayout);
        permitLayout = findViewById(R.id.infoPermitTextInputLayout);

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    if (permitEditText.getText().toString().isEmpty())
                        findViewById(R.id.updateButton).setEnabled(false);
                    else
                        findViewById(R.id.updateButton).setEnabled(true);
                } else {
                        findViewById(R.id.updateButton).setEnabled(true);
                }
            }
        });

        permitEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                findViewById(R.id.updateButton).setEnabled(true);
                permitChanged = true;
            }

            @Override
            public void afterTextChanged(Editable editable) {
                findViewById(R.id.updateButton).setEnabled(true);
                permitChanged = true;
            }
        });

        // Get shared preference to pull user information
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);

        // Get username of the user from the shared preference
        String username = sharedPref.getString("username", null);

        // Database instance
        firestore = FirebaseFirestore.getInstance();

        // Load the hint information from the user collection in the database
        if (username != null) {
            firestore.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Set the hint for both the text fields
                                nameLayout.setHint(document.getString("name"));
                                String permitNum = document.getString("permit");
                                if (permitNum.isEmpty())
                                    permitLayout.setHint("Permit Number (Optional)");
                                else
                                    permitLayout.setHint(document.getString("permit"));
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        }

        // Gets user's administrator access
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
                            findViewById(R.id.adminScreenButton).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.adminScreenButton).setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });

        // BACK BUTTON
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (!nameEditText.getText().toString().isEmpty() || !permitEditText.getText().toString().isEmpty()) {
                // Notify the user that the changes have not been saved if they leave with text in the text fields
                Toast.makeText(getApplicationContext(), "Changes not saved!", Toast.LENGTH_SHORT).show();
            }

            // Send the user back to the maps activity
            Intent intent = new Intent(InfoActivity.this, MapsActivity.class);
            startActivity((intent));
        });


        //Logout Button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            //Remove user information from local shared preferences
            SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefBack.edit();
            editor.putString("username", null);
            editor.putString("password", null);
            editor.apply();

            // Send the user to the login screen.
            Intent intent = new Intent(InfoActivity.this, LoginActivity.class);
            startActivity((intent));
        });


        // UPDATE BUTTON
        Button updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(view -> firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            //If the user has entered something in the text field then add it to the update,
                            // otherwise just keep what was already there in the database
                            String name, permit;
                            if (nameEditText.getText().toString().isEmpty()) {
                                name = document.getString("name");
                            } else {
                                name = nameEditText.getText().toString().trim();
                            }

                            if (permitEditText.getText().toString().isEmpty()) {
                                String savedPermitNumber = document.getString("permit");
                                if (permitChanged) {
                                    permit = "Permit Number (Optional)";
                                    permitChanged = false;
                                } else
                                    permit = savedPermitNumber;
                            } else {
                                permit = permitEditText.getText().toString().trim();
                            }

                            // Update the database for that user
                            firestore.collection("Users")
                                    .document(document.getId())
                                    .update("name", name, "permit", permit)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getApplicationContext(), "Profile Updated", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Error Updating Profile", Toast.LENGTH_SHORT).show());

                            // Clear fields and reset the hints
                            nameEditText.setText("");
                            permitEditText.setText("");
                            nameLayout.setHint(name);
                            permitLayout.setHint(permit);
                            nameEditText.clearFocus();
                            permitEditText.clearFocus();
                            updateButton.setEnabled(false);
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }));

        // ADMIN BUTTON
        Button adminButton = findViewById(R.id.adminScreenButton);
        adminButton.setOnClickListener(v -> {
            // Send the user to admin screen
            Intent intent = new Intent(InfoActivity.this, AdminActivity.class);
            startActivity((intent));
        });
        }
    }