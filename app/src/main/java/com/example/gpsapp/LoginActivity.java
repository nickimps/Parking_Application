package com.example.gpsapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText getUsername = findViewById(R.id.usernameInsertText);
        EditText getPassword = findViewById(R.id.passwordInsertText);


        //Add constraints
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> {
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
                    .addOnCompleteListener(task -> {
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
                    });
        });

        Button regButton = findViewById(R.id.goToRegisterButton);
        regButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity((intent));
        });

    }
}