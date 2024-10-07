package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;
import com.example.datacollectorx.util.BuildingAdapter;
import com.example.datacollectorx.view.RoomSelectionActivity;
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

    // Callback interface to handle Firestore response
    private interface FirestoreCallback {
        void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Step 1: Load roomDataMap from JSON again
        Map<String, List<String>> roomDataMap = loadRoomDataFromJson();

        // Step 2: Fetch scannedRoomsMap from Firestore again
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchScannedRoomsFromFirestore(userId, new FirestoreCallback() {
            @Override
            public void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap) {
                // Step 3: Reinitialize the adapter to refresh the data
                BuildingAdapter adapter = new BuildingAdapter(
                        buildingImages,
                        buildingLabels,
                        buildingCodes,
                        roomDataMap,
                        scannedRoomsMap,
                        new BuildingAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {
                                // Pass the building code to RoomSelectionActivity
                                Intent intent = new Intent(BuildingSelectionActivity.this, RoomSelectionActivity.class);
                                intent.putExtra("building_code", buildingCodes.get(position));
                                startActivity(intent);
                            }
                        }
                );

                // Step 4: Set the new adapter to the RecyclerView
                recyclerView.setAdapter(adapter);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_selection);

        // Bind UI elements
        buttonBack = findViewById(R.id.buttonGoBack_building_selection);
        buttonBack.setOnClickListener(v -> {
            finish();
        });
        recyclerView = findViewById(R.id.recyclerViewBuildings);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns

        // Initialize building images (use your drawable resource IDs)
        buildingImages = new ArrayList<>();
        buildingImages.add(R.drawable.athabasca);   // ATH
        buildingImages.add(R.drawable.assinioba);   // ASB
        buildingImages.add(R.drawable.cab);         // CAB
        buildingImages.add(R.drawable.sab);         // SAB
        buildingImages.add(R.drawable.pembina);     // PEMB
        buildingImages.add(R.drawable.csc);         // CSC
        buildingImages.add(R.drawable.sub);         // SUB
        buildingImages.add(R.drawable.ccis);        // CCIS

        // Initialize building labels
        buildingLabels = new ArrayList<>();
        buildingLabels.add("Athabasca Hall (ATH)");
        buildingLabels.add("Assiniobia Hall (ASH)");
        buildingLabels.add("Central Academic Building (CAB)");
        buildingLabels.add("South Academic Building (SAB)");
        buildingLabels.add("Pembina Hall (PBH)");
        buildingLabels.add("Computing Science Center (CSC)");
        buildingLabels.add("Student Union Building (SUB)");
        buildingLabels.add("CCIS");

        // Initialize building codes
        buildingCodes = new ArrayList<>();
        buildingCodes.add("ATH");
        buildingCodes.add("ASH");
        buildingCodes.add("CAB");
        buildingCodes.add("SAB");
        buildingCodes.add("PBH");
        buildingCodes.add("CSC");
        buildingCodes.add("SUB");
        buildingCodes.add("CCIS");


        Map<String, List<String>> roomDataMap = loadRoomDataFromJson();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchScannedRoomsFromFirestore(userId, new FirestoreCallback() {
            @Override
            public void onCallback(Map<String, Map<String, Boolean>> scannedRoomsMap) {
                // Initialize the adapter after loading both maps
                BuildingAdapter adapter = new BuildingAdapter(
                        buildingImages,
                        buildingLabels,
                        buildingCodes,
                        roomDataMap,
                        scannedRoomsMap,
                        new BuildingAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {
                                // Pass the building code to RoomSelectionActivity
                                Intent intent = new Intent(BuildingSelectionActivity.this, RoomSelectionActivity.class);
                                intent.putExtra("building_code", buildingCodes.get(position));
                                startActivity(intent);
                            }
                        }
                );

                // Set the adapter to the RecyclerView
                recyclerView.setAdapter(adapter);
            }
        });
    }
}
