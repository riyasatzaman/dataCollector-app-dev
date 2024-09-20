package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;
import com.example.datacollectorx.util.BuildingAdapter;
import com.example.datacollectorx.view.RoomSelectionActivity;

import java.util.ArrayList;
import java.util.List;

public class BuildingSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Integer> buildingImages;
    private List<String> buildingLabels;
    private List<String> buildingCodes;  // List to hold building codes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_selection);

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

        // Set up the adapter and pass the image list, label list, and building codes
        BuildingAdapter adapter = new BuildingAdapter(buildingImages, buildingLabels, new BuildingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Pass the building code to RoomSelectionActivity
                Intent intent = new Intent(BuildingSelectionActivity.this, RoomSelectionActivity.class);
                intent.putExtra("building_code", buildingCodes.get(position));  // Pass the building code (e.g., "ATH")
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
