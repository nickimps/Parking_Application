package com.parking.linkandpark;

import static android.content.ContentValues.TAG;
import static com.parking.linkandpark.MapsActivity.firestore;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class LoginActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_CODE = 0;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 2;
    private TextInputEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameLayout, passwordLayout;
    //Fetch the stored information when the app is loaded, this function is called when the app is opened again

    //firebase authentication
    public static FirebaseAuth mAuth;
    public static FirebaseUser mCurrentUser;

    //Encryption and Decryption
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";
    private static final String Key = "jnansdbhi1j23-0390fhia'p;qaenfpoa828";
    private static String decryptedPassword = "";
    private static String encryptedPassword = "";

    @Override
    protected void onResume() {
        super.onResume();

        // DONT NEED THIS ANYMORE KEEPING JUST INCASE THOUGH
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            // Get the stored information within the shared preference
//            SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
//
//            // Get stored username and password for the logged in user
//            String username = sharedPref.getString("username", null);
//            String password = sharedPref.getString("password", null);
//
//            //decrypts the password
//            try {
//                //decrypts the password
//                decryptedPassword = decrypt(password);
//            } catch (Exception e) {
//                Log.e(TAG, "decrypt password:error", e);
//            }
//
//            // Bypass login if login details exist
//            if (username != null && password != null) {
//                // Perform query to get the user that has matching username and password so that we can auto login
//                firestore.collection("Users")
//                        .whereEqualTo("username", username)
//                        .whereEqualTo("password", password)
//                        .get()
//                        .addOnCompleteListener(task -> {
//                            if (task.isSuccessful()) {
//                                if (!task.getResult().isEmpty()) {
//                                    usernameEditText.setText(username);
//                                    passwordEditText.setText(decryptedPassword);
//                                }
//                            } else {
//                                Log.d(TAG, "Error getting documents: ", task.getException());
//                            }
//                        });
//            }
//        }
    }

    // Saves the information when the app is closed, this function runs when the app is closed.
    @Override
    protected void onPause() {
        super.onPause();

        // Create the shared preference to store the information
        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        // Create an editor that allows us to impute information into the shared preference
        SharedPreferences.Editor editor = sharedPref.edit();

        // Only save the text fields if there is information in them.
        if (!usernameEditText.getText().toString().isEmpty() && !passwordEditText.getText().toString().isEmpty()) {
            //encrypt password before storing
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

    //Runs when the app is created
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Have user accept location permissions
        getAllPermissions();

        // REGISTER CLICKABLE TEXT
        createClickableRegisterText();

        // Store the ids of the edit text fields
        usernameEditText = findViewById(R.id.usernameTextInputEditText);
        passwordEditText = findViewById(R.id.passwordTextInputEditText);
        usernameLayout = findViewById(R.id.usernameTextInputLayout);
        passwordLayout = findViewById(R.id.passwordTextInputLayout);

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
                    findViewById(R.id.loginButton).setEnabled(!passwordEditText.getText().toString().isEmpty());
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
                    findViewById(R.id.loginButton).setEnabled(false);
                } else {
                    passwordLayout.setError(null);

                    // Disable or enable the login button if user name has no text
                    findViewById(R.id.loginButton).setEnabled(!usernameEditText.getText().toString().isEmpty());
                }
            }
        });

        //Enable enter button to auto login on phone
        passwordEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                login();
                return true;
            }
            return false;
        });

        // LOGIN BUTTON
        Button logButton = findViewById(R.id.loginButton);
        logButton.setOnClickListener(v -> login());

        //create an instance of the user authentication object
        mCurrentUser = mAuth.getCurrentUser();

        //if auth user not signed in
        if(mCurrentUser == null){
            mAuth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful())
                            Log.d(TAG, "Signed in anonymously");
                        else
                            Log.d(TAG, "Anonymous sign in failed");
                    });
        }

    }

    //CODE MODIFIED FROM: https://github.com/Everyday-Programmer/Encryption-Decryption-Android/blob/main/app/src/main/java/com/example/encryptiondecryption/EncryptDecrypt.java
    //Create Encryption function
    public static String encrypt(String value) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(Key.getBytes(), ALGORITHM); //specifies algorithm using set key
        Cipher cipher = Cipher.getInstance(MODE); //specifies the set mode of blowfish
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes())); //sets it to encryption mode using initialization vector IV
        byte[] values = cipher.doFinal(value.getBytes()); //value converted into byte array
        return Base64.encodeToString(values, Base64.DEFAULT); //byte array is then encrypted and returned
    }

    //Create Decryption Function
    public static String decrypt(String value) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] values = Base64.decode(value, Base64.DEFAULT); //creates byte array for the value
        SecretKeySpec secretKeySpec = new SecretKeySpec(Key.getBytes(), ALGORITHM); //specifies algorithm using set key
        Cipher cipher = Cipher.getInstance(MODE); //specifies the set mode of blowfish
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes())); //sets it to decryption mode using initialization vector IV
        return new String(cipher.doFinal(values)); //decrypts the info and returns it
    }


    private void getAllPermissions() {
        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fine location has been accepted by user

            // Need this for certain versions of android apparently
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    askForBackgroundPermission();
        }
        else {
            // Fine Location Permission is not granted so ask for permission
            tellWhyWeNeedIt();
        }
    }

    private void tellWhyWeNeedIt() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Location Permission Needed!")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(LoginActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE))
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        // Permission is denied
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            // User granted location permission
            // Now check if android version >= 11, if >= 11 check for Background Location Permission
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        askForBackgroundPermission(); // Ask for Background Location Permission if background location not allowed yet
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void askForBackgroundPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Background Location Permission Needed!, please tap \"Allow all the time\" in the next screen")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(LoginActivity.this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE))
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        // User declined for Background Location Permission.
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
        }
    }

    public void login() {
        // Get the text from the text fields
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        //get the encryptedPassword
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
                        if (!task.getResult().isEmpty()) {
                            findViewById(R.id.loginButton).setEnabled(true);
                            //create an instance of the user authentication object
                            mCurrentUser = mAuth.getCurrentUser();

                            //if auth user not signed in
                            if(mCurrentUser == null){
                                mAuth.signInAnonymously()
                                        .addOnCompleteListener(task1 -> {
                                            if(task1.isSuccessful())
                                                Log.d(TAG, "Signed in anonymously");
                                            else
                                                Log.d(TAG, "Anonymous sign in failed");
                                        });
                            }

                            // Add user information to local shared preferences
                            SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPrefBack.edit();
                            editor.putString("username", username);
                            editor.putString("password", encryptedPassword);
                            editor.apply();

                            // Send the user to the maps activity
                            Intent intentToMap = new Intent(LoginActivity.this, MapsActivity.class);
                            startActivity((intentToMap));
                            finish();
                            findViewById(R.id.loginButton).setEnabled(false);
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
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
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