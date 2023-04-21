package com.parking.linkandpark;

import static com.parking.linkandpark.MapsActivity.firestore;
import static com.parking.linkandpark.LoginActivity.mAuth;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;

public class InfoActivity extends AppCompatActivity {

    private static final String TAG = "InfoActivity";
    Boolean isAdmin = false, permitChanged = false;
    TextInputEditText nameEditText, permitEditText;
    TextInputLayout nameLayout, permitLayout;

    /**
     * Default onCreate to initialize everything we need
     *
     * @param savedInstanceState The instance state if it was saved
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Change the title of the action bar
        Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");

        // Get the text fields ids
        nameEditText = findViewById(R.id.infoNameTextInputEditText);
        permitEditText = findViewById(R.id.infoPermitTextInputEditText);
        nameLayout = findViewById(R.id.infoNameTextInputLayout);
        permitLayout = findViewById(R.id.infoPermitTextInputLayout);

        // Listener for the name textfield to enable or disable the update button if there is no
        // text in the textfield
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1)
                    findViewById(R.id.updateButton).setEnabled(!Objects.requireNonNull(permitEditText.getText()).toString().isEmpty());
                else
                    findViewById(R.id.updateButton).setEnabled(true);
            }
        });

        // Listener to enable or disable the update button if there was text or not
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

                                // Get the permit number field if there is one, if not then
                                // we want to leave it as an informative message
                                assert permitNum != null;
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
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the user is in the list, then we update the bool
                        for (QueryDocumentSnapshot document : task.getResult())
                            isAdmin = document.getBoolean("isAdmin");

                        // If the user was admin, we want to change somethings otherwise we don't
                        if (Boolean.TRUE.equals(isAdmin))
                            findViewById(R.id.adminScreenButton).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.adminScreenButton).setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });

        // LOGOUT BUTTON
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            //Remove user information from local shared preferences
            SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefBack.edit();
            editor.putString("username", null);
            editor.putString("password", null);
            editor.apply();

            // Sign out user
            mAuth.signOut();

            // Send the user to the login screen.
            Intent intent = new Intent(InfoActivity.this, LoginActivity.class);
            startActivity((intent));
            finish();
        });


        // UPDATE BUTTON
        Button updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(view -> firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // If the user has entered something in the text field then add it to the update,
                            // otherwise just keep what was already there in the database
                            String name, permit;
                            if (Objects.requireNonNull(nameEditText.getText()).toString().isEmpty())
                                name = document.getString("name");
                            else
                                name = nameEditText.getText().toString().trim();

                            // Update the permit number if we need to
                            if (Objects.requireNonNull(permitEditText.getText()).toString().isEmpty()) {
                                String savedPermitNumber = document.getString("permit");
                                if (permitChanged) {
                                    permit = "Permit Number (Optional)";
                                    permitChanged = false;
                                } else
                                    permit = savedPermitNumber;
                            } else
                                permit = permitEditText.getText().toString().trim();

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