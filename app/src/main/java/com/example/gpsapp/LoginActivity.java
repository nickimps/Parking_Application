package com.example.gpsapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
//import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;
    EditText editUsername, editPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText getUsername = (EditText) findViewById(R.id.usernameInsertText);
        EditText getPassword = (EditText) findViewById(R.id.passwordInsertText);

        //Database
        firestore = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", "Mason");
        user.put("lastName", "Tommasini");
        user.put("Description", "AWESOME EPIC MODE HEHE");

        //firestore.collection("Users").add(user);
        firestore.collection("Users").document("Mason Info").set(user);

        //Add constraints
        Button button = findViewById(R.id.loginButton);
        //button.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, MapsActivity.class)));
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              String username = getUsername.getText().toString();
              String password = getPassword.getText().toString();
              System.out.println(username);
              System.out.println(password);
              Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
              startActivity((intent));
            }
        });


    }
}