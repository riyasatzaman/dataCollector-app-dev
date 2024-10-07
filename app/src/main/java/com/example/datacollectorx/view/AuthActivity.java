package com.example.datacollectorx.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.datacollectorx.R;
import com.example.datacollectorx.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends BaseActivity {

    private AuthViewModel authViewModel;
    private EditText editTextEmail, editTextPassword;
    private Button buttonRegister, buttonLogin;
    private FirebaseFirestore db;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize ViewModel and Firestore
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        db = FirebaseFirestore.getInstance();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Bind UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Set up observers
        authViewModel.getUserLiveData().observe(this, new Observer<FirebaseUser>() {
            @Override
            public void onChanged(FirebaseUser firebaseUser) {
                if (firebaseUser != null) {
                    Toast.makeText(AuthActivity.this, "Welcome, " + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                    checkLocationServices(firebaseUser);
                }
            }
        });

        authViewModel.getAuthErrorLiveData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null) {
                    Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up button click listeners
        buttonRegister.setOnClickListener(view -> signUpUser());
        buttonLogin.setOnClickListener(view -> loginUser());
    }

    private void signUpUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.signUp(email, password);
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.signIn(email, password);
    }

    private void checkLocationServices(FirebaseUser firebaseUser) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isLocationEnabled) {
            showLocationServicesDialog(firebaseUser);
        } else {
            checkTermsAgreement(firebaseUser);
        }
    }

    private void showLocationServicesDialog(FirebaseUser firebaseUser) {
        new AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage("This app requires location services to be enabled. Please enable location services.")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Redirect the user to the location settings
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Show toast and close the app
                        Toast.makeText(AuthActivity.this, "App cannot function without location services.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void checkTermsAgreement(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean hasAgreedToTerms = documentSnapshot.getBoolean("hasAgreedToTerms");
                        if (hasAgreedToTerms == null || !hasAgreedToTerms) {
                            showTermsAndConditionsDialog(firebaseUser);
                        } else {
                            checkDeviceCompatibility();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to retrieve user data
                    Toast.makeText(AuthActivity.this, "Error checking terms agreement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showTermsAndConditionsDialog(FirebaseUser firebaseUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions");

        // Set up the terms text
        final TextView termsTextView = new TextView(this);
        termsTextView.setText("List of terms and conditions...");

        // Set up the checkbox
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText("I agree");

        // Layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(termsTextView);
        layout.addView(checkBox);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Agree", (dialog, which) -> {
            if (checkBox.isChecked()) {
                // Update the Firestore document to indicate agreement
                db.collection("users").document(firebaseUser.getUid())
                        .update("hasAgreedToTerms", true)
                        .addOnSuccessListener(aVoid -> {
                            checkDeviceCompatibility();
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure to update user data
                            Toast.makeText(AuthActivity.this, "Error saving agreement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "You must agree to the terms to use this app.", Toast.LENGTH_SHORT).show();
                showTermsAndConditionsDialog(firebaseUser); // Show dialog again if not agreed
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            finish(); // Close the app if the user does not agree
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void checkDeviceCompatibility() {
        // Check if the device has the required sensors
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (accelerometer == null || magnetometer == null || gyroscope == null) {
            showIncompatibilityDialog();
        } else {
            navigateToMainActivity();
        }
    }

    private void showIncompatibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Device Not Compatible")
                .setMessage("Your device is not compatible with the requirements of the study, so you cannot participate. Thank you for your interest!")
                .setPositiveButton("OK", (dialog, which) -> {
                    finish(); // Close the app if the device is not compatible
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
