package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;
import com.example.datacollectorx.util.RoomAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private EditText searchEditText;
    private List<String> roomList;  // List of all rooms in the building
    private List<String> scannedRooms = new ArrayList<>();  // List of scanned rooms for the building

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String buildingCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selection);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            finish();
        });

        recyclerView = findViewById(R.id.recyclerViewRooms);
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        buildingCode = getIntent().getStringExtra("building_code");  // Get the building code

        roomList = getRoomsForBuilding(buildingCode);  // Parse and load room data from JSON

        // Fetch the user's scanned rooms from Firestore for this building
        fetchScannedRoomsForBuilding(buildingCode);

        // Add the search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                roomAdapter.getFilter().filter(charSequence);  // Call the filter method in the adapter
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void setupRecyclerView(List<String> scannedRooms) {
        // Initialize the adapter with the room list and the scanned rooms
        roomAdapter = new RoomAdapter(roomList, scannedRooms, new RoomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String room) {
                if (!scannedRooms.contains(room)) {
                    // Redirect to OCR activity only if the room has not been scanned
                    Intent intent = new Intent(RoomSelectionActivity.this, RoomOCRActivity.class);
                    intent.putExtra("building_code", buildingCode);  // Pass the building code
                    intent.putExtra("room", room);  // Pass the room name
                    startActivity(intent);

                } else {
                    // If the room is already scanned, notify the user
                    Toast.makeText(RoomSelectionActivity.this, "Room already scanned: " + room, Toast.LENGTH_LONG).show();
                }
            }
        });

        // Set up the RecyclerView with the adapter
        recyclerView.setAdapter(roomAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch the scanned rooms again when returning to the activity
        fetchScannedRoomsForBuilding(buildingCode);
    }

    // Load rooms from the JSON file based on the building code
    private List<String> getRoomsForBuilding(String buildingCode) {
        List<String> rooms = new ArrayList<>();
        try {
            // Open the JSON file from assets
            InputStream is = getAssets().open("rooms.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");

            // Parse JSON using Gson
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> buildingData = gson.fromJson(json, type);

            // Check if the building code exists in the JSON and retrieve the corresponding rooms
            if (buildingData.containsKey(buildingCode)) {
                rooms = buildingData.get(buildingCode);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return rooms;  // Return the list of rooms for the selected building
    }

    // Fetch scanned rooms from Firestore for the selected building
    private void fetchScannedRoomsForBuilding(String buildingCode) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Fetch scanned rooms as a Map
                Map<String, Boolean> scannedRoomsMap = (Map<String, Boolean>) documentSnapshot.get("scannedRooms." + buildingCode);

                if (scannedRoomsMap != null) {
                    // Convert map keys (room names) into a List of scanned rooms
                    List<String> scannedRooms = new ArrayList<>(scannedRoomsMap.keySet());

                    // Now, pass this scannedRooms list to your adapter or any other logic
                    setupRecyclerView(scannedRooms);
                } else {
                    // If there are no scanned rooms for this building
                    setupRecyclerView(new ArrayList<>()); // Pass an empty list
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(RoomSelectionActivity.this, "Error fetching scanned rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

}
