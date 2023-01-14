package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Document;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Bypass login
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Users")
                .whereEqualTo("lastDeviceID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if(!task.getResult().isEmpty()) {
                            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                            startActivity((intent));
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });

        setContentView(R.layout.activity_login);

        EditText getUsername = findViewById(R.id.usernameInsertText);
        EditText getPassword = findViewById(R.id.passwordInsertText);

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
                            if(!task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    //Update the lastDeviceID
                                    firestore.collection("Users")
                                            .document(document.getId())
                                            .update("lastDeviceID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully updated!"))
                                            .addOnFailureListener(e -> Log.w(TAG, "Error updating document", e));

                                    Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                                    startActivity((intent));
                                    break;
                                }
                            }
                            else if(username.isEmpty() || password.isEmpty()){
                                Toast.makeText(getApplicationContext(),"Both username & password must be entered!",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"Username or password invalid!",Toast.LENGTH_SHORT).show();
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