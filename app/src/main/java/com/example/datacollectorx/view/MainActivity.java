package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewUid;
    private Button buttonSignOut;
    private Button buttonWithdraw;
    private Button buttonShowStats;
    private Button buttonTestSensors;
    private Button buttonBeginScanning;


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

        // Get the current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Display the UID
            String uid = currentUser.getUid();
            textViewUid.setText("UID: " + uid);
        }


        buttonTestSensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to LiveSensorDataActivity
                Intent intent = new Intent(MainActivity.this, LiveSensorDataActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener for "Begin Scanning and Earn Money" button
        buttonBeginScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Begin Scanning and Earn Money
                Toast.makeText(MainActivity.this, "Begin Scanning and Earn Money clicked", Toast.LENGTH_SHORT).show();
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

        // Set up the profile view button
        buttonShowStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               toggleFragment();
            }
        });
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


    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_activity_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void removeFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(fragment);
        transaction.commit();
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
