package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class RoomSelectionActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private EditText searchEditText;
    private List<String> roomList;       // Full list of rooms for the building
    private List<String> scannedRooms = new ArrayList<>();  // Scanned rooms for the building

    private FirebaseFirestore db;
    private String buildingCode;

    // Simple callback interface to know when fetching is complete
    public interface OnFetchCompleteListener {
        void onComplete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selection);

        db = FirebaseFirestore.getInstance();

        Button buttonBack = findViewById(R.id.buttonBack);
        // When back is clicked, update the list before finishing.
        buttonBack.setOnClickListener(v -> {
            fetchScannedRoomsForBuilding(buildingCode, new OnFetchCompleteListener() {
                @Override
                public void onComplete() {
                    // Once data is updated, finish the activity.
                    finish();
                }
            });
        });

        recyclerView = findViewById(R.id.recyclerViewRooms);
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get the building code passed from the previous activity
        buildingCode = getIntent().getStringExtra("building_code");

        // Load the full list of rooms from JSON based on the building code
        roomList = getRoomsForBuilding(buildingCode);

        // Initial fetch of scanned rooms from Firestore (without callback)
        fetchScannedRoomsForBuilding(buildingCode, null);

        // Add search functionality to filter the room list
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {  }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (roomAdapter != null) {
                    roomAdapter.getFilter().filter(charSequence);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {  }
        });
    }

    // Sets up the adapter (or creates it) with the provided scanned rooms list.
    private void setupRecyclerView(List<String> scannedRooms) {
        roomAdapter = new RoomAdapter(roomList, scannedRooms, new RoomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String room) {
                if (!scannedRooms.contains(room)) {
                    // Launch RoomRecordActivity in guide mode:
                    Intent intent = new Intent(RoomSelectionActivity.this, RoomRecordActivity.class);
                    intent.putExtra("building_code", buildingCode);
                    intent.putExtra("room", room);
                    intent.putStringArrayListExtra("rooms_list", new ArrayList<>(roomList));
                    intent.putExtra("current_index", roomList.indexOf(room));
                    startActivity(intent);
                } else {
                    Toast.makeText(RoomSelectionActivity.this, "Room already scanned: " + room, Toast.LENGTH_LONG).show();
                }
            }
        });
        recyclerView.setAdapter(roomAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear any active search filter
        searchEditText.setText("");
        // Refresh scanned rooms data
        fetchScannedRoomsForBuilding(buildingCode, null);
    }

    // Loads room list from a JSON file based on the building code.
    private List<String> getRoomsForBuilding(String buildingCode) {
        List<String> rooms = new ArrayList<>();
        try {
            InputStream is = getAssets().open("rooms.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> buildingData = gson.fromJson(json, type);

            if (buildingData.containsKey(buildingCode)) {
                rooms = buildingData.get(buildingCode);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return rooms;
    }

    // Fetches scanned rooms from Firestore for the given building.
    // If a callback is provided, it calls it after updating the adapter.
    private void fetchScannedRoomsForBuilding(String buildingCode, OnFetchCompleteListener listener) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            List<String> updatedScannedRooms = new ArrayList<>();
            if (documentSnapshot.exists()) {
                Map<String, Boolean> scannedRoomsMap = (Map<String, Boolean>) documentSnapshot.get("scannedRooms." + buildingCode);
                if (scannedRoomsMap != null) {
                    updatedScannedRooms = new ArrayList<>(scannedRoomsMap.keySet());
                }
                Log.d("RoomSelectionActivity", "Fetched scanned rooms: " + updatedScannedRooms.toString());
            }
            if (roomAdapter == null) {
                setupRecyclerView(updatedScannedRooms);
            } else {
                roomAdapter.updateScannedRooms(updatedScannedRooms);
            }
            if (listener != null) {
                listener.onComplete();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(RoomSelectionActivity.this, "Error fetching scanned rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onComplete();
            }
        });
    }
}
