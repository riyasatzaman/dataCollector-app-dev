package com.example.datacollectorx.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRecordActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String buildingCode;
    private String room;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_record);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get the room and building code from the intent
        buildingCode = getIntent().getStringExtra("building_code");
        Log.d("RoomRecordActivity", "Building code: " + buildingCode);
        room = getIntent().getStringExtra("room");

        Button buttonRecordRoom = findViewById(R.id.buttonRecordRoom);
        buttonRecordRoom.setOnClickListener(v -> recordScannedRoom(buildingCode, room));
    }

    // Function to record the room as scanned
    private void recordScannedRoom(String buildingCode, String room) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create the path to the user document
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Update the room as scanned (ensure that the map key is a string)
        Map<String, Object> updates = new HashMap<>();
        updates.put("scannedRooms." + buildingCode + "." + room, true);  // Ensure all keys are strings

        // Update the Firestore document with the scanned room
        ((DocumentReference) userDocRef).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RoomRecordActivity.this, "Room recorded successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate back or to another activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RoomRecordActivity.this, "Error recording room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
