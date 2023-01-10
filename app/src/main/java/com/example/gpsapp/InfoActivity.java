package com.example.gpsapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Button button = findViewById(R.id.goBackButton);
        button.setOnClickListener(view -> startActivity(new Intent(InfoActivity.this, MapsActivity.class)));

        }
    }