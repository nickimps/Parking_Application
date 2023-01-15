package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
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

        // Save the text field ids
        EditText getUsername = findViewById(R.id.registerUsernameText);
        EditText getPassword = findViewById(R.id.registerPasswordText);
        EditText getName = findViewById(R.id.registerNameText);
        EditText getPermit = findViewById(R.id.registerPermitText);

        // Get the register button and add an onClick listener
        Button register = findViewById(R.id.registerButton);
        register.setOnClickListener(view -> {
            //Get the information within the text fields
            String username = getUsername.getText().toString();
            String password = getPassword.getText().toString();
            String name = getName.getText().toString();
            String permit = getPermit.getText().toString();

            //Database instance
            firestore = FirebaseFirestore.getInstance();

            //Perform query to get the user by username lookup
            firestore.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // If the user exists and the fields were not empty, then add the user
                            if(task.getResult().isEmpty() && !username.isEmpty() && !password.isEmpty() && !name.isEmpty()) {
                                // Create new user object, with the user's information, then add to database
                                User user = new User(username, password, name, permit);
                                firestore.collection("Users").add(user);

                                //Change the activity to the maps activity screen
                                Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
                                startActivity((intent));

                                //Create message to let user know the creation was successful
                                Toast.makeText(getApplicationContext(),"Account successfully created!",Toast.LENGTH_SHORT).show();
                            }
                            else if(username.isEmpty() || password.isEmpty() || name.isEmpty()){
                                Toast.makeText(getApplicationContext(),"Username, password and name must be all be filled",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"The username already exists",Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        });

        //BACK BUTTON
        Button goBack = findViewById(R.id.backToLogin);
        goBack.setOnClickListener(v -> {
            // Change the activity to the login screen
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity((intent));
        });

    }
}