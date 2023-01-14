package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;


public class RegisterActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText getUsername = findViewById(R.id.registerUsernameText);
        EditText getPassword = findViewById(R.id.registerPasswordText);
        EditText getName = findViewById(R.id.registerNameText);
        EditText getPermit = findViewById(R.id.registerPermitText);

        Button register = findViewById(R.id.registerButton);
        register.setOnClickListener(view -> {
        String username = getUsername.getText().toString();
        String password = getPassword.getText().toString();
        String name = getName.getText().toString();
        String permit = getPermit.getText().toString();

        //Database
        firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if(task.getResult().isEmpty()) {
                            //Create new user object to add to database
                            User user = new User(username, password, name, permit, Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
                            firestore.collection("Users").add(user);
                            Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
                            startActivity((intent));
                            Toast.makeText(getApplicationContext(),"Account successfully created!",Toast.LENGTH_SHORT).show();
                        }
                        else if(username.isEmpty() || password.isEmpty() || name.isEmpty()){
                            Toast.makeText(getApplicationContext(),"Username, password and name must be all be filled!",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"This username already exists!",Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
        });

        Button goBack = findViewById(R.id.backToLogin);
        goBack.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity((intent));
        });

    }
}