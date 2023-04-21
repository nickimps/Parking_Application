package com.parking.linkandpark;

import static com.parking.linkandpark.MapsActivity.firestore;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameLayout, passwordLayout;
    public static FirebaseAuth mAuth;
    public static FirebaseUser mCurrentUser;
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";
    private static final String Key = "jnansdbhi1j23-0390fhia'p;qaenfpoa828";
    private static String encryptedPassword = "";

    // Saves the information when the app is closed, this function runs when the app is closed.
    @Override
    protected void onPause() {
        super.onPause();

        // Create the shared preference to store the information
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        // Create an editor that allows us to impute information into the shared preference
        SharedPreferences.Editor editor = sharedPref.edit();

        // Only save the text fields if there is information in them.
        if (!Objects.requireNonNull(usernameEditText.getText()).toString().isEmpty() && !Objects.requireNonNull(passwordEditText.getText()).toString().isEmpty()) {
            // Encrypt password before storing
            try {
                encryptedPassword = encrypt(passwordEditText.getText().toString().trim());
            } catch (Exception e) {
                Log.e(TAG, "encrypt password:error", e);
            }

            // Store information from the text fields
            editor.putString("username", usernameEditText.getText().toString().trim());
            editor.putString("password", encryptedPassword);

            // Commit the changes to the editor
            editor.apply();
        }
    }

    /**
     * Runs when the app is created
     *
     * @param savedInstanceState the instance state to be restored if needed
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Make the register text clickable and actually have functionality
        createClickableRegisterText();

        // Store the ids of the edit text fields
        usernameEditText = findViewById(R.id.usernameTextInputEditText);
        passwordEditText = findViewById(R.id.passwordTextInputEditText);
        usernameLayout = findViewById(R.id.usernameTextInputLayout);
        passwordLayout = findViewById(R.id.passwordTextInputLayout);

        // Initialize the login button to disabled
        findViewById(R.id.loginButton).setEnabled(false);

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
                    findViewById(R.id.loginButton).setEnabled(false);
                } else {
                    usernameLayout.setError(null);

                    // Enable or disable the button depending on if the password field has text
                    findViewById(R.id.loginButton).setEnabled(!Objects.requireNonNull(passwordEditText.getText()).toString().isEmpty());
                }
            }
        });

        // Listener for the password too
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < 1) {
                    passwordLayout.setError("Required");
                    findViewById(R.id.loginButton).setEnabled(false);
                } else {
                    passwordLayout.setError(null);

                    // Disable or enable the login button if user name has no text
                    findViewById(R.id.loginButton).setEnabled(!Objects.requireNonNull(usernameEditText.getText()).toString().isEmpty());
                }
            }
        });

        // Enable enter button to auto login on phone keyboard
        passwordEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                login();
                return true;
            }
            return false;
        });

        // LOGIN BUTTON - calls login function
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> login());

        // Ensure user can read from DB
        mCurrentUser = mAuth.getCurrentUser();

        // If auth user not signed in
        if(mCurrentUser == null){
            mAuth.signInAnonymously()
                    .addOnCompleteListener(task1 -> {
                        if(task1.isSuccessful())
                            Log.d(TAG, "Signed in anonymously");
                        else
                            Log.e(TAG, "Anonymous sign in failed", task1.getException());
                    });
        }
    }

    //Code Inspiration: https://github.com/Everyday-Programmer/Encryption-Decryption-Android/blob/main/app/src/main/java/com/example/encryptiondecryption/EncryptDecrypt.java
    /**
     * Create Encryption function
     *
     * @param value the value to be encrypted
     * @return encrypted password
     */
    public static String encrypt(String value) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(Key.getBytes(), ALGORITHM);             // specifies algorithm using set key
        Cipher cipher = Cipher.getInstance(MODE);                                               // specifies the set mode of blowfish
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));    // sets it to encryption mode using initialization vector IV
        byte[] values = cipher.doFinal(value.getBytes());                                       // value converted into byte array
        return Base64.encodeToString(values, Base64.DEFAULT);                                   // byte array is then encrypted and returned
    }

    /**
     * Logs in user by getting username and password and querying database
     */
    public void login() {
        // Get the text from the text fields
        String username = Objects.requireNonNull(usernameEditText.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();

        // Get the encrypted password
        try {
            encryptedPassword = encrypt(password);
        } catch (Exception e) {
            Log.e(TAG, "encrypt password:error", e);
        }

        // Perform query to get the user that has matching username and password so that we can log in the user
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", encryptedPassword)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if we have a result from the query
                        if (!task.getResult().isEmpty()) {
                            findViewById(R.id.loginButton).setEnabled(true);

                            // Add user information to local shared preferences
                            SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPrefBack.edit();
                            editor.putString("username", username);
                            editor.putString("password", encryptedPassword);
                            editor.apply();

                            // Ask for permissions here before we move on
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (activityRecognitionPermissionApproved()) {
                                    // Send the user to the maps activity
                                    Intent intentToMap = new Intent(LoginActivity.this, MapsActivity.class);
                                    startActivity((intentToMap));
                                    finish();
                                    findViewById(R.id.loginButton).setEnabled(false);

                                } else {
                                    Intent startIntent = new Intent(LoginActivity.this, PermissionRationalActivity.class);
                                    startActivity(startIntent);
                                }
                            } else {
                                // Send the user to the maps activity
                                Intent intentToMap = new Intent(LoginActivity.this, MapsActivity.class);
                                startActivity((intentToMap));
                                finish();
                                findViewById(R.id.loginButton).setEnabled(false);
                            }
                        } else if (username.isEmpty() && password.isEmpty()) {
                            usernameLayout.setError("Required");
                            passwordLayout.setError("Required");
                        } else if (username.isEmpty()) {
                            usernameLayout.setError("Required");
                        } else if (password.isEmpty()) {
                            passwordLayout.setError("Required");
                        } else {
                            usernameLayout.setError(" ");
                            passwordLayout.setError("Username or password are incorrect");
                        }
                    } else {
                        Log.e(TAG, "Login Error: ", task.getException());
                        Toast.makeText(this, "Error logging in", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * On devices Android 10 and beyond (29+), you need to ask for the ACTIVITY_RECOGNITION via the
     * run-time permissions.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean activityRecognitionPermissionApproved() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Create a clickable textview to bring the user to the register screen
     */
    public void createClickableRegisterText() {

        // Get id of textview and save its default string
        TextView textView = findViewById(R.id.registerTextView);
        String text = "Don't have an account? Register here";

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
                // Go to register screen on click
                Intent intent = new Intent(LoginActivity.this, eulaActivity.class);
                startActivity((intent));
            }
        };

        // Set which part is clickable (just the register here part)
        ss.setSpan(clickableSpan, 23,36, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the textview to the new clickable text
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}