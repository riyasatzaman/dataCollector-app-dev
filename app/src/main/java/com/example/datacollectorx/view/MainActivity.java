package com.example.datacollectorx.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewUid;
    private Button buttonSignOut;
    private Button buttonWithdraw;
    private Button buttonShowStats;
    private Button buttonTestSensors;
    private Button buttonBeginScanning;
    private Button buttonCustomLabel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        textViewUid = findViewById(R.id.textViewUid);
        buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonWithdraw = findViewById(R.id.buttonWithdraw);
        buttonShowStats = findViewById(R.id.stats);
        buttonTestSensors = findViewById(R.id.buttonTestSensors);
        buttonBeginScanning = findViewById(R.id.buttonBeginScanning);
        buttonCustomLabel  = findViewById(R.id.customLabel);

        // Get the current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Display the UID
            String uid = currentUser.getUid();
            textViewUid.setText("UID: " + uid);
        }

        // Set click listener for "Test Sensors" button
        buttonTestSensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check location permission before proceeding
                if (checkLocationPermission()) {
                    // If permission is granted, proceed to LiveSensorDataActivity
                    Intent intent = new Intent(MainActivity.this, LiveSensorDataActivity.class);
                    startActivity(intent);
                }
            }
        });

        // Set click listener for "Begin Scanning and Earn Money" button
        buttonBeginScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check location permission before proceeding
                if (checkLocationPermission()) {
                    // If permission is granted, proceed to BuildingSelectionActivity
                    Intent intent = new Intent(MainActivity.this, BuildingSelectionActivity.class);
                    startActivity(intent);
                }
            }
        });

        // Set up the sign-out button
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOutUser();
            }
        });

        // Set up the withdraw button
        buttonWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWithdrawConfirmationDialog();
            }
        });

        buttonCustomLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an AlertDialog to get the custom label input
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter Custom Label");

                // Set up the input field
                final EditText input = new EditText(MainActivity.this);
                input.setHint("Custom label");
                builder.setView(input);

                // Set up the Record button
                builder.setPositiveButton("Record", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String customLabel = input.getText().toString().trim();
                        if (!customLabel.isEmpty()) {
                            // Launch the recording activity and pass the custom label
                            Intent intent = new Intent(MainActivity.this, RoomRecordActivity.class);
                            intent.putExtra("room", customLabel);
                            intent.putExtra("isCustomLabel", true);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Label cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Set up the Cancel button
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // Show the dialog
                builder.show();
            }
        });


        // Set up the profile view button
        buttonShowStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFragment();
            }
        });
    }

    // Method to check and request location permission
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        // Permission is already granted
        return true;
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, proceed with the last clicked action if necessary
                Toast.makeText(MainActivity.this, "Location permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                // Permission was denied, show a toast message
                Toast.makeText(MainActivity.this, "You need to give location permission for the app to work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        // Redirect to AuthActivity
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    private void showWithdrawConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Withdrawal")
                .setMessage("Are you sure you want to withdraw? All your data will be deleted, and your earnings will be set to zero.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User confirmed to withdraw
                    withdrawFromSurvey();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User canceled the dialog
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private void withdrawFromSurvey() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Step 1: Delete the user document from Firestore
            db.collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Step 2: Delete the user from Firebase Authentication
                        currentUser.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(MainActivity.this, "User withdrawn, data deleted, and account removed.", Toast.LENGTH_SHORT).show();
                                    // Sign out the user
                                    signOutUser();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MainActivity.this, "Failed to delete user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to withdraw: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void toggleFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment fragment = fragmentManager.findFragmentById(R.id.main_activity_container);

        if (fragment == null) {
            // Fragment is not currently added, so add it
            fragment = new UserStatsFragment();
            transaction.add(R.id.main_activity_container, fragment, "UserStatsFragment");
            buttonShowStats.setText("Hide Stats");
        } else {
            // Fragment is currently added, so remove it
            transaction.remove(fragment);
            buttonShowStats.setText("Show Stats");
        }

        // Commit the transaction
        transaction.commit();
    }
}