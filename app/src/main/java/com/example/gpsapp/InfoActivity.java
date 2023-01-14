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

                firestore = FirebaseFirestore.getInstance();

//                firestore.collection("Users")
//                        .whereEqualTo("username", username)//Use device id instead
//                        .get()
//                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                if (task.isSuccessful()) {
//                                    boolean register = true;
//                                    for (QueryDocumentSnapshot document : task.getResult()) {
//                                        register = false;
//                                    }
//                                    if(register){
//                                        Map<String, Object> user = new HashMap<>();
//                                        user.put("username", username);
//                                        user.put("password", password);
//                                        user.put("name", name);
//                                        user.put("permit", permit);
//                                        firestore.collection("Users").add(user);
//                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
//                                        startActivity((intent));
//                                        Toast.makeText(getApplicationContext(),"Account successfully created!",Toast.LENGTH_SHORT).show();
//                                    }
//                                    else if(username.isEmpty() || password.isEmpty() || name.isEmpty()){
//                                        Toast.makeText(getApplicationContext(),"Username, password and name must be all be filled!",Toast.LENGTH_SHORT).show();
//                                    }
//                                    else{
//                                        Toast.makeText(getApplicationContext(),"This username already exists!",Toast.LENGTH_SHORT).show();
//                                    }
//
//                                } else {
//                                    Log.d(TAG, "Error getting documents: ", task.getException());
//                                }
//                            }
//                        });
            }
        }


        );




        }
    }