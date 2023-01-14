package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class InfoActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        EditText getName = (EditText) findViewById(R.id.nameInsertText);
        EditText getPermit = (EditText) findViewById(R.id.permitInsertText);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Changes not saved!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(InfoActivity.this, MapsActivity.class);
                startActivity((intent));

                //TODO: Make hint get information from the database.
                // Add a exit button as well as a save (move save button below textfields and turn save into exit)
                //  With that we may need a pop up to say 'discarding changes' (easier than a confirmation popup imo)
            }
        });

        Button updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = getName.getText().toString();
                String permit = getPermit.getText().toString();

//                //Database
//                firestore = FirebaseFirestore.getInstance();
//                firestore.collection("Users")
//                        .whereEqualTo("username", name)
//                        .whereEqualTo("password", permit)
//                        .get()
//                        .addOnCompleteListener(task -> {
//                            if (task.isSuccessful()) {
//                                if (!task.getResult().isEmpty()) {
//                                    for (QueryDocumentSnapshot document : task.getResult()) {
//                                        //Update the lastDeviceID
//                                        firestore.collection("Users")
//                                                .document(document.getId())
//                                                .update("lastDeviceID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
//                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully updated!"))
//                                                .addOnFailureListener(e -> Log.w(TAG, "Error updating document", e));
//
//                                        Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//                                        startActivity((intent));
//                                        break;
//                                    }
//                                } else if (username.isEmpty() || password.isEmpty()) {
//                                    Toast.makeText(getApplicationContext(), "Both username & password must be entered!", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    Toast.makeText(getApplicationContext(), "Username or password invalid!", Toast.LENGTH_SHORT).show();
//                                }
//
//                            } else {
//                                Log.d(TAG, "Error getting documents: ", task.getException());
//                            }
//                        });
//
//            }
//        });



        }
    }