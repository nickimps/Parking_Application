package com.example.gpsapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Add constraints
        Button button = findViewById(R.id.loginButton);
        button.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, MapsActivity.class)));
    }
}