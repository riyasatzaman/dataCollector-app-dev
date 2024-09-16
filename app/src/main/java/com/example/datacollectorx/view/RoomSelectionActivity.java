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
    private List<String> roomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selection);
        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            finish();
        });


        recyclerView = findViewById(R.id.recyclerViewRooms);
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String buildingCode = getIntent().getStringExtra("building_code");  // Get the building code

        roomList = getRoomsForBuilding(buildingCode);  // Parse and load room data from JSON

        roomAdapter = new RoomAdapter(roomList, new RoomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String room) {
                // Handle room click, redirect to sensor collection activity
                Toast.makeText(RoomSelectionActivity.this, "Selected room: " + room, Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(roomAdapter);

        // Implement search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                roomAdapter.getFilter().filter(s);  // Filter the room list as the user types
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
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
}
