package com.example.gpsapp;

import static android.content.ContentValues.TAG;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;


public class RegisterActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    private TextInputEditText usernameEditText, passwordEditText, nameEditText, permitEditText;
    public static TextInputLayout usernameLayout, passwordLayout, nameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViewById(R.id.registerButton).setEnabled(false);

        // Save the text field and layout ids
        usernameEditText = findViewById(R.id.regUsernameTextInputEditText);
        passwordEditText = findViewById(R.id.regPasswordTextInputEditText);
        nameEditText = findViewById(R.id.regNameTextInputEditText);
        permitEditText = findViewById(R.id.regPermitTextInputEditText);
        usernameLayout = findViewById(R.id.regUsernameTextInputLayout);
        passwordLayout = findViewById(R.id.regPasswordTextInputLayout);
        nameLayout = findViewById(R.id.regNameTextInputLayout);

        // LOGIN CLICKABLE TEXT
        createClickableLoginText();

        // Create listeners to remove error message on text fields
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    usernameLayout.setError("Required");
                    findViewById(R.id.registerButton).setEnabled(false);
                } else {
                    usernameLayout.setError(null);
                    if (passwordEditText.getText().toString().isEmpty() || nameEditText.getText().toString().isEmpty())
                        findViewById(R.id.registerButton).setEnabled(false);
                    else
                        findViewById(R.id.registerButton).setEnabled(true);
                }
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    passwordLayout.setError("Required");
                    findViewById(R.id.registerButton).setEnabled(false);
                } else {
                    passwordLayout.setError(null);
                    if (usernameEditText.getText().toString().isEmpty() || nameEditText.getText().toString().isEmpty())
                        findViewById(R.id.registerButton).setEnabled(false);
                    else
                        findViewById(R.id.registerButton).setEnabled(true);
                }
            }
        });

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    nameLayout.setError("Required");
                    findViewById(R.id.registerButton).setEnabled(false);
                } else {
                    nameLayout.setError(null);
                    if (usernameEditText.getText().toString().isEmpty() || passwordEditText.getText().toString().isEmpty())
                        findViewById(R.id.registerButton).setEnabled(false);
                    else
                        findViewById(R.id.registerButton).setEnabled(true);
                }
            }
        });

        // Get the register button and add an onClick listener
        Button register = findViewById(R.id.registerButton);
        register.setOnClickListener(view -> {
            //Get the information within the text fields
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();
            String permit = permitEditText.getText().toString().trim();

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
                                // Create new user object, with the user's information, then add to database (default admin privileges are false)
                                User user = new User(username, password, name, permit, false);
                                firestore.collection("Users").add(user);

                                //Add user information to local shared preferences
                                SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPrefBack.edit();
                                editor.putString("username", username);
                                editor.putString("password", password);
                                editor.apply();

                                //Change the activity to the maps activity screen
                                Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
                                startActivity((intent));

                                //Create message to let user know the creation was successful
                                Toast.makeText(getApplicationContext(),"Account successfully created!",Toast.LENGTH_SHORT).show();
                            } else if (!task.getResult().isEmpty()) {
                                usernameLayout.setError("Username is taken");
                            } else if(username.isEmpty() && password.isEmpty() && name.isEmpty()) {
                                usernameLayout.setError("Required");
                                passwordLayout.setError("Required");
                                nameLayout.setError("Required");
                            } else if(username.isEmpty() && password.isEmpty()) {
                                usernameLayout.setError("Required");
                                passwordLayout.setError("Required");
                            } else if(username.isEmpty() && name.isEmpty()) {
                                usernameLayout.setError("Required");
                                nameLayout.setError("Required");
                            } else if(password.isEmpty() && name.isEmpty()) {
                                passwordLayout.setError("Required");
                                nameLayout.setError("Required");
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        });
    }

    /**
     * Create a clickable textview to bring the user to the register screen
     */
    public void createClickableLoginText() {

        // Get id of textview and save its default string
        TextView textView = findViewById(R.id.registerTextView);
        String text = "Already have an account? Login here";

        // Create spannable string
        SpannableString ss = new SpannableString(text);

        // Create listener for the onClick
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(ds.linkColor);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
            @Override
            public void onClick(@NonNull View view) {
                // Change the activity to the login screen
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity((intent));
            }
        };

        // Set which part is clickable (just the register here part)
        ss.setSpan(clickableSpan, 25,35, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the textview to the new clickable text
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}