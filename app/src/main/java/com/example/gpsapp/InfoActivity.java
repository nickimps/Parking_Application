package com.example.gpsapp;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    Boolean isAdmin = false;

    TextInputEditText nameEditText, permitEditText;
    TextInputLayout nameLayout, permitLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

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
                    nameLayout.setError("Required");
                    if (permitEditText.getText().toString().isEmpty())
                        findViewById(R.id.updateButton).setEnabled(false);
                    else
                        findViewById(R.id.updateButton).setEnabled(true);
                } else {
                    nameLayout.setError(null);
                        findViewById(R.id.updateButton).setEnabled(true);
                }
            }
        });

        permitEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    permitLayout.setError("Required");
                    if (nameEditText.getText().toString().isEmpty())
                        findViewById(R.id.updateButton).setEnabled(false);
                    else
                        findViewById(R.id.updateButton).setEnabled(true);
                } else {
                    permitLayout.setError(null);
                    findViewById(R.id.updateButton).setEnabled(true);
                }
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
                            // Only update if fields have information in them, otherwise let user know there is no information there
                            if (nameEditText.getText().toString().isEmpty() && permitEditText.getText().toString().isEmpty()) {
                                Toast.makeText(getApplicationContext(), "Please enter either a name or permit number", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                //If the user has entered something in the text field then add it to the update,
                                // otherwise just keep what was already there in the database
                                String name, permit;
                                if (nameEditText.getText().toString().isEmpty()) {
                                    name = document.getString("name");
                                } else {
                                    name = nameEditText.getText().toString().trim();
                                }

                                if (permitEditText.getText().toString().isEmpty()) {
                                    permit = document.getString("permit");
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
                            }
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }));
        }
    }