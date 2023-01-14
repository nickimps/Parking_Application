package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class InfoActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        EditText getName = (EditText) findViewById(R.id.nameInsertText);
        EditText getPermit = (EditText) findViewById(R.id.permitInsertText);

        Button updateButton = findViewById(R.id.goBackButton);
        //button.setOnClickListener(view -> startActivity(new Intent(InfoActivity.this, MapsActivity.class)));
        updateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = getName.getText().toString();
                String permit = getPermit.getText().toString();

                //TODO: Make hint get information from the database.
                // Add a exit button as well as a save (move save button below textfields and turn save into exit)
                //  With that we may need a pop up to say 'discarding changes' (easier than a confirmation popup imo)
            }
        }


        );




        }
    }