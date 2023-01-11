package com.example.gpsapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



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
        button.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, MapsActivity.class)));
    }
}