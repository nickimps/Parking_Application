package com.example.gpsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button register = findViewById(R.id.registerButton);
        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                //Map<String, Object> user = new HashMap<>();
                //user.put("username", username);
                //user.put("password", password);

                //firestore.collection("Users").add(user);
               //firestore.collection("Users").add(user);

                Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
            }}
        );



        Button goBack = findViewById(R.id.backToLogin);
        goBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity((intent));
            }
        });

    }
}