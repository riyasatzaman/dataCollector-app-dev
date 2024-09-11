package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datacollectorx.R;
import com.example.datacollectorx.util.BuildingAdapter;

import java.util.ArrayList;
import java.util.List;

public class BuildingSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Integer> buildingImages;
    private List<String> buildingLabels;  // New list for labels
    private Button buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_selection);



        recyclerView = findViewById(R.id.recyclerViewBuildings);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns

        // Initialize building images (use your drawable resource IDs)
        buildingImages = new ArrayList<>();
        buildingImages.add(R.drawable.athabasca);
        buildingImages.add(R.drawable.assinioba);
        buildingImages.add(R.drawable.cab);
        buildingImages.add(R.drawable.sab);
        buildingImages.add(R.drawable.pembina);
        buildingImages.add(R.drawable.csc);
        buildingImages.add(R.drawable.sub);
        buildingImages.add(R.drawable.ccis);

        // Initialize building labels
        buildingLabels = new ArrayList<>();
        buildingLabels.add("Athabasca Hall (ATH)");
        buildingLabels.add("Assiniobia Hall (ASB)");
        buildingLabels.add("Central Academic Building (CAB)");
        buildingLabels.add("South Academic Building (SAB)");
        buildingLabels.add("Pembina Hall (PEMB)");
        buildingLabels.add("Computing Science Center (CSC)");
        buildingLabels.add("Student Union Building (SUB)");
        buildingLabels.add("CCIS");


        buttonBack = findViewById(R.id.buttonGoBack_building_selection);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity and go back
            }
        });
        // Set up the adapter and pass the image list and label list
        BuildingAdapter adapter = new BuildingAdapter(buildingImages, buildingLabels, new BuildingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Handle grid item click (redirect to another activity, pass building position, etc.)
                Toast.makeText(BuildingSelectionActivity.this, "Building " + (position + 1) + " clicked", Toast.LENGTH_SHORT).show();


            }
        });

        recyclerView.setAdapter(adapter);
    }
}
