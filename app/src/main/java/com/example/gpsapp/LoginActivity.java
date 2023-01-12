package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;
    EditText editUsername, editPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText getUsername = (EditText) findViewById(R.id.usernameInsertText);
        EditText getPassword = (EditText) findViewById(R.id.passwordInsertText);
        TextView register = (TextView) findViewById(R.id.register);


        //Add constraints
        Button button = findViewById(R.id.loginButton);
        //button.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, MapsActivity.class)));
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              String username = getUsername.getText().toString();
              String password = getPassword.getText().toString();
//              System.out.println(username);
//              System.out.println(password);

                //Database
                firestore = FirebaseFirestore.getInstance();

                firestore.collection("Users")
                        .whereEqualTo("username", username)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    boolean login = false;
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                        login = true;
                                    }
                                    if(login){
                                        Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                                        startActivity((intent));
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
                            }
                        });



//              if(docRefPass.equals(password)){
//                  Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//                  startActivity((intent));
//              }

//                Map<String, Object> user = new HashMap<>();
//                user.put("username", username);
//                user.put("password", password);
//
//                firestore.collection("Users").add(user);
//                firestore.collection("Users").add(user);

//              Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//              startActivity((intent));
            }
        });


    }
}