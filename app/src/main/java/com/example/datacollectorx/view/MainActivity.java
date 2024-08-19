package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private Button buttonProfileView;

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
        buttonProfileView = findViewById(R.id.stats);

        // Get the current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Display the UID
            String uid = currentUser.getUid();
            textViewUid.setText("UID: " + uid);
        }

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
                withdrawFromSurvey();
            }
        });

        // Set up the profile view button
        buttonProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewUserProfile();
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

    private void withdrawFromSurvey() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Delete the user document from Firestore
            db.collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "User withdrawn and data deleted", Toast.LENGTH_SHORT).show();
                        // Sign out the user
                        signOutUser();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to withdraw: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void viewUserProfile() {
        Toast.makeText(MainActivity.this, "Viewing user stats", Toast.LENGTH_SHORT).show();
    }
}
