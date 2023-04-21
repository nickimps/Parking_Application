package com.parking.linkandpark;

import static android.content.ContentValues.TAG;
import static com.parking.linkandpark.MapsActivity.firestore;
import static com.parking.linkandpark.LoginActivity.mAuth;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText usernameEditText, passwordEditText, nameEditText, permitEditText;
    public TextInputLayout usernameLayout, passwordLayout, nameLayout;
    private FirebaseUser mCurrentUser;
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";
    private static final String Key = "jnansdbhi1j23-0390fhia'p;qaenfpoa828";

    /**
     * onCreate to initialize variables the activity information
     *
     * @param savedInstanceState The instance state to be loaded in
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Default button to false
        findViewById(R.id.registerButton).setEnabled(false);

        // Save the text field and layout ids
        usernameEditText = findViewById(R.id.regUsernameTextInputEditText);
        passwordEditText = findViewById(R.id.regPasswordTextInputEditText);
        nameEditText = findViewById(R.id.regNameTextInputEditText);
        permitEditText = findViewById(R.id.regPermitTextInputEditText);
        usernameLayout = findViewById(R.id.regUsernameTextInputLayout);
        passwordLayout = findViewById(R.id.regPasswordTextInputLayout);
        nameLayout = findViewById(R.id.regNameTextInputLayout);

        // Create the login text to be clickable and have functionality
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
                    if (Objects.requireNonNull(passwordEditText.getText()).toString().isEmpty() || Objects.requireNonNull(nameEditText.getText()).toString().isEmpty())
                        findViewById(R.id.registerButton).setEnabled(false);
                    else
                        findViewById(R.id.registerButton).setEnabled(true);
                }
            }
        });

        // Create listeners to remove error message on text fields
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
                    if (Objects.requireNonNull(usernameEditText.getText()).toString().isEmpty() || Objects.requireNonNull(nameEditText.getText()).toString().isEmpty())
                        findViewById(R.id.registerButton).setEnabled(false);
                    else
                        findViewById(R.id.registerButton).setEnabled(true);
                }
            }
        });

        // Create listeners to remove error message on text fields
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
                    if (Objects.requireNonNull(usernameEditText.getText()).toString().isEmpty() || Objects.requireNonNull(passwordEditText.getText()).toString().isEmpty())
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
            String username = Objects.requireNonNull(usernameEditText.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();
            String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
            String permit = Objects.requireNonNull(permitEditText.getText()).toString().trim();

            // This is for the requirements
            String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{4,}$";
            Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
            Matcher matcher = pattern.matcher(password);

            Boolean passwordRequirements = matcher.matches();

            //Perform query to get the user by username lookup
            firestore.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // If the user exists and the fields were not empty, then add the user
                            if(task.getResult().isEmpty() && username.contains("@") && !password.isEmpty() && !name.isEmpty() && passwordRequirements) {

                                // Encrypts the password before sending to DB and shared preferences
                                try {
                                    String encryptedPassword = encrypt(password);

                                    // Create new user object, with the user's information, then add to database (default admin privileges are false)
                                    User user = new User(username, encryptedPassword, name, permit, false);
                                    firestore.collection("Users").document(username).set(user);

                                    // Add user information to local shared preferences
                                    SharedPreferences sharedPrefBack = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPrefBack.edit();
                                    editor.putString("username", username);
                                    editor.putString("password", encryptedPassword);
                                    editor.apply();

                                } catch (Exception e) {
                                    Log.e(TAG, "Exception: ", e);
                                }

                                // Create an instance of the user authentication object
                                mCurrentUser = mAuth.getCurrentUser();

                                // If auth user not signed in
                                if(mCurrentUser == null){
                                    mAuth.signInAnonymously().addOnCompleteListener(task1 -> {
                                        if(task1.isSuccessful())
                                            System.out.println("Anonymously signed in");
                                        else
                                            System.out.println("Anonymous sign in failed");
                                    });
                                }

                                // Create message to let user know the creation was successful
                                Toast.makeText(getApplicationContext(),"Account successfully created!",Toast.LENGTH_SHORT).show();

                                // Ask for permissions here before we move on
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (activityRecognitionPermissionApproved()) {
                                        // Send the user to the maps activity
                                        Intent intentToMap = new Intent(RegisterActivity.this, MapsActivity.class);
                                        startActivity((intentToMap));
                                        finish();
                                    } else {
                                        Intent startIntent = new Intent(RegisterActivity.this, PermissionRationalActivity.class);
                                        startActivity(startIntent);
                                    }
                                } else {
                                    // Send the user to the maps activity
                                    Intent intentToMap = new Intent(RegisterActivity.this, MapsActivity.class);
                                    startActivity((intentToMap));
                                    finish();
                                }
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
                            } else if(!(username.contains("@"))) {
                                usernameLayout.setError("Must contain @");
                            } else if(!passwordRequirements) {
                                passwordLayout.setError("Must contain upper/lower case letters, numbers, and special characters");
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    });
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
     * Create Encryption function
     */
    public static String encrypt(String value) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(Key.getBytes(), ALGORITHM);             //specifies algorithm using set key
        Cipher cipher = Cipher.getInstance(MODE);                                               //specifies the set mode of blowfish
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));    //sets it to encryption mode using initialization vector IV
        byte[] values = cipher.doFinal(value.getBytes());                                       //value converted into byte array
        return Base64.encodeToString(values, Base64.DEFAULT);                                   //byte array is then encrypted and returned
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
                finish();
            }
        };

        // Set which part is clickable (just the register here part)
        ss.setSpan(clickableSpan, 25,35, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the textview to the new clickable text
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}