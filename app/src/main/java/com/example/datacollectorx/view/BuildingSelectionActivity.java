package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;
import com.example.datacollectorx.util.BuildingAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingSelectionActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private List<Integer> buildingImages;
    private List<String> buildingLabels;
    private List<String> buildingCodes;  // List to hold building codes
    private Button buttonBack;

    // Load room data from the JSON file
    private Map<String, List<String>> loadRoomDataFromJson() {
        Map<String, List<String>> roomDataMap = new HashMap<>();
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
            Type type = new TypeToken<Map<String, List<String>>>() {
            }.getType();
            roomDataMap = gson.fromJson(json, type);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return roomDataMap;
    }

    // Fetch user's scanned rooms from Firestore
    private void fetchScannedRoomsFromFirestore(String userId, FirestoreCallback firestoreCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve scannedRoomsMap from Firestore
                        Map<String, Map<String, Boolean>> scannedRoomsMap = (Map<String, Map<String, Boolean>>) documentSnapshot.get("scannedRooms");

                        if (scannedRoomsMap == null) {
                            scannedRoomsMap = new HashMap<>();
                        }

                        firestoreCallback.onCallback(scannedRoomsMap);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    firestoreCallback.onCallback(new HashMap<>());  // Return an empty map on failure
                });
    }

    // Fetch global buildings user count from the admin collection
    private void fetchBuildingsMapFromAdmin(FirestoreCallback firestoreCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admin").document("cQMK4loeC3SVPdaBzKDi")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the buildings map (building code -> count of users)
                        Map<String, Long> buildingsMap = (Map<String, Long>) documentSnapshot.get("buildings");

                        if (buildingsMap == null) {
                            buildingsMap = new HashMap<>();
                        }

                        firestoreCallback.onBuildingsMapCallback(buildingsMap);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    firestoreCallback.onBuildingsMapCallback(new HashMap<>());  // Return an empty map on failure
                });
    }

    // Callback interface to handle Firestore response
    private interface FirestoreCallback {
        void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap);

        void onBuildingsMapCallback(Map<String, Long> buildingsMap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Step 1: Load roomDataMap from JSON
        Map<String, List<String>> roomDataMap = loadRoomDataFromJson();

        // Step 2: Fetch scannedRoomsMap from Firestore
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchScannedRoomsFromFirestore(userId, new FirestoreCallback() {
            @Override
            public void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap) {
                // Step 3: Fetch the buildingsMap from the admin collection (user count per building)
                fetchBuildingsMapFromAdmin(new FirestoreCallback() {
                    @Override
                    public void onBuildingsMapCallback(Map<String, Long> buildingsMap) {
                        // Step 4: Initialize the adapter with the scannedRoomsMap and buildingsMap
                        BuildingAdapter adapter = new BuildingAdapter(
                                buildingImages,
                                buildingLabels,
                                buildingCodes,
                                roomDataMap,
                                scannedRoomsMap,
                                buildingsMap,  // Pass the buildings map (global user count per building)
                                new BuildingAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(int position) {
                                        String buildingCode = buildingCodes.get(position);

                                        // Check if the building reached the limit of 10 users
                                        if (buildingsMap.containsKey(buildingCode)) {
                                            Object value = buildingsMap.get(buildingCode);

                                            // Safely cast to Long
                                            long buildingCount;
                                            if (value instanceof Long) {
                                                buildingCount = (Long) value;
                                            } else if (value instanceof String) {
                                                try {
                                                    buildingCount = Long.parseLong((String) value);  // Try to convert String to Long
                                                } catch (NumberFormatException e) {
                                                    buildingCount = 0;  // Default to 0 in case of an error
                                                }
                                            } else {
                                                buildingCount = 0;  // Default if it's neither Long nor String
                                            }

                                            if (buildingCount >= 10) {
                                                // Show modal or toast message
                                                showBuildingLimitReached(buildingLabels.get(position));
                                            } else {
                                                // Pass the building code to FloorSelectionActivity
                                                Intent intent = new Intent(BuildingSelectionActivity.this, FloorSelectionActivity.class);
                                                intent.putExtra("building_code", buildingCodes.get(position));
                                                startActivity(intent);
                                            }
                                        }
                                    }
                                }
                        );

                        // Step 5: Set the adapter to the RecyclerView
                        recyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap) {
                        // This method isn't used here, but is required for the interface
                    }
                });
            }

            @Override
            public void onBuildingsMapCallback(Map<String, Long> buildingsMap) {
                // Not used here
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_selection);

        // Bind UI elements
        buttonBack = findViewById(R.id.buttonGoBack_building_selection);
        buttonBack.setOnClickListener(v -> finish());
        recyclerView = findViewById(R.id.recyclerViewBuildings);

        // Set a GridLayoutManager with 2 columns.
        // Using a SpanSizeLookup so that if there's only one item it spans both columns (centering it).
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // With one item, force it to span both columns.
                return 2;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);

        // Initialize building arrays
        buildingImages = new ArrayList<>();
        buildingImages.add(R.drawable.cab); // CAB image

        buildingLabels = new ArrayList<>();
        buildingLabels.add("Central Academic Building (CAB)");

        buildingCodes = new ArrayList<>();
        buildingCodes.add("CAB");
    }


    // Show a modal or toast when building limit is reached
    private void showBuildingLimitReached(String buildingLabel) {

    new AlertDialog.Builder(this)
            .setTitle("Building Limit Reached")
            .setMessage("The building " + buildingLabel + " has reached the maximum number of users, please select another building, or check back soon!")
            .setPositiveButton("OK", (dialog, which) -> {
                // Do nothing
            })
            .show();
    }
}
